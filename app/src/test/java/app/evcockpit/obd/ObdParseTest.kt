package app.evcockpit.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObdParseTest {
    private fun pid(len: Int, scale: Double, offset: Double = 0.0) = Pid(
        "t", "log", "000000", null, len, 0, scale, offset, false, "", 1000, false, "",
    )

    @Test fun socParsesAndScales() {
        val d = Elm327.parseUds("22028C", "62028CC8")!!
        assertEquals(80.0, PidTable.decode(pid(1, 0.4), d)!!, 1e-9)
    }

    @Test fun currentScalesWithOffset() {
        val d = Elm327.parseUds("221E3D", "621E3D00027100")!!
        assertEquals(100.0, PidTable.decode(pid(4, 0.01, -1500.0), d)!!, 1e-9)
    }

    @Test fun spacedResponseWorks() {
        val d = Elm327.parseUds("221E3B", "62 1E 3B 05 A0")!!
        assertEquals(360.0, PidTable.decode(pid(2, 0.25), d)!!, 1e-9)
    }

    @Test fun noDataIsNull() {
        assertNull(Elm327.parseUds("22028C", "NO DATA"))
        assertNull(Elm327.parseUds("22028C", "?"))
    }

    @Test fun rxHeader() {
        assertEquals("17FE007B", Elm327.rxFor("17FC007B"))
    }
}
