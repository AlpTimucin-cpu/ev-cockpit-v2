package app.evcockpit.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeTestReportTextTest {
    @Test fun redactsVinRawQueryAndSecrets() {
        for (raw in listOf(
            "23:01 [B standart] 0902 => 49 02 31",
            "Authorization: Bearer xxxxx",
            "api_key=secret-very-sensitive",
            "VIN: WVW12345678901234",
        )) assertEquals("[HASSAS SATIR GİZLENDİ]", SafeTestReportText.sanitizeLine(raw))
    }

    @Test fun redactsCoordinatePairs() {
        assertEquals("[HASSAS SATIR GİZLENDİ]",
            SafeTestReportText.sanitizeLine("GPS 38.267123, 34.687321"))
        assertEquals("[HASSAS SATIR GİZLENDİ]",
            SafeTestReportText.sanitizeLine("konum: 0"))
    }

    @Test fun normalObdLogsRemainReadable() {
        assertEquals("23:01 22028C NO DATA x5",
            SafeTestReportText.sanitizeLine("23:01 22028C NO DATA x5"))
    }

    @Test fun onlyLast200LinesAndNoKeysOrCoordinates() {
        val raw = (1..205).map { "OBD line " + it } +
            listOf("Authorization: Bearer FORBIDDEN", "GPS 38.267123,34.687321")
        val report = SafeTestReportText.build("0.2.4", "17", "Test Device",
            listOf("Bluetooth: izin verildi"), "Tanı yanıtı yok", raw)
        assertFalse(report.contains("OBD line 1\n"))
        assertTrue(report.contains("OBD line 10"))
        assertTrue(report.contains("[HASSAS SATIR GİZLENDİ]"))
        assertFalse(report.contains("FORBIDDEN"))
        assertFalse(report.contains("38.267123"))
    }

    @Test fun systemCompatibilityIsIncludedWithSanitization() {
        val report = SafeTestReportText.build(
            appVersion = "0.3.21", androidVersion = "17", deviceModel = "Test",
            permissionStatuses = emptyList(), diagnosisSummary = "DOĞRULANMADI",
            lastObdLogLines = emptyList(),
            compatibilityReport = "Android: 17 (API 37)\\nWebView sürümü: 140\\nVIN: DO_NOT_LEAK",
        )
        assertTrue(report.contains("Sistem uyumluluk tanısı"))
        assertTrue(report.contains("WebView sürümü: 140"))
        assertFalse(report.contains("DO_NOT_LEAK"))
        assertTrue(report.contains("[HASSAS SATIR GİZLENDİ]"))
    }
}
