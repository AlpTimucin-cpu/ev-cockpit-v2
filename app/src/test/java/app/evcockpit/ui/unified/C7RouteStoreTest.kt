package app.evcockpit.ui.unified

import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.domain.nav.WgsPoint
import app.evcockpit.navigation.DestinationCodec
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.SavedDestination
import org.junit.Assert.*
import org.junit.Test

class C7RouteStoreTest {
    private val a = Place("a", "TEST A", 38.0, 34.0, "PHOTON")
    private val b = Place("b", "TEST B", 39.0, 35.0, "PHOTON")
    private val route = RouteModel("r", listOf(WgsPoint(38.0, 34.0),
        WgsPoint(39.0, 35.0)), 150000.0, 7200.0, source = "OSRM")
    private val state = UnifiedUiState(origin = a, destination = b,
        routes = Loadable.Ready(listOf(route)), mode = SheetMode.RoutePreview(0))
    private fun saved() = requireNotNull(C7RouteCodec.from(state, "TEST ROUTE", 12345L))

    @Test fun routeRoundTripRetainsOnlyHistoricalSummaryAndEndpoints() {
        val original = saved()
        val json = C7RouteCodec.encode(listOf(original))
        assertEquals(listOf(original), C7RouteCodec.decode(json))
        assertFalse(json.contains("maneuvers"))
        assertFalse(json.contains("geometry"))
        assertFalse(json.contains("soc"))
    }

    @Test fun liveGpsIsNotRestoredAsCurrentOrigin() {
        val gps = a.copy(id = "gps", source = "GPS")
        val last = C7RouteCodec.session(gps, b)
        assertNull(last.origin)
        assertEquals(C7RouteCodec.historical(b), last.destination)
        assertEquals(last, C7RouteCodec.decodeLast(C7RouteCodec.encodeLast(last)))
        val historical = C7RouteCodec.historical(gps)
        assertNotEquals("gps", historical.id)
        assertEquals("KAYIT", historical.source)
        assertEquals(historical, C7RouteCodec.session(historical, b).origin)
    }

    @Test fun malformedAndInvalidRoutesAreIgnored() {
        assertTrue(C7RouteCodec.decode("not json").isEmpty())
        assertNull(C7RouteCodec.from(state.copy(routes = Loadable.Idle), "TEST", 1L))
        assertFalse(C7RouteCodec.valid(saved().copy(distanceMeters = Double.NaN)))
        assertFalse(C7RouteCodec.valid(saved().copy(endpoints = C7Endpoints(a, a))))
        assertEquals(C7Endpoints(), C7RouteCodec.decodeLast("{broken"))
    }

    @Test fun routeLibraryIsBoundedAndDuplicateIdsReplace() {
        val first = saved()
        val many = (0..30).map { first.copy(id = "id-$it") }
        assertEquals(C7RouteCodec.MAX_ROUTES, C7RouteCodec.decode(C7RouteCodec.encode(many)).size)
        val updated = first.copy(title = "UPDATED")
        assertEquals(listOf(updated), C7RouteCodec.add(listOf(first), updated))
    }

    @Test fun independentAddressLimitsKeepHomeAndWork() {
        val home = SavedDestination("HOME", "TEST HOME", GeoPoint(38.0, 34.0))
        val work = SavedDestination("WORK", "TEST WORK", GeoPoint(38.1, 34.0))
        var items = listOf(home, work)
        for (i in 0..24) {
            items = DestinationCodec.add(items, SavedDestination("FAVORITE", "F$i", GeoPoint(38.0, 34.0 + i / 100.0)))
            items = DestinationCodec.add(items, SavedDestination("RECENT", "R$i", GeoPoint(38.0, 34.0 + i / 100.0)))
        }
        assertEquals(20, items.count { it.type == "FAVORITE" })
        assertEquals(10, items.count { it.type == "RECENT" })
        assertTrue(items.contains(home))
        assertTrue(items.contains(work))
        assertEquals(items, DestinationCodec.decode(DestinationCodec.encode(items)))
        val moved = home.copy(point = GeoPoint(37.0, 34.0))
        assertEquals(listOf(moved), DestinationCodec.add(items, moved).filter { it.type == "HOME" })
    }
}
