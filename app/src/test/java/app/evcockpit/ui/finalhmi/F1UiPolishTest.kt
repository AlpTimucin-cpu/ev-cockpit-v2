package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

/** G1, G4, G5, G6: UI durum mantığına ilişkin JVM testleri. */
class F1UiPolishTest {
    @Test fun kisaHaritaDokunmaHedefiVeDikeyOran() {
        val touch = CockpitDesignTokens.mapControlTouchTarget.value
        assertTrue("Minimum 48 dp", touch >= 48f)
        assertEquals("Kısa modda tek düğme satırı", 48f, touch, 0.01f)
        // 360dp telefonun F1 harita görünümünün yaklaşık 200dp iç alanı.
        assertTrue("200dp haritada düğme şeridi en fazla %25 yükseklik almalı",
            touch / 200f <= 0.25f)
    }

    @Test fun kartMinimumuVeEtiketAraligi() {
        assertTrue(CockpitDesignTokens.metricCardMinHeight.value in 72f..88f)
        assertTrue(CockpitDesignTokens.spaceXS.value < 10f)
    }

    @Test fun havaDemosuYagisUyarisiAcikYazilir() {
        assertEquals("Yağış uyarısı var", rainWarningText(true))
        assertEquals("Yağış uyarısı yok", rainWarningText(false))
        assertEquals("Yağış: —", rainWarningText(null))
    }

    @Test fun bataryaCubuguYuzdeDegeriyleEslesir() {
        assertEquals(0.74f, batteryFillFraction(74.0)!!, 0.00001f)
        assertEquals(0f, batteryFillFraction(0.0)!!, 0.00001f)
        assertEquals(1f, batteryFillFraction(100.0)!!, 0.00001f)
    }

    @Test fun bilinmeyenVeGecersizBataryayiSifirSanmaz() {
        assertNull(batteryFillFraction(null))
        assertNull(batteryFillFraction(-1.0))
        assertNull(batteryFillFraction(101.0))
        assertNull(batteryFillFraction(Double.NaN))
        assertNull(batteryFillFraction(Double.POSITIVE_INFINITY))
    }
}
