package app.evcockpit.ui.unified

import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.domain.nav.WgsPoint
import app.evcockpit.ui.unified.map.OverlayPoint
import org.junit.Assert.*
import org.junit.Test

class C4RouteOverlaysTest {
    private val start = Place("a", "TEST A", 37.97, 34.68, "TEST")
    private val end = Place("b", "TEST B", 36.89, 30.70, "TEST")
    private val first = RouteModel("a", listOf(WgsPoint(37.97, 34.68),
        WgsPoint(37.5, 33.0), WgsPoint(36.89, 30.70)), 400000.0, 15000.0, source = "TEST")
    private val second = first.copy(id = "b", geometry = listOf(
        WgsPoint(37.97, 34.68), WgsPoint(37.3, 32.0), WgsPoint(36.89, 30.70)))

    @Test fun endpointsAloneDoNotCreateAFakeRoute() {
        val result = C4RouteOverlays.from(UnifiedUiState(origin = start, destination = end))
        assertEquals(OverlayPoint(start.latitude, start.longitude), result.origin)
        assertNotNull(result.destination)
        assertTrue(result.selectedRoute.isEmpty())
        assertTrue(result.alternativeRoutes.isEmpty())
    }

    @Test fun alternativeSelectionPreservesProviderGeometry() {
        val state = UnifiedUiState(routes = Loadable.Ready(listOf(first, second)),
            mode = SheetMode.RoutePreview(0))
        val selected = SheetReducer.reduce(state, UnifiedEvent.ChooseAlternative(1))
        val result = C4RouteOverlays.from(selected)
        assertEquals(second.geometry.map { OverlayPoint(it.lat, it.lon) }, result.selectedRoute)
        assertEquals(listOf(first.geometry.map { OverlayPoint(it.lat, it.lon) }), result.alternativeRoutes)
    }

    @Test fun failedRouteClearsAllLines() {
        val state = UnifiedUiState(routes = Loadable.Ready(listOf(first, second)),
            mode = SheetMode.RoutePreview(0))
        val result = C4RouteOverlays.from(SheetReducer.reduce(state,
            UnifiedEvent.RoutesFailed(UserMessage("TEST"))))
        assertTrue(result.selectedRoute.isEmpty())
        assertTrue(result.alternativeRoutes.isEmpty())
    }
}
