package app.evcockpit.obd

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Synthetic vehicle responses only. No actual VIN, private PID or real car data. */
class ObdDiagnosticPrivacyTest {
    @Test fun vinAndPassiveCanPayloadNeverEnterShareableDiagnosticLines() = runBlocking {
        val vin = "WVWZZZ1JZXW000001" // deliberately fictional
        val canPayload = "17FE007B 08 62 02 8C 99 88 77"
        val output = mutableListOf<String>()
        val io = FakeDiagnosticIo(vin, canPayload)
        val result = ObdDiagnosticEngine(io, output::add).run()
        val exported = output.joinToString("\n")
        val steps = result.steps.joinToString("\n") { it.raw }
        assertTrue(io.commands.contains("0902"))
        assertFalse(exported.contains(vin))
        assertFalse(steps.contains(vin))
        assertFalse(exported.contains(canPayload))
        assertFalse(steps.contains(canPayload))
        assertTrue(result.steps.any { it.command == "0902" &&
            it.raw.contains("GİZLENDİ") })
        assertTrue(result.steps.any { it.command.startsWith("ATMA") &&
            it.raw.contains("okunabilir çerçeve: 1") })
        assertEquals(1, result.monitorFrames)
    }

    @Test fun ordinaryElmIdentityWithUnlabelledVinIsRedacted() = runBlocking {
        val vin = "WVWZZZ1JZXW000001"
        val output = mutableListOf<String>()
        val result = ObdDiagnosticEngine(FakeDiagnosticIo(vin, "") {
            if (it == "ATI") "ELM327 v2.1 $vin" else null
        }, output::add).run()
        assertFalse(output.joinToString().contains(vin))
        assertFalse(result.steps.joinToString().contains(vin))
    }

    private class FakeDiagnosticIo(
        private val vin: String, private val monitor: String,
        private val extra: (String) -> String? = { null },
    ) : ObdTransport {
        val commands = mutableListOf<String>()
        override suspend fun open() = Unit
        override suspend fun write(text: String) { commands += text }
        override suspend fun readUntilPrompt(timeoutMs: Long): String {
            val command = commands.lastOrNull() ?: ""
            return extra(command) ?: when {
                command == "0902" -> "49 02 01 $vin"
                command.startsWith("AT") -> "OK"
                else -> "NO DATA"
            }
        }
        override suspend fun monitorFor(durationMs: Long): String = monitor
        override fun close() = Unit
    }
}
