package app.evcockpit.planner.data

import app.evcockpit.planner.route.RoutePoint
import app.evcockpit.planner.charging.*
import app.evcockpit.planner.model.SourcedValue
import kotlinx.coroutines.runBlocking
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
}
