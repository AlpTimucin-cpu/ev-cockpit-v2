package app.evcockpit.ui.unified

import app.evcockpit.charging.ocm.OcmStation
import app.evcockpit.charging.tariffs.*
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.ui.unified.map.OverlayModel
import app.evcockpit.weather.RouteHourlyWeather
import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.charging.ChargerConfidence
import app.evcockpit.planner.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class C5ProjectionTest {
    private val station = OcmStation(1L, "TEST", "TestNetwork", GeoPoint(38.0, 34.0),
        100.0, false, true, true, listOf("CCS (Type 2)"), "Public", null)
    private val forecast = C5Forecast(GeoPoint(38.0, 34.0),
        RouteHourlyWeather("TEST", 1000L, 10.0, null, null, 75))
    private val charger = ChargerCandidate("a", "TEST", "TestNetwork", 38.0, 34.0,
        app.evcockpit.planner.model.SourcedValue(100.0, "TEST", false, "TEST"),
        ChargerConfidence.OSM_TAGGED_CCS2)
    private val plan = PlanResult.Success(
        stops = listOf(PlannedStop(charger, 20, 60, 30.0, 0.0, 0.0)),
        arrivalSocPct = 20, optimisticArrivalSocPct = 25.0, pessimisticArrivalSocPct = 15.0,
        distanceKm = 200.0, drivingMinutes = 120.0, chargingMinutes = 30.0,
        stopAndDetourMinutes = 0.0, riskPenaltyMinutes = 0.0, physicalMinutes = 150.0,
        riskAdjustedMinutes = 150.0, warnings = emptyList())

    @Test fun genericCcsDoesNotProveCcs2() {
        assertTrue(C5Projection.explicitCcs2(station))
        assertFalse(C5Projection.explicitCcs2(station.copy(connectors = listOf("CCS Type 1"))))
        assertFalse(C5Projection.explicitCcs2(station.copy(connectors = listOf("CCS"))))
    }

    @Test fun oldRouteCannotDrawStationOrWeather() {
        val data = C5Data(routeId = "old", stations = listOf(station), forecasts = listOf(forecast))
        assertEquals(OverlayModel(), C5Projection.overlay(OverlayModel(), data, "new"))
        val current = C5Projection.overlay(OverlayModel(), data, "old")
        assertEquals(1, current.stations.size)
        assertEquals(true, current.weatherAlerts.single().rainExpected)
    }

    @Test fun missingRainProbabilityIsNotAWarning() {
        val noRain = forecast.copy(value = forecast.value.copy(rainProbabilityPct = null))
        val overlay = C5Projection.overlay(OverlayModel(), C5Data("a", forecasts = listOf(noRain)), "a")
        assertTrue(overlay.weatherAlerts.isEmpty())
    }

    @Test fun missingOrStaleTariffNeverMeansFreeCharging() {
        assertNull(C5Projection.plannedCost(plan, 50.0, C5Data()))
        val old = OperatorTariff("test", "TestNetwork", emptyList(), null, 9.0,
            LocalDate.now().minusDays(30), "https://example.invalid")
        val data = C5Data(catalog = TariffCatalog(listOf(old)))
        assertNull(C5Projection.plannedCost(plan, 50.0, data))
        val current = data.copy(catalog = TariffCatalog(listOf(old.copy(updatedAt = LocalDate.now()))))
        assertEquals(180.0, C5Projection.plannedCost(plan, 50.0, current)!!, 0.001)
        assertEquals(0.0, C5Projection.plannedCost(plan.copy(stops = emptyList()), 50.0, C5Data())!!, 0.001)
    }
}
