package app.evcockpit.obd

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothEofTest {
    @Test fun existingBytesAreNotChanged() {
        val stream = ByteArrayInputStream(byteArrayOf(0x41, 0x3e))
        assertEquals(0x41, readConnectedByte(stream))
        assertEquals(0x3e, readConnectedByte(stream))
    }

    @Test fun eofRaisesDisconnectInsteadOfReturningNegativeByte() {
        val stream = ByteArrayInputStream(byteArrayOf(0x41))
        assertEquals(0x41, readConnectedByte(stream))
        val exception = runCatching { readConnectedByte(stream) }.exceptionOrNull()
        assertTrue(exception is IOException)
        assertTrue(exception?.message?.contains("EOF") == true)
    }
}
