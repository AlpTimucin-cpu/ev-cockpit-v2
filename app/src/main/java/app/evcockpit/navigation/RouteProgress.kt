package app.evcockpit.navigation

import kotlin.math.cos
import kotlin.math.hypot

data class RoutePosition(
    val remainingMeters: Double,
    val nextManeuverIndex: Int?,
    val distanceToManeuverMeters: Double?,
)

/**
 * Approximate projection of a GPS coordinate on the route polyline.
 * If farther than 250m, returns null: no fabricated on-route progress.
 */
object RouteProgress {
    fun locate(route: NavigationRoute, point: GeoPoint, maxDistanceMeters: Double = 250.0): RoutePosition? {
        val geometry = route.geometry
        if (geometry.size < 2) return null
        val cumulative = DoubleArray(geometry.size)
        for (i in 1 until geometry.size) {
            cumulative[i] = cumulative[i - 1] + distance(geometry[i - 1], geometry[i])
        }
        val totalGeometry = cumulative.last()
        if (totalGeometry <= 0.0) return null

        var minDistance = Double.MAX_VALUE
        var along = 0.0
        for (i in 0 until geometry.lastIndex) {
            val a = geometry[i]
            val b = geometry[i + 1]
            val latitudeRadian = Math.toRadians((a.latitude + b.latitude) / 2.0)
            val metresPerLon = 111_320.0 * cos(latitudeRadian)
            val x = (point.longitude - a.longitude) * metresPerLon
            val y = (point.latitude - a.latitude) * 111_320.0
            val dx = (b.longitude - a.longitude) * metresPerLon
            val dy = (b.latitude - a.latitude) * 111_320.0
            val lengthSquared = dx * dx + dy * dy
            if (lengthSquared <= 0.0) continue
            val fraction = ((x * dx + y * dy) / lengthSquared).coerceIn(0.0, 1.0)
            val away = hypot(x - dx * fraction, y - dy * fraction)
            if (away < minDistance) {
                minDistance = away
                along = cumulative[i] + (cumulative[i + 1] - cumulative[i]) * fraction
            }
        }
        if (minDistance > maxDistanceMeters) return null

        val nextIndex = route.maneuvers.indexOfFirst { step ->
            cumulative.getOrNull(step.geometryIndex)?.let { it + 5.0 >= along } == true
        }.takeIf { it >= 0 }
        val nextDistance = nextIndex?.let { i ->
            (cumulative[route.maneuvers[i].geometryIndex] - along).coerceAtLeast(0.0)
        }
        return RoutePosition(
            remainingMeters = (route.distanceMeters * (totalGeometry - along) / totalGeometry)
                .coerceIn(0.0, route.distanceMeters),
            nextManeuverIndex = nextIndex,
            distanceToManeuverMeters = nextDistance,
        )
    }

    private fun distance(a: GeoPoint, b: GeoPoint): Double {
        val latRad = Math.toRadians((a.latitude + b.latitude) / 2.0)
        return hypot(
            (b.longitude - a.longitude) * 111_320.0 * cos(latRad),
            (b.latitude - a.latitude) * 111_320.0,
        )
    }
}
