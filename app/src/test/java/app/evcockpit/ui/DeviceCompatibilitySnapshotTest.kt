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

    @Test fun reportIncludesAndroid17ProvidersButNoIdentifiers() {
        val snapshot = DeviceCompatibilitySnapshot(
            androidRelease = "17", apiLevel = 37, rom = "LineageOS",
            preciseLocationGranted = true, approximateLocationGranted = true,
            gpsProviderEnabled = true, networkProviderEnabled = true,
            bluetoothPermissionGranted = true, bluetoothEnabled = true,
            notificationsEnabled = true, batteryOptimizationExempt = false,
            gmsStatus = "KURULU (paket görünür)",
            androidAutoStatus = "KURULU (paket görünür)",
            ttsEngineStatus = "HİZMET GÖRÜNÜR",
            classicBluetoothAvailable = true,
            webViewVersion = "140.0.7339.0",
            pageSizeBytes = 16384,
        )
        val safe = snapshot.safeReport()
        assertTrue(safe.contains("Google Play Hizmetleri (GMS): KURULU"))
        assertTrue(safe.contains("Android Auto uygulaması: KURULU"))
        assertTrue(safe.contains("Klasik Bluetooth donanımı: EVET"))
        assertTrue(safe.contains("Linux sayfa boyutu: 16384 bayt"))
        assertTrue(safe.contains("WebView sürümü: 140.0.7339.0"))
        assertFalse(safe.contains("IMEI"))
        assertFalse(safe.contains("android_id"))
    }

    @Test fun missingTurkishOfflineVoiceIsNotClaimedAsWorking() {
        assertTrue(TtsOfflineEvidence.describe(true, false).contains("bulunamadı"))
        assertTrue(TtsOfflineEvidence.describe(true, true).contains("DOĞRULANMADI"))
        assertTrue(TtsOfflineEvidence.describe(false, null).contains("DOĞRULANMADI"))
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
