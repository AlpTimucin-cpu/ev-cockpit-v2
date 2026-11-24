package app.evcockpit.ui.unified

import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.ui.unified.map.FollowMode

sealed interface SheetMode {
    data object Idle : SheetMode
    data class RoutePreview(val selected: Int) : SheetMode
    data object Navigating : SheetMode
    data class StopDetail(val stationId: String) : SheetMode
    data object Energy : SheetMode
}

enum class SheetDetent { PEEK, HALF, FULL }
enum class SourcedOrigin { GPS, OBD, MANUAL, MODEL, YOK }

data class SourcedValue<T>(
    val value: T? = null,
    val source: SourcedOrigin = SourcedOrigin.YOK,
    val ageMs: Long? = null,
)

data class Place(
    val id: String, val name: String,
    val latitude: Double, val longitude: Double,
    val source: String,
) {
    val valid: Boolean get() = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}

data class StationOnRoute(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val powerKw: Double?,
    val hasCcs2: Boolean,
    val alongRouteMeters: Double?,
    val detourMeters: Double?,
)

data class WeatherPoint(
    val distanceAlongMeters: Double,
    val temperatureC: Double?,
    val rainWarning: Boolean?,
    val forecastTimeEpochMs: Long?,
)

data class VehicleFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double?,
    val speedMetersPerSecond: Double?,
    val bearingDegrees: Double?,
    val timestampEpochMs: Long,
)

data class UserMessage(val textTr: String, val actionTr: String? = null)
data class Banner(val message: UserMessage, val kind: BannerKind)
enum class BannerKind { INFO, WARNING, ERROR }

sealed interface Loadable<out T> {
    data object Idle : Loadable<Nothing>
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Failed(val reason: UserMessage) : Loadable<Nothing>
}

object UnifiedDefaults {
    const val MIN_ARRIVAL_SOC = 15.0
}

data class UnifiedUiState(
    val mode: SheetMode = SheetMode.Idle,
    val detent: SheetDetent = SheetDetent.PEEK,
    val origin: Place? = null,
    val destination: Place? = null,
    val routes: Loadable<List<RouteModel>> = Loadable.Idle,
    val stations: Loadable<List<StationOnRoute>> = Loadable.Idle,
    val weather: Loadable<List<WeatherPoint>> = Loadable.Idle,
    val vehicle: VehicleFix? = null,
    val followMode: FollowMode = FollowMode.OFF,
    val soc: SourcedValue<Double> = SourcedValue(),
    val minArrivalSoc: Double = UnifiedDefaults.MIN_ARRIVAL_SOC,
    val banners: List<Banner> = emptyList(),
    val exitRequested: Boolean = false,
) {
    val currentRoute: RouteModel?
        get() = (routes as? Loadable.Ready)?.value
            ?.getOrNull((mode as? SheetMode.RoutePreview)?.selected ?: 0)
}
