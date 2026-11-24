package app.evcockpit.domain.nav

import kotlin.math.*

/** Saf koordinat türü, Android/Compose/MapLibre bağımlılığı yok. */
data class WgsPoint(val lat: Double, val lon: Double)

object PolylineGeometry {
    const val EARTH_RADIUS_M = 6_371_000.0

    /**
     * Douglas–Peucker. Metrik uzaklık yerel equirectangular projeksiyonla
     * yaklaşık hesaplanır; orijinal rota bu fonksiyonla değiştirilemez.
     */
    fun simplify(original: List<WgsPoint>, toleranceMeters: Double): List<WgsPoint> {
        if (original.size < 3 || !toleranceMeters.isFinite() ||
            toleranceMeters <= 0.0) return original
        val keep = BooleanArray(original.size)
        keep[0] = true
        keep[original.lastIndex] = true
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(0 to original.lastIndex)
        while (stack.isNotEmpty()) {
            val (start, end) = stack.removeLast()
            if (end <= start + 1) continue
            var maxDistance = -1.0
            var index = -1
            for (i in start + 1 until end) {
                val distance = distanceToSegment(original[i], original[start], original[end])
                if (distance > maxDistance) {
                    maxDistance = distance
                    index = i
                }
            }
            if (maxDistance > toleranceMeters && index in start + 1 until end) {
                keep[index] = true
                stack.addLast(start to index)
                stack.addLast(index to end)
            }
        }
        return original.filterIndexed { index, _ -> keep[index] }
    }

    private fun distanceToSegment(p: WgsPoint, a: WgsPoint, b: WgsPoint): Double {
        val cosLat = cos(Math.toRadians((a.lat + b.lat) / 2.0))
        fun x(q: WgsPoint) = Math.toRadians(q.lon - a.lon) * cosLat * EARTH_RADIUS_M
        fun y(q: WgsPoint) = Math.toRadians(q.lat - a.lat) * EARTH_RADIUS_M
        val bx = x(b)
        val by = y(b)
        val px = x(p)
        val py = y(p)
        val denominator = bx * bx + by * by
        val t = if (denominator <= 1e-8) 0.0
            else ((px * bx + py * by) / denominator).coerceIn(0.0, 1.0)
        return hypot(px - t * bx, py - t * by)
    }

    /** 0/360 geçişinde yön ani 180° sapmasın: sin/cos dairesel ortalama. */
    fun circularHeadingDegrees(previous: Double?, incoming: Double, weight: Double): Double? {
        if (!incoming.isFinite() || !weight.isFinite() || weight !in 0.0..1.0) return null
        if (previous == null || !previous.isFinite()) return normalize(incoming)
        val theta1 = Math.toRadians(previous)
        val theta2 = Math.toRadians(incoming)
        val sx = (1.0 - weight) * cos(theta1) + weight * cos(theta2)
        val sy = (1.0 - weight) * sin(theta1) + weight * sin(theta2)
        return if (abs(sx) + abs(sy) < 1e-10) normalize(incoming)
            else normalize(Math.toDegrees(atan2(sy, sx)))
    }

    private fun normalize(angle: Double): Double = ((angle % 360.0) + 360.0) % 360.0
}
