package app.evcockpit.ui.tesla

import app.evcockpit.navigation.GeoPoint
import org.junit.Assert.*
import org.junit.Test

class RoutePolylineProgressTest {
    private val route=listOf(GeoPoint(0.0,0.0),GeoPoint(0.0,1.0),GeoPoint(0.0,2.0))
    @Test fun missingProgressDisplaysEntireRouteAsRemaining() {
        val result=RoutePolylineProgress.split(route, Double.NaN)
        assertTrue(result.passed.isEmpty())
        assertEquals(route,result.remaining)
    }
    @Test fun halfwayMarksPastAsMutedAndUpcomingAsActive() {
        val result=RoutePolylineProgress.split(route,0.5)
        assertTrue(result.passed.size >= 2)
        assertTrue(result.remaining.size >= 2)
        assertEquals(result.passed.last(),result.remaining.first())
        assertEquals(1.0,result.remaining.first().longitude,0.001)
    }
    @Test fun completedRouteHasNoUpcomingSegment() {
        val result=RoutePolylineProgress.split(route,1.0)
        assertEquals(route,result.passed)
        assertTrue(result.remaining.isEmpty())
    }
}
