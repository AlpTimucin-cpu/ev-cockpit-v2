package app.evcockpit.ui.unified

import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.ui.unified.map.FollowMode

sealed interface UnifiedEvent {
    data class ChooseOrigin(val place: Place?) : UnifiedEvent
    data class ChooseDestination(val place: Place?) : UnifiedEvent
    data object LoadRoutes : UnifiedEvent
    data class RoutesReady(val values: List<RouteModel>) : UnifiedEvent
    data class RoutesFailed(val reason: UserMessage) : UnifiedEvent
    data class ChooseAlternative(val index: Int) : UnifiedEvent
    data object StartNavigation : UnifiedEvent
    data object StopNavigation : UnifiedEvent
    data class ShowStation(val id: String) : UnifiedEvent
    data object ShowEnergy : UnifiedEvent
    data class SetDetent(val value: SheetDetent) : UnifiedEvent
    data class SetFollow(val value: FollowMode) : UnifiedEvent
    data class UpdateSoc(val sourced: SourcedValue<Double>) : UnifiedEvent
    data class UpdateVehicle(val fix: VehicleFix?) : UnifiedEvent
    data class UpdateStations(val result: Loadable<List<StationOnRoute>>) : UnifiedEvent
    data class UpdateWeather(val result: Loadable<List<WeatherPoint>>) : UnifiedEvent
    data class AddBanner(val value: Banner) : UnifiedEvent
    data object Back : UnifiedEvent
    data object ExitConsumed : UnifiedEvent
    data object ResetToIdle : UnifiedEvent
}
