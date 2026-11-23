# F1 telefon ekranı hata ve yerleşim düzeltmesi — tam dosyalar

**Dal:** `fix/f1-map-portrait-layout-20261123`  
**Önceki temel:** `feature/stage-f1-map-first-shell-20261123` (PR #114)  
**Sürüm:** `0.6.1-F1-ui-fix` (48)

## Ekran görüntüsünden tespit edilenler

1. MapLibre logosu var ama harita bütünüyle siyah. Olası nedenler arasında stil/karo istek hatası, grafik çizim yüzeyi ve ROM/OpenGL uyumsuzluğu var; yalnız PNG'den kesin teşhis konulamaz.
2. Harita altındaki dört özet aynı görüntü katmanında, uzun şarj fiyat etiketi ve MapLibre atfıyla çakışıyor.
3. Telefon metrik kartlarının minimum yüksekliği fazla, boşluklar ekranı gereksiz uzatıyor.
4. Konum/rota bulunmadığı açıkça belirtilmeli; demo telemetrisi gerçek sayı sanılmamalı.

## Yapılanlar

- F1 MapView `MapLibreMapOptions...textureMode(true)` ile Compose uyumlu TextureView çizimine geçirildi; A–C haritaları aynen duruyor.
- Harita yükleme/asenkron stil, hata, 15 saniye başarısızlık göstergesi, yeniden deneme ve otomatik koyu→sokak stili yedeği eklendi.
- Harita üstündeki özet, harita ile aynı `Surface` içinde fakat **ayrı Column bölümüne** taşındı. İki sütun/dört metrik, uzun maliyet metnine üç satıra kadar yer bırakıyor.
- Dikey kısa haritada dört araç düğmesi yan yana; geniş haritada sol kenarda dikey.
- MapLibre atfını kapatan kontrol/orta metin kaldırıldı, "GPS değil" notu özet bölümüne taşındı.
- Metrik kartlarının minimum yüksekliği 112dp → 88dp; telefon harita alanı 400dp → 388dp.
- Yalnız F1 versiyonu artırıldı; A–C API/OBD/harita işlevleri ve Mola değişmedi.

## Dürüst doğrulama

GitHub Actions son commit için yeşil olmadan bu düzeltmenin derlendiği söylenmez. CI geçmesi bile POCO F5/LineageOS 24 Android 17 fiziksel GPU görüntüsünün düzeldiği anlamına **gelmez**. Cihazda tekrar siyah harita görülürse görüntüdeki yükleme/hata mesajını bildirmek gerekir. Android Auto için ekran vaadi yoktur.

Kaynaklar: [MapLibre textureMode](https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.maps/-map-libre-map-options/texture-mode.html), [MapLibre map load errors](https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.maps/-map-libre-map/set-style.html).

## Tam içerikler


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
        versionCode = 48
        versionName = "0.6.1-F1-ui-fix"
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
    val metricCardMinHeight = 88.dp
    val chargingCardMinHeight = 140.dp
    val mapSummaryHeight = 116.dp
    val mapControlsVerticalMinHeight = 280.dp
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
        active.setStyle(layer.styleUrl) {
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

                val actions: @Composable () -> Unit = {
                    MapToolButton("+", "Yakınlaştır", map != null) {
                        map?.animateCamera(CameraUpdateFactory.zoomIn())
                    }
                    MapToolButton("−", "Uzaklaştır", map != null) {
                        map?.animateCamera(CameraUpdateFactory.zoomOut())
                    }
                    MapToolButton("◎", "Konum (F3 aşamasında)", false) { }
                    Box {
                        MapToolButton("▤", "Harita katmanı", true) {
                            layerMenu = true
                        }
                        DropdownMenu(
                            expanded = layerMenu,
                            onDismissRequest = { layerMenu = false },
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
                                        layerMenu = false
                                        fallbackUsed = false
                                        issue = null
                                        layer = candidate
                                        requestVersion++
                                    },
                                )
                            }
                        }
                    }
                }
                // Kısa yatay ve telefon haritasında dört dikey düğme tüm yolu kapatmaz.
                if (maxHeight >= t.mapControlsVerticalMinHeight) {
                    Column(
                        Modifier.align(Alignment.CenterStart)
                            .padding(t.spaceXS),
                        verticalArrangement = Arrangement.spacedBy(t.spaceXS),
                    ) { actions() }
                } else {
                    Row(
                        Modifier.align(Alignment.TopStart)
                            .padding(t.spaceXS),
                        horizontalArrangement = Arrangement.spacedBy(t.spaceXS),
                    ) { actions() }
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

@Composable
private fun MapToolButton(
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val t = CockpitDesignTokens
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(t.spaceXXS),
        shape = t.cornerSmall,
        modifier = Modifier.sizeIn(minWidth = t.target, minHeight = t.target)
            .semantics { contentDescription = description },
    ) {
        Text(label, color = if (enabled) t.text else t.secondary,
            fontSize = t.headText, fontWeight = FontWeight.SemiBold)
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
