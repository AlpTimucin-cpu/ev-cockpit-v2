package app.evcockpit.ui.unified

import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.domain.nav.WgsPoint
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.GeoPoint
import org.junit.Assert.*
import org.junit.Test

class C6NavigationPolicyTest {
    private val route = RouteModel("r", listOf(WgsPoint(38.0, 34.0),
        WgsPoint(38.01, 34.0)), 1113.2, 120.0,
        maneuvers = listOf(app.evcockpit.domain.nav.RouteManeuver(
            WgsPoint(38.01, 34.0), "OSRM", null, null, 0.0,
            "Hedefe ulaştınız", 1, 123)), source = "TEST")
    private val fix = GpsSnapshot(point = GeoPoint(38.005, 34.0),
        accuracyMeters = 5f, provider = "gps", receivedElapsedRealtimeMs = 1000L,
        active = true, speedKmh = 40.0, courseDegrees = 0.0)

    @Test fun realFixProvidesVehicleAndRemainingRoute() {
        val p = C6NavigationPolicy.progress(C5Projection.navigation(route), fix, 1500L, 10000L)
        assertNotNull(p.vehicle)
        assertEquals(9500L, p.vehicle!!.timestampEpochMs)
        assertEquals(556.6, p.remainingMeters!!, 1.0)
        assertEquals(60.0, p.remainingSeconds!!, 1.0)
        assertEquals("Hedefe ulaştınız", p.instruction)
        assertFalse(p.offRoute)
    }

    @Test fun staleCoarseOrUnknownAccuracyNeverCreatesCarMarker() {
        assertNull(C6NavigationPolicy.vehicle(fix, 100000L, 100000L))
        assertNull(C6NavigationPolicy.vehicle(fix.copy(provider = "network"), 1500L, 10000L))
        assertNull(C6NavigationPolicy.vehicle(fix.copy(accuracyMeters = null), 1500L, 10000L))
        assertNull(C6NavigationPolicy.vehicle(fix.copy(accuracyMeters = 100f), 1500L, 10000L))
    }

    @Test fun offRouteDoesNotInventDistanceOrSpeech() {
        val p = C6NavigationPolicy.progress(C5Projection.navigation(route),
            fix.copy(point = GeoPoint(38.005, 34.1)), 1500L, 10000L)
        assertTrue(p.offRoute)
        assertNull(p.remainingMeters)
        assertNull(p.instruction)
    }

    @Test fun arrivalRequiresActualProximityToDestination() {
        val p = C6NavigationPolicy.progress(C5Projection.navigation(route),
            fix.copy(point = GeoPoint(38.01, 34.0)), 1500L, 10000L)
        assertTrue(p.arrived)
    }
}
