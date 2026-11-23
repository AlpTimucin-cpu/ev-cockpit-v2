package app.evcockpit.ui

import org.junit.Assert.*
import org.junit.Test

class MapLayerPreferencesTest {
    @Test fun openFreeMapKatmanlariAnahtarIstemiyor() {
        val open = listOf(MapLayer.DARK, MapLayer.LIBERTY, MapLayer.BRIGHT, MapLayer.POSITRON)
        assertEquals(6, MapLayer.entries.size)
        open.forEach { layer ->
            assertTrue(layer.styleUrl.startsWith("https://tiles.openfreemap.org/styles/"))
            assertEquals(layer.styleUrl, MapStyleResolver.url(layer, null))
        }
    }

    @Test fun uyduVeTopografiAnahtarsizAcilamaz() {
        assertNull(MapStyleResolver.url(MapLayer.SATELLITE, null))
        assertNull(MapStyleResolver.url(MapLayer.TOPOGRAPHIC, ""))
        assertNull(MapStyleResolver.url(MapLayer.TOPOGRAPHIC, "url?key=evil"))
        assertEquals(
            "https://api.maptiler.com/maps/satellite/style.json?key=OrnekKey_123",
            MapStyleResolver.url(MapLayer.SATELLITE, "OrnekKey_123"),
        )
        assertEquals(
            "https://api.maptiler.com/maps/topo/style.json?key=OrnekKey_123",
            MapStyleResolver.url(MapLayer.TOPOGRAPHIC, "OrnekKey_123"),
        )
    }

    @Test fun kayitliKatmanVeBozukTercihGuvenliCozulur() {
        assertEquals(MapLayer.DARK, MapLayer.fromId(null))
        assertEquals(MapLayer.DARK, MapLayer.fromId(""))
        assertEquals(MapLayer.DARK, MapLayer.fromId("https://evil.example"))
        assertEquals(MapLayer.SATELLITE, MapLayer.fromId("satellite"))
        assertEquals(MapLayer.TOPOGRAPHIC, MapLayer.fromId("topographic"))
    }
}
