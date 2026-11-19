package app.evcockpit.obd

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdDiagnosticEngineTest {
    @Test fun canonicalPositiveResponsesOnly() {
        assertTrue(DiagnosticEvidence.hasMode01("7E8 06 41 00 BE 1F A8 13", "00"))
        assertTrue(DiagnosticEvidence.hasMode01("410D00", "0D"))
        assertFalse(DiagnosticEvidence.hasMode01("NO DATA", "00"))
        assertFalse(DiagnosticEvidence.hasMode01("OK", "0D"))
        assertTrue(DiagnosticEvidence.hasUds("17FE007B 04 62 02 8C C8", "028C"))
        assertFalse(DiagnosticEvidence.hasUds("7F 22 31", "028C"))
        assertFalse(DiagnosticEvidence.hasUds("NO DATA", "028C"))
    }

    @Test fun monitorCountsOnlyFrameLikeLines() {
        val ids = DiagnosticEvidence.monitorIdentifiers(
            "ATMA\r\n17FE007B 04 62 02 8C C8\r\n7E8 06 41 00 BE 1F A8\r\nOK\r\n")
        assertEquals(listOf("17FE007B", "7E8"), ids)
    }

    @Test fun profileOnlyIfPositivePids() {
        assertEquals(DiagnosticProfile.NONE, DiagnosticEvidence.propose(false, false, false))
        assertEquals(DiagnosticProfile.STANDARD_SPEED, DiagnosticEvidence.propose(true, false, false))
        assertEquals(DiagnosticProfile.MEB_SHORT_HEADER, DiagnosticEvidence.propose(false, false, true))
        assertEquals(DiagnosticProfile.MEB_FULL_HEADER, DiagnosticEvidence.propose(true, true, true))
    }

    @Test fun runsEverySectionWithoutInventingVehicleResponse() = runBlocking {
        val fake = FakeIo()
        val output = ObdDiagnosticEngine(fake) {}.run()
        assertEquals(DiagnosticProfile.NONE, output.proposedProfile)
        assertEquals(0, output.monitorFrames)
        assertTrue(fake.commands.contains("ATSP0"))
        assertTrue(fake.commands.contains("0100"))
        assertTrue(fake.commands.contains("0120"))
        assertTrue(fake.commands.contains("0140"))
        assertTrue(fake.commands.contains("0902"))
        assertTrue(fake.commands.contains("010D"))
        assertTrue(fake.commands.contains("ATSP6"))
        assertTrue(fake.commands.contains("ATSP7"))
        assertTrue(fake.commands.contains("ATSH17FC007B"))
        assertTrue(fake.commands.contains("ATCP17"))
        assertTrue(fake.commands.contains("ATSHFC007B"))
        assertTrue(fake.commands.contains("0322028C55555555"))
        assertTrue(fake.commands.contains("ATSH710"))
        assertTrue(fake.commands.contains("ATSH7E4"))
        assertTrue(fake.commands.contains("22210E"))
        assertTrue(fake.monitorInvoked)
    }

    @Test fun positiveMeb8HeaderIsPreferred() = runBlocking {
        val fake = FakeIo(answers = mapOf("22028C" to "62 02 8C C8", "010D" to "41 0D 00"))
        val output = ObdDiagnosticEngine(fake) {}.run()
        assertEquals(DiagnosticProfile.MEB_FULL_HEADER, output.proposedProfile)
    }

    private class FakeIo(private val answers: Map<String, String> = emptyMap()) : ObdTransport {
        val commands = mutableListOf<String>()
        var monitorInvoked = false
        override suspend fun open() {}
        override suspend fun write(text: String) { commands += text }
        override suspend fun readUntilPrompt(timeoutMs: Long): String =
            answers[commands.last()] ?: if (commands.last().startsWith("AT")) "OK" else "NO DATA"
        override suspend fun monitorFor(durationMs: Long): String {
            monitorInvoked = true
            return ""
        }
        override fun close() {}
    }
}
