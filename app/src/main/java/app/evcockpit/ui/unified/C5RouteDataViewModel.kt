package app.evcockpit.ui.unified

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.charging.ocm.*
import app.evcockpit.charging.tariffs.*
import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.weather.*
import app.evcockpit.ui.unified.map.*
import app.evcockpit.planner.model.PlanResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class C5Forecast(val location: GeoPoint, val value: RouteHourlyWeather)

data class C5Data(
    val routeId: String? = null,
    val stations: List<OcmStation> = emptyList(),
    val forecasts: List<C5Forecast> = emptyList(),
    val stationMessage: String? = null,
    val weatherFailed: Boolean = false,
    val missingKey: Boolean = false,
    val stationBusy: Boolean = false,
    val weatherBusy: Boolean = false,
    val complete: Boolean = false,
    val catalog: TariffCatalog = TariffCatalog(emptyList()),
    val manual: Map<String, ManualPrice> = emptyMap(),
    val tariffOrigin: String? = null,
)

object C5Projection {
    const val RAIN_WARNING_PERCENT = 60

    fun navigation(route: RouteModel): NavigationRoute = NavigationRoute(
        geometry = route.geometry.map { GeoPoint(it.lat, it.lon) },
        distanceMeters = route.distanceMeters,
        durationSeconds = route.durationSeconds,
        maneuvers = route.maneuvers.mapNotNull { step ->
            val index = step.geometryIndex ?: return@mapNotNull null
            val text = step.instructionTr?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val type = step.providerType ?: return@mapNotNull null
            if (index !in route.geometry.indices) return@mapNotNull null
            app.evcockpit.navigation.RouteManeuver(text, index, type)
        },
        provider = route.source,
    )

    // The broad OCM parser also recognizes generic CCS. Do not call it confirmed CCS2.
    fun explicitCcs2(station: OcmStation): Boolean = station.hasDc &&
        station.connectors.any { label ->
            val name = label.lowercase(java.util.Locale.ROOT)
            name.contains("ccs2") || name.contains("ccs 2") ||
                name.contains("combo 2") || name.contains("type 2 (combo") ||
                name.contains("ccs (type 2") || name.contains("ccs type 2")
        }

    fun overlay(base: OverlayModel, data: C5Data, routeId: String?): OverlayModel {
        if (routeId == null || data.routeId != routeId) return base
        return base.copy(
            stations = data.stations.filter(::explicitCcs2).map {
                OverlayStation(it.id.toString(), OverlayPoint(it.point.latitude, it.point.longitude),
                    it.powerKw, dc = true)
            },
            weatherAlerts = data.forecasts.mapNotNull {
                val probability = it.value.rainProbabilityPct ?: return@mapNotNull null
                OverlayWeatherAlert(OverlayPoint(it.location.latitude, it.location.longitude),
                    probability >= RAIN_WARNING_PERCENT)
            },
        )
    }

    fun plannedCost(plan: PlanResult.Success, capacityKwh: Double, data: C5Data): Double? {
        if (!capacityKwh.isFinite() || capacityKwh <= 0.0) return null
        var total = 0.0
        for (stop in plan.stops) {
            val tariff = TariffResolver.resolve(data.catalog, data.manual, stop.charger.operator, ChargingKind.DC)
            val price = tariff.priceTlPerKwh ?: return null
            if (tariff.stale || !price.isFinite() || price <= 0.0) return null
            val delta = stop.departureSocPct - stop.arrivalSocPct
            if (delta !in 0..100) return null
            total += delta / 100.0 * capacityKwh * price
        }
        return total.takeIf { it.isFinite() }
    }
}

class C5RouteDataViewModel(application: Application) : AndroidViewModel(application) {
    private val ocm = OcmRepository()
    private val keys = OcmKeyStore(application)
    private val tariffs = TariffRepository(application)
    private val weather = OpenMeteoHourlyRouteClient()
    private val mutable = MutableStateFlow(C5Data())
    val state = mutable.asStateFlow()
    private var stationJob: Job? = null
    private var weatherJob: Job? = null
    private var generation = 0L

    fun selectRoute(id: String?) {
        if (mutable.value.routeId == id) return
        generation++
        stationJob?.cancel()
        weatherJob?.cancel()
        mutable.value = C5Data(routeId = id)
    }

    fun refresh(route: RouteModel) {
        selectRoute(route.id)
        if (mutable.value.stationBusy || mutable.value.weatherBusy) return
        val token = ++generation
        val nav = C5Projection.navigation(route)
        mutable.value = C5Data(routeId = route.id, stationBusy = true, weatherBusy = true)
        stationJob = viewModelScope.launch {
            try {
                // Tariff failure must not prevent a station inventory request.
                val snapshot = try { tariffs.load() } catch (c: CancellationException) { throw c }
                    catch (_: Exception) { null }
                val manual = try { tariffs.manualOverrides() } catch (c: CancellationException) { throw c }
                    catch (_: Exception) { emptyMap() }
                if (token != generation) return@launch
                mutable.value = mutable.value.copy(
                    catalog = snapshot?.catalog ?: TariffCatalog(emptyList()),
                    tariffOrigin = snapshot?.origin, manual = manual)
                val key = keys.read()
                if (key.isNullOrBlank()) {
                    if (token == generation) mutable.value = mutable.value.copy(
                        missingKey = true, stationBusy = false)
                    return@launch
                }
                val result = ocm.search(nav.geometry, key)
                if (token == generation) mutable.value = mutable.value.copy(
                    stations = result.stations.filter(C5Projection::explicitCcs2),
                    stationMessage = result.message, complete = result.complete, stationBusy = false)
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                if (token == generation) mutable.value = mutable.value.copy(stationBusy = false, complete = false)
            }
        }
        weatherJob = viewModelScope.launch {
            try {
                val departure = System.currentTimeMillis() / 1000L
                val samples = RouteHourlyForecastPolicy.sample(nav, departure)
                val forecasts = weather.forecast(nav, departure).mapNotNull { value ->
                    samples.firstOrNull { it.label == value.label && it.expectedEpochSec == value.expectedEpochSec }
                        ?.let { C5Forecast(it.location, value) }
                }
                if (token == generation) mutable.value = mutable.value.copy(
                    forecasts = forecasts, weatherBusy = false, weatherFailed = forecasts.isEmpty())
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                if (token == generation) mutable.value = mutable.value.copy(weatherBusy = false, weatherFailed = true)
            }
        }
    }
}
