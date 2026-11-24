package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class DashboardMapperAndLayoutTest {
    private val now = 1_795_460_000_000L

    @Test fun gercekModBosKalirDemoGorunmez() {
        val empty = RealDashboardMapper.map(RealDashboardInput(), now)
        assertFalse(empty.demo)
        assertNull(empty.speedKmh.value)
        assertNull(empty.socPercent.value)
        assertNull(empty.estimatedRangeKm.value)
        assertNull(empty.chargeTotalLabel.value)
        assertTrue(empty.weatherSamples.isEmpty())
    }

    @Test fun hizYalnizTazeGpsKaynaklidir() {
        val valid = RealDashboardMapper.map(RealDashboardInput(
            gpsSpeedMetersPerSecond = StampedValue(
                25.0, DashboardOrigin.GPS, now - 2_000L),
        ), now)
        assertEquals(90.0, valid.speedKmh.value!!, 0.001)
        assertEquals(DashboardOrigin.GPS, valid.speedKmh.source)
        val stale = RealDashboardMapper.map(RealDashboardInput(
            gpsSpeedMetersPerSecond = StampedValue(
                25.0, DashboardOrigin.GPS, now - 20_000L),
        ), now)
        assertNull(stale.speedKmh.value)
        val wrong = RealDashboardMapper.map(RealDashboardInput(
            gpsSpeedMetersPerSecond = StampedValue(
                25.0, DashboardOrigin.OBD_VERIFIED, now),
        ), now)
        assertNull(wrong.speedKmh.value)
    }

    @Test fun manuelSocGuzergahIlerlemesiYokkenAyniKalir() {
        val data = RealDashboardInput(
            manualStartSocPercent = StampedValue(80.0, DashboardOrigin.MANUAL, now),
            modelEnergySpentKwh = StampedValue(10.0, DashboardOrigin.MODEL, now),
            vehicleUsableCapacityKwh = StampedValue(52.0, DashboardOrigin.MODEL, now),
            modelConsumptionKwhPer100Km = StampedValue(16.0, DashboardOrigin.MODEL, now),
            routeProgressReliable = false,
        )
        val manual = RealDashboardMapper.map(data, now)
        assertEquals(80.0, manual.socPercent.value!!, 0.001)
        assertEquals(DashboardOrigin.MANUAL, manual.socPercent.source)
        val progressed = RealDashboardMapper.map(
            data.copy(routeProgressReliable = true), now)
        assertEquals(60.769230769, progressed.socPercent.value!!, 0.00001)
        assertEquals(DashboardOrigin.MODEL, progressed.socPercent.source)
        assertEquals(197.5, progressed.estimatedRangeKm.value!!, 0.01)
    }

    @Test fun veriEskidiyseUzerineTahminYapilmaz() {
        val data = RealDashboardInput(
            manualStartSocPercent = StampedValue(78.0, DashboardOrigin.MANUAL, now),
            modelEnergySpentKwh = StampedValue(6.0, DashboardOrigin.MODEL, now - 300_000L),
            modelConsumptionKwhPer100Km = StampedValue(16.0, DashboardOrigin.MODEL, now - 300_000L),
            vehicleUsableCapacityKwh = StampedValue(52.0, DashboardOrigin.MODEL, now),
            routeProgressReliable = true,
        )
        val actual = RealDashboardMapper.map(data, now)
        assertEquals(78.0, actual.socPercent.value!!, 0.001)
        assertEquals(DashboardOrigin.MANUAL, actual.socPercent.source)
        assertNull(actual.estimatedRangeKm.value)
    }

    @Test fun pencereSiniflariVeAltBesSekme() {
        assertFalse(F1DashboardLayoutPolicy.useWideMapFirst(393, 873))
        assertFalse(F1DashboardLayoutPolicy.useWideMapFirst(700, 400))
        assertTrue(F1DashboardLayoutPolicy.useWideMapFirst(960, 540))
        assertTrue(F1DashboardLayoutPolicy.useWideMapFirst(1280, 800))
        assertEquals(listOf("Harita", "Rota", "Şarj", "Enerji", "Ayarlar"),
            F1DashboardLayoutPolicy.tabs)
        assertEquals(5, FinalDashboardTab.entries.size)
    }

    @Test fun tiklamaHedefiEnAz56Dp() {
        assertTrue(CockpitDesignTokens.target.value >= 56f)
        assertTrue(CockpitDesignTokens.tabBarHeight.value >= 56f)
    }
}
