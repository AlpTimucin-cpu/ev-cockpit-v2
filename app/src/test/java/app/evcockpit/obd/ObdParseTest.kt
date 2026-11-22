package app.evcockpit.obd

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ObdParseTest {
    private fun pid(len: Int, scale: Double, offset: Double = 0.0) = Pid(
        "t", "log", "000000", null, len, 0, scale, offset, false, "", 1000, false, "",
    )

    @Test fun socParsesAndScales() {
        val data = Elm327.parseUds("22028C", "62028CC8")!!
        assertEquals(80.0, PidTable.decode(pid(1, 0.4), data)!!, 1e-9)
    }

    @Test fun currentScalesWithOffset() {
        val data = Elm327.parseUds("221E3D", "621E3D00027100")!!
        assertEquals(100.0, PidTable.decode(pid(4, 0.01, -1500.0), data)!!, 1e-9)
    }

    @Test fun spacedResponseWorks() {
        val data = Elm327.parseUds("221E3B", "62 1E 3B 05 A0")!!
        assertEquals(360.0, PidTable.decode(pid(2, 0.25), data)!!, 1e-9)
    }

    @Test fun noDataIsNull() {
        assertNull(Elm327.parseUds("22028C", "NO DATA"))
        assertNull(Elm327.parseUds("22028C", "?"))
    }

    @Test fun rxHeader() {
        assertEquals("17FE007B", Elm327.rxFor("17FC007B"))
    }

    @Test fun cloneErrorsNeverDecodeAsData() {
        for (answer in listOf(
            "BUFFER FULL", "CAN ERROR", "BUS ERROR", "RX ERROR",
            "UNABLE TO CONNECT", "STOPPED", "BUSY", "?",
        )) {
            assertNull("Unexpected payload from $answer", Elm327.parseUds("22028C", answer))
        }
    }

    @Test fun echoAndLineNumbersAreHandled() {
        val data = Elm327.parseUds("22028C", "22028C\r0: 62 02 8C C8\r>")!!
        assertEquals(80.0, PidTable.decode(pid(1, 0.4), data)!!, 1e-9)
    }

    @Test fun partialDataAndNegativeUdsAreRejected() {
        assertNull(Elm327.parseUds("22028C", "62028CC"))
        assertNull(Elm327.parseUds("22028C", "7F 22 31"))
        assertNull(Elm327.parseUds("22028C", "GARBLED"))
    }

    @Test fun optionalAtFailureDoesNotAbort() = runBlocking {
        val fake = FakeTransport(
            mapOf("ATCRA17FE007B" to "?", "ATFCSH17FC007B" to "?"),
        )
        val elm = Elm327(fake)
        assertTrue(elm.initialize(ObdConfig("7", "17FC007B", 52.0, emptyList())))
        assertTrue(fake.commands.contains("ATCRA17FE007B"))
        assertTrue(fake.commands.contains("ATSP7"))
        assertTrue(fake.commands.contains("ATSH17FC007B"))
        elm.setHeader("17FC007B") // cached header, no redundant commands
        assertEquals(1, fake.commands.count { it == "ATSH17FC007B" })
        elm.setHeader("17FC0076")
        assertFalse(fake.commands.contains("ATCRA17FE0076")) // family disabled
    }

    @Test fun requiredAtHeaderFailureAborts() = runBlocking {
        val fake = FakeTransport(mapOf("ATSH17FC007B" to "?"))
        try {
            Elm327(fake).initialize(ObdConfig("7", "17FC007B", 52.0, emptyList()))
            fail("ATSH rejection must fail closed")
        } catch (expected: ObdProtocolException) {
            assertTrue(expected.message!!.contains("ATSH"))
        }
    }

    @Test fun independentCanFramesMustNotBeJoinedIntoOneUdsPayload() {
        // Neither individual responder line contains the expected positive DID.
        assertNull(Elm327.parseUds("22028C", "62 02\r8C C8"))
        assertNull(Elm327.parseUds("22028C", "17FE007B 02 62 02\r17FE0076 02 8C C8"))
        assertNull(Elm327.parseUds("22028C", "62 02 8C")) // no data
    }

    @Test fun oneFramedResponseReturnsOnlyItsOwnPayload() {
        val response = "17FE007B 04 62 02 8C C8\r17FE0076 04 62 02 8C FF"
        val data = Elm327.parseUds("22028C", response)!!
        assertEquals(1, data.size)
        assertEquals(80.0, PidTable.decode(pid(1, 0.4), data)!!, 1e-9)
        val elevenBit = Elm327.parseUds("22028C", "7E8 04 62 02 8C C8")!!
        assertEquals(1, elevenBit.size)
        assertEquals(80.0, PidTable.decode(pid(1, 0.4), elevenBit)!!, 1e-9)
    }

    @Test fun requestEchoWrongDidAndOddByteAlignmentNeverDecode() {
        assertNull(Elm327.parseUds("22028C", "22028C"))
        assertNull(Elm327.parseUds("22028C", "17FE007B 04 62 02 8D C8"))
        assertNull(Elm327.parseUds("22028C", "F62 02 8C C8"))
    }

    private class FakeTransport(private val answers: Map<String, String>) : ObdTransport {
        val commands = mutableListOf<String>()
        override suspend fun open() = Unit
        override suspend fun write(text: String) { commands += text }
        override suspend fun readUntilPrompt(timeoutMs: Long): String =
            answers[commands.last()] ?: "OK"
        override fun close() = Unit
    }
}
