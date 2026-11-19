package app.evcockpit.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualSocTest {
    @Test fun manualIsUsedIfObdMissing() {
        val out = SocSelector.select(false, false, null, 56.0)
        assertEquals(56.0, out.pct!!, 1e-9)
        assertEquals("MANUEL — araç göstergesinden", out.source)
    }
    @Test fun obdOverridesManualButIsStillUnverified() {
        val out = SocSelector.select(true, false, 54.0, 56.0)
        assertEquals(54.0, out.pct!!, 1e-9)
        assertEquals("OBD — PID doğrulanmadı", out.source)
    }
    @Test fun staleDisconnectedObdNeverOverridesManual() {
        assertEquals(56.0, SocSelector.select(false, false, 54.0, 56.0).pct!!, 1e-9)
    }
    @Test fun missingReadingsAreNotInvented() {
        val out = SocSelector.select(false, false, null, null)
        assertNull(out.pct)
        assertEquals("SoC verisi yok", out.source)
    }
    @Test fun manualInputAcceptsCommaAndRejectsInvalid() {
        assertEquals(56.5, ManualSocInput.parse(" 56,5 ")!!, 1e-9)
        assertNull(ManualSocInput.parse("101"))
        assertNull(ManualSocInput.parse("-1"))
        assertNull(ManualSocInput.parse(""))
        assertNull(ManualSocInput.parse("NaN"))
    }
    @Test fun demoMustBeLabeledAsNotReal() {
        assertEquals("DEMO — gerçek değil",
            SocSelector.select(true, true, 72.0, 56.0).source)
    }
}
