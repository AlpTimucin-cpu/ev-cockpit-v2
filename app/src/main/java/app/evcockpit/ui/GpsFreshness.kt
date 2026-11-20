package app.evcockpit.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import app.evcockpit.location.GpsFixAgePolicy
import app.evcockpit.location.GpsSnapshot
import kotlinx.coroutines.delay

/**
 * Infrequent one-shot invalidation: if GPS updates stop, stale speed/remaining
 * distance disappear after 15 s *without* requiring a new GPS callback.
 *
 * No polling, network requests, background service, or new permissions.
 */
@Composable
fun rememberGpsFreshness(gps: GpsSnapshot): Boolean {
    val fresh by produceState(
        initialValue = gps.isFresh(SystemClock.elapsedRealtime()),
        key1 = gps.receivedElapsedRealtimeMs,
        key2 = gps.point,
    ) {
        val now = SystemClock.elapsedRealtime()
        value = gps.isFresh(now)
        if (value) {
            val remaining = GpsFixAgePolicy.timeUntilStaleMs(
                now, gps.receivedElapsedRealtimeMs,
            )
            if (remaining > 0L) delay(remaining)
            value = false
        }
    }
    return fresh
}
