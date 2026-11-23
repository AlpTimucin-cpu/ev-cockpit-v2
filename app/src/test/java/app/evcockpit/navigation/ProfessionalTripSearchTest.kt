package app.evcockpit.navigation

import org.junit.Assert.*
import org.junit.Test

class ProfessionalTripSearchTest {
    @Test fun citySearchAcceptsThreeLettersButRejectsTooShortOrHugeQueries() {
        assertFalse(AutocompletePolicy.eligible("Ka"))
        assertTrue(AutocompletePolicy.eligible("Kay"))
        assertTrue(AutocompletePolicy.eligible(" Antalya "))
        assertFalse(AutocompletePolicy.eligible("x".repeat(121)))
        assertEquals("antalya", AutocompletePolicy.normalized("  ANTALYA  "))
    }

    @Test fun openMeteoResultsHaveRealCoordinatesAndNamesOnly() {
        val body = """{"results":[
        {"name":"Antalya","admin1":"Antalya","country":"Türkiye","latitude":36.8969,"longitude":30.7133},
        {"name":"Kayseri","admin1":"Kayseri","country":"Türkiye","latitude":38.7333,"longitude":35.4833},
        {"name":"Bad","latitude":120.0,"longitude":35.48}]}"""
        val found = OpenMeteoCityParser.parse(body)
        assertEquals(2, found.size)
        assertEquals("Antalya, Türkiye", found[0].label)
        assertEquals(36.8969, found[0].point.latitude, 0.0001)
        assertEquals(30.7133, found[0].point.longitude, 0.0001)
        assertEquals("Kayseri, Türkiye", found[1].label)
        assertTrue(OpenMeteoCityParser.parse("{}").isEmpty())
        assertTrue(OpenMeteoCityParser.parse("not-json").isEmpty())
    }

    @Test fun routeOptionsUseOnlyProviderReturnedFeatures() {
        val json = """{"type":"FeatureCollection","features":[
            {"type":"Feature","geometry":{"type":"LineString","coordinates":[[35.0,38.0],[35.5,38.2]]},
            "properties":{"summary":{"distance":50000,"duration":2700},"segments":[]}},
            {"type":"Feature","geometry":{"type":"LineString","coordinates":[[35.0,38.0],[35.6,38.2]]},
            "properties":{"summary":{"distance":55000,"duration":3060},"segments":[]}}]}"""
        val found = OrsGeoJsonParser.parseAll(json)
        assertEquals(2, found.size)
        assertEquals(50_000.0, found[0].distanceMeters, 0.01)
        assertEquals(55_000.0, found[1].distanceMeters, 0.01)
        assertEquals(35.6, found[1].geometry.last().longitude, 0.0001)
        assertEquals(found[0], OrsGeoJsonParser.parse(json))
    }

    @Test fun alternativesAreRequestedOnlyWithinConservativeDistanceBound() {
        val from = GeoPoint(38.0, 35.0)
        val nearby = GeoPoint(38.1, 35.2)
        val distant = GeoPoint(36.9, 30.7)
        assertTrue(OrsAlternativePolicy.eligible(from, nearby))
        assertFalse(OrsAlternativePolicy.eligible(from, distant))
        assertFalse(OrsAlternativePolicy.eligible(from, from))
        assertTrue(OrsAlternativePolicy.label(1, false).contains("100 km"))
        assertTrue(OrsAlternativePolicy.label(2, true).contains("2 gerçek"))
    }

    @Test fun alternativeRoutesPayloadNeverEmbedsAuthKey() {
        val locations = listOf(GeoPoint(38.0,35.0),GeoPoint(38.2,35.3))
        val json = org.json.JSONObject(OrsViaRequest.build(locations, alternatives = true))
        assertEquals(3, json.getJSONObject("alternative_routes").getInt("target_count"))
        assertEquals(2, json.getJSONArray("coordinates").length())
        assertFalse(json.toString().contains("Authorization"))
        assertFalse(json.toString().contains("apiKey"))
        val normal = org.json.JSONObject(OrsViaRequest.build(locations))
        assertFalse(normal.has("alternative_routes"))
        assertThrows(IllegalArgumentException::class.java) {
            OrsViaRequest.build(locations + GeoPoint(39.0,35.4), alternatives = true)
        }
    }
}
