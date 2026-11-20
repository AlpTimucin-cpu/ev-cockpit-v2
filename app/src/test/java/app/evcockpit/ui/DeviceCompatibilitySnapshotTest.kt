package app.evcockpit.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCompatibilitySnapshotTest {
    @Test fun reportUsesOnlySafeDeviceCapabilities() {
        val snapshot = DeviceCompatibilitySnapshot(
            androidRelease = "17", apiLevel = 37, rom = "LineageOS (ROM işareti bulundu)",
            preciseLocationGranted = false, approximateLocationGranted = true,
            gpsProviderEnabled = true, networkProviderEnabled = false,
            bluetoothPermissionGranted = false, bluetoothEnabled = null,
            notificationsEnabled = false, batteryOptimizationExempt = false,
        )
        val report = snapshot.safeReport()
        assertTrue(report.contains("Android: 17 (API 37)"))
        assertTrue(report.contains("ROM: LineageOS"))
        assertTrue(report.contains("Hassas konum izni: HAYIR"))
        assertTrue(report.contains("Yaklaşık konum izni: EVET"))
        assertTrue(report.contains("Bluetooth açık: İZİN YOK / BİLİNMİYOR"))
        assertTrue(report.contains("gerçek GPS"))
        assertFalse(report.contains("IMEI"))
        assertFalse(report.contains("FINGERPRINT"))
    }

    @Test fun reportShowsGrantedPermissionsWithoutClaimingRealDeviceTests() {
        val snapshot = DeviceCompatibilitySnapshot(
            "16", 36, "Android / ROM türü doğrulanamadı", true, true, true, true,
            true, true, true, true,
        )
        assertTrue(snapshot.safeReport().contains("Bluetooth açık: EVET"))
        assertTrue(snapshot.safeReport().contains("arka plan sürekliliği DOĞRULANMADI"))
    }
}
