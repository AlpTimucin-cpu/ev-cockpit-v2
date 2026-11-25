package app.evcockpit.ui.unified.map

import org.junit.Assert.*
import org.junit.Test

class OverlayModelTest {
    @Test fun baslangictaSahteRotaIstasyonHavaAracUretilmez() {
        val empty = OverlayModel()
        assertFalse(empty.hasAnyGeometry)
        assertTrue(empty.selectedRoute.isEmpty())
        assertTrue(empty.alternativeRoutes.isEmpty())
        assertTrue(empty.stations.isEmpty())
        assertTrue(empty.weatherAlerts.isEmpty())
        assertNull(empty.vehicle)
    }

    @Test fun wgs84KoordinatSinirlariDogrulanir() {
        assertTrue(OverlayPoint(90.0, 180.0).valid())
        assertTrue(OverlayPoint(-90.0, -180.0).valid())
        assertFalse(OverlayPoint(91.0, 0.0).valid())
        assertFalse(OverlayPoint(Double.NaN, 34.0).valid())
    }

    @Test fun overlayKimlikleriTekilVeSiraDogrudur() {
        val ids = buildList {
            repeat(3) {
                add(OverlayIds.alternativeSource(it))
                add(OverlayIds.alternativeLayer(it))
            }
            addAll(listOf(
                OverlayIds.SELECTED_ROUTE_SOURCE, OverlayIds.SELECTED_ROUTE_CASING,
                OverlayIds.SELECTED_ROUTE_LAYER,
                OverlayIds.WEATHER_SOURCE, OverlayIds.WEATHER_LAYER,
                OverlayIds.STATIONS_SOURCE, OverlayIds.STATIONS_LAYER,
                OverlayIds.ENDPOINTS_SOURCE, OverlayIds.ENDPOINTS_LAYER,
                OverlayIds.VEHICLE_ACCURACY_SOURCE, OverlayIds.VEHICLE_ACCURACY_LAYER,
                OverlayIds.VEHICLE_SOURCE, OverlayIds.VEHICLE_LAYER,
            ))
        }
        assertEquals(ids.size, ids.distinct().size)
        assertEquals("src-route-sel", OverlayIds.SELECTED_ROUTE_SOURCE)
    }

    @Test fun gucOlcumunuTahminEtmedenKabulEder() {
        val station = OverlayStation("s1", OverlayPoint(39.0, 35.0), null, dc = true)
        assertNull(station.powerKw)
        assertFalse(station.selected)
    }
}
