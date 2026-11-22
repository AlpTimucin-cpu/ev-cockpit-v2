package app.evcockpit.ui.tesla

import app.evcockpit.obd.VehicleState
import org.junit.Assert.*
import org.junit.Test

class ProCockpitPolicyTest {
    @Test fun missingSensorsNeverBecomeVerifiedMeasurements() {
        val labels = ProCockpitPolicy.status(
            DriveMode.UNKNOWN, VehicleState(), DriveModeSnapshot(), SourceKind.YOK)
        assertEquals("HIZ BELİRSİZ", labels.driveLabel)
        assertTrue(labels.obdLabel.contains("BAĞLI DEĞİL"))
        assertTrue(labels.gpsLabel.contains("YOK/ESKİ"))
        assertTrue(labels.socLabel.contains("YOK"))
    }

    @Test fun connectedObdIsStillNotAValidatedPid() {
        val snap = DriveModeSnapshot(mode = DriveMode.DRIVING,
            obdSocFresh = true, gpsFresh = true)
        val status = ProCockpitPolicy.status(
            DriveMode.DRIVING, VehicleState(connected = true),
            snap, SourceKind.OBD)
        assertEquals("SÜRÜŞ • GÜVENLİ", status.driveLabel)
        assertTrue(status.obdLabel.contains("DOĞRULANMADI"))
        assertTrue(status.socLabel.contains("DOĞRULANMADI"))
        assertEquals("GPS • GÜNCEL", status.gpsLabel)
    }

    @Test fun demoAndManualSourcesNeverMaskEachOther() {
        val demo = ProCockpitPolicy.status(DriveMode.UNKNOWN,
            VehicleState(connected = true, demo = true),
            DriveModeSnapshot(), SourceKind.DEMO)
        assertTrue(demo.obdLabel.contains("DEMO"))
        assertTrue(demo.socLabel.contains("DEMO"))
        val manual = ProCockpitPolicy.status(DriveMode.PARKED,
            VehicleState(), DriveModeSnapshot(mode = DriveMode.PARKED),
            SourceKind.MANUEL)
        assertEquals("SoC • MANUEL", manual.socLabel)
    }

    @Test fun batteryProgressDoesNotInventOrClampInvalidValues() {
        assertEquals(0f, ProCockpitPolicy.batteryPercent(null))
        assertEquals(0f, ProCockpitPolicy.batteryPercent(Double.NaN))
        assertEquals(0f, ProCockpitPolicy.batteryPercent(-5.0))
        assertEquals(0f, ProCockpitPolicy.batteryPercent(105.0))
        assertEquals(0.64f, ProCockpitPolicy.batteryPercent(64.0), 1e-6f)
        assertEquals(1f, ProCockpitPolicy.batteryPercent(100.0))
    }

    @Test fun actionDescriptionsCorrespondToRealScreensAndDrivePolicy() {
        assertTrue(ProCockpitPolicy.actionSubtitle(TeslaAction.ROTA).contains("navigasyon"))
        assertTrue(ProCockpitPolicy.actionSubtitle(TeslaAction.MOLA).contains("Orijinal"))
        assertFalse(DriveModePolicy.allowedAction(TeslaAction.VIDEO, DriveMode.DRIVING, true))
        assertFalse(DriveModePolicy.allowedAction(TeslaAction.PLAN, DriveMode.UNKNOWN, true))
        assertTrue(DriveModePolicy.allowedAction(TeslaAction.ROTA, DriveMode.DRIVING, false))
    }

    @Test fun noPlanDoesNotAdvertiseNonexistentChargingStops() {
        assertTrue(ProCockpitPolicy.planLabel(null).contains("HENÜZ YOK"))
    }
}
