package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class DebugDemoMapperIsolationTest {
    private val now = 1_795_460_000_000L

    @Test fun demoModeliYalnizAyrikDemoMapperUretir() {
        val demo = DemoDashboardMapper.map(now)
        assertTrue(demo.demo)
        assertEquals(DashboardOrigin.DEMO, demo.socPercent.source)
        assertEquals(DashboardOrigin.DEMO, demo.speedKmh.source)
        assertTrue(demo.weatherSamples.isNotEmpty())
        val real = RealDashboardMapper.map(
            RealDashboardInput(
                gpsSpeedMetersPerSecond = StampedValue(
                    25.0, DashboardOrigin.DEMO, now),
                obdVerifiedSocPercent = StampedValue(
                    75.0, DashboardOrigin.DEMO, now),
                modelConsumptionKwhPer100Km = StampedValue(
                    17.0, DashboardOrigin.DEMO, now),
            ), now)
        assertFalse(real.demo)
        assertNull(real.speedKmh.value)
        assertNull(real.socPercent.value)
        assertNull(real.consumptionKwhPer100Km.value)
    }

}
