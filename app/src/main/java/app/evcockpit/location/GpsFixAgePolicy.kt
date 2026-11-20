package app.evcockpit.location

/**
 * The platform can deliver a delayed cached fix via LocationManager. Treat
 * the fix's *monotonic acquisition time* as authoritative, not callback time.
 *
 * The elapsedRealtime clock includes device sleep; wall clock is never used.
 */
object GpsFixAgePolicy {
    const val MAX_AGE_MS = 15_000L
    private const val FUTURE_TOLERANCE_MS = 2_000L

    /** Returns the monotonic acquisition timestamp or null for an invalid/old fix. */
    fun acceptedTimestampMs(nowElapsedMs: Long, fixElapsedNanos: Long): Long? {
        if (nowElapsedMs <= 0L || fixElapsedNanos <= 0L) return null
        val fixMs = fixElapsedNanos / 1_000_000L
        if (fixMs <= 0L) return null
        if (fixMs > nowElapsedMs) {
            if (fixMs - nowElapsedMs > FUTURE_TOLERANCE_MS) return null
            // Nanosecond-to-millisecond conversion / reporting jitter is tolerated.
            return nowElapsedMs
        }
        if (nowElapsedMs - fixMs > MAX_AGE_MS) return null
        return fixMs
    }

    /** Delay until the first instant this fix is considered stale. */
    fun timeUntilStaleMs(nowElapsedMs: Long, timestampMs: Long): Long {
        if (timestampMs <= 0L || nowElapsedMs < timestampMs) return 0L
        val elapsed = nowElapsedMs - timestampMs
        return if (elapsed > MAX_AGE_MS) 0L else MAX_AGE_MS - elapsed + 1L
    }
}
