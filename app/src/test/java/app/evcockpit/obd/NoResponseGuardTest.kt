package app.evcockpit.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoResponseGuardTest {
    @Test fun stopsAfterFiveFullUnansweredCycles() {
        val guard = NoResponseGuard()
        repeat(4) { assertFalse(guard.recordFullCycle(false)) }
        assertTrue(guard.recordFullCycle(false))
        assertEquals(5, guard.consecutiveUnansweredCycles)
    }

    @Test fun anyPositiveResponseResetsConsecutiveCounter() {
        val guard = NoResponseGuard()
        repeat(4) { guard.recordFullCycle(false) }
        assertFalse(guard.recordFullCycle(true))
        assertEquals(0, guard.consecutiveUnansweredCycles)
        assertFalse(guard.recordFullCycle(false))
    }

    @Test fun standardSpeedParsingRequiresPositiveMode01Pid0d() {
        assertEquals(0.0, Elm327.parseStandardSpeed("41 0D 00")!!, 1e-9)
        assertEquals(120.0, Elm327.parseStandardSpeed("7E8 03 41 0D 78")!!, 1e-9)
        assertNull(Elm327.parseStandardSpeed("010D"))
        assertNull(Elm327.parseStandardSpeed("NO DATA"))
        assertNull(Elm327.parseStandardSpeed("41 00 80 00"))
    }

    @Test fun paddedProfileOnlyFromPositivePaddedUds() {
        assertEquals(DiagnosticProfile.MEB_SHORT_PADDED,
            DiagnosticEvidence.propose(false, false, false, true))
    }
}
