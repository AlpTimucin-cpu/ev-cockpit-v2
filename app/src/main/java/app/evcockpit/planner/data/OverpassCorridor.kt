package app.evcockpit.planner.data

import app.evcockpit.planner.route.RoutePoint
import app.evcockpit.planner.route.RouteDistance
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.planner.model.ModelParameters
import app.evcockpit.planner.charging.ChargerCandidate
import kotlin.math.*

/** Douglas–Peucker perpendicular tolerance, then ≤90 km and ≤30 vertices per public query. */
class CorridorChunker(
    val toleranceMeters:Double=400.0,
    val maxChunkMeters:Double=90_000.0,
    val maxVertices:Int=30,
) {
    init { require(toleranceMeters>0 && maxChunkMeters>0 && maxVertices>=2) }
    private fun distanceFromSegment(p:RoutePoint,a:RoutePoint,b:RoutePoint):Double {
        val scale=111_320.0
        val cosLatitude=cos(Math.toRadians((a.lat+b.lat)/2))
        val bx=(b.lon-a.lon)*cosLatitude*scale
        val by=(b.lat-a.lat)*scale
        val px=(p.lon-a.lon)*cosLatitude*scale
        val py=(p.lat-a.lat)*scale
        val d=bx*bx+by*by
        val t=if(d==0.0)0.0 else ((px*bx+py*by)/d).coerceIn(0.0,1.0)
        return hypot(px-t*bx,py-t*by)
    }
    fun simplify(points:List<RoutePoint>):List<RoutePoint> {
        require(points.size>=2)
        val retain=BooleanArray(points.size)
        retain[0]=true;retain[retain.lastIndex]=true
        fun recurse(a:Int,b:Int) {
            if(b-a<=1)return
            var max=-1.0;var index=-1
            for(i in a+1 until b) {
                val d=distanceFromSegment(points[i],points[a],points[b])
                if(d>max){max=d;index=i}
            }
            if(max>toleranceMeters) {
                retain[index]=true;recurse(a,index);recurse(index,b)
            }
        }
        recurse(0,points.lastIndex)
        return points.filterIndexed { index,_->retain[index] }
    }
    fun chunks(points:List<RoutePoint>):List<List<RoutePoint>> {
        val cleaned=simplify(points)
        val expanded=mutableListOf(cleaned.first())
        // Densification avoids gaps when DP reduces straight hundreds of km to one segment.
        for((a,b) in cleaned.zipWithNext()) {
            val count=ceil((b.distanceMeters-a.distanceMeters)/10_000.0).toInt().coerceAtLeast(1)
            for(j in 1..count) {
                val f=j.toDouble()/count
                expanded+=RoutePoint(a.lat+(b.lat-a.lat)*f,a.lon+(b.lon-a.lon)*f,
                    a.distanceMeters+(b.distanceMeters-a.distanceMeters)*f)
            }
        }
        val out=mutableListOf<List<RoutePoint>>()
        var current=mutableListOf(expanded.first())
        for(p in expanded.drop(1)) {
            if(current.size>=2 && (p.distanceMeters-current.first().distanceMeters>maxChunkMeters ||
                current.size>=maxVertices)) {
                out+=current.toList()
                current=mutableListOf(current.last())
            }
            current+=p
        }
        if(current.size>=2)out+=current
        require(out.isNotEmpty())
        return out
    }
    fun query(chunk:List<RoutePoint>,radiusMeters:Int=3000):String {
        require(chunk.size in 2..maxVertices && radiusMeters in 1000..10000)
        val pairs=chunk.joinToString(",") {
            "%.5f,%.5f".format(java.util.Locale.US,it.lat,it.lon)
        }
        val q="[out:json][timeout:25];nwr[\"amenity\"=\"charging_station\"]"+
            "(around:$radiusMeters,$pairs);out center tags;"
        require(q.length<=4500)
        return q
    }
}
data class RouteProjection(val distanceAlongMeters:Double,val awayMeters:Double,
    val detourKm:Double,val detourMinutes:Double, val note:String="TAHMİN — rota sapması doğrulanmadı")
object RouteProjectionEstimator {
    fun project(charger:ChargerCandidate,route:List<RoutePoint>,
        params:ModelParameters=ModelParameters()):RouteProjection {
        require(route.size>=2 && params.detourSpeedKmh.value>0)
        val point=GeoPoint(charger.latitude,charger.longitude)
        var best=Double.POSITIVE_INFINITY;var along=0.0
        for((a,b) in route.zipWithNext()) {
            val start=GeoPoint(a.lat,a.lon);val end=GeoPoint(b.lat,b.lon)
            // local tangent approximation only; final detour MUST be rerouted via route provider.
            val e=RouteDistance.haversine(start,end)
            val ac=RouteDistance.haversine(start,point)
            val bc=RouteDistance.haversine(end,point)
            val t=if(e==0.0)0.0 else
                ((ac*ac+e*e-bc*bc)/(2*e*e)).coerceIn(0.0,1.0)
            val approximate=GeoPoint(a.lat+(b.lat-a.lat)*t,
                a.lon+(b.lon-a.lon)*t)
            val lateral=RouteDistance.haversine(approximate,point)
            if(lateral<best) {
                best=lateral
                along=a.distanceMeters+(b.distanceMeters-a.distanceMeters)*t
            }
        }
        val detourKm=best*2/1000.0
        return RouteProjection(along,best,detourKm,detourKm/
            params.detourSpeedKmh.value*60.0)
    }
}
