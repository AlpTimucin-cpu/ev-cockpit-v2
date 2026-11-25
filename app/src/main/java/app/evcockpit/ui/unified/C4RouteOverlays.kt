package app.evcockpit.ui.unified

import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.ui.unified.map.OverlayModel
import app.evcockpit.ui.unified.map.OverlayPoint

/** Pure projection: no invented geometry and no line from endpoints alone. */
object C4RouteOverlays {
    fun from(state: UnifiedUiState): OverlayModel {
        val routes = (state.routes as? Loadable.Ready)?.value.orEmpty()
        fun points(route: RouteModel) = route.geometry.map { OverlayPoint(it.lat, it.lon) }
        return OverlayModel(
            selectedRoute = routes.getOrNull(state.selectedRouteIndex)?.let(::points).orEmpty(),
            alternativeRoutes = routes.filterIndexed { i, _ -> i != state.selectedRouteIndex }.map(::points),
            origin = state.origin?.takeIf { it.valid }?.let { OverlayPoint(it.latitude, it.longitude) },
            destination = state.destination?.takeIf { it.valid }?.let { OverlayPoint(it.latitude, it.longitude) },
        )
    }
}
