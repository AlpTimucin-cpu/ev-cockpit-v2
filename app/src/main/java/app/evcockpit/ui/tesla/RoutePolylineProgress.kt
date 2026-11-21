package app.evcockpit.ui.tesla

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.planner.route.RouteDistance

/** Visual-only split of an existing route: past = muted, remaining = high contrast. */
data class RoutePolylineParts(
    val passed: List<GeoPoint>,
    val remaining: List<GeoPoint>,
)

object RoutePolylineProgress {
    fun split(path: List<GeoPoint>, progress: Double): RoutePolylineParts {
        if (path.size < 2) return RoutePolylineParts(emptyList(), path)
        if (!progress.isFinite() || progress <= 0.0)
            return RoutePolylineParts(emptyList(), path)
        if (progress >= 1.0) return RoutePolylineParts(path, emptyList())
        val lengths = path.zipWithNext().map { (a,b) -> RouteDistance.haversine(a,b) }
        val total = lengths.sum()
        if (!total.isFinite() || total <= 0.0) return RoutePolylineParts(emptyList(), path)
        val target = progress * total
        var along = 0.0
        for (i in lengths.indices) {
            val next = along + lengths[i]
            if (next >= target && lengths[i] > 0.0) {
                val fraction = ((target - along) / lengths[i]).coerceIn(0.0,1.0)
                val a=path[i]; val b=path[i+1]
                val midpoint = GeoPoint(a.latitude + (b.latitude-a.latitude)*fraction,
                    a.longitude + (b.longitude-a.longitude)*fraction)
                return RoutePolylineParts(
                    passed=path.take(i+1)+midpoint,
                    remaining=listOf(midpoint)+path.drop(i+1),
                )
            }
            along=next
        }
        return RoutePolylineParts(path,emptyList())
    }
}
