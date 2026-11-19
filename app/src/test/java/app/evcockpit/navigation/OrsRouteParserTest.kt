package app.evcockpit.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrsRouteParserTest {
    private val example = """
        {"type":"FeatureCollection","features":[{"type":"Feature",
        "geometry":{"type":"LineString","coordinates":[[34.6,37.9],[30.7,36.9]]},
        "properties":{"summary":{"distance":380000,"duration":14000},
        "segments":[{"steps":[{"instruction":"Sağa dön","type":1,"way_points":[0,1]},
                              {"instruction":"Hedefe ulaştınız","type":10,"way_points":[1,1]}]}]}}]}
    """.trimIndent()

    @Test fun parsesCoordinatesAndTurkishInstructions() {
        val route = OrsGeoJsonParser.parse(example)
        assertEquals(2, route.geometry.size)
        assertEquals(37.9, route.geometry[0].latitude, 0.0001)
        assertEquals(34.6, route.geometry[0].longitude, 0.0001)
        assertEquals(380000.0, route.distanceMeters, 0.1)
        assertEquals(14000.0, route.durationSeconds, 0.1)
        assertEquals("Sağa dön", route.maneuvers[0].instructionTr)
        assertEquals(10, route.maneuvers[1].type)
    }

    @Test fun rejectsEmptyRoute() {
        val bad = """{"type":"FeatureCollection","features":[]}"""
        try {
            OrsGeoJsonParser.parse(bad)
            throw AssertionError("Empty route should fail")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("Rota bulunamadı"))
        }
    }

    @Test fun rejectsInvalidCoordinates() {
        try {
            GeoPoint(92.0, 35.0)
            throw AssertionError("Invalid latitude accepted")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test fun keepsFixedHttpsEndpoint() {
        assertTrue(OrsRouteProvider.ENDPOINT.startsWith("https://api.heigit.org/"))
    }
}
