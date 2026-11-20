package app.evcockpit.planui

import app.evcockpit.obd.SocReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDataEvidenceTest {
    private fun audit(
        soc: SocReading = SocReading(null, "SoC verisi yok"),
        route: Boolean = false,
        gps: Boolean = false,
        fresh: Boolean = false,
        accuracy: Float? = null,
        searched: Boolean = false,
        stations: Int = 0,
        tagged: Int = 0,
        plan: Boolean = false,
        provider: String? = null,
    ) = TripDataEvidence.evaluate(
        soc, route, provider, gps, fresh, accuracy,
        searched, stations, tagged, plan,
    )

    @Test fun demoAndMissingInputsNeverBecomeVerified() {
        val result = audit(soc = SocReading(80.0, "DEMO — gerçek değil"))
        assertEquals(EvidenceLevel.MISSING, result.items[0].level)
        assertEquals(5, result.missingCount)
    }

    @Test fun manualSocAndFreshGpsKeepRealWorldCautions() {
        val result = audit(
            soc = SocReading(61.0, "MANUEL — araç göstergesinden"),
            gps = true, fresh = true, accuracy = 7.0f, route = true,
            provider = "ORS", searched = true, stations = 4, tagged = 2,
            plan = true,
        )
        assertEquals(EvidenceLevel.CAUTION, result.items[0].level)
        assertEquals(EvidenceLevel.INPUT_AVAILABLE, result.items[1].level)
        assertEquals(EvidenceLevel.CAUTION, result.items[2].level)
        assertEquals(EvidenceLevel.CAUTION, result.items[3].level)
        assertTrue(result.items[3].explanation.contains("2 OSM CCS2"))
        assertTrue(result.safeSummary().contains("TAHMİN"))
    }

    @Test fun obdPidAndCoarseGpsAreNotOvertrusted() {
        val result = audit(
            soc = SocReading(35.0, "OBD — PID doğrulanmadı"),
            gps = true, fresh = true, accuracy = 250f,
        )
        assertEquals(EvidenceLevel.CAUTION, result.items[0].level)
        assertEquals(EvidenceLevel.CAUTION, result.items[1].level)
        assertTrue(result.items[0].explanation.contains("DOĞRULANMADI"))
    }

    @Test fun missingStationsAreNeverConfusedWithSuccessfulLiveAvailability() {
        val result = audit(searched = true, stations = 0)
        assertEquals(EvidenceLevel.MISSING, result.items[3].level)
        assertTrue(result.items[3].explanation.contains("kesin değildir"))
    }

    @Test fun numberOfTaggedCandidatesIsClampedAndProviderSanitized() {
        val result = audit(
            soc = SocReading(55.0, "MANUEL"),
            route = true, provider = "https://server.invalid/?key=not-for-display",
            searched = true, stations = 2, tagged = 99,
        )
        assertTrue(result.items[3].explanation.contains("2 OSM CCS2"))
        assertFalse(result.safeSummary().contains("not-for-display"))
    }

    @Test fun obdEvidenceSeparatesSessionFromValidVehicleReadings() {
        assertTrue(ObdEvidence.describe(false, false, null, null)[0].contains("bağlantısı yok"))
        assertTrue(ObdEvidence.describe(true, false, null, null)
            .any { it.contains("yanıtı yok") })
        assertTrue(ObdEvidence.describe(true, true, 89.0, 50.0)[0].contains("DEMO"))
        assertTrue(ObdEvidence.describe(true, false, 89.0, 50.0)
            .any { it.contains("PID ölçeği DOĞRULANMADI") })
    }
}
