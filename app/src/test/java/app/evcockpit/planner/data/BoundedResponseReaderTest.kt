package app.evcockpit.planner.data

import java.io.IOException
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedResponseReaderTest {
    @Test fun emptyAndSmallJsonArePreservedExactly() {
        assertEquals("", BoundedResponseReader.read(StringReader(""), 10))
        assertEquals("{}", BoundedResponseReader.read(StringReader("{}"), 2))
    }

    @Test fun oversizedResponseIsRejectedInsteadOfTruncated() {
        assertThrows(IOException::class.java) {
            BoundedResponseReader.read(StringReader("123456"), 5)
        }
        assertEquals("12345", BoundedResponseReader.read(StringReader("12345"), 5))
    }

    @Test fun largeInputNeverLoadsUnboundedBeforeLimitCheck() {
        val text="x".repeat(2_000_001)
        assertThrows(IOException::class.java) {
            BoundedResponseReader.read(StringReader(text))
        }
    }
}
