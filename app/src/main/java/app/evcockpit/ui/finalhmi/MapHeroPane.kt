package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.evcockpit.ui.MapLayer
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * F1: Gerçek MapLibre temel haritası (OpenFreeMap stil).
 * GPS, adres sorgusu, rota veya şarj verisiyle henüz BAĞLANTISI YOK.
 * Haritada rastgele rota, işaretçi ve trafik rengi çizilmez.
 */
@Composable
fun MapHeroPane(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    onOpenRouteTab: () -> Unit = {},
) {
    val t = CockpitDesignTokens
    val preview = LocalInspectionMode.current
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var layer by remember { mutableStateOf(MapLayer.DARK) }
    var layerMenu by remember { mutableStateOf(false) }
    val map = remember { mutableStateOf<MapLibreMap?>(null) }

    // F1'de mevcut MapLibre Android SDK'sını kullanıyoruz; kaynak ağ bağlantısı
    // yalnız harita stil/karolarına aittir, araç veya kullanıcı GPS verisi gönderilmez.
    val mapView = remember(context, preview) {
        if (preview) null else {
            MapLibre.getInstance(context)
            MapView(context).apply {
                onCreate(null)
                getMapAsync { instance ->
                    map.value = instance
                    instance.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(39.0, 35.0), 5.2))
                    instance.setStyle(MapLayer.DARK.styleUrl)
                }
            }
        }
    }
    DisposableEffect(owner, mapView) {
        if (mapView == null) return@DisposableEffect onDispose { }
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (!started) { mapView.onStart(); started = true }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!resumed) { mapView.onResume(); resumed = true }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (resumed) { mapView.onPause(); resumed = false }
                }
                Lifecycle.Event.ON_STOP -> {
                    if (started) { mapView.onStop(); started = false }
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart(); started = true
        }
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume(); resumed = true
        }
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
            map.value = null
        }
    }
    LaunchedEffect(layer, map.value) {
        val active = map.value ?: return@LaunchedEffect
        if (layer.styleUrl.isNotBlank()) active.setStyle(layer.styleUrl)
    }

    Surface(
        modifier = modifier,
        shape = t.corner,
        color = t.mapFallback,
        border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (mapView != null) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(t.mapFallback),
                    contentAlignment = Alignment.Center) {
                    Text("MapLibre önizleme alanı\nRota ve istasyon işaretçisi yok",
                        color = t.secondary,
                        fontSize = t.bodyText)
                }
            }

            // Üstteki iki alan F1'de sahte adres sorgusu yapmaz; Rota sekmesine yönlendirir.
            Row(
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(t.mutedScrim)
                    .padding(t.spaceXS),
                horizontalArrangement = Arrangement.spacedBy(t.spaceXS),
            ) {
                OutlinedButton(
                    onClick = onOpenRouteTab,
                    modifier = Modifier.weight(1f).heightIn(min = t.target),
                    shape = t.cornerSmall,
                ) {
                    Text("Nereden  —", color = t.text, fontSize = t.bodyText,
                        maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenRouteTab,
                    modifier = Modifier.weight(1f).heightIn(min = t.target),
                    shape = t.cornerSmall,
                ) {
                    Text("Nereye  —", color = t.text, fontSize = t.bodyText,
                        maxLines = 1)
                }
            }

            // Gerçek manevra verisi F2'de bağlanacak; yoksa yön çizimi uydurulmaz.
            if (state.maneuverText.value != null &&
                state.maneuverText.timestampEpochMs != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart)
                        .padding(top = t.mapSummaryHeight, start = t.spaceS),
                    shape = t.cornerSmall,
                    color = t.mutedScrim,
                ) {
                    Column(Modifier.padding(t.spaceS)) {
                        Text("YÖNLENDİRME", color = t.accent, fontSize = t.labelText)
                        Text(state.maneuverText.value ?: "—", color = t.text)
                        Text(DashboardFormat.number(state.maneuverDistanceKm, " km sonra"),
                            color = t.secondary, fontSize = t.bodyText)
                    }
                }
            }

            Column(
                Modifier.align(Alignment.CenterStart)
                    .padding(t.spaceXS),
                verticalArrangement = Arrangement.spacedBy(t.spaceXS),
            ) {
                @Composable fun mapButton(
                    symbol: String,
                    description: String,
                    enabled: Boolean,
                    action: () -> Unit,
                ) {
                    OutlinedButton(
                        onClick = action,
                        enabled = enabled,
                        contentPadding = PaddingValues(t.spaceXXS),
                        shape = t.cornerSmall,
                        modifier = Modifier.sizeIn(minWidth = t.target, minHeight = t.target)
                            .semantics { contentDescription = description },
                    ) { Text(symbol, color = if (enabled) t.text else t.secondary,
                        fontSize = t.headText) }
                }
                mapButton("+", "Haritayı yakınlaştır", map.value != null) {
                    map.value?.animateCamera(CameraUpdateFactory.zoomIn())
                }
                mapButton("−", "Haritayı uzaklaştır", map.value != null) {
                    map.value?.animateCamera(CameraUpdateFactory.zoomOut())
                }
                mapButton("◎", "Konum (F3'te bağlanacak)", false) { }
                Box {
                    mapButton("▤", "Harita katmanını seç", true) { layerMenu = true }
                    DropdownMenu(
                        expanded = layerMenu,
                        onDismissRequest = { layerMenu = false },
                    ) {
                        val options = listOf(
                            "Sokak" to MapLayer.LIBERTY,
                            "Uydu" to MapLayer.SATELLITE,
                            "Arazi" to MapLayer.TOPOGRAPHIC,
                            "Koyu" to MapLayer.DARK,
                        )
                        options.forEach { (title, option) ->
                            DropdownMenuItem(
                                text = { Text(if (option.styleUrl.isBlank())
                                    "$title • API anahtarı F2" else title) },
                                enabled = option.styleUrl.isNotBlank(),
                                onClick = { layer = option; layerMenu = false },
                                modifier = Modifier.heightIn(min = t.target),
                            )
                        }
                    }
                }
            }

            Text(
                "Türkiye genel haritası • GPS konumu değil",
                color = t.secondary,
                fontSize = t.tinyText,
                modifier = Modifier.align(Alignment.Center).background(t.mutedScrim)
                    .padding(t.spaceXS),
            )

            // Harita altında tek yatay özet; fiyatı bilinmeyen durak asla 0 TL sayılmaz.
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(t.mutedScrim)
                    .padding(t.spaceXS),
                verticalArrangement = Arrangement.spacedBy(t.spaceXXS),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                    MapSummaryValue(
                        "Varış", DashboardFormat.number(state.arrivalMinutes, " dk"),
                        Modifier.weight(1f))
                    MapSummaryValue(
                        "Kalan", DashboardFormat.number(state.remainingDistanceKm, " km"),
                        Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                    MapSummaryValue(
                        "Varış SoC (tahmini)",
                        DashboardFormat.number(state.arrivalSocPercent, " %"),
                        Modifier.weight(1f))
                    MapSummaryValue(
                        "Şarj ücreti (tahmini)",
                        state.chargeTotalLabel.value ?: "—",
                        Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MapSummaryValue(label: String, value: String, modifier: Modifier) {
    val t = CockpitDesignTokens
    Column(modifier) {
        Text(label, color = t.secondary, fontSize = t.labelText, maxLines = 1)
        Text(value, color = t.text, fontSize = t.bodyText, maxLines = 1)
    }
}
