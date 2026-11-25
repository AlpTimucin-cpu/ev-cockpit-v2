package app.evcockpit.ui.finalhmi

import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.obd.VehicleState
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.ui.tesla.DriveModeSnapshot
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class C8ProductionIntegrationTest {
    private val base = DashboardUiState()
    private val profile = VehicleProfile.defaults()

    @Test fun staleAndDemoObdDoNotLeakIntoRealDashboard() {
        val vehicle = VehicleState(connected = true, demo = true,
            socPct = 90.0, speedKmh = 100.0, hvVolt = 400.0, hvAmp = 40.0)
        val state = C8DashboardProjection.map(base, GpsSnapshot(), vehicle,
            DriveModeSnapshot(obdSocFresh = true, obdSpeedFresh = true, obdPowerFresh = true),
            55.0, null, profile, 1000L)
        assertEquals(55.0, state.socPercent.value!!, 0.001)
        assertEquals(DashboardOrigin.MANUAL, state.socPercent.source)
        assertNull(state.powerKw.value)
        assertNull(state.speedKmh.value)
    }

    @Test fun freshObdIsExplicitlyUnverifiedAndPowerIsNotFabricated() {
        val vehicle = VehicleState(connected = true, socPct = 67.0,
            speedKmh = 80.0, hvVolt = 400.0, hvAmp = 25.0)
        val flags = DriveModeSnapshot(obdSocFresh = true, obdSpeedFresh = true, obdPowerFresh = true)
        val state = C8DashboardProjection.map(base, GpsSnapshot(), vehicle,
            flags, null, null, profile, 1000L)
        assertEquals(DashboardOrigin.OBD_UNVERIFIED, state.socPercent.source)
        assertEquals(DashboardOrigin.OBD_UNVERIFIED, state.powerKw.source)
        assertEquals(10.0, state.powerKw.value!!, 0.001)
        val stale = C8DashboardProjection.map(base, GpsSnapshot(), vehicle,
            DriveModeSnapshot(), null, null, profile, 1000L)
        assertNull(stale.socPercent.value)
        assertNull(stale.powerKw.value)
    }

    @Test fun freshGpsWinsOverUnverifiedObdSpeed() {
        val fix = GpsSnapshot(point = GeoPoint(38.0, 34.0), provider = "gps",
            active = true, speedKmh = 60.0, accuracyMeters = 4f, receivedElapsedRealtimeMs = 1000L)
        val state = C8DashboardProjection.map(base, fix, VehicleState(connected = true, speedKmh = 80.0),
            DriveModeSnapshot(obdSpeedFresh = true), null, null, profile, 1100L)
        assertEquals(DashboardOrigin.GPS, state.speedKmh.source)
        assertEquals(60.0, state.speedKmh.value!!, 0.001)
    }

    @Test fun mainActivityUsesUnifiedMapAndSharedRoute() {
        val cwd = File(System.getProperty("user.dir"))
        val app = listOf(File(cwd, "src/main/java/app/evcockpit/MainActivity.kt"),
            File(cwd, "app/src/main/java/app/evcockpit/MainActivity.kt")).first { it.isFile }
        val source = app.readText()
        assertTrue(source.contains("UnifiedMapScreen(vm = unified"))
        assertTrue(source.contains("navigation = selectedRoute"))
        assertFalse(source.contains("RouteScreen("))
        assertFalse(source.contains("nav.navigateVia"))
    }
}
