package app.evcockpit.obd

import org.junit.Assert.assertEquals
import org.junit.Test

class ObdValueQualityTest {
    @Test fun validPhysicalRangesRemainUnverifiedButCanBeDisplayed() {
        assertEquals(ObdReadingVerdict.ACCEPT, ObdValueQuality.assess("soc", 52.5))
        assertEquals(ObdReadingVerdict.ACCEPT, ObdValueQuality.assess("soc", 0.0))
        assertEquals(ObdReadingVerdict.ACCEPT, ObdValueQuality.assess("soc", 100.0))
        assertEquals(ObdReadingVerdict.ACCEPT, ObdValueQuality.assess("volt", 360.0))
        assertEquals(ObdReadingVerdict.ACCEPT, ObdValueQuality.assess("amp", -450.0))
        assertEquals(ObdReadingVerdict.ACCEPT, ObdValueQuality.assess("speed", 120.0))
    }

    @Test fun invalidAndNonFiniteNumbersNeverPass() {
        val bad = listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
        for (role in listOf("soc", "volt", "amp", "speed")) {
            for (v in bad) {
                assertEquals(ObdReadingVerdict.INVALID_NUMBER, ObdValueQuality.assess(role, v))
            }
        }
    }

    @Test fun decodingNoiseAndUnknownRolesAreRejected() {
        assertEquals(ObdReadingVerdict.OUT_OF_BOUNDS, ObdValueQuality.assess("soc", 104.0))
        assertEquals(ObdReadingVerdict.OUT_OF_BOUNDS, ObdValueQuality.assess("soc", -0.01))
        assertEquals(ObdReadingVerdict.OUT_OF_BOUNDS, ObdValueQuality.assess("speed", 450.0))
        assertEquals(ObdReadingVerdict.OUT_OF_BOUNDS, ObdValueQuality.assess("volt", 5000.0))
        assertEquals(ObdReadingVerdict.OUT_OF_BOUNDS, ObdValueQuality.assess("amp", -10000.0))
        assertEquals(ObdReadingVerdict.UNMAPPED_ROLE, ObdValueQuality.assess("log", 99.0))
    }

    @Test fun suddenSocOutlierIsRejectedButStableTrendPasses() {
        val oldMs = 100_000L
        assertEquals(ObdReadingVerdict.SOC_JUMP,
            ObdValueQuality.assess("soc", 82.0, 60.0, oldMs, oldMs + 2_000L))
        assertEquals(ObdReadingVerdict.ACCEPT,
            ObdValueQuality.assess("soc", 60.5, 60.0, oldMs, oldMs + 2_000L))
        assertEquals(ObdReadingVerdict.ACCEPT,
            ObdValueQuality.assess("soc", 80.0, 60.0, oldMs, oldMs + 2_000L))
    }

    @Test fun reconnectAfterStaleReadAllowsNewSocBaseline() {
        val oldMs = 100_000L
        assertEquals(ObdReadingVerdict.ACCEPT,
            ObdValueQuality.assess("soc", 90.0, 40.0, oldMs, oldMs + 60_000L))
        assertEquals(ObdReadingVerdict.ACCEPT,
            ObdValueQuality.assess("soc", 90.0, 40.0, 0L, oldMs + 100L))
        assertEquals(ObdReadingVerdict.ACCEPT,
            ObdValueQuality.assess("soc", 90.0, 40.0, oldMs, oldMs - 1L))
    }
}
