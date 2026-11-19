package app.evcockpit.obd

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectBackoffTest {
    @Test fun exponentialDelayIsBounded() {
        assertEquals(1_000L, ReconnectBackoff.delayMs(1))
        assertEquals(2_000L, ReconnectBackoff.delayMs(2))
        assertEquals(4_000L, ReconnectBackoff.delayMs(3))
        assertEquals(8_000L, ReconnectBackoff.delayMs(4))
        assertEquals(16_000L, ReconnectBackoff.delayMs(5))
        assertEquals(30_000L, ReconnectBackoff.delayMs(6))
        assertEquals(30_000L, ReconnectBackoff.delayMs(100))
    }
}
