package app.evcockpit.ui.unified.map

import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap

enum class FollowMode { OFF, FOLLOW, FOLLOW_BEARING }

/**
 * Tek MapLibreMap kamera denetleyicisi. Gesture -> OFF; GPS yoksa hareket yok.
 * Panel üst/alt doluluğu px olarak gelir; Compose dp→px dönüşümü UI'dadır.
 */
class CameraController(private val map: MapLibreMap) {
    private val t = CockpitDesignTokens
    var mode: FollowMode = FollowMode.OFF
        private set
    private val gesture = MapLibreMap.OnCameraMoveStartedListener { reason ->
        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE)
            mode = FollowMode.OFF
    }

    init { map.addOnCameraMoveStartedListener(gesture) }

    fun setMode(next: FollowMode) { mode = next }

    fun follow(fix: OverlayVehicle) {
        if (mode == FollowMode.OFF || !fix.point.valid()) return
        val current = map.cameraPosition
        val bearing = if (mode == FollowMode.FOLLOW_BEARING)
            fix.bearingDegrees?.takeIf { it.isFinite() } ?: current.bearing
        else current.bearing
        val camera = CameraPosition.Builder()
            .target(LatLng(fix.point.latitude, fix.point.longitude))
            .zoom(current.zoom)
            .bearing(bearing)
            .tilt(current.tilt)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera),
            t.cameraAnimationMs)
    }

    fun fitRoute(
        coordinates: List<OverlayPoint>,
        topPaddingPx: Int,
        bottomPaddingPx: Int,
        leftPaddingPx: Int = 0,
        rightPaddingPx: Int = 0,
    ) {
        val valid = coordinates.filter { it.valid() }
        if (valid.size < 2) return
        val bounds = LatLngBounds.Builder().apply {
            valid.forEach { include(LatLng(it.latitude, it.longitude)) }
        }.build()
        // Kullanıcı haritayı elle sürüklediğinde devam eden takip yeniden açılmaz.
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(
            bounds,
            leftPaddingPx.coerceAtLeast(0),
            topPaddingPx.coerceAtLeast(0),
            rightPaddingPx.coerceAtLeast(0),
            bottomPaddingPx.coerceAtLeast(0),
        ), t.cameraAnimationMs)
    }

    fun dispose() {
        map.removeOnCameraMoveStartedListener(gesture)
    }
}
