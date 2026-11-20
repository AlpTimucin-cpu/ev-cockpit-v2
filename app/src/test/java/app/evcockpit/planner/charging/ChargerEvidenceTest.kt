package app.evcockpit.planner.charging

import app.evcockpit.planner.model.SourcedValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargerEvidenceTest {
    private fun candidate(
        confidence: ChargerConfidence,
        access: String = "bilinmiyor",
    ): ChargerCandidate = ChargerCandidate(
        id = "osm-test-1",
        name = "Sentetik istasyon",
        operator = "Test",
        latitude = 38.0,
        longitude = 34.0,
        powerKw = SourcedValue(75.0, "OSM source", false),
        confidence = confidence,
        access = access,
    )

    @Test fun evenCcs2TagNeverClaimsPhysicalValidation() {
        val info = ChargerEvidence.details(candidate(ChargerConfidence.OSM_TAGGED_CCS2))
            .joinToString("\n")
        assertTrue(info.contains("OSM CCS2 etiketi var"))
        assertTrue(info.contains("cihaz doğrulanmadı"))
        assertTrue(info.contains("doluluk bilinmiyor"))
        assertTrue(info.contains("tarife/fiyat doğrulanmadı"))
    }

    @Test fun unknownConnectorRemainsUnknown() {
        val info = ChargerEvidence.details(candidate(ChargerConfidence.UNKNOWN))
            .joinToString("\n")
        assertTrue(info.contains("Bağlantı tipi BİLİNMİYOR"))
        assertTrue(info.contains("Erişim: bilinmiyor"))
    }

    @Test fun stationLabelsDoNotClaimLiveOperatorData() {
        val info = ChargerEvidence.details(candidate(ChargerConfidence.PROBABLE_CCS2, "yes"))
            .joinToString("\n")
        assertTrue(info.contains("Erişim: yes"))
        assertFalse(info.contains("Şarj cihazı çalışıyor"))
    }
}
