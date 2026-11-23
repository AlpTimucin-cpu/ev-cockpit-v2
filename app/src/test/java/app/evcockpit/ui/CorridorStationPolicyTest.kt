package app.evcockpit.ui

import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.charging.ChargerConfidence
import app.evcockpit.planner.model.SourcedValue
import org.junit.Assert.*
import org.junit.Test

class CorridorStationPolicyTest {
    private fun make(id: Int, distance: Double): ChargerCandidate =
        ChargerCandidate(
            id = "station-" + id, name = "Durak " + id,
            operator = "Bilinmiyor", latitude = 38.0, longitude = 35.0,
            powerKw = SourcedValue(11.0, "OSM/TAHMİN", false),
            confidence = ChargerConfidence.UNKNOWN,
            distanceAlongMeters = distance,
        )

    @Test fun listIsDistanceOrderedAndNeverInventsAnExtraStation() {
        val source = listOf(make(3, 31_000.0), make(1, 10_000.0), make(2, 20_000.0))
        val sorted = CorridorStationPolicy.sorted(source)
        assertEquals(listOf("station-1", "station-2", "station-3"),
            sorted.map { it.id })
        assertEquals(source.size, sorted.size)
        assertEquals("station-1", CorridorStationPolicy.slice(source, 1)[0].id)
        assertEquals(0, CorridorStationPolicy.slice(source, -1).size)
    }

    @Test fun mapIsBoundedButFullSearchListRemainsAccessible() {
        val stations = (1..245).map { make(it, it * 1_000.0) }.reversed()
        assertEquals(245, CorridorStationPolicy.sorted(stations).size)
        assertEquals(CorridorStationPolicy.MAX_MAP_MARKERS,
            CorridorStationPolicy.mapMarkers(stations).size)
        assertEquals("station-1", CorridorStationPolicy.mapMarkers(stations)[0].id)
        assertEquals(30, CorridorStationPolicy.PAGE_SIZE)
    }

    @Test fun unknownSocketAndPowerAreNeverMarkedVerified() {
        val unknown = make(1, 10_000.0)
        assertFalse(unknown.powerKw.verified)
        assertTrue(CorridorStationPolicy.confidenceLabel(unknown)
            .contains("bilinmiyor"))
        assertEquals("doluluk bilinmiyor", unknown.occupancyText)
        assertTrue(CorridorStationPolicy.confidenceLabel(
            unknown.copy(confidence = ChargerConfidence.OSM_TAGGED_CCS2))
            .contains("doğrulanmadı"))
    }
}
