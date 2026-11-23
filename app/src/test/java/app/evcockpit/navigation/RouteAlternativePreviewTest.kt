package app.evcockpit.navigation

import app.evcockpit.planner.model.VehicleProfile
import org.junit.Assert.*
import org.junit.Test

class RouteAlternativePreviewTest {
    private val car = VehicleProfile.defaults()
    private fun route(km: Double, hours: Double): NavigationRoute =
        NavigationRoute(
            geometry = listOf(GeoPoint(37.9, 34.7), GeoPoint(36.9, 30.7)),
            distanceMeters = km * 1000.0, durationSeconds = hours * 3600.0,
            maneuvers = emptyList(), provider = "TEST",
        )

    @Test fun enerjiKisaRotadaPozitifVeObdOlmadanHesaplanir() {
        val result = RouteAlternativePreview.calculate(route(20.0, 0.4), car, null)
        assertNotNull(result)
        assertTrue(result!!.routeEnergyKwh > 0.0)
        assertNull(result.arrivalSocPct)
        assertNull(result.chargingCostTl)
    }

    @Test fun uzunRotadaEksikEnerjiSifirGibiYazilmaz() {
        val result = RouteAlternativePreview.calculate(route(600.0, 7.0), car, 50.0, 15.0)!!
        assertTrue(result.requiresCharging == true)
        assertNull(result.arrivalSocPct)
        assertTrue(result.energyDeficitKwh!! > 0)
        assertTrue(result.extraKwhForMinimum!! > 0)
        assertNull(result.chargingMinutes)
        assertNull(result.chargingCostTl)
    }

    @Test fun kisaRotadaAraSarjGerekmiyorsaSifirYolUcreti() {
        val result = RouteAlternativePreview.calculate(route(8.0, 0.2), car, 80.0, 10.0)!!
        assertEquals(false, result.requiresCharging)
        assertEquals(0.0, result.chargingMinutes!!, 0.001)
        assertEquals(0.0, result.chargingCostTl!!, 0.001)
        assertTrue(result.arrivalSocPct!! in 10.0..80.0)
    }

    @Test fun gecersizParametrelerUydurmaSonucOlusturmaz() {
        assertNull(RouteAlternativePreview.calculate(route(0.0, 1.0), car, 70.0))
        assertNull(RouteAlternativePreview.calculate(route(20.0, 0.0), car, 70.0))
        assertNull(RouteAlternativePreview.calculate(route(20.0, 0.4), car, 70.0, 110.0))
        assertNull(RouteAlternativePreview.calculate(route(20.0, 0.4), car, 70.0, -1.0))
    }
}
