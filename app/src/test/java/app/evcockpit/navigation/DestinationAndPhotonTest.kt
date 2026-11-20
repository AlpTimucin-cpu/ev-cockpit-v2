package app.evcockpit.navigation

import org.junit.Assert.*
import org.junit.Test

class DestinationAndPhotonTest {
    @Test fun codecKeepsHomeWorkAndTenLatestRecents() {
        var list = emptyList<SavedDestination>()
        list = DestinationCodec.add(list, SavedDestination("HOME", "Ev", GeoPoint(38.0,35.0)))
        list = DestinationCodec.add(list, SavedDestination("WORK", "İş", GeoPoint(38.1,35.1)))
        repeat(15) {
            list = DestinationCodec.add(list,
                SavedDestination("RECENT", "Hedef", GeoPoint(38.0,35.0 + it/100.0)))
        }
        val decoded = DestinationCodec.decode(DestinationCodec.encode(list))
        assertEquals(12, decoded.size)
        assertEquals(10, decoded.count { it.type == "RECENT" })
        assertTrue(decoded.any { it.type == "HOME" })
        assertTrue(decoded.any { it.type == "WORK" })
    }
    @Test fun invalidOrEmptyStoredDestinationsCannotCrash() {
        assertTrue(DestinationCodec.decode("not json").isEmpty())
        assertTrue(DestinationCodec.decode(null).isEmpty())
    }
    @Test fun photonRequiresTapWithConservativeThrottle() {
        assertFalse(PhotonPolicy.canRequest("ab", 15_000))
        assertFalse(PhotonPolicy.canRequest("Kayseri", 500))
        assertTrue(PhotonPolicy.canRequest("Kayseri", 10_000))
    }
    @Test fun parsesGeoJsonLonBeforeLatAndLimitsResults() {
        val raw = """{"features":[{"properties":{"name":"Kayseri","country":"Türkiye"},"geometry":{"coordinates":[35.47,38.73]}}]}"""
        val items = PhotonParser.parse(raw)
        assertEquals(1, items.size)
        assertEquals(38.73, items[0].point.latitude, 0.000001)
        assertEquals(35.47, items[0].point.longitude, 0.000001)
    }
}
