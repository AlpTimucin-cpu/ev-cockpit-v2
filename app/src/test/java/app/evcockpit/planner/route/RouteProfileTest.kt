package app.evcockpit.planner.route

import app.evcockpit.navigation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RouteProfileTest {
    private val path=listOf(GeoPoint(38.0,35.0),GeoPoint(38.8,35.8))
    @Test fun samplerBoundsPointCountAndMonotonicDistance() {
        val p=RouteSampler(250.0,500).sample(path)
        assertTrue(p.size<=500)
        assertTrue(p.zipWithNext().all { it.first.distanceMeters<it.second.distanceMeters })
        assertTrue(p.last().distanceMeters>0)
    }
    @Test fun smoothingReducesFakeClimbButPreservesPeak() {
        val noisy=listOf(100.0,115.0,100.0,115.0,180.0,115.0,100.0,115.0,100.0)
        val smooth=ElevationSmoother.smooth(noisy)
        assertTrue(ElevationSmoother.climb(smooth)<ElevationSmoother.climb(noisy))
        assertTrue(smooth.max()>140.0)
    }
    @Test fun speedIsBoundedAndRouteTimeConsistent() {
        val pts=RouteSampler().sample(path)
        val plan=SpeedProfileBuilder.fromRoute(pts,120000.0,3600.0,110.0)
        assertTrue(plan.speedsKmh.all { it<=110.0 })
        assertTrue(plan.hours>0)
    }
    @Test fun elevationServiceFailureProducesExplicitWarning() = runBlocking {
        val f=object:ElevationClient {
            override suspend fun elevations(points:List<RoutePoint>):List<Double> {
                throw IllegalStateException("offline")
            }
        }
        val nav=NavigationRoute(path,120000.0,4000.0,emptyList())
        val result=RouteProfileAssembler(f).prepare(nav)
        assertFalse(result.slopeKnown)
        assertTrue(result.slopeWarning.contains("EĞİM VERİSİ YOK"))
    }
    @Test fun nativeElevationTakesPriorityOverRemote() = runBlocking {
        var calls=0
        val f=object:ElevationClient {
            override suspend fun elevations(points:List<RoutePoint>):List<Double> {
                calls++;return points.map{0.0}
            }
        }
        val nav=NavigationRoute(path,120000.0,4000.0,emptyList(),
            elevationsMeters=listOf(100.0,500.0))
        val result=RouteProfileAssembler(f).prepare(nav)
        assertEquals(0,calls)
        assertTrue(result.slopeKnown)
        assertTrue(result.rawClimbMeters>300.0)
    }
    @Test fun distanceIsPositiveAndNoRealWorldClaim() {
        assertTrue(RouteDistance.haversine(path[0],path[1])>0)
        assertEquals(0.0,RouteDistance.haversine(path[0],path[0]),0.0001)
    }
}
