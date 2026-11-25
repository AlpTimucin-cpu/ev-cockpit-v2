package app.evcockpit.ui.unified

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.OsrmRouteProvider
import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.domain.nav.WgsPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * C3 durum katmanı: ağ/OBD/GPS çağrısı yapmaz. Gerçek repo bağları C4–C7'de.
 * SavedStateHandle'da yalnız küçük koordinat ve ekran tercihleri saklanır.
 * Uygulama öldükten sonra rota geometrisi saklanmaz; yenisi hesaplanmalıdır.
 */
@HiltViewModel
class UnifiedViewModel @Inject constructor(
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(restore())
    val state: StateFlow<UnifiedUiState> = mutable

    private var routeJob: Job? = null
    private var requestGeneration = 0L

    /** Explicit request only; changing either endpoint invalidates an in-flight response. */
    fun calculateRoutes(failure: UserMessage, invalid: UserMessage) {
        val before = mutable.value
        val origin = before.origin
        val destination = before.destination
        if (origin == null || destination == null || !origin.valid || !destination.valid ||
            (origin.latitude == destination.latitude && origin.longitude == destination.longitude)) {
            dispatch(UnifiedEvent.AddBanner(Banner(invalid, BannerKind.WARNING)))
            return
        }
        if (routeJob?.isActive == true) return
        val generation = ++requestGeneration
        dispatch(UnifiedEvent.LoadRoutes)
        routeJob = viewModelScope.launch {
            try {
                val routes = OsrmRouteProvider().calculateAlternatives(
                    GeoPoint(origin.latitude, origin.longitude),
                    GeoPoint(destination.latitude, destination.longitude), "",
                ).mapIndexed { index, route ->
                    RouteModel(
                        id = "osrm-$generation-$index",
                        geometry = route.geometry.map { WgsPoint(it.latitude, it.longitude) },
                        distanceMeters = route.distanceMeters,
                        durationSeconds = route.durationSeconds,
                        source = route.provider,
                        maneuvers = route.maneuvers.mapNotNull { step ->
                            val point = route.geometry.getOrNull(step.geometryIndex) ?: return@mapNotNull null
                            app.evcockpit.domain.nav.RouteManeuver(
                                WgsPoint(point.latitude, point.longitude), "OSRM", null, null, 0.0,
                                step.instructionTr, step.geometryIndex, step.type,
                            )
                        },
                    )
                }
                if (generation == requestGeneration)
                    dispatch(UnifiedEvent.RoutesReady(routes))
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                if (generation == requestGeneration)
                    dispatch(UnifiedEvent.RoutesFailed(failure))
            }
        }
    }

    fun dispatch(event: UnifiedEvent) {
        if (event is UnifiedEvent.ChooseOrigin || event is UnifiedEvent.ChooseDestination) {
            requestGeneration++
            routeJob?.cancel()
        }
        val after = SheetReducer.reduce(mutable.value, event)
        mutable.value = after
        persist(after)
    }

    private fun restore(): UnifiedUiState {
        fun read(prefix: String): Place? {
            val lat = saved.get<Double>("${prefix}_lat") ?: return null
            val lon = saved.get<Double>("${prefix}_lon") ?: return null
            val name = saved.get<String>("${prefix}_name") ?: return null
            return Place(prefix, name, lat, lon,
                saved.get<String>("${prefix}_source") ?: "KAYIT")
                .takeIf { it.valid }
        }
        val origin = read("origin")
        val destination = read("destination")
        val mode = saved.get<String>("sheet_mode") ?: "Idle"
        val detent = runCatching {
            SheetDetent.valueOf(saved.get<String>("sheet_detent") ?: "PEEK")
        }.getOrDefault(SheetDetent.PEEK)
        // Rota yalnız yeni ağ sorgusuyla üretilebilir. Kayıtlı seçimi gösteremeyiz.
        val staleRoute = mode in setOf("RoutePreview", "Navigating")
        return UnifiedUiState(
            origin = origin,
            destination = destination,
            mode = SheetMode.Idle,
            detent = if (staleRoute) SheetDetent.HALF else detent,
            routes = if (staleRoute) Loadable.Failed(UserMessage(
                "Önceki rota yeniden hesaplanmalı.", "Yeniden dene"))
                else Loadable.Idle,
            banners = if (staleRoute) listOf(Banner(
                UserMessage("Kayıtlı rota yeniden doğrulanmalı.", "Rota hesapla"),
                BannerKind.INFO))
                else emptyList(),
        )
    }

    private fun persist(s: UnifiedUiState) {
        fun write(prefix: String, place: Place?) {
            saved["${prefix}_lat"] = place?.latitude
            saved["${prefix}_lon"] = place?.longitude
            saved["${prefix}_name"] = place?.name
            saved["${prefix}_source"] = place?.source
        }
        write("origin", s.origin)
        write("destination", s.destination)
        saved["sheet_detent"] = s.detent.name
        saved["sheet_mode"] = when (s.mode) {
            is SheetMode.Idle -> "Idle"
            is SheetMode.RoutePreview -> "RoutePreview"
            is SheetMode.Navigating -> "Navigating"
            is SheetMode.StopDetail -> "StopDetail"
            is SheetMode.Energy -> "Energy"
        }
        saved["selected_index"] = s.selectedRouteIndex
    }
}
