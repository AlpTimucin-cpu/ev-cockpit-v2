package app.evcockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.awaitCancellation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.annotations.IconFactory
import app.evcockpit.planner.charging.ChargerCandidate
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import kotlin.math.abs
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.RouteProgress
import android.os.SystemClock

/**
 * MapLibre Native + OpenFreeMap public online tiles. No offline tile download.
 * Initial camera shows Türkiye; it is NOT a claimed GPS location.
 * Provider availability / phone rendering have not yet been device-verified.
 */
@Composable
fun OnlineMap(
    modifier: Modifier = Modifier,
    route: NavigationRoute?,
    destination: GeoPoint?,
    gps: GpsSnapshot,
    weatherSummary: String = "",
    onSelectDestination: (GeoPoint) -> Unit,
    stopMarkers: List<ChargerCandidate> = emptyList(),
    onStopSelected: (ChargerCandidate) -> Unit = {},
    focusedStop: ChargerCandidate? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapReady = remember { mutableStateOf<MapLibreMap?>(null) }
    val chosenDestination = rememberUpdatedState(onSelectDestination)
    val chosenStop = rememberUpdatedState(onStopSelected)
    val displayedStops = rememberUpdatedState(stopMarkers)
    val mapView = remember(context) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.addOnMapLongClickListener { location ->
                    chosenDestination.value(GeoPoint(location.latitude, location.longitude))
                    true
                }
                map.setOnMarkerClickListener { marker ->
                    val found = displayedStops.value.firstOrNull {
                        abs(it.latitude - marker.position.latitude) < 0.00001 &&
                        abs(it.longitude - marker.position.longitude) < 0.00001
                    }
                    if(found!=null) { chosenStop.value(found);true } else false
                }
                map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                    mapReady.value = map
                }
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.0, 35.0), 5.2))
            }
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        var destroyed = false
        fun destroyOnce() {
            if (!destroyed) {
                destroyed = true
                mapView.onDestroy()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> destroyOnce()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapReady.value = null
            if (!destroyed) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
                destroyOnce()
            }
        }
    }

    val loadedMap = mapReady.value
    LaunchedEffect(loadedMap, focusedStop) {
        if(loadedMap!=null && focusedStop!=null) {
            loadedMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(focusedStop.latitude,focusedStop.longitude),12.0))
        }
    }
    LaunchedEffect(loadedMap, route, destination, stopMarkers) {
        val currentMap = loadedMap ?: return@LaunchedEffect
        var currentLine: Polyline? = null
        var currentMarker: Marker? = null
        val chargingMarkers = mutableListOf<Marker>()
        try {
            if (route != null) {
                currentLine = currentMap.addPolyline(
                    PolylineOptions()
                        .addAll(route.geometry.map { LatLng(it.latitude, it.longitude) })
                        .color(android.graphics.Color.rgb(42, 160, 255))
                        .width(5f),
                )
            }
            if (destination != null) {
                currentMarker = currentMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(destination.latitude, destination.longitude))
                        .title("Seçilen hedef"),
                )
            }
            stopMarkers.forEach { station ->
                val color=PlannerMarkerPolicy.color(station)
                val bitmap=Bitmap.createBitmap(44,44,Bitmap.Config.ARGB_8888)
                val draw=AndroidCanvas(bitmap)
                val paint=Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color=android.graphics.Color.WHITE
                draw.drawCircle(22f,22f,21f,paint)
                paint.color=color
                draw.drawCircle(22f,22f,17f,paint)
                val icon=IconFactory.getInstance(context).fromBitmap(bitmap)
                chargingMarkers+=currentMap.addMarker(MarkerOptions()
                    .position(LatLng(station.latitude,station.longitude))
                    .title(PlannerMarkerPolicy.label(station).take(120))
                    .snippet("doluluk bilinmiyor • veri TAHMİN").icon(icon))
            }
            awaitCancellation()
        } finally {
            currentLine?.let { currentMap.removePolyline(it) }
            currentMarker?.let { currentMap.removeMarker(it) }
            chargingMarkers.forEach { currentMap.removeMarker(it) }
        }
    }

    val position = if (route != null && gps.isFresh(SystemClock.elapsedRealtime()) &&
        gps.point != null
    ) RouteProgress.locate(route, gps.point) else null
    val maneuver = position?.nextManeuverIndex?.let { route?.maneuvers?.getOrNull(it) }
    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Text(
            when {
                route != null -> "OpenFreeMap • rota çizgisi (sahada doğrulanmadı)"
                destination != null -> "Hedef seçildi • Rota sekmesinde hesapla"
                else -> "OpenFreeMap • hedef seçmek için haritaya uzun bas"
            },
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopStart)
                .background(Color(0xC9000000)).padding(8.dp),
        )
        if (weatherSummary.isNotBlank()) {
            Text(weatherSummary, color = Color.White, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopEnd)
                    .background(Color(0xD9000000)).padding(6.dp))
        }
        Text(MapAttribution.LABEL, color = Color.White, fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomEnd)
                .background(Color(0xDD000000)).padding(horizontal = 7.dp, vertical = 4.dp))
        if (maneuver != null) {
            Text(
                "SONRAKİ MANEVRA • ${maneuver.instructionTr}\n" +
                    "Yaklaşık: ${position.distanceToManeuverMeters?.toInt() ?: "--"} m",
                color = Color.White, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomStart)
                    .background(Color(0xD9000000)).padding(10.dp),
            )
        }
    }
}
