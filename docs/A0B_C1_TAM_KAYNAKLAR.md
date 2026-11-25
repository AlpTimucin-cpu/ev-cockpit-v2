# A0b C1 — Tek MapHost / beş tam kaynak dosya

**Dal:** `feature/a0b-unified-map-20261124` · **Baz:** A0 0.7.0 · **PR:** #119

İnceleme: F1 MapHeroPane eski MapView oluşturucusu, OnlineMap ise diğer oluşturucuydu. RouteScreen, PlannerScreen ve ChargingExplorerScreen OnlineMap kullanıyor. MainActivity NavHost'u F1 içerir. Host Activity üzerinde kalır; MapView TextureView, 15sn timeout, DARK→LIBERTY yedeği, atıflar ve DEBUG kayıtları korunur. Bir sekmede iki harita istendiğinde yalnız biri MapView'ı alır, diğeri sahte harita çizmez. C2 overlay, C3 panel ve diğer C4–C8 işlevleri henüz uygulanmadı. CI ve fiziksel GPU testi ayrıca doğrulanmalıdır.


---

### 1. `app/src/main/java/app/evcockpit/ui/unified/map/MapHost.kt`

```kotlin
package app.evcockpit.ui.unified.map

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.evcockpit.BuildConfig
import app.evcockpit.ui.MapLayer
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.finalhmi.F1DarkRoadContrast
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import kotlin.math.roundToInt

/** Tek MapLibre durum makinesi; yol ve istasyon katmanları C2'de taşınacak. */
enum class MapHostLoadState { LOADING, READY, ERROR }

/**
 * Activity seviyesinde sahiplik. Yerel Android uygulama context'i yalnız MapLibre
 * başlatma için kullanılır; View, NavHost sekmeleri arasında yeniden yaratılmaz.
 * MapView ömrü tek bir LifecycleOwner ile yönetilir.
 */
class MapHostState internal constructor(context: Context) {
    private val appContext = context.applicationContext
    var map by mutableStateOf<MapLibreMap?>(null)
        private set
    var readyMap by mutableStateOf<MapLibreMap?>(null)
        private set
    var layer by mutableStateOf(MapLayer.DARK)
        private set
    var styleUrl by mutableStateOf(MapLayer.DARK.styleUrl)
        private set
    var version by mutableIntStateOf(0)
        private set
    var loadState by mutableStateOf(MapHostLoadState.LOADING)
        private set
    var issue by mutableStateOf<String?>(null)
        private set
    var fallbackUsed by mutableStateOf(false)
        private set
    var appliedStyleVersion by mutableIntStateOf(0)
        private set

    private val slots = LinkedHashSet<Any>()
    var activeSlot by mutableStateOf<Any?>(null)
        private set

    val mapView: MapView

    init {
        MapLibre.getInstance(appContext)
        // Bu, src/main/java altındaki MapView oluşturan TEK satırdır.
        mapView = MapView(
            appContext,
            MapLibreMapOptions.createFromAttributes(appContext).textureMode(true),
        ).apply {
            onCreate(null)
            addOnDidFailLoadingMapListener(MapView.OnDidFailLoadingMapListener {
                debug("Yükleme hatası: katman=${layer.id}, durum=$loadState, yedek=$fallbackUsed")
                if (loadState == MapHostLoadState.READY) {
                    debug("Hazır stilin geçici karo hatası; yedek geçiş yok.")
                } else if (!fallbackUsed && layer == MapLayer.DARK) {
                    fallbackUsed = true
                    issue = "Koyu harita yüklenemedi; Sokak katmanı denendi."
                    debug("DARK → LIBERTY yedek geçiş")
                    requestStyle(MapLayer.LIBERTY)
                } else {
                    loadState = MapHostLoadState.ERROR
                    issue = "Harita veya karolar yüklenemedi. İnterneti denetle ya da tekrar dene."
                }
            })
            getMapAsync { instance ->
                val px = (CockpitDesignTokens.mapAttributionSafeInset.value *
                    appContext.resources.displayMetrics.density).roundToInt()
                instance.uiSettings.apply {
                    setLogoGravity(Gravity.BOTTOM or Gravity.START)
                    setAttributionGravity(Gravity.BOTTOM or Gravity.END)
                    setLogoMargins(px, 0, 0, px)
                    setAttributionMargins(0, 0, px, px)
                }
                instance.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(39.0, 35.0), 5.2,
                ))
                map = instance
            }
        }
    }

    fun claim(slot: Any) {
        slots.add(slot)
        if (activeSlot == null) activeSlot = slots.firstOrNull()
    }

    fun release(slot: Any) {
        slots.remove(slot)
        if (activeSlot === slot) activeSlot = slots.firstOrNull()
    }

    fun requestStyle(requestedLayer: MapLayer, url: String = requestedLayer.styleUrl) {
        if (url.isBlank()) return
        if (layer == requestedLayer && styleUrl == url &&
            (loadState == MapHostLoadState.READY || loadState == MapHostLoadState.LOADING)) return
        layer = requestedLayer
        styleUrl = url
        version++
    }

    fun selectLayer(selected: MapLayer, url: String = selected.styleUrl) {
        if (url.isBlank()) return
        fallbackUsed = false
        issue = null
        layer = selected
        styleUrl = url
        version++
    }

    fun retry() {
        issue = null
        fallbackUsed = false
        version++
    }

    /** İptal edilen eski isteğin callback'i yeni durumu veya kontrastı bozamaz. */
    suspend fun loadStyleForRequest() {
        val active = map ?: return
        val requested = layer
        val requestedUrl = styleUrl
        val requestedVersion = version
        if (requestedUrl.isBlank()) return
        debug("Stil isteği: ${requested.id}; sürüm=$requestedVersion; yedek=$fallbackUsed")
        loadState = MapHostLoadState.LOADING
        readyMap = null
        if (!fallbackUsed) issue = null
        active.setStyle(requestedUrl) { style ->
            if (layer != requested || version != requestedVersion) {
                debug("Eski callback yok: ${requested.id}, aktif=${layer.id}")
            } else {
                val changed = if (requested == MapLayer.DARK) {
                    F1DarkRoadContrast.mappings.mapNotNull { spec ->
                        runCatching {
                            val line = style.getLayer(spec.layerId) as? LineLayer
                                ?: return@runCatching null
                            line.setProperties(PropertyFactory.lineColor(
                                spec.color.toArgb(),
                            ))
                            spec.layerId
                        }.getOrNull()
                    }
                } else emptyList()
                readyMap = active
                loadState = MapHostLoadState.READY
                if (!fallbackUsed) issue = null
                appliedStyleVersion++
                debug("Stil hazır: ${requested.id}, kontrast=${changed.joinToString()}")
            }
        }
        delay(15_000L)
        if (layer == requested && version == requestedVersion &&
            loadState == MapHostLoadState.LOADING) {
            loadState = MapHostLoadState.ERROR
            issue = "Harita 15 saniye içinde açılamadı. Yeniden dene."
            debug("Stil zaman aşımı: ${requested.id}")
        }
    }

    private fun debug(message: String) {
        if (BuildConfig.DEBUG) Log.d("F1Map", message)
    }
}

/** Tek örnek tüm sekmelerin ÜZERİNDE yaşar; NavHost sayfaları yalnız görünüm kiralar. */
val LocalSharedMapHost = staticCompositionLocalOf<MapHostState?> { null }

@Composable
fun ProvideSharedMapHost(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    val inspection = LocalInspectionMode.current
    val host = remember(context, inspection) {
        if (inspection) null else MapHostState(context)
    }

    DisposableEffect(owner, host) {
        if (host == null) return@DisposableEffect onDispose { }
        val view = host.mapView
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) {
                    view.onStart(); started = true
                }
                Lifecycle.Event.ON_RESUME -> if (!resumed) {
                    view.onResume(); resumed = true
                }
                Lifecycle.Event.ON_PAUSE -> if (resumed) {
                    view.onPause(); resumed = false
                }
                Lifecycle.Event.ON_STOP -> if (started) {
                    view.onStop(); started = false
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            view.onStart(); started = true
        }
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            view.onResume(); resumed = true
        }
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (resumed) view.onPause()
            if (started) view.onStop()
            (view.parent as? ViewGroup)?.removeView(view)
            view.onDestroy()
        }
    }

    LaunchedEffect(host?.map, host?.layer, host?.styleUrl, host?.version) {
        host?.loadStyleForRequest()
    }
    CompositionLocalProvider(LocalSharedMapHost provides host, content = content)
}

/**
 * Aynı MapView aynı anda iki AndroidView'ın altında bulunamaz. İlk görünür
 * görünüm kiralar; ötekiler gerçek harita gibi davranan sahte görüntü çizmez.
 */
@Composable
fun MapHostViewport(
    modifier: Modifier = Modifier,
    unavailableMessage: String = "Harita başka bir panelde görüntüleniyor.",
    requestedSlot: Any? = null,
) {
    val host = LocalSharedMapHost.current
    val inspection = LocalInspectionMode.current
    val localSlot = remember { Any() }
    val slot = requestedSlot ?: localSlot
    DisposableEffect(host, slot) {
        host?.claim(slot)
        onDispose { host?.release(slot) }
    }
    if (!inspection && host != null && host.activeSlot === slot) {
        AndroidView(
            modifier = modifier,
            factory = {
                (host.mapView.parent as? ViewGroup)?.removeView(host.mapView)
                host.mapView
            },
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(unavailableMessage, color = CockpitDesignTokens.secondary)
        }
    }
}
```

---

### 2. `app/src/main/java/app/evcockpit/ui/finalhmi/MapHeroPane.kt`

```kotlin
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import android.view.Gravity
import android.util.Log
import app.evcockpit.BuildConfig
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
import app.evcockpit.ui.unified.map.LocalSharedMapHost
import app.evcockpit.ui.unified.map.MapHostLoadState
import app.evcockpit.ui.unified.map.MapHostViewport
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
    val inspection = LocalInspectionMode.current
    val host = LocalSharedMapHost.current
    var layerMenu by remember { mutableStateOf(false) }
    val layer = host?.layer ?: MapLayer.DARK
    val map = host?.map
    val loadState = host?.loadState ?: MapHostLoadState.LOADING
    val issue = host?.issue

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
                MapHostViewport(
                    modifier = Modifier.fillMaxSize(),
                    unavailableMessage = "Harita gösterilemiyor • yalnız gerçek harita verisi",
                )

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
                                host?.selectLayer(candidate)
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
                                host?.selectLayer(candidate)
                            },
                        )
                    }
                }

                if (!inspection && loadState != MapHostLoadState.READY) {
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
                                if (loadState == MapHostLoadState.LOADING)
                                    "Harita yükleniyor…" else "Harita gösterilemiyor",
                                color = t.text, fontSize = t.bodyText,
                            )
                            if (loadState == MapHostLoadState.ERROR) {
                                OutlinedButton(
                                    onClick = {
                                        host?.retry()
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
 * Kontrast eşlemesi saf veridir: katman ID'si ve renk yalnız token dosyasından.
 * OpenFreeMap sunucusunun stil JSON'u değiştirilmez.
 */
internal data class F1RoadContrastSpec(val layerId: String, val color: Color)

internal object F1DarkRoadContrast {
    val mappings: List<F1RoadContrastSpec> get() = listOf(
        F1RoadContrastSpec(
            "highway_major_subtle", CockpitDesignTokens.mapRoadMajorContrast),
        F1RoadContrastSpec(
            "highway_minor", CockpitDesignTokens.mapRoadMinorContrast),
        F1RoadContrastSpec(
            "highway_motorway_subtle", CockpitDesignTokens.mapRoadMinorContrast),
    )
}

/** Sadece gerçekten bulunan ve güncellenen LineLayer ID'lerini döndürür. */
private fun applyDarkRoadContrast(style: Style): List<String> =
    F1DarkRoadContrast.mappings.mapNotNull { spec ->
        runCatching {
            val line = style.getLayer(spec.layerId) as? LineLayer
                ?: return@runCatching null
            line.setProperties(PropertyFactory.lineColor(spec.color.toArgb()))
            spec.layerId
        }.getOrNull()
    }

/** Yalnız DEBUG derlemesinde katman/fallback/kontrast tanısı. */
private fun f1MapLog(message: String) {
    if (BuildConfig.DEBUG) Log.d("F1Map", message)
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
```

---

### 3. `app/src/main/java/app/evcockpit/ui/OnlineMap.kt`

```kotlin
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
```

---

### 4. `app/src/main/java/app/evcockpit/MainActivity.kt`

```kotlin
package app.evcockpit

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.navigation.NavigationViewModel
import app.evcockpit.navigation.PlaceAutocompleteViewModel
import app.evcockpit.obd.ManualSocStore
import app.evcockpit.obd.ObdLogShare
import app.evcockpit.obd.ObdViewModel
import app.evcockpit.obd.SafeTestReportExporter
import app.evcockpit.planui.PlannerViewModel
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.storage.VehicleProfileStore
import app.evcockpit.ui.DisplayPreferences
import app.evcockpit.ui.ObdTestScreen
import app.evcockpit.ui.PlannerScreen
import app.evcockpit.ui.RouteScreen
import app.evcockpit.ui.RouteSettingsScreen
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.finalhmi.FinalCockpitTheme
import app.evcockpit.ui.finalhmi.FinalDashboardTab
import app.evcockpit.ui.finalhmi.MapFirstDashboardScreen
import app.evcockpit.ui.finalhmi.MapFirstDashboardViewModel
import app.evcockpit.ui.unified.map.ProvideSharedMapHost
import app.evcockpit.ui.tesla.DriveModeViewModel
import app.evcockpit.ui.tesla.SafeSocSelection
import app.evcockpit.weather.RouteHourlyWeatherViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A0: Tek ana Activity, NavHost başlangıcı HARİTA / F1.
 * Mola, medya ve eski PARK / ŞARJ arayüzü bu gezinmede bulunmaz.
 * OBD, GPS, rota, planlama depoları ve DataStore aynı kalır.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val displayPrefs = remember { DisplayPreferences(applicationContext) }
            val keepScreenOn by displayPrefs.keepScreenOn.collectAsStateWithLifecycle(false)
            val darkPref by displayPrefs.darkTheme.collectAsStateWithLifecycle(true)
            DisposableEffect(keepScreenOn) {
                if (keepScreenOn)
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }

            val f1: MapFirstDashboardViewModel = viewModel()
            val dashboard by f1.state.collectAsStateWithLifecycle()
            val obd: ObdViewModel = viewModel()
            val nav: NavigationViewModel = viewModel()
            val search: PlaceAutocompleteViewModel = viewModel()
            val planner: PlannerViewModel = viewModel()
            val weather: RouteHourlyWeatherViewModel = viewModel()
            val drive: DriveModeViewModel = viewModel()
            val scope = rememberCoroutineScope()

            val obdState by obd.state.collectAsStateWithLifecycle()
            val gps by obd.gps.collectAsStateWithLifecycle()
            val obdLog by obd.log.collectAsStateWithLifecycle()
            val logFilePath by obd.logFilePath.collectAsStateWithLifecycle()
            val diagnosisPath by obd.diagnosisFilePath.collectAsStateWithLifecycle()
            val diagnosisSummary by obd.diagnosisSummary.collectAsStateWithLifecycle()
            val devices by obd.devices.collectAsStateWithLifecycle()
            val obdMessage by obd.message.collectAsStateWithLifecycle()
            val driveState by drive.state.collectAsStateWithLifecycle()
            val navState by nav.state.collectAsStateWithLifecycle()
            val voiceState by nav.voice.collectAsStateWithLifecycle()
            val weatherSummary by weather.summary.collectAsStateWithLifecycle()
            val planState by planner.state.collectAsStateWithLifecycle()

            val manualStore = remember { ManualSocStore(applicationContext) }
            val manualSoc by manualStore.socPct.collectAsStateWithLifecycle(null)
            val profileStore = remember { VehicleProfileStore(applicationContext) }
            val profile by profileStore.profile.collectAsStateWithLifecycle(
                VehicleProfile.defaults())
            val socReading = SafeSocSelection.select(obdState, manualSoc, driveState)
            var diagnosisError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(navState.route) {
                // İstek yalnız mevcut ViewModel'de yapılır; ekran G/Ç işlemez.
                weather.refresh(navState.route)
            }

            // İzinler yalnız ilgili kullanıcı eylemiyle istenir; açılışta izin yağmuru yok.
            val gpsPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { result ->
                if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                ) obd.startGps() else obd.denyGps()
            }
            val bluetoothPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { _ -> obd.refreshBonded() }

            val openDiagnostics: () -> Unit = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothPermission.launch(
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                } else {
                    obd.refreshBonded()
                }
            }

            FinalCockpitTheme {
                ProvideSharedMapHost {
                    MapFirstDashboardScreen(
                    state = dashboard,
                    onDemoChange = f1::showDemo,
                    demoAvailable = f1.demoAvailable,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    tabContent = { tab, openDiagnosticRoute, navigateToTab ->
                        when (tab) {
                            FinalDashboardTab.ROTA -> RouteScreen(
                                state = navState,
                                gps = gps,
                                socPct = socReading.pct,
                                socSource = socReading.source,
                                vehicleProfile = profile,
                                voice = voiceState,
                                onToggleVoice = nav::toggleVoice,
                                onSpeakNextManeuver = nav::testNextManeuver,
                                onCalculate = nav::calculateSelected,
                                onSelectDestination = nav::selectDestination,
                                onSelectNamedDestination = nav::selectNamedDestination,
                                onSelectOrigin = nav::selectOrigin,
                                onUseGpsOrigin = nav::useGpsOrigin,
                                onClearDestinationSelection = nav::clearDestinationSelection,
                                onSelectAlternative = nav::selectAlternative,
                                onManualSoc = { value ->
                                    scope.launch { manualStore.save(value) }
                                },
                                searchVm = search,
                                routeWeather = weatherSummary,
                                onClear = nav::clear,
                            )
                            FinalDashboardTab.SARJ -> PlannerScreen(
                                vm = planner,
                                navigation = navState.route,
                                soc = socReading,
                                gps = gps,
                                onOpenRoute = { navigateToTab(FinalDashboardTab.ROTA) },
                                onOpenSettings = { navigateToTab(FinalDashboardTab.AYARLAR) },
                                onNavigate = { stops, destination ->
                                    nav.navigateVia(stops, destination)
                                    navigateToTab(FinalDashboardTab.ROTA)
                                },
                                onSelectDestination = nav::selectDestination,
                            )
                            FinalDashboardTab.ENERJI -> A0EnergyPlaceholder()
                            FinalDashboardTab.AYARLAR -> RouteSettingsScreen(
                                gps = gps,
                                darkTheme = darkPref,
                                keepScreenOn = keepScreenOn,
                                onDarkTheme = { value ->
                                    scope.launch { displayPrefs.setDarkTheme(value) }
                                },
                                onKeepScreenOn = { value ->
                                    scope.launch { displayPrefs.setKeepScreenOn(value) }
                                },
                                manualSocPct = manualSoc,
                                onSaveManualSoc = { value ->
                                    scope.launch { manualStore.save(value) }
                                },
                                onClearManualSoc = {
                                    scope.launch { manualStore.clear() }
                                },
                                onStartGps = {
                                    gpsPermission.launch(arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ))
                                },
                                onStopGps = obd::stopGps,
                                onOpenObdTest = {
                                    openDiagnostics()
                                    openDiagnosticRoute()
                                },
                            )
                            else -> Unit // HARİTA, NavHost'un ilk hedefidir.
                        }
                    },
                    diagnosticContent = {
                        ObdTestScreen(
                            state = obdState,
                            log = obdLog,
                            logFilePath = logFilePath,
                            diagnosisFilePath = diagnosisPath,
                            diagnosisSummary = diagnosisSummary,
                            devices = devices,
                            message = diagnosisError ?: obdMessage,
                            onRefresh = obd::refreshBonded,
                            onConnect = obd::connect,
                            onDiagnose = obd::diagnose,
                            onExportTestReport = { note ->
                                diagnosisError = null
                                scope.launch {
                                    try {
                                        val file = withContext(Dispatchers.IO) {
                                            SafeTestReportExporter.create(
                                                this@MainActivity, diagnosisSummary,
                                                obdLog.takeLast(200), note)
                                        }
                                        ObdLogShare.launch(this@MainActivity, file)
                                    } catch (_: Exception) {
                                        diagnosisError = "OBD test raporu paylaşılamadı."
                                    }
                                }
                            },
                            onShareDiagnosis = {
                                if (!ObdLogShare.launch(this@MainActivity, diagnosisPath))
                                    diagnosisError = "Tanı günlüğü bulunamadı."
                            },
                            onDemo = {
                                // OBD testindeki örnek değerler hiçbir zaman üretimde açılmaz.
                                if (BuildConfig.DEBUG) obd.startDemo()
                            },
                            onStop = obd::stop,
                            onShareLog = {
                                if (!ObdLogShare.launch(this@MainActivity, logFilePath))
                                    diagnosisError = "Paylaşılacak günlük bulunamadı."
                            },
                            onOpenBluetoothSettings = {
                                try {
                                    startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                } catch (_: Exception) {
                                    diagnosisError = "Bluetooth ayarları açılamadı."
                                }
                            },
                        )
                    },
                    )
                }
            }
        }
    }
}

@Composable
private fun A0EnergyPlaceholder() {
    val t = CockpitDesignTokens
    Surface(
        color = t.panel, shape = t.corner,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            Modifier.padding(t.spaceXL),
            verticalArrangement = Arrangement.spacedBy(t.spaceM),
        ) {
            Text("Enerji", color = t.text, fontSize = t.headText)
            Text("Henüz doğrulanmış bir yolculuk enerji kaydı bulunmuyor.",
                color = t.secondary, fontSize = t.bodyText)
            Text("Mevcut enerji ve şarj hesapları Şarj sekmesindeki gelişmiş planlayıcıda korunuyor.",
                color = t.secondary, fontSize = t.bodyText)
        }
    }
}
```

---

### 5. `app/src/test/java/app/evcockpit/ui/unified/map/SingleMapHostSourceTest.kt`

```kotlin
package app.evcockpit.ui.unified.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * C1 kabul kapısı: src/main/java altında tek MapView oluşturulabilir.
 * Kod inceleme testi; POCO F5 native GPU/saha kanıtı değildir.
 */
class SingleMapHostSourceTest {
    @Test
    fun mapViewYalnizMapHostDosyasindaOlusturulur() {
        val cwd = File(System.getProperty("user.dir"))
        val roots = listOf(
            File(cwd, "src/main/java"),
            File(cwd, "app/src/main/java"),
        )
        val root = roots.firstOrNull { it.isDirectory }
        assertTrue("CI kaynak dizini bulunamadı: ${cwd.absolutePath}", root != null)

        val expression = Regex("""\bMapView\s*\(""")
        val matches = root!!.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { expression.containsMatchIn(it.value) }
                    .map { line -> "${file.relativeTo(root).path}:${line.index + 1}" }
            }
            .toList()

        assertEquals(
            "Yeni MapView yapıcıları yalnız MapHost.kt dosyasında olmalı: $matches",
            1, matches.size,
        )
        assertTrue(
            "Tek MapView kaynağı MapHost.kt olmalı: $matches",
            matches.single().startsWith(
                "app/evcockpit/ui/unified/map/MapHost.kt:",
            ),
        )
    }
}
```
