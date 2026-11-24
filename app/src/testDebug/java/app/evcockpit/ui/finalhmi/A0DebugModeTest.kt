package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class A0DebugModeTest {
    @Test fun demoKullaniciAcanaKadarGercekModBosKalir() {
        assertTrue(BuildTypeDemoProvider.available)
        val real = RealDashboardMapper.map(RealDashboardInput(), 1_795_460_000_000L)
        assertFalse(real.demo)
        assertNull(real.socPercent.value)
        assertNull(real.speedKmh.value)
    }

    @Test fun debugOrnegiAyriKaynakVeAcikDemoEtiketlidir() {
        val state = BuildTypeDemoProvider.state(1_795_460_000_000L)
        assertTrue(state.demo)
        assertEquals(DashboardOrigin.DEMO, state.socPercent.source)
    }
}
