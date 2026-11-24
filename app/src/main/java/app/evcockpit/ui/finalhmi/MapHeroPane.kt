package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import android.view.Gravity
import kotlin.math.roundToInt
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.maps.Style
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.evcockpit.ui.MapLayer
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView

/** F1 yalnız temel haritayı açar; GPS, rota ve şarj istasyonu TAKLİDİ yapmaz. */
private enum class MapLoadState { LOADING, READY, ERROR }

@Composable
fun MapHeroPane(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    onOpenRouteTab: () -> Unit = {},
) {
    val t = CockpitDesignTokens
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val owner = LocalLifecycleOwner.current
    var layer by remember { mutableStateOf(MapLayer.DARK) }
    var layerMenu by remember { mutableStateOf(false) }
    var loadState by remember { mutableStateOf(MapLoadState.LOADING) }
    var issue by remember { mutableStateOf<String?>(null) }
    var fallbackUsed by remember { mutableStateOf(false) }
    var requestVersion by remember { mutableIntStateOf(0) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    // Harita görüntüsü F1'de gerçek bir ağ kaynağı kullanır.
    // Yüklenmezse siyah ekranı başarı sanmayız; açıklama ve yeniden deneme sunarız.
    val mapView = remember(context, inspection) {
        if (inspection) null else {
            MapLibre.getInstance(context)
            // Compose içinde varsayılan GLSurfaceView siyah kalabilmektedir.
            // TextureView, Compose katmanlarıyla aynı kompozisyon yüzeyinde çizilir.
            MapView(context, MapLibreMapOptions.createFromAttributes(context)
                .textureMode(true)).apply {
                onCreate(null)
                addOnDidFailLoadingMapListener(
                    MapView.OnDidFailLoadingMapListener { _ ->
                        if (!fallbackUsed && layer == MapLayer.DARK) {
                            fallbackUsed = true
                            issue = "Koyu harita yüklenemedi; Sokak katmanı denendi."
                            layer = MapLayer.LIBERTY
                        } else {
                            loadState = MapLoadState.ERROR
                            issue = "Harita veya karolar yüklenemedi. İnterneti denetle ya da tekrar dene."
                        }
                    },
                )
                getMapAsync { ready ->
                    // Logo ve bilgi atfı haritanın iki ayrı alt köşesinde görünür kalır.
                    // MapLibre kenar boşluğu PIXEL ister, dp'yi yalnız burada çeviririz.
                    val marginPx = (t.mapAttributionSafeInset.value *
                        context.resources.displayMetrics.density).roundToInt()
                    ready.uiSettings.apply {
                        setLogoGravity(Gravity.BOTTOM or Gravity.START)
                        setAttributionGravity(Gravity.BOTTOM or Gravity.END)
                        setLogoMargins(marginPx, 0, 0, marginPx)
                        setAttributionMargins(0, 0, marginPx, marginPx)
                    }
                    map = ready
                    // Başlangıç Türkiye'ye genel bakış; kullanıcının GPS konumu DEĞİL.
                    ready.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(39.0, 35.0), 5.2,
                    ))
                }
            }
        }
    }

    DisposableEffect(owner, mapView) {
        if (mapView == null) return@DisposableEffect onDispose { }
        var started = false
        var resumed = false
        val listener = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) {
                    mapView.onStart()
                    started = true
                }
                Lifecycle.Event.ON_RESUME -> if (!resumed) {
                    mapView.onResume()
                    resumed = true
                }
                Lifecycle.Event.ON_PAUSE -> if (resumed) {
                    mapView.onPause()
                    resumed = false
                }
                Lifecycle.Event.ON_STOP -> if (started) {
                    mapView.onStop()
                    started = false
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(listener)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart(); started = true
        }
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume(); resumed = true
        }
        onDispose {
            owner.lifecycle.removeObserver(listener)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
            map = null
        }
    }

    // Stil yükleme ASENKRON; callback gelmeden başarılı/boş harita demeyiz.
    // Aynı stili getMapAsync + LaunchedEffect içinden iki kez yüklemekten kaçınılır.
    LaunchedEffect(map, layer, requestVersion) {
        val active = map ?: return@LaunchedEffect
        if (layer.styleUrl.isBlank()) return@LaunchedEffect
        loadState = MapLoadState.LOADING
        if (!fallbackUsed) issue = null
        active.setStyle(layer.styleUrl) { style ->
            if (layer == MapLayer.DARK) {
                applyDarkRoadContrast(style)
            }
            loadState = MapLoadState.READY
            // Yedek katmana geçildiyse uyarı kaybolmasın.
            if (!fallbackUsed) issue = null
        }
        delay(15_000L)
        if (loadState == MapLoadState.LOADING) {
            loadState = MapLoadState.ERROR
            issue = "Harita 15 saniye içinde açılamadı. Yeniden dene."
        }
    }

    Surface(
        modifier = modifier,
        shape = t.corner,
        color = t.mapFallback,
        border = BorderStroke(t.cardOutlineWidth, t.outline),
    ) {
        // F1 önceki sürümde özet haritanın üzerine biniyor, OSM atfı ile
        // ve uzun fiyat etiketiyle çakışıyordu. Header/harita/özet ayrı alan.
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(t.panel)
                    .padding(t.spaceXS),
                horizontalArrangement = Arrangement.spacedBy(t.spaceXS),
            ) {
                listOf("Nereden  —", "Nereye  —").forEach { label ->
                    OutlinedButton(
                        onClick = onOpenRouteTab,
                        modifier = Modifier.weight(1f).heightIn(min = t.target),
                        shape = t.cornerSmall,
                        contentPadding = PaddingValues(horizontal = t.spaceXS),
                    ) {
                        Text(label, color = t.text, fontSize = t.bodyText,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f).background(t.mapFallback),
            ) {
                if (mapView != null) {
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "MapLibre önizleme\nGerçek rota yüklenmedi",
                            color = t.secondary, fontSize = t.bodyText,
                        )
                    }
                }

                // Kısa harita: 48dp × 3 düğme, tek satır ve SAĞ üst köşe.
                // F3 konum eylemi devre dışı olduğundan kısa moddan çıkarılır.
                // Bu yerleşim Ankara gibi merkez harita etiketlerini örtmez.
                if (maxHeight < t.mapControlsVerticalMinHeight) {
                    Row(
                        Modifier.align(Alignment.TopEnd)
                            .padding(horizontal = t.mapControlSpacing)
                            .background(t.mutedScrim, t.cornerSmall),
                        horizontalArrangement = Arrangement.spacedBy(t.mapControlSpacing),
                    ) {
                        MapToolButton("+", "Yakınlaştır", map != null, compact = true) {
                            map?.animateCamera(CameraUpdateFactory.zoomIn())
                        }
                        MapToolButton("−", "Uzaklaştır", map != null, compact = true) {
                            map?.animateCamera(CameraUpdateFactory.zoomOut())
                        }
                        MapLayerSelectButton(
                            compact = true,
                            layerMenu = layerMenu,
                            onMenuChange = { layerMenu = it },
                            onChoose = { candidate ->
                                fallbackUsed = false
                                issue = null
                                layer = candidate
                                requestVersion++
                            },
                        )
                    }
                } else {
                    Column(
                        Modifier.align(Alignment.CenterStart)
                            .padding(t.spaceXS)
                            .background(t.mutedScrim, t.cornerSmall),
                        verticalArrangement = Arrangement.spacedBy(t.spaceXS),
                    ) {
                        MapToolButton("+", "Yakınlaştır", map != null) {
                            map?.animateCamera(CameraUpdateFactory.zoomIn())
                        }
                        MapToolButton("−", "Uzaklaştır", map != null) {
                            map?.animateCamera(CameraUpdateFactory.zoomOut())
                        }
                        MapToolButton("◎", "Konum (F3 aşamasında)", false) { }
                        MapLayerSelectButton(
                            compact = false,
                            layerMenu = layerMenu,
                            onMenuChange = { layerMenu = it },
                            onChoose = { candidate ->
                                fallbackUsed = false
                                issue = null
                                layer = candidate
                                requestVersion++
                            },
                        )
                    }
                }

                if (!inspection && loadState != MapLoadState.READY) {
                    Surface(
                        color = t.mutedScrim,
                        shape = t.cornerSmall,
                        modifier = Modifier.align(Alignment.TopCenter)
                            .padding(t.spaceS),
                    ) {
                        Column(
                            Modifier.padding(t.spaceS),
                            verticalArrangement = Arrangement.spacedBy(t.spaceXXS),
                        ) {
                            Text(
                                if (loadState == MapLoadState.LOADING)
                                    "Harita yükleniyor…" else "Harita gösterilemiyor",
                                color = t.text, fontSize = t.bodyText,
                            )
                            if (loadState == MapLoadState.ERROR) {
                                OutlinedButton(
                                    onClick = {
                                        issue = null
                                        fallbackUsed = false
                                        requestVersion++
                                    },
                                    modifier = Modifier.heightIn(min = t.target),
                                ) { Text("Yeniden dene", fontSize = t.bodyText) }
                            }
                        }
                    }
                }

                issue?.let { message ->
                    Text(
                        message,
                        color = t.caution,
                        fontSize = t.labelText,
                        modifier = Modifier.align(Alignment.TopEnd)
                            .padding(t.spaceXS)
                            .background(t.mutedScrim, t.cornerSmall)
                            .padding(t.spaceXS),
                    )
                }

            }

            // Doğrudan harita üstüne çizilmez: uzun maliyet bilgisi satır atlayabilir.
            Column(
                Modifier.fillMaxWidth().background(t.panel)
                    .padding(t.spaceS),
                verticalArrangement = Arrangement.spacedBy(t.spaceS),
            ) {
                Text(
                    "Türkiye genel haritası • GPS konumu değil",
                    color = t.secondary,
                    fontSize = t.tinyText,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceM)) {
                    MapSummaryValue(
                        "Varış (tahmini)",
                        DashboardFormat.number(state.arrivalMinutes, " dk"),
                        Modifier.weight(1f),
                    )
                    MapSummaryValue(
                        "Kalan mesafe",
                        DashboardFormat.number(state.remainingDistanceKm, " km"),
                        Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceM)) {
                    MapSummaryValue(
                        "Varış SoC (tahmini)",
                        DashboardFormat.number(state.arrivalSocPercent, " %"),
                        Modifier.weight(1f),
                    )
                    MapSummaryValue(
                        "Şarj ücreti (tahmini)",
                        state.chargeTotalLabel.value ?: "—",
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 48dp kompakt dokunma hedefi, 56dp standart hedef.
 * Dış yarı saydam kaplamayla karanlık karo üstünde de okunabilir.
 */
@Composable
private fun MapToolButton(
    label: String,
    description: String,
    enabled: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val t = CockpitDesignTokens
    val targetSize = if (compact) t.mapControlTouchTarget else t.target
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = t.mutedScrim,
            disabledContainerColor = t.mutedScrim,
        ),
        contentPadding = PaddingValues(t.spaceXXS),
        shape = if (compact) t.mapControlVisualRadius else t.cornerSmall,
        modifier = Modifier.size(targetSize)
            .semantics { contentDescription = description },
    ) {
        Text(label, color = if (enabled) t.text else t.secondary,
            fontSize = t.headText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MapLayerSelectButton(
    compact: Boolean,
    layerMenu: Boolean,
    onMenuChange: (Boolean) -> Unit,
    onChoose: (MapLayer) -> Unit,
) {
    val t = CockpitDesignTokens
    Box {
        MapToolButton(
            "▤", "Harita katmanı", true, compact = compact,
        ) { onMenuChange(true) }
        DropdownMenu(
            expanded = layerMenu,
            onDismissRequest = { onMenuChange(false) },
        ) {
            listOf(
                "Sokak" to MapLayer.LIBERTY,
                "Uydu" to MapLayer.SATELLITE,
                "Arazi" to MapLayer.TOPOGRAPHIC,
                "Koyu" to MapLayer.DARK,
            ).forEach { (name, candidate) ->
                DropdownMenuItem(
                    text = {
                        Text(if (candidate.styleUrl.isBlank())
                            "$name • F2'de etkinleşecek" else name)
                    },
                    enabled = candidate.styleUrl.isNotBlank(),
                    modifier = Modifier.heightIn(min = t.target),
                    onClick = {
                        onMenuChange(false)
                        onChoose(candidate)
                    },
                )
            }
        }
    }
}

/**
 * OpenFreeMap DARK stilinin doğrulanan iki yol katmanında sınırlı renk iyileştirmesi.
 * Kaynak stil URL'si ve başka katmanlar değişmez. Stil yeniden yüklenince sıfırlanır.
 * Stil üreticisi katman adını değiştirirse sessizce atlanır.
 */
private fun applyDarkRoadContrast(style: Style) {
    val t = CockpitDesignTokens
    runCatching {
        (style.getLayer("highway_major_subtle") as? LineLayer)
            ?.setProperties(PropertyFactory.lineColor(t.mapRoadMajorContrast.toArgb()))
    }
    runCatching {
        (style.getLayer("highway_minor") as? LineLayer)
            ?.setProperties(PropertyFactory.lineColor(t.mapRoadMinorContrast.toArgb()))
    }
}

@Composable
private fun MapSummaryValue(label: String, value: String, modifier: Modifier) {
    val t = CockpitDesignTokens
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(t.spaceXXS),
    ) {
        Text(
            label, color = t.secondary, fontSize = t.labelText, maxLines = 2,
        )
        Text(
            value, color = t.text, fontSize = t.bodyText,
            maxLines = 3, overflow = TextOverflow.Ellipsis,
        )
    }
}
