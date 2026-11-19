package app.evcockpit.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArrivalEnergyEstimatorTest {
    @Test fun estimatesArrivalUsingUserAverage() {
        val value = ArrivalEnergyEstimator.estimate(52.0, 80.0, 100.0, 18.0)
        assertEquals(23.6, value!!, 0.001)
    }

    @Test fun missingSocOrConsumptionSuppressesOutput() {
        assertNull(ArrivalEnergyEstimator.estimate(52.0, null, 100.0, 18.0))
        assertNull(ArrivalEnergyEstimator.estimate(52.0, 80.0, 100.0, null))
    }

    @Test fun refusesInvalidValues() {
        assertNull(ArrivalEnergyEstimator.estimate(52.0, 150.0, 100.0, 18.0))
        assertNull(ArrivalEnergyEstimator.estimate(52.0, 80.0, -20.0, 18.0))
    }

    @Test fun routeProgressFromStartAndNearDestination() {
        val route = NavigationRoute(
            geometry = listOf(GeoPoint(37.0, 34.0), GeoPoint(37.01, 34.0)),
            distanceMeters = 1100.0,
            durationSeconds = 90.0,
            maneuvers = listOf(RouteManeuver("Sağa dön", 1, 1)),
        )
        val start = RouteProgress.locate(route, GeoPoint(37.0, 34.0))!!
        assertEquals(1100.0, start.remainingMeters, 0.001)
        val end = RouteProgress.locate(route, GeoPoint(37.01, 34.0))!!
        assertEquals(0.0, end.remainingMeters, 0.001)
        assertNull(RouteProgress.locate(route, GeoPoint(38.0, 34.0)))
    }
}
