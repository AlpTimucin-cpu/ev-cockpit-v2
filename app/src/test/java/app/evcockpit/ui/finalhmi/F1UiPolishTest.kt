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
    @Test fun hava393DpUcOrnekEsitHucresindeKirpilmadanYerlesir() {
        val t = CockpitDesignTokens
        assertEquals(WeatherLayoutMode.EQUAL_WEIGHT,
            weatherLayoutMode(345f, 3, t.spaceS.value, t.weatherCellMinWidth.value))
        assertEquals(WeatherLayoutMode.EQUAL_WEIGHT,
            weatherLayoutMode(318f, 3, t.spaceS.value, t.weatherCellMinWidth.value))
    }

    @Test fun hava360DpBuyukMinHucreKaydirmayaGecer() {
        assertEquals(WeatherLayoutMode.SCROLL,
            weatherLayoutMode(318f, 3, 9f, 128f))
        assertEquals(WeatherLayoutMode.SCROLL,
            weatherLayoutMode(200f, 3, 9f, 96f))
    }

    @Test fun havaDortVeBesOrnekteTelefondaKaydirir() {
        val t = CockpitDesignTokens
        assertEquals(WeatherLayoutMode.SCROLL,
            weatherLayoutMode(345f, 4, t.spaceS.value, t.weatherCellMinWidth.value))
        assertEquals(WeatherLayoutMode.SCROLL,
            weatherLayoutMode(345f, 5, t.spaceS.value, t.weatherCellMinWidth.value))
    }

    @Test fun havaSifirBirVeGecersizOlculer() {
        assertEquals(WeatherLayoutMode.EQUAL_WEIGHT,
            weatherLayoutMode(0f, 0, 9f, 96f))
        assertEquals(WeatherLayoutMode.EQUAL_WEIGHT,
            weatherLayoutMode(100f, 1, 9f, 96f))
        assertEquals(WeatherLayoutMode.SCROLL,
            weatherLayoutMode(Float.NaN, 3, 9f, 96f))
    }

    @Test fun karanlikYolKontrastEslestirmesiSafVeTekil() {
        val specs = F1DarkRoadContrast.mappings
        assertTrue(specs.isNotEmpty())
        assertEquals(specs.size, specs.map { it.layerId }.distinct().size)
        assertEquals(CockpitDesignTokens.mapRoadMajorContrast,
            specs.first { it.layerId == "highway_major_subtle" }.color)
        assertEquals(CockpitDesignTokens.mapRoadMinorContrast,
            specs.first { it.layerId == "highway_minor" }.color)
        assertEquals(CockpitDesignTokens.mapRoadMinorContrast,
            specs.first { it.layerId == "highway_motorway_subtle" }.color)
    }

}
