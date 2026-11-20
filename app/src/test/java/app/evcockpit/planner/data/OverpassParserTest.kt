package app.evcockpit.planner.data

import app.evcockpit.planner.charging.*
import org.junit.Assert.*
import org.junit.Test

class OverpassParserTest {
    private val raw="""{"elements":[
       {"type":"node","id":1,"lat":38.7,"lon":35.5,"tags":{
        "amenity":"charging_station","operator":"Trugo","socket:type2_combo":"2",
        "socket:type2_combo:output":"180 kW","access":"yes","fee":"yes"}},
       {"type":"way","id":2,"center":{"lat":38.7001,"lon":35.5001},"tags":{
        "amenity":"charging_station","operator":"Other","socket:type2_combo":"yes",
        "socket:type2_combo:output":"120000 W"}},
       {"type":"relation","id":3,"center":{"lat":40.0,"lon":33.0},"tags":{
        "amenity":"charging_station","operator":"Unlisted"}},
       {"type":"node","id":4,"lat":40.1,"lon":33.1,"tags":{"amenity":"parking"}}
    ]}"""
    @Test fun taggedPowerAndWayRelationCenter() {
        val p=OverpassParser.parse(raw)
        assertEquals(3,p.size)
        assertEquals(ChargerConfidence.OSM_TAGGED_CCS2,p.first().confidence)
        assertEquals(180.0,p.first().powerKw.value,0.01)
        assertEquals(120.0,p[1].powerKw.value,0.01)
        assertEquals(ChargerConfidence.UNKNOWN,p[2].confidence)
        assertEquals("doluluk bilinmiyor",p[2].occupancyText)
    }
    @Test fun dedupKeepsHighestEvidence() {
        val d=OverpassParser.dedupe(OverpassParser.parse(raw))
        assertEquals(2,d.size)
        assertEquals(180.0,d.first().powerKw.value,0.01)
    }
    @Test fun unknownTagNotAutomaticallyRemoved() {
        val p=OverpassParser.parse(raw)
        assertEquals(3,ChargerFiltering.apply(p,ChargerFilter()).size)
        assertEquals(2,ChargerFiltering.apply(p,ChargerFilter(onlyOsmTaggedCcs2=true)).size)
        assertEquals(1,ChargerFiltering.apply(p,ChargerFilter(minKw=130.0)).size)
    }
    @Test fun malformedJsonFailsClosed() {
        assertThrows(Exception::class.java){OverpassParser.parse("{unclosed")}
        assertNull(OverpassParser.kw("not a watt"))
        assertNull(OverpassParser.kw("600 kW"))
    }
    @Test fun malformedStationDoesNotHideOtherValidStations() {
        val payload="""{"elements":[
            {"type":"node","id":7,"lat":"wrong","lon":35.5,"tags":{"amenity":"charging_station"}},
            {"type":"node","id":8,"lat":38.7,"lon":35.6,
             "tags":{"amenity":"charging_station","socket:type2_combo":"yes",
             "socket:type2_combo:output":"50 kW"}},
            12345,
            {"type":"way","id":9,"tags":{"amenity":"charging_station"}}
        ]}"""
        val parsed=OverpassParser.parse(payload)
        assertEquals(1,parsed.size)
        assertEquals("node:8",parsed.first().id)
        assertEquals(50.0,parsed.first().powerKw.value,0.01)
    }
    @Test fun knownDcNetworkOnlyImpliesProbable() {
        val p=OverpassParser.parse("""{"elements":[{"type":"node","id":5,"lat":38.8,
          "lon":35.6,"tags":{"amenity":"charging_station","operator":"ZES"}}]}""")
        assertEquals(ChargerConfidence.PROBABLE_CCS2,p[0].confidence)
        assertFalse(p[0].powerKw.verified)
    }
}
