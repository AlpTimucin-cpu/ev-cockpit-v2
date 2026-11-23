package app.evcockpit.planner.data

import app.evcockpit.planner.route.RoutePoint
import app.evcockpit.planner.charging.*
import app.evcockpit.planner.model.SourcedValue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class OverpassNetworkLogicTest {
    private val route=listOf(RoutePoint(38.0,35.0,0.0),
        RoutePoint(38.5,35.0,55000.0),RoutePoint(39.0,35.0,110000.0))
    @Test fun corridorLimitsDistanceAndQueryLength() {
        val chunks=CorridorChunker().chunks(route)
        assertTrue(chunks.size>=2)
        chunks.forEach {
            assertTrue(it.last().distanceMeters-it.first().distanceMeters<=90001.0)
            assertTrue(CorridorChunker().query(it).length<=4500)
        }
    }
    @Test fun cacheExpiresWithFakeClock() {
        val dir=Files.createTempDirectory("m8-cache").toFile()
        var clock=0L
        val c=ChargerCache(dir,{clock})
        c.write("queryA","{\"elements\":[]}")
        assertFalse(c.read("queryA")!!.stale)
        clock=8L*24*60*60*1000
        assertTrue(c.read("queryA")!!.stale)
        assertNull(c.read("queryB"))
        dir.deleteRecursively()
        Unit
    }
    @Test fun fakeNetworkDoesNotRequestIfFreshCache() = runBlocking {
        val dir=Files.createTempDirectory("m8-cache").toFile()
        val chunker=CorridorChunker()
        chunker.chunks(route).forEach { c ->
            ChargerCache(dir).write(chunker.query(c),"{\"elements\":[]}")
        }
        var calls=0
        val network=object:OverpassTransport {
            override suspend fun post(query:String):OverpassResponse {
                calls++;return OverpassResponse(200,"{\"elements\":[]}")
            }
        }
        val result=OverpassRepository(network,ChargerCache(dir),sleep={}).search(route)
        assertEquals(0,calls);assertFalse(result.incomplete)
        dir.deleteRecursively()
        Unit
    }
    @Test fun rateLimitRetriesAreBoundedAndObservable() = runBlocking {
        val dir=Files.createTempDirectory("m8-retry").toFile()
        var calls=0;val wait=mutableListOf<Long>()
        val network=object:OverpassTransport {
            override suspend fun post(query:String):OverpassResponse {
                calls++;return OverpassResponse(429,"",1)
            }
        }
        val result=OverpassRepository(network,ChargerCache(dir),
            sleep={ wait.add(it) }).search(route)
        assertTrue(result.incomplete)
        assertEquals(CorridorChunker().chunks(route).size*3,calls)
        assertTrue(wait.contains(1000L))
        dir.deleteRecursively()
        Unit
    }
    @Test fun cancellationIsNeverSwallowed() = runBlocking {
        val dir=Files.createTempDirectory("m8-cancel").toFile()
        val network=object:OverpassTransport {
            override suspend fun post(query:String):OverpassResponse {
                throw CancellationException("user cancelled")
            }
        }
        try {
            OverpassRepository(network,ChargerCache(dir),sleep={}).search(route)
            fail("CancellationException must propagate")
        } catch (_:CancellationException) { /* success */ }
        finally { dir.deleteRecursively() }
        Unit
    }
    @Test fun validNetworkReplySurvivesUnwritableCache() = runBlocking {
        val dir=Files.createTempDirectory("m8-ro-cache").toFile()
        val blocked=java.io.File(dir,"blocked").apply { writeText("not a directory") }
        val network=object:OverpassTransport {
            override suspend fun post(query:String)=OverpassResponse(200,
                """{"elements":[{"type":"node","id":123,"lat":38.25,"lon":35.01,
                    "tags":{"amenity":"charging_station","socket:type2_combo":"yes",
                    "socket:type2_combo:output":"50 kW"}}]}""")
        }
        val result=OverpassRepository(network,ChargerCache(blocked),sleep={})
            .search(route.take(2))
        assertFalse(result.incomplete)
        assertEquals(1,result.chargers.size)
        assertEquals(50.0,result.chargers.first().powerKw.value,0.01)
        dir.deleteRecursively()
        Unit
    }

    @Test fun invalidHttp200IsNotSavedForSevenDays() = runBlocking {
        val dir=Files.createTempDirectory("m8-invalid-json").toFile()
        val cache=ChargerCache(dir)
        var calls=0
        val network=object:OverpassTransport {
            override suspend fun post(query:String):OverpassResponse {
                calls++
                return OverpassResponse(200,"<html>service overloaded</html>")
            }
        }
        val result=OverpassRepository(network,cache,sleep={}).search(route.take(2))
        assertTrue(result.incomplete)
        assertEquals(1,calls)
        val query=CorridorChunker().query(CorridorChunker().chunks(route.take(2)).single())
        assertNull(cache.read(query))
        dir.deleteRecursively()
        Unit
    }
    @Test fun customCandidateIsProjectedWithoutNetwork() = runBlocking {
        val dir=Files.createTempDirectory("m8-custom").toFile()
        val cache=ChargerCache(dir)
        val chunker=CorridorChunker()
        chunker.chunks(route).forEach {cache.write(chunker.query(it),"{\"elements\":[]}")}
        val point=ChargerCandidate("custom","Test","manual",38.25,35.01,
            SourcedValue(60.0,"KULLANICI",false),ChargerConfidence.UNKNOWN)
        val result=OverpassRepository(object:OverpassTransport{
            override suspend fun post(query:String)=OverpassResponse(500,"")
        },cache,sleep={}).search(route,extra=listOf(point))
        assertEquals(1,result.chargers.size)
        assertTrue(result.chargers[0].distanceAlongMeters>0)
        assertTrue(result.chargers[0].detourMinutes>=0)
        dir.deleteRecursively()
        Unit
    }
    @Test fun allOsmReturnedStationsAreRetainedBeforePlanFilters() = runBlocking {
        val dir = Files.createTempDirectory("m8-all-stations").toFile()
        try {
            val json = """{"elements":[
               {"type":"node","id":11,"lat":38.2,"lon":35.01,
                "tags":{"amenity":"charging_station","name":"CCS2 A",
                        "socket:type2_combo":"yes",
                        "socket:type2_combo:output":"50 kW"}},
               {"type":"node","id":12,"lat":38.3,"lon":35.03,
                "tags":{"amenity":"charging_station","name":"Bilinmeyen B"}}
            ]}"""
            val network = object : OverpassTransport {
                override suspend fun post(query: String) = OverpassResponse(200, json)
            }
            val result = OverpassRepository(network, ChargerCache(dir), sleep = {})
                .search(route.take(2),
                    filter = ChargerFilter(onlyOsmTaggedCcs2 = true))
            assertFalse(result.incomplete)
            assertEquals(1, result.chargers.size)
            assertEquals(2, result.allCandidates.size)
            assertTrue(result.allCandidates.any { it.name == "Bilinmeyen B" })
            assertTrue(result.allCandidates.any {
                it.confidence == ChargerConfidence.UNKNOWN })
            assertFalse(result.allCandidates.any { it.powerKw.verified })
        } finally {
            dir.deleteRecursively()
        }
    }
}
