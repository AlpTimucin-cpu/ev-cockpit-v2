package app.evcockpit.ui

import org.junit.Assert.*
import org.junit.Test

class MapLayerPreferencesTest {
    @Test fun supportedLayersUseOnlyOpenFreeMapHttpsVectorStyles() {
        assertEquals(4, MapLayer.entries.size)
        assertEquals(MapLayer.DARK, MapLayer.fromId("dark"))
        assertEquals(MapLayer.LIBERTY, MapLayer.fromId("liberty"))
        assertEquals(MapLayer.BRIGHT, MapLayer.fromId("bright"))
        assertEquals(MapLayer.POSITRON, MapLayer.fromId("positron"))
        MapLayer.entries.forEach { layer ->
            assertTrue(layer.styleUrl.startsWith("https://tiles.openfreemap.org/styles/"))
            assertFalse(layer.id.contains("satellite"))
        }
    }

    @Test fun corruptOrUnknownMapPreferenceDefaultsToDarkWithoutInventingLayer() {
        assertEquals(MapLayer.DARK, MapLayer.fromId(null))
        assertEquals(MapLayer.DARK, MapLayer.fromId(""))
        assertEquals(MapLayer.DARK, MapLayer.fromId("satellite"))
        assertEquals(MapLayer.DARK, MapLayer.fromId("https://evil.com/styles"))
    }
}
