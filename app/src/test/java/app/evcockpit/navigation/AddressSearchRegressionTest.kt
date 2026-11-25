package app.evcockpit.navigation

import org.junit.Assert.*
import org.junit.Test

class AddressSearchRegressionTest {
    @Test fun localityFallbackDoesNotPretendToResolveStreetOrHouseNumber() {
        assertTrue(AutocompletePolicy.isLocalityQuery("Niğde"))
        assertTrue(AutocompletePolicy.isLocalityQuery("Niğde Bor"))
        assertFalse(AutocompletePolicy.isLocalityQuery("Atatürk Caddesi"))
        assertFalse(AutocompletePolicy.isLocalityQuery("Atatürk 12"))
        assertFalse(AutocompletePolicy.isLocalityQuery("Merkez Mahallesi"))
    }

    @Test fun mapTilerParserUsesLongitudeLatitudeAndRejectsMalformedFeatures() {
        val raw = """{"features":[{"place_name":"TEST ADDRESS","center":[34.0,38.0]},{"place_name":"INVALID","center":[999,999]},{"center":[34,38]}]}"""
        val results = MapTilerAddressParser.parse(raw)
        assertEquals(1, results.size)
        assertEquals("TEST ADDRESS", results.first().label)
        assertEquals(GeoPoint(38.0, 34.0), results.first().point)
    }

    @Test fun photonPacingRemainsConservative() {
        assertFalse(PhotonPolicy.canRequest("Niğde", PhotonPolicy.MIN_INTERVAL_MS - 1))
        assertTrue(PhotonPolicy.canRequest("Niğde", PhotonPolicy.MIN_INTERVAL_MS))
        assertFalse(PhotonPolicy.canRequest("Ni", PhotonPolicy.MIN_INTERVAL_MS))
    }
}
