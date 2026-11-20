package app.evcockpit.location

import android.location.Location
import android.os.SystemClock
import app.evcockpit.navigation.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow

data class GpsSnapshot(
    val point: GeoPoint? = null,
    val speedKmh: Double? = null,
    val accuracyMeters: Float? = null,
    val provider: String? = null,
    val receivedElapsedRealtimeMs: Long = 0L,
    val active: Boolean = false,
    val status: String = "Konum takibi başlatılmadı",
) {
    fun isFresh(nowElapsedRealtimeMs: Long): Boolean =
        point != null && receivedElapsedRealtimeMs > 0L &&
            nowElapsedRealtimeMs - receivedElapsedRealtimeMs in 0L..GpsFixAgePolicy.MAX_AGE_MS
}

class LocationRepository {
    val state = MutableStateFlow(GpsSnapshot())

    fun starting() {
        state.value = GpsSnapshot(active = true, status = "GPS konumu bekleniyor")
    }

    fun failure(text: String) {
        state.value = GpsSnapshot(status = text)
    }

    fun stop() {
        state.value = GpsSnapshot(status = "Konum takibi durduruldu")
    }

    fun update(location: Location) {
        val now = SystemClock.elapsedRealtime()
        // A delayed/cached LocationManager callback must never be stamped "fresh".
        val fixTime = GpsFixAgePolicy.acceptedTimestampMs(
            now, location.elapsedRealtimeNanos,
        ) ?: return
        val previous = state.value
        // Prefer fresh GNSS data over coarse network fixes.
        if (location.provider != android.location.LocationManager.GPS_PROVIDER &&
            previous.provider == android.location.LocationManager.GPS_PROVIDER &&
            previous.isFresh(now)
        ) return

        val point = runCatching { GeoPoint(location.latitude, location.longitude) }.getOrNull()
            ?: return
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        val speed = if (location.hasSpeed() && location.speed.isFinite() &&
            location.speed in 0f..83.333f && (accuracy == null || accuracy <= 100f)
        ) location.speed.toDouble() * 3.6 else null
        state.value = GpsSnapshot(
            point = point,
            speedKmh = speed,
            accuracyMeters = accuracy,
            provider = location.provider,
            receivedElapsedRealtimeMs = fixTime,
            active = true,
            status = if (location.provider == android.location.LocationManager.GPS_PROVIDER) {
                "GPS sinyali alındı (doğruluk: ${accuracy?.toInt() ?: "?"} m)"
            } else "Yaklaşık ağ konumu; kesin GPS değil",
        )
    }
}
