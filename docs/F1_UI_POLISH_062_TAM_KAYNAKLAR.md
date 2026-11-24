# F1 Dikey Telefon Ekranı Cilası — TAM Değişen Dosyalar

**Dal:** `fix/f1-ui-polish-20261124`
**Temel:** `fix/f1-map-portrait-layout-20261123`
**Uygulama sürümü:** `0.6.2-F1-ui-polish`, `versionCode=49`
**Hedef:** POCO F5, LineageOS 24 / Android 17 dikey F1 önizlemesi

Bu belge, değişen **7 dosyanın eksiksiz son içeriğini** içerir; “geri kalanı aynı” veya üç noktayla atlama yoktur. Mevcut A–C, Android Auto ve Mola dosyaları korunmuştur. F1 haritasındaki TextureView, 15 saniyelik zaman aşımı, alternatif stil, yeniden dene ve MapView yaşam döngüsü korundu.

## G1–G6: Tespit / müdahale

| Görev | Tespit | Değişiklik |
|---|---|---|
| G1 | Kısa haritada dört 56dp düğme sol üstte geniş bir yatay Row oluşturuyor; Ankara etiketine biniyordu | Kısa modda **sağ üstte yalnız + / − / katman**, her biri 48dp dokunma alanı; devre dışı konum kısa modda gizli. 48dp yükseklik 192dp ve üzeri harita görünümünün ≤%25'idir. Büyük haritada dört 56dp kontrol sol dikey kalır. Yarı saydam koyu arka plan var |
| G2 | Logo ile attribution aynı alt köşede görünüyordu | `getMapAsync` içinde logo sol alt, attribution sağ alt; 12dp güvenli marjin piksele çevrilir. İkisi de etkin/görünür kalır |
| G3 | OpenFreeMap DARK `highway_minor=#181818`, `highway_major_subtle=#2a2a2a`, `highway_motorway_subtle=#181818` | Yalnız bu bilinen yol `LineLayer` katmanlarının rengi `Style` callback sonrası token renklerle hafif yükseltilir. Stil kaynağına dokunulmadı, katman yoksa `runCatching` ile atlanır; diğer stil yüklenince sıfırlanır |
| G4 | `Arrangement.SpaceBetween` ve minimum 88dp, etiket/değer aralığını açıyordu | `Arrangement.spacedBy(spaceXS)`, minimum 72dp, metinde `maxLines=2` kesimi kaldırıldı; font 1.3 için gerektiği kadar uzar |
| G5 | Demo modelinde ortadaki örneğin `rainExpected=true` olması gerçek bir uyarıydı; fakat metni yalnız “Yağış uyarısı” yazıyordu | “Yağış uyarısı var” ve uyarı rengi; minimum 128dp kart, uzun metinde yumuşak satır kırımı, “DEMO / ÖRNEK” başlığı |
| G6 | Material3 `LinearProgressIndicator` sağ uç stop indicator noktası anlamsızdı | Tek segmentli `Box` çubuğu, yalnız 0–100 arası SoC ile oransal uzunluk, erişilebilir `contentDescription`; demo %74 → 0,74 oranı JVM testinde |

**Doğrulama sınırı:** GitHub Actions'da `gradle :app:testDebugUnitTest :app:assembleDebug` çalışır. Repo içinde yerel `gradlew` sarmalayıcısı yoktur; telefonda terminal/Android SDK gerekmiyor. Android Studio Compose Preview için 360×800, 411×891 ve fontScale 1.3 tanımlandı ancak grafik çıktı/render testleri GitHub CI'da bulunmaz. POCO F5'te gerçek GPU/a11y kontrolü kullanıcı tarafından yapılacak. CI son commit yeşil olmadan “derlendi” denmez.

**Kaynak:** OpenFreeMap dark style `hyperknot/openfreemap-styles/styles/dark/style.json` (`highway_major_subtle`, `highway_minor`, `highway_motorway_subtle` katmanları); MapLibre Android `UiSettings.setLogoMargins` ve `setAttributionMargins` API.

## Telefonda açılış

1. [Taslak PR](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/116) üzerinden yapılan commit'leri incele (dosyalar zaten yüklü).
2. [GitHub Actions](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions) altında `fix/f1-ui-polish-20261124` dalının **son başarılı** Android CI çalışmasını aç.
3. `Artifacts → ev-cockpit-v2-debug` ZIP dosyasını indir; `app-debug.apk`'yi kur.
4. Uygulamadaki Ayarlar > F1 yeni ekran önizlemesi (DEMO) bölümünü aç.
5. Dikey haritada sağ üst + / − / katman, iki ayrı MapLibre atfı; alt kart etiket/değer, örnek hava uyarısı ve düzgün pil çubuğunu kontrol et.

## Değişen 7 dosyanın eksiksiz içerikleri


---

### 1. `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "app.evcockpit"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.evcockpit"
        minSdk = 28
        targetSdk = 35
        versionCode = 49
        versionName = "0.6.2-F1-ui-polish"
        val ciSha = (System.getenv("GITHUB_SHA") ?: "unknown")
            .take(40).replace(Regex("[^0-9a-f]"), "")
        buildConfigField("String", "CI_COMMIT", "\"$ciSha\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // Public TEST certificate only. Never use it for production/release signing.
    System.getenv("EV_COCKPIT_PUBLIC_DEBUG_P12")?.takeIf { it.isNotBlank() }?.let { keyPath ->
        val shared = signingConfigs.create("persistentDebug") {
            storeFile = file(keyPath)
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        buildTypes.getByName("debug").signingConfig = shared
    }
}

dependencies {
    // Hilt, uygulama ve servisler arasında aynı konum deposunu paylaşır.
    implementation("com.google.dagger:hilt-android:2.57.1")
    kapt("com.google.dagger:hilt-compiler:2.57.1")
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.maplibre.gl:android-sdk:11.13.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.media3:media3-exoplayer:1.10.0")
    implementation("androidx.media3:media3-ui:1.10.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
```

---

### 2. `app/src/main/java/app/evcockpit/ui/finalhmi/CockpitDesignTokens.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * F1 ve sonraki F ekranları için TEK tasarım token kaynağı.
 * Buradaki renk ve ölçüler fiziksel araçtan okunan veri değildir.
 */
object CockpitDesignTokens {
    val background = Color(0xFF080E19)
    val panel = Color(0xFF121E30)
    val panelRaised = Color(0xFF1C2B41)
    val mapFallback = Color(0xFF0D1C2B)
    val outline = Color(0xFF30425A)
    val text = Color(0xFFF4F8FF)
    val secondary = Color(0xFFACC0D4)
    val accent = Color(0xFF1677FF)
    val accentSoft = Color(0xFF14345C)
    val regenerate = Color(0xFF32CF99)
    val traction = Color(0xFF1677FF)
    val caution = Color(0xFFFFC878)
    val error = Color(0xFFFF7B84)
    val neutralTrack = Color(0xFF42536A)
    val demoBanner = Color(0xFF4C2A06)
    val mutedScrim = Color(0xCC0A1423)

    val corner = RoundedCornerShape(18.dp)
    val cornerSmall = RoundedCornerShape(16.dp)
    val cornerTiny = RoundedCornerShape(10.dp)
    val cardOutlineWidth = 1.dp
    val spaceXXS = 3.dp
    val spaceXS = 6.dp
    val spaceS = 9.dp
    val spaceM = 12.dp
    val spaceL = 16.dp
    val spaceXL = 20.dp
    val spaceXXL = 28.dp
    val target = 56.dp
    val topBarMinHeight = 56.dp
    val tabBarHeight = 70.dp
    val bannerMinHeight = 36.dp
    val phoneMapHeight = 388.dp
    val compactMapHeight = 312.dp
    val wideWeatherHeight = 112.dp
    val speedCardMinHeight = 116.dp
    val metricCardMinHeight = 72.dp
    val chargingCardMinHeight = 140.dp
    val mapSummaryHeight = 116.dp
    val mapControlsVerticalMinHeight = 280.dp
    // Kısa haritada sağ üstte üç düğme; 48dp asgari erişilebilir dokunma hedefi.
    val mapControlTouchTarget = 48.dp
    val mapControlVisualRadius = RoundedCornerShape(10.dp)
    val mapControlSpacing = 4.dp
    val mapAttributionSafeInset = 12.dp
    val mapRoadMajorContrast = Color(0xFF435160)
    val mapRoadMinorContrast = Color(0xFF303940)
    val weatherSampleMinWidth = 128.dp
    val batteryBarHeight = 6.dp
    val mapBorderRadius = 18.dp
    val mapAttributionPadding = 4.dp

    const val mapWeight = 0.65f
    const val dataWeight = 0.35f
    val expandedMinWidth = 840.dp
    val compactMaxWidth = 600.dp

    val speedText = 48.sp
    val metricNumber = 25.sp
    val headText = 21.sp
    val titleText = 17.sp
    val bodyText = 14.sp
    val labelText = 12.sp
    val tinyText = 11.sp
    val tabText = 13.sp
    val fontWeightStrong = FontWeight.SemiBold
}

private val finalColors = darkColorScheme(
    primary = CockpitDesignTokens.accent,
    background = CockpitDesignTokens.background,
    surface = CockpitDesignTokens.panel,
    onBackground = CockpitDesignTokens.text,
    onSurface = CockpitDesignTokens.text,
    onPrimary = CockpitDesignTokens.text,
    secondary = CockpitDesignTokens.secondary,
    outline = CockpitDesignTokens.outline,
    error = CockpitDesignTokens.error,
)

private val finalType = Typography(
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.bodyText,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.bodyText,
        fontWeight = FontWeight.Normal,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.titleText,
        fontWeight = FontWeight.Medium,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.titleText,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.headText,
        fontWeight = FontWeight.SemiBold,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = CockpitDesignTokens.labelText,
        fontWeight = FontWeight.Normal,
    ),
)

@Composable
fun FinalCockpitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = finalColors, typography = finalType, content = content)
}
```

---

### 3. `app/src/main/java/app/evcockpit/ui/finalhmi/MapHeroPane.kt`

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
    // Türkiye genel açılış kamerası zoom 5,2: bu katman yalnız zoom < 6'da var.
    runCatching {
        (style.getLayer("highway_motorway_subtle") as? LineLayer)
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
```

---

### 4. `app/src/main/java/app/evcockpit/ui/finalhmi/TripMetricsPane.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * Hız yalnız doğrulanmış GPS metre/saniye verisinden gelir;
 * F1 DEMO sayıları ekranda büyük DEMO şeridiyle ayrılır.
 */
@Composable
fun TripMetricsPane(state: DashboardUiState, modifier: Modifier = Modifier) {
    val t = CockpitDesignTokens
    Column(modifier, verticalArrangement = Arrangement.spacedBy(t.spaceS)) {
        Surface(color = t.panel, shape = t.corner,
            border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline)) {
            Column(Modifier.fillMaxWidth().padding(t.spaceM),
                verticalArrangement = Arrangement.spacedBy(t.spaceXXS)) {
                Text(if (state.demo) "HIZ • DEMO" else "HIZ • GPS",
                    color = t.secondary, fontSize = t.labelText)
                Text(DashboardFormat.number(state.speedKmh, " km/sa"),
                    color = t.text, fontSize = t.speedText,
                    fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        Surface(color = t.panel, shape = t.corner,
            border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline)) {
            Column(Modifier.padding(t.spaceS),
                verticalArrangement = Arrangement.spacedBy(t.spaceS)) {
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                    InfoMetric("Batarya" + when (state.socPercent.source) {
                        DashboardOrigin.MANUAL -> " (manuel)"
                        DashboardOrigin.MODEL -> " (tahmini)"
                        DashboardOrigin.DEMO -> " (DEMO)"
                        else -> ""
                    }, DashboardFormat.number(state.socPercent, " %"),
                        Modifier.weight(1f))
                    InfoMetric("Menzil (tahmini)",
                        DashboardFormat.number(state.estimatedRangeKm, " km"),
                        Modifier.weight(1f))
                }
                val soc = state.socPercent.value?.takeIf { it.isFinite() &&
                    it in 0.0..100.0 }
                val fraction = batteryFillFraction(soc)
                // Material3 LinearProgressIndicator'ın sağ uçtaki stop noktası
                // kullanıcıya hedef SoC gibi görünüyordu. Tek, oransal çubuk.
                Box(
                    Modifier.fillMaxWidth()
                        .height(t.batteryBarHeight)
                        .clip(t.cornerTiny)
                        .background(t.neutralTrack)
                        .semantics {
                            contentDescription = if (soc == null)
                                "Batarya seviyesi bilinmiyor"
                            else "Batarya seviyesi yüzde ${DashboardFormat.whole(soc)}" +
                                if (state.demo) ", örnek veri" else ""
                        },
                ) {
                    if (fraction != null && fraction > 0f) {
                        Box(
                            Modifier.fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(t.accent),
                        )
                    }
                }
                if (soc == null) {
                    Text("Batarya seviyesi: —", color = t.secondary,
                        fontSize = t.labelText)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                    InfoMetric("Tüketim (tahmini)",
                        state.consumptionKwhPer100Km.value?.takeIf { it.isFinite() }
                            ?.let { DashboardFormat.oneDecimal(it) + " kWh/100 km" }
                            ?: "— kWh/100 km", Modifier.weight(1f))
                    val power = state.powerKw.value?.takeIf { it.isFinite() }
                    InfoMetric("Anlık güç",
                        power?.let { DashboardFormat.oneDecimal(it) + " kW" } ?: "— kW",
                        Modifier.weight(1f),
                        valueColor = when {
                            power == null -> t.text
                            power < 0.0 -> t.regenerate
                            else -> t.traction
                        })
                }
            }
        }
    }
}

@Composable
private fun InfoMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = CockpitDesignTokens.text,
) {
    val t = CockpitDesignTokens
    Column(
        modifier.background(t.panelRaised, t.cornerSmall)
            .heightIn(min = t.metricCardMinHeight)
            .padding(t.spaceS),
        verticalArrangement = Arrangement.spacedBy(t.spaceXS),
    ) {
        Text(label, color = t.secondary, fontSize = t.labelText)
        Text(value, color = valueColor,
            fontSize = t.titleText, fontWeight = FontWeight.SemiBold)
    }
}

/** Pil göstergesi ile yazılı SoC aynı ve yalnız geçerli yüzdeyle doludur. */
internal fun batteryFillFraction(socPercent: Double?): Float? =
    socPercent?.takeIf { it.isFinite() && it in 0.0..100.0 }
        ?.div(100.0)?.toFloat()
```

---

### 5. `app/src/main/java/app/evcockpit/ui/finalhmi/RouteWeatherStrip.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * F1 hava şeridi: Open-Meteo çağrısı yapmaz, var olan zamanlı veriyi çizer.
 * Uydurma güneş/yağmur ikonu ve trafik lejantı bulunmaz.
 */
@Composable
fun RouteWeatherStrip(state: DashboardUiState, modifier: Modifier = Modifier) {
    val t = CockpitDesignTokens
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = t.panel,
        shape = t.corner,
        border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline),
    ) {
        Column(Modifier.padding(t.spaceM),
            verticalArrangement = Arrangement.spacedBy(t.spaceS)) {
            Text(if (state.demo) "ROTA BOYUNCA HAVA • DEMO / ÖRNEK"
                else "ROTA BOYUNCA HAVA", color = t.text,
                fontSize = t.titleText)
            if (state.weatherSamples.isEmpty()) {
                Text("Sıcaklık: — • Yağış: —", color = t.secondary, fontSize = t.bodyText)
                Text("Hava verileri F4 aşamasında bağlanacak.",
                    color = t.secondary, fontSize = t.labelText)
            } else {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                ) {
                    state.weatherSamples.forEach { sample ->
                        Column(
                            Modifier.background(t.panelRaised, t.cornerSmall)
                                .widthIn(min = t.weatherSampleMinWidth)
                                .padding(t.spaceS),
                            verticalArrangement = Arrangement.spacedBy(t.spaceXS),
                        ) {
                            Text("≈" + DashboardFormat.whole(sample.distanceAlongKm) + " km",
                                color = t.accent, fontSize = t.labelText)
                            val temp = sample.temperatureC.value?.takeIf { it.isFinite() }
                                ?.let { DashboardFormat.whole(it) + " °C" } ?: "— °C"
                            Text(temp, color = t.text, fontSize = t.titleText)
                            Text(
                                rainWarningText(sample.rainExpected.value), color = if (sample.rainExpected.value == true)
                                    t.caution else t.secondary,
                                fontSize = t.labelText,
                                softWrap = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** true = uyarı VAR; eski kısa metin yanlışlıkla belirsiz kalıyordu. */
internal fun rainWarningText(value: Boolean?): String = when (value) {
    true -> "Yağış uyarısı var"
    false -> "Yağış uyarısı yok"
    null -> "Yağış: —"
}
```

---

### 6. `app/src/main/java/app/evcockpit/ui/finalhmi/MapFirstDashboardPreview.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Üç farklı dp pencere. Bunlar fiziksel cihaz ölçümü değildir.
 * Preview'de MapLibre native View açılmaz: sahte rota gösterilmez.
 */
@Preview(
    name = "F1 960x540 yatay",
    widthDp = 960, heightDp = 540,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Yatay960() {
    F1DemoPreview()
}

@Preview(
    name = "F1 1280x800 geniş yatay",
    widthDp = 1280, heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Yatay1280() {
    F1DemoPreview()
}

@Preview(
    name = "F1 393x873 POCO F5 dikey düzen",
    widthDp = 393, heightDp = 873,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Dikey393() {
    F1DemoPreview()
}

/**
 * G1/G4/G5 için sınır dp genişlikleri ve büyütülmüş sistem yazısı.
 * Bunlar IDE Compose önizlemeleridir, cihazda ekran görüntüsü testi değildir.
 */
@Preview(
    name = "F1 G1 360x800 dar telefon",
    widthDp = 360, heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Dikey360() {
    F1DemoPreview()
}

@Preview(
    name = "F1 G1 411x891 geniş telefon",
    widthDp = 411, heightDp = 891,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Dikey411() {
    F1DemoPreview()
}

@Preview(
    name = "F1 G4 fontscale 1.3 360x800",
    widthDp = 360, heightDp = 800,
    fontScale = 1.3f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Dikey360BuyukYazi() {
    F1DemoPreview()
}

@Composable
private fun F1DemoPreview() {
    FinalCockpitTheme {
        MapFirstDashboardScreen(
            state = DemoDashboardMapper.map(1_795_460_000_000L),
            onDemoChange = {},
        )
    }
}
```

---

### 7. `app/src/test/java/app/evcockpit/ui/finalhmi/F1UiPolishTest.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

/** G1, G4, G5, G6: UI durum mantığına ilişkin JVM testleri. */
class F1UiPolishTest {
    @Test fun kisaHaritaDokunmaHedefiVeDikeyOran() {
        val touch = CockpitDesignTokens.mapControlTouchTarget.value
        assertTrue("Minimum 48 dp", touch >= 48f)
        assertEquals("Kısa modda tek düğme satırı", 48f, touch, 0.01f)
        // 360dp telefonun F1 harita görünümünün yaklaşık 200dp iç alanı.
        assertTrue("200dp haritada düğme şeridi en fazla %25 yükseklik almalı",
            touch / 200f <= 0.25f)
    }

    @Test fun kartMinimumuVeEtiketAraligi() {
        assertTrue(CockpitDesignTokens.metricCardMinHeight.value in 72f..88f)
        assertTrue(CockpitDesignTokens.spaceXS.value < 10f)
    }

    @Test fun havaDemosuYagisUyarisiAcikYazilir() {
        assertEquals("Yağış uyarısı var", rainWarningText(true))
        assertEquals("Yağış uyarısı yok", rainWarningText(false))
        assertEquals("Yağış: —", rainWarningText(null))
    }

    @Test fun bataryaCubuguYuzdeDegeriyleEslesir() {
        assertEquals(0.74f, batteryFillFraction(74.0)!!, 0.00001f)
        assertEquals(0f, batteryFillFraction(0.0)!!, 0.00001f)
        assertEquals(1f, batteryFillFraction(100.0)!!, 0.00001f)
    }

    @Test fun bilinmeyenVeGecersizBataryayiSifirSanmaz() {
        assertNull(batteryFillFraction(null))
        assertNull(batteryFillFraction(-1.0))
        assertNull(batteryFillFraction(101.0))
        assertNull(batteryFillFraction(Double.NaN))
        assertNull(batteryFillFraction(Double.POSITIVE_INFINITY))
    }
}
```
