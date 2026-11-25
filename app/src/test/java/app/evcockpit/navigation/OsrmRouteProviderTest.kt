package app.evcockpit.navigation

import org.junit.Assert.*
import org.junit.Test

class OsrmRouteProviderTest {
    @Test fun osrmUrlKoordinatlariBoylamEnlemSirasiIleYazar() {
        val url = OsrmRequest.url(GeoPoint(37.9698, 34.6764), GeoPoint(36.8969, 30.7133))
        assertTrue(url.startsWith("https://routing.openstreetmap.de/routed-car/"))
        assertTrue(url.contains("34.676400,37.969800;30.713300,36.896900"))
        assertTrue(url.contains("alternatives=3"))
        assertTrue(url.contains("steps=true"))
    }

    @Test fun gercekGeoJsonAlternatifleriAyrisiyorVeSecilebilir() {
        val raw = """{
          "code":"Ok",
          "routes":[
            {"distance":12000.0,"duration":720.0,
             "geometry":{"type":"LineString","coordinates":[[34.6764,37.9698],[34.7000,37.9500],[34.8000,37.9000]]},
             "legs":[{"steps":[{"name":"Ana Yol","maneuver":{"type":"depart","modifier":"straight","location":[34.6764,37.9698]}}]}]},
            {"distance":14000.0,"duration":850.0,
             "geometry":{"type":"LineString","coordinates":[[34.6764,37.9698],[34.6800,37.9400],[34.8000,37.9000]]},
             "legs":[{"steps":[]}]}
          ]}"""
        val actual = OsrmResponseParser.parse(raw)
        assertEquals(2, actual.size)
        assertEquals(12_000.0, actual[0].distanceMeters, 0.001)
        assertEquals(14_000.0, actual[1].distanceMeters, 0.001)
        assertEquals(37.95, actual[0].geometry[1].latitude, 0.0001)
        assertTrue(actual[0].maneuvers.first().instructionTr.contains("Yola çıkın"))
        assertEquals(0, actual[0].maneuvers.first().geometryIndex)
        assertTrue(actual.all { it.provider.contains("OSRM") })
    }

    @Test fun tekGercekRotaGeldiğindeSahteAlternatifYok() {
        val raw = """{"code":"Ok","routes":[{"distance":120.0,"duration":20.0,
          "geometry":{"type":"LineString","coordinates":[[34.5,38.0],[34.6,38.1]]},"legs":[]}]}"""
        assertEquals(1, OsrmResponseParser.parse(raw).size)
        assertThrows(IllegalArgumentException::class.java) {
            OsrmResponseParser.parse("""{"code":"NoRoute","routes":[]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OsrmResponseParser.parse("""{"code":"Ok","routes":[]}""")
        }
    }

    @Test fun turkceManevraVeFotonTurkiyeOnceligi() {
        assertEquals("Yola devam edin", OsrmResponseParser.instruction("continue", "", ""))
        assertEquals("sola yönüne devam edin", OsrmResponseParser.instruction("turn", "left", ""))
        val query = PhotonPolicy.requestUrl("Niğde otogar", GeoPoint(38.1, 34.7))
        // Explicit unsupported lang can cause a 400; use the documented Accept-Language header.
        assertFalse(query.contains("lang="))
        assertTrue(PhotonPolicy.ACCEPT_LANGUAGE.startsWith("tr"))
        assertTrue(query.contains("lat=38.10000"))
        assertTrue(query.contains("lon=34.70000"))
        assertTrue(query.contains("zoom=6"))
        assertFalse(query.contains("nominatim"))
        assertTrue(PhotonPolicy.requestUrl("Kay", null).contains("lat=39.00000"))
    }
}
