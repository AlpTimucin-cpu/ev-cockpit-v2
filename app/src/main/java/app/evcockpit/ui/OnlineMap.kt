package app.evcockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import kotlinx.coroutines.launch
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
import app.evcockpit.charging.ocm.OcmStation
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import kotlin.math.abs
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import app.evcockpit.ui.unified.map.LocalSharedMapHost
import app.evcockpit.ui.unified.map.MapHostViewport
import app.evcockpit.ui.unified.map.MapHostLoadState
import org.maplibre.android.maps.MapLibreMap
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.RouteProgress
import app.evcockpit.ui.tesla.RoutePolylineProgress

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
    onLoadFailed: () -> Unit = {},
    onStyleReady: () -> Unit = {},
    canChangeLayer: Boolean = true,
    alternativeRoutes: List<NavigationRoute> = emptyList(),
    ocmStations: List<OcmStation> = emptyList(),
    onOcmStationSelected: (OcmStation) -> Unit = {},
    focusedOcmStation: OcmStation? = null,
) {
    val context = LocalContext.current
    val layerPreferences = remember(context) { MapLayerPreferences(context) }
    val layer by layerPreferences.selected.collectAsState(initial = MapLayer.DARK)
    val mapTilerStore = remember(context) { MapTilerKeyStore(context) }
    val mapTilerKey by mapTilerStore.key.collectAsState(initial = null)
    val styleUrl = MapStyleResolver.url(layer, mapTilerKey)
        ?: MapLayer.DARK.styleUrl
    val scope = rememberCoroutineScope()
    var followCamera by remember { mutableStateOf(false) }
    var tiltedCamera by remember { mutableStateOf(false) }
    var northUp by remember { mutableStateOf(false) }
    val layerMenu = remember { mutableStateOf(false) }
    val chosenDestination = rememberUpdatedState(onSelectDestination)
    val chosenStop = rememberUpdatedState(onStopSelected)
    val displayedStops = rememberUpdatedState(stopMarkers)
    val displayedOcmStations = rememberUpdatedState(ocmStations)
    val chosenOcmStation = rememberUpdatedState(onOcmStationSelected)
    val mapFailureCallback = rememberUpdatedState(onLoadFailed)
    val mapReadyCallback = rememberUpdatedState(onStyleReady)

    // Yalnızca MainActivity üzerindeki tek MapHost'un paylaşılan haritası.
    // Ekranlar MapView oluşturmaz, lifecycle başlatmaz veya yok etmez.
    val host = LocalSharedMapHost.current
    val slot = remember { Any() }
    val loadedMap = host?.readyMap?.takeIf { host.activeSlot === slot }

    LaunchedEffect(host, layer, styleUrl) {
        host?.requestStyle(layer, styleUrl)
    }
    LaunchedEffect(host?.loadState) {
        if (host?.loadState == MapHostLoadState.ERROR) mapFailureCallback.value()
    }
    LaunchedEffect(loadedMap, host?.appliedStyleVersion) {
        if (loadedMap != null) mapReadyCallback.value()
    }

    // Harita etkileşimleri ekrana özeldir ve panel terk edilince temizlenir.
    DisposableEffect(loadedMap) {
        val current = loadedMap ?: return@DisposableEffect onDispose { }
        val longPress = MapLibreMap.OnMapLongClickListener { location ->
            chosenDestination.value(GeoPoint(location.latitude, location.longitude))
            true
        }
        current.addOnMapLongClickListener(longPress)
        current.setOnMarkerClickListener { marker ->
            val charger = displayedStops.value.firstOrNull {
                abs(it.latitude - marker.position.latitude) < 0.00001 &&
                abs(it.longitude - marker.position.longitude) < 0.00001
            }
            if (charger != null) {
                chosenStop.value(charger)
                true
            } else {
                val ocm = displayedOcmStations.value.firstOrNull {
                    abs(it.point.latitude - marker.position.latitude) < 0.00001 &&
                    abs(it.point.longitude - marker.position.longitude) < 0.00001
                }
                if (ocm != null) {
                    chosenOcmStation.value(ocm)
                    true
                } else false
            }
        }
        onDispose {
            current.removeOnMapLongClickListener(longPress)
            current.setOnMarkerClickListener { false }
        }
    }
    LaunchedEffect(loadedMap, focusedStop) {
        if(loadedMap!=null && focusedStop!=null) {
            followCamera = false
            loadedMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(focusedStop.latitude,focusedStop.longitude),12.0))
        }
    }
    LaunchedEffect(loadedMap, focusedOcmStation) {
        if (loadedMap != null && focusedOcmStation != null) {
            followCamera = false
            loadedMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(focusedOcmStation.point.latitude, focusedOcmStation.point.longitude), 13.0))
        }
    }
    val gpsFresh = rememberGpsFreshness(gps)
    // Takip kapalıyken de 2B/3B değişimi mevcut harita merkezinde uygulanır.
    LaunchedEffect(loadedMap, tiltedCamera, northUp) {
        val map = loadedMap ?: return@LaunchedEffect
        if (followCamera) return@LaunchedEffect
        val current = map.cameraPosition
        val camera = CameraPosition.Builder()
            .target(current.target)
            .zoom(current.zoom)
            .bearing(if (northUp) 0.0 else current.bearing)
            .tilt(if (tiltedCamera) 45.0 else 0.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
    }
    // Takip isteğe bağlıdır; GPS güncel değilse kamera sahte konuma gitmez.
    // GNSS course hareket yönüdür; pusula okumak gibi yorumlanmaz.
    LaunchedEffect(loadedMap, gps.point, gpsFresh, gps.courseDegrees,
        followCamera, tiltedCamera, northUp) {
        val map = loadedMap ?: return@LaunchedEffect
        val point = gps.point ?: return@LaunchedEffect
        if (!followCamera || !gpsFresh) return@LaunchedEffect
        val camera = CameraPosition.Builder()
            .target(LatLng(point.latitude, point.longitude))
            .zoom(15.0)
            .bearing(if (northUp) 0.0 else gps.courseDegrees ?: 0.0)
            .tilt(if (tiltedCamera) 45.0 else 0.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
    }
    // Rota değiştiğinde kamera sadece GERÇEK seçilen yol geometrisini çerçeveler.
    LaunchedEffect(loadedMap, route) {
        val map = loadedMap ?: return@LaunchedEffect
        val path = route?.geometry?.takeIf { it.size >= 2 } ?: return@LaunchedEffect
        if (followCamera) return@LaunchedEffect
        runCatching {
            val bounds = LatLngBounds.Builder()
            val step = (path.size / 1_000).coerceAtLeast(1)
            path.filterIndexed { index, _ -> index % step == 0 || index == path.lastIndex }
                .forEach { point -> bounds.include(LatLng(point.latitude, point.longitude)) }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 72))
        }
    }
    LaunchedEffect(loadedMap, route, alternativeRoutes, destination, stopMarkers,
        ocmStations, gps.point, gpsFresh) {
        val currentMap = loadedMap ?: return@LaunchedEffect
        var currentLine: Polyline? = null
        var passedLine: Polyline? = null
        var currentMarker: Marker? = null
        val chargingMarkers = mutableListOf<Marker>()
        val secondaryLines = mutableListOf<Polyline>()
        val ocmPins = mutableListOf<Marker>()
        try {
            // Alternatifler soluk; yalnız seçilen güzergâh canlı yeşildir.
            alternativeRoutes.filter { it != route }.take(2).forEach { alternative ->
                if (alternative.geometry.size >= 2) {
                    val step = (alternative.geometry.size / 2_500).coerceAtLeast(1)
                    val faint = alternative.geometry.filterIndexed { index, _ ->
                        index % step == 0 || index == alternative.geometry.lastIndex
                    }
                    secondaryLines += currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(faint.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(111, 129, 146))
                            .width(5f),
                    )
                }
            }
            if (route != null) {
                val position = if (gpsFresh && gps.point != null)
                    RouteProgress.locate(route, gps.point) else null
                val progress = if (position != null && route.distanceMeters > 0.0)
                    ((route.distanceMeters - position.remainingMeters) / route.distanceMeters)
                        .coerceIn(0.0, 1.0) else 0.0
                val parts = RoutePolylineProgress.split(route.geometry, progress)
                if (parts.passed.size >= 2) {
                    passedLine = currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(parts.passed.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(100, 110, 117))
                            .width(8f),
                    )
                }
                if (parts.remaining.size >= 2) {
                    currentLine = currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(parts.remaining.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(61, 220, 132))
                            .width(8f),
                    )
                }
            }
            if (destination != null) {
                currentMarker = currentMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(destination.latitude, destination.longitude))
                        .title("Seçilen hedef"),
                )
            }
            ocmStations.take(250).forEach { station ->
                // Pin renklendirmesi rapor edilen istasyon gücünü gösterir; canlı durum DEĞİL.
                val fill = when {
                    station.powerKw == null -> android.graphics.Color.GRAY
                    station.powerKw >= 150.0 -> android.graphics.Color.rgb(173, 90, 225)
                    station.hasDc || station.powerKw >= 43.0 ->
                        android.graphics.Color.rgb(17, 205, 180)
                    station.hasAc -> android.graphics.Color.rgb(65, 157, 238)
                    else -> android.graphics.Color.GRAY
                }
                val bitmap = Bitmap.createBitmap(42, 42, Bitmap.Config.ARGB_8888)
                val draw = AndroidCanvas(bitmap)
                val brush = Paint(Paint.ANTI_ALIAS_FLAG)
                brush.color = android.graphics.Color.WHITE
                draw.drawCircle(21f, 21f, 20f, brush)
                brush.color = fill
                draw.drawCircle(21f, 21f, 16f, brush)
                val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
                ocmPins += currentMap.addMarker(MarkerOptions()
                    .position(LatLng(station.point.latitude, station.point.longitude))
                    .title(station.name.take(100))
                    .snippet("${station.powerGroup} • doluluk bilinmiyor")
                    .icon(icon))
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
            passedLine?.let { currentMap.removePolyline(it) }
            currentMarker?.let { currentMap.removeMarker(it) }
            chargingMarkers.forEach { currentMap.removeMarker(it) }
            ocmPins.forEach { currentMap.removeMarker(it) }
            secondaryLines.forEach { currentMap.removePolyline(it) }
        }
    }

    val position = if (route != null && gpsFresh &&
        gps.point != null
    ) RouteProgress.locate(route, gps.point) else null
    val maneuver = position?.nextManeuverIndex?.let { route?.maneuvers?.getOrNull(it) }
    Box(modifier) {
        MapHostViewport(
            modifier = Modifier.fillMaxSize(),
            requestedSlot = slot,
            unavailableMessage = "Harita başka bir bölümde görüntüleniyor.",
        )
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            OutlinedButton(onClick = { layerMenu.value = true }, enabled = canChangeLayer) {
                Text("Katman: " + layer.titleTr, fontSize = 14.sp, color = Color.White)
            }
            DropdownMenu(expanded = layerMenu.value,
                onDismissRequest = { layerMenu.value = false }) {
                MapLayer.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.titleTr + if (option == layer) " ✓" else "") },
                        enabled = MapStyleResolver.url(option, mapTilerKey) != null,
                        onClick = {
                            layerMenu.value = false
                            scope.launch { layerPreferences.choose(option) }
                        },
                    )
                }
            }
        }
        if (canChangeLayer) {
            Column(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .background(Color(0x99000000)).padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                OutlinedButton(
                    enabled = gpsFresh && gps.point != null,
                    onClick = { followCamera = !followCamera },
                ) { Text(if (followCamera) "Takibi kapat" else "Konumumu izle") }
                OutlinedButton(onClick = { tiltedCamera = !tiltedCamera }) {
                    Text(if (tiltedCamera) "2B görünüm" else "3B açı (45°)")
                }
                OutlinedButton(onClick = { northUp = !northUp }) {
                    Text(if (northUp) "Hareket yönü" else "Kuzey yukarı")
                }
                if (!gpsFresh || gps.point == null) {
                    Text("GPS güncel değil", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Text(
            when {
                route != null -> "OpenFreeMap • rota çizgisi (sahada doğrulanmadı)"
                destination != null -> "Hedef seçildi • Rota sekmesinde hesapla"
                else -> "OpenFreeMap • hedef seçmek için haritaya uzun bas"
            },
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.TopCenter)
                .background(Color(0xC9000000)).padding(8.dp),
        )
        if (weatherSummary.isNotBlank()) {
            Text(weatherSummary, color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopEnd)
                    .background(Color(0xD9000000)).padding(6.dp))
        }
        Text(MapAttribution.LABEL, color = Color.White, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomEnd)
                .background(Color(0xDD000000)).padding(horizontal = 7.dp, vertical = 4.dp))
        if (maneuver != null) {
            Text(
                "SONRAKİ MANEVRA • ${maneuver.instructionTr}\n" +
                    "Yaklaşık: ${position.distanceToManeuverMeters?.toInt() ?: "--"} m",
                color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomStart)
                    .background(Color(0xD9000000)).padding(10.dp),
            )
        }
    }
}
