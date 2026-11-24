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
) {
    val host = LocalSharedMapHost.current
    val inspection = LocalInspectionMode.current
    val slot = remember { Any() }
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
