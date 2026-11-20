package app.evcockpit.planner.route

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import kotlin.math.*

data class RoutePoint(val lat: Double,val lon: Double,val distanceMeters: Double,
    val elevationMeters: Double? = null, val speedKmh: Double? = null)
data class RouteProfile(val points: List<RoutePoint>,val slopeKnown: Boolean,
    val slopeWarning: String, val rawClimbMeters: Double, val smoothedClimbMeters: Double,
    val durationHours: Double, val source: String = "TAHMİN / DOĞRULANMADI")
object RouteDistance {
    private const val EARTH_RADIUS_M=6371000.0 // WGS84 spherical approximation
    fun haversine(a:GeoPoint,b:GeoPoint):Double {
        val dLat=Math.toRadians(b.latitude-a.latitude)
        val dLon=Math.toRadians(b.longitude-a.longitude)
        val h=sin(dLat/2).pow(2)+cos(Math.toRadians(a.latitude))*
            cos(Math.toRadians(b.latitude))*sin(dLon/2).pow(2)
        return 2*EARTH_RADIUS_M*asin(sqrt(h.coerceIn(0.0,1.0)))
    }
}
class RouteSampler(
    private val preferredStepMeters:Double=400.0,
    private val maxPoints:Int=4000,
) {
    fun sample(path:List<GeoPoint>):List<RoutePoint> {
        require(path.size>=2 && preferredStepMeters>0 && maxPoints>=2)
        val distance=path.zipWithNext().map { RouteDistance.haversine(it.first,it.second) }
        val total=distance.sum()
        require(total>0)
        val step=max(preferredStepMeters,total/(maxPoints-1))
        val count=min(maxPoints,ceil(total/step).toInt()+1).coerceAtLeast(2)
        val cumulative=buildList { add(0.0);distance.forEach { add(last()+it) } }
        var i=0
        return (0 until count).map { n ->
            val s=if(n==count-1)total else total*n/(count-1)
            while(i<distance.lastIndex && cumulative[i+1]<s) i++
            val a=path[i];val b=path[i+1];val d=distance[i]
            val t=if(d==0.0)0.0 else ((s-cumulative[i])/d).coerceIn(0.0,1.0)
            RoutePoint(a.latitude+(b.latitude-a.latitude)*t,
                a.longitude+(b.longitude-a.longitude)*t,s)
        }
    }
}
object ElevationSmoother {
    fun smooth(values:List<Double>, windowRadius:Int=1):List<Double> {
        require(windowRadius>=0 && values.all { it.isFinite() })
        return values.indices.map { i ->
            val a=(i-windowRadius).coerceAtLeast(0)
            val b=(i+windowRadius).coerceAtMost(values.lastIndex)
            values.subList(a,b+1).average()
        }
    }
    fun climb(values:List<Double>):Double = values.zipWithNext()
        .sumOf { (a,b)->(b-a).coerceAtLeast(0.0) }
}
data class SpeedPlan(val speedsKmh:List<Double>,val hours:Double,val note:String)
object SpeedProfileBuilder {
    fun fromRoute(points:List<RoutePoint>,providerDistanceMeters:Double,
        providerDurationSeconds:Double,maxCruiseKmh:Double=120.0,
        percentage:Double=100.0):SpeedPlan {
        require(providerDistanceMeters>0 && providerDurationSeconds>0 && maxCruiseKmh>0)
        require(percentage in 20.0..150.0 && points.size>=2)
        // No road class returned by existing provider: don't claim statutory road limits.
        val providerAverage=providerDistanceMeters/providerDurationSeconds*3.6
        val speed=min(maxCruiseKmh,providerAverage*percentage/100.0).coerceAtLeast(5.0)
        val intervals=points.zipWithNext().map { (a,b)->(b.distanceMeters-a.distanceMeters)/1000.0 }
        return SpeedPlan(List(intervals.size){speed},
            intervals.sumOf { it/speed },
            "Sağlayıcı toplam mesafe/süre ortalaması; yol sınıfı/gerçek hız DOĞRULANMADI")
    }
}
interface ElevationClient {
    suspend fun elevations(points:List<RoutePoint>):List<Double>
}
class RouteProfileAssembler(
    private val elevationClient:ElevationClient,
    private val sampler:RouteSampler=RouteSampler(),
) {
    suspend fun prepare(route:NavigationRoute,maxSpeedKmh:Double=120.0):RouteProfile {
        val points=sampler.sample(route.geometry)
        val native=route.elevationsMeters
        val fromNative=if(native!=null && native.size==route.geometry.size) {
            val orig=route.geometry.zip(native).map { (p,h)->p to h }
            val cumulative=buildList {
                add(0.0)
                orig.zipWithNext().forEach { (a,b)->
                    add(last()+RouteDistance.haversine(a.first,b.first))
                }
            }
            var j=0
            val total=cumulative.last()
            points.map { point ->
                val target=if(points.last().distanceMeters==0.0)0.0
                    else point.distanceMeters/points.last().distanceMeters*total
                while(j<cumulative.size-2 && cumulative[j+1]<target)j++
                val span=cumulative[j+1]-cumulative[j]
                val t=if(span==0.0)0.0 else ((target-cumulative[j])/span).coerceIn(0.0,1.0)
                native[j]+(native[j+1]-native[j])*t
            }
        } else null
        val elevations=fromNative ?: runCatching { elevationClient.elevations(points) }.getOrNull()
        val usable=elevations?.takeIf { it.size==points.size && it.all { v->v.isFinite() } }
        val smoothed=usable?.let { ElevationSmoother.smooth(it,1) }
        val speed=SpeedProfileBuilder.fromRoute(points,route.distanceMeters,route.durationSeconds,maxSpeedKmh)
        return RouteProfile(points.mapIndexed { index,p ->
            p.copy(elevationMeters=smoothed?.get(index),
                speedKmh=speed.speedsKmh.getOrNull(index))
        },usable!=null,
            if(usable==null)"EĞİM VERİSİ YOK — tahmin zayıf"
            else if(fromNative!=null)"ORS yükseklik verisi — sahada DOĞRULANMADI"
            else "Open-Meteo DEM — sahada DOĞRULANMADI",
            usable?.let(ElevationSmoother::climb) ?: 0.0,
            smoothed?.let(ElevationSmoother::climb) ?: 0.0,speed.hours)
    }
}
