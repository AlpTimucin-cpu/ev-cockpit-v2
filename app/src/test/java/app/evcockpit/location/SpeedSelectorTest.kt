package app.evcockpit.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedSelectorTest {
    @Test fun usesObdWhenConnectedButLabelsUnverified() {
        val chosen = SpeedSelector.select(true, false, 65.0, 58.0, true)
        assertEquals(65.0, chosen.kmh!!, 0.01)
        assertEquals("OBD — PID doğrulanmadı", chosen.source)
    }

    @Test fun usesFreshGpsWhenObdMissing() {
        val chosen = SpeedSelector.select(false, false, null, 58.0, true)
        assertEquals(58.0, chosen.kmh!!, 0.01)
        assertEquals("GPS — yedek", chosen.source)
    }

    @Test fun refusesStaleGpsAndInvalidObd() {
        val chosen = SpeedSelector.select(true, false, -5.0, 58.0, false)
        assertNull(chosen.kmh)
        assertEquals("Hız verisi yok", chosen.source)
    }

    @Test fun demoNeverMislabelsAsRealGps() {
        val chosen = SpeedSelector.select(true, true, 60.0, 20.0, true)
        assertEquals("DEMO — gerçek değil", chosen.source)
    }
}
