# A0b C2 — GeoJSON üst katmanları / 10 TAM kaynak dosyası

**Depo:** AlpTimucin-cpu/ev-cockpit-v2  
**Dal:** `feature/a0b-unified-map-20261124`  
**Baz:** C1 tek MapHost / A0 0.7.0  
**CI:** Son commit başarılı olmadıkça derlendi denmez.

## İşlev ve sınır
- MapLibre stilinde alternatif rota, seçili rota + casing, hava uyarısı, istasyonlar, uç noktalar, araç doğruluğu ve araç için **sabit, idempotent** GeoJSON kaynak/katman.
- Harita ikonları telefon içinde Android Canvas ile çizilir, Bitmap bellekte önbelleklenir; dış görsel veya yeni kütüphane yok.
- Stil yeniden yüklendiğinde katmanlar tekrar eklenir; veri yoksa boş FeatureCollection kullanılır, sahte rota veya şarj işaretçisi oluşturulmaz.
- Rota geometrisi Douglas–Peucker kopyasıyla sadeleştirilebilir; domain orijinali korunur. Başlık yönü dairesel sin/cos ortalamasıyla yumuşatılır; düşük hızda dondurulur; araç imleci en çok 2 Hz.
- Kamera kontrolleri OFF/FOLLOW/FOLLOW_BEARING; kullanıcı jesti FOLLOW'u OFF yapar; koordinat verisi yoksa kamera kendiliğinden takip etmez.
- **Henüz canlı veri bağlanmadı:** C4 gerçek rota, C5 OCM/hava, C3 panel, C6 navigasyon, C7 kayıt, C8 uygulama menüsü ve 0.7.1/52 finali. Bu yalnız C2 veri çizim altyapısıdır.
- Harita dokunuş/lifecycle/atıflar, Mola ve Android Auto kodu değiştirilmedi.
- Resmî MapLibre kaynağı: https://maplibre.org/maplibre-native/android/examples/geojson-guide/ ve https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.maps/-style/index.html

## Cihazda doğrulanacaklar
- [ ] Stil değiştirince mevcut varsa katmanlar yeniden kuruluyor, iki kat oluşmuyor.
- [ ] Gerçek rota bağlandığında alternatif gri, seçili mavi ve casing görünüyor; boşken çizim yok.
- [ ] İstasyon ikonları güç sınıfıyla tutarlı; fiyat/canlı müsaitlik tahmini yok.
- [ ] Araç hızı/heading gelmeden ekranda uydurma yön gösterilmiyor.
- [ ] Kullanıcı harita jesti FOLLOW'u kapatıyor.
- [ ] MapLibre logo/atıf ve tek View sahibi önceki C1 gibi korunuyor.

## Değişen 10 dosyanın TAM içeriği

---

### 1. `app/src/main/java/app/evcockpit/ui/finalhmi/CockpitDesignTokens.kt`

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
    // A0b/C2: harita GeoJSON katman ve çizim değerleri tek kaynaktan.
    val routeAltColor = Color(0xFF8292A8)
    val routeCasingColor = Color(0xFF102033)
    val stationAcColor = Color(0xFF4FA0EF)
    val stationDc50Color = Color(0xFF20BFA4)
    val stationDc100Color = Color(0xFFAA76E6)
    val stationDc150Color = Color(0xFFDA83D9)
    val vehicleAccuracyFill = Color(0x331677FF)
    val startPinColor = Color(0xFF25CF8F)
    val finishPinColor = Color(0xFFFFB364)
    val weatherIconColor = caution
    val routeSelectedWidth = 6.dp
    val routeCasingWidth = 9.dp
    val routeAlternativeWidth = 5.dp
    val stationMarkerSize = 42.dp
    val vehicleMarkerSize = 34.dp
    val endpointMarkerSize = 36.dp
    val weatherMarkerSize = 30.dp
    val mapLabelSize = 12.sp
    val mapMarkerSelectedScale = 1.3f
    const val maxAlternativeOverlayRoutes = 3
    const val accuracyCircleVertices = 64
    const val maxOverlayStations = 250
    const val maxOverlayWeatherAlerts = 120
    const val overlayMaxSampledRoutePoints = 2500
    const val overlaySimplificationToleranceMeters = 12.0
    const val overlayCarHeadingFreezeMetersPerSecond = 1.5
    const val overlayMaxPositionHz = 2.0
    const val cameraAnimationMs = 300
    val mapControlTouchTarget = 48.dp
    val mapControlVisualRadius = RoundedCornerShape(10.dp)
    val mapControlSpacing = 4.dp
    val mapAttributionSafeInset = 12.dp
    val mapRoadMajorContrast = Color(0xFF435160)
    val mapRoadMinorContrast = Color(0xFF303940)
    // 393dp telefonda 3 örnek yan yana; 4+ örnekte eşik aşılıyorsa kaydır.
    val weatherCellMinWidth = 96.dp
    // Kaydırılabilir kart içinde uzun Türkçe uyarı metni satır kırabilsin.
    val weatherScrollCellWidth = 144.dp
    val weatherScrollFadeWidth = 20.dp
    val transparent = Color.Transparent
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

### 2. `app/src/main/java/app/evcockpit/ui/unified/map/OverlayIds.kt`

```kotlin
package app.evcockpit.ui.unified.map

/** MapLibre stil URL'si değişse de overlay kimlikleri sabittir. */
internal object OverlayIds {
    const val SELECTED_ROUTE_SOURCE = "src-route-sel"
    const val SELECTED_ROUTE_CASING = "lyr-route-casing"
    const val SELECTED_ROUTE_LAYER = "lyr-route-sel"
    const val WEATHER_SOURCE = "src-weather"
    const val WEATHER_LAYER = "lyr-weather"
    const val STATIONS_SOURCE = "src-stations"
    const val STATIONS_LAYER = "lyr-stations"
    const val ENDPOINTS_SOURCE = "src-endpoints"
    const val ENDPOINTS_LAYER = "lyr-endpoints"
    const val VEHICLE_ACCURACY_SOURCE = "src-vehicle-accuracy"
    const val VEHICLE_ACCURACY_LAYER = "lyr-vehicle-accuracy"
    const val VEHICLE_SOURCE = "src-vehicle"
    const val VEHICLE_LAYER = "lyr-vehicle"
    const val START_ICON = "img-origin"
    const val FINISH_ICON = "img-destination"
    const val WEATHER_ICON = "img-weather"
    const val VEHICLE_ICON = "img-vehicle"
    const val VEHICLE_UNKNOWN_ICON = "img-vehicle-unknown"
    const val STATION_AC_ICON = "img-station-ac"
    const val STATION_50_ICON = "img-station-dc50"
    const val STATION_100_ICON = "img-station-dc100"
    const val STATION_150_ICON = "img-station-dc150"
    const val STATION_UNKNOWN_ICON = "img-station-unknown"

    fun alternativeSource(index: Int) = "src-route-alt-$index"
    fun alternativeLayer(index: Int) = "lyr-route-alt-$index"
}
```

---

### 3. `app/src/main/java/app/evcockpit/ui/unified/map/OverlayModel.kt`

```kotlin
package app.evcockpit.ui.unified.map

/**
 * Android ve harita motorundan bağımsız gerçek veriler.
 * Varsayılan boş: konum/rota/hava/istasyon asla kendiliğinden yaratılmaz.
 */
data class OverlayPoint(val latitude: Double, val longitude: Double) {
    fun valid(): Boolean = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
}

data class OverlayStation(
    val id: String,
    val point: OverlayPoint,
    val powerKw: Double?,
    val dc: Boolean,
    val selected: Boolean = false,
)

data class OverlayWeatherAlert(
    val point: OverlayPoint,
    val rainExpected: Boolean?,
    val text: String? = null,
)

data class OverlayVehicle(
    val point: OverlayPoint,
    val bearingDegrees: Double? = null,
    val accuracyMeters: Double? = null,
    val timestampEpochMs: Long,
    val speedMetersPerSecond: Double? = null,
)

data class OverlayModel(
    val selectedRoute: List<OverlayPoint> = emptyList(),
    val alternativeRoutes: List<List<OverlayPoint>> = emptyList(),
    val weatherAlerts: List<OverlayWeatherAlert> = emptyList(),
    val stations: List<OverlayStation> = emptyList(),
    val origin: OverlayPoint? = null,
    val destination: OverlayPoint? = null,
    val vehicle: OverlayVehicle? = null,
    val zoomLevel: Double? = null,
) {
    val hasAnyGeometry: Boolean get() =
        selectedRoute.isNotEmpty() || alternativeRoutes.isNotEmpty() ||
            weatherAlerts.isNotEmpty() || stations.isNotEmpty() ||
            origin != null || destination != null || vehicle != null
}
```

---

### 4. `app/src/main/java/app/evcockpit/domain/nav/PolylineGeometry.kt`

```kotlin
package app.evcockpit.domain.nav

import kotlin.math.*

/** Saf koordinat türü, Android/Compose/MapLibre bağımlılığı yok. */
data class WgsPoint(val lat: Double, val lon: Double)

object PolylineGeometry {
    const val EARTH_RADIUS_M = 6_371_000.0

    /**
     * Douglas–Peucker. Metrik uzaklık yerel equirectangular projeksiyonla
     * yaklaşık hesaplanır; orijinal rota bu fonksiyonla değiştirilemez.
     */
    fun simplify(original: List<WgsPoint>, toleranceMeters: Double): List<WgsPoint> {
        if (original.size < 3 || !toleranceMeters.isFinite() ||
            toleranceMeters <= 0.0) return original
        val keep = BooleanArray(original.size)
        keep[0] = true
        keep[original.lastIndex] = true
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(0 to original.lastIndex)
        while (stack.isNotEmpty()) {
            val (start, end) = stack.removeLast()
            if (end <= start + 1) continue
            var maxDistance = -1.0
            var index = -1
            for (i in start + 1 until end) {
                val distance = distanceToSegment(original[i], original[start], original[end])
                if (distance > maxDistance) {
                    maxDistance = distance
                    index = i
                }
            }
            if (maxDistance > toleranceMeters && index in start + 1 until end) {
                keep[index] = true
                stack.addLast(start to index)
                stack.addLast(index to end)
            }
        }
        return original.filterIndexed { index, _ -> keep[index] }
    }

    private fun distanceToSegment(p: WgsPoint, a: WgsPoint, b: WgsPoint): Double {
        val cosLat = cos(Math.toRadians((a.lat + b.lat) / 2.0))
        fun x(q: WgsPoint) = Math.toRadians(q.lon - a.lon) * cosLat * EARTH_RADIUS_M
        fun y(q: WgsPoint) = Math.toRadians(q.lat - a.lat) * EARTH_RADIUS_M
        val bx = x(b)
        val by = y(b)
        val px = x(p)
        val py = y(p)
        val denominator = bx * bx + by * by
        val t = if (denominator <= 1e-8) 0.0
            else ((px * bx + py * by) / denominator).coerceIn(0.0, 1.0)
        return hypot(px - t * bx, py - t * by)
    }

    /** 0/360 geçişinde yön ani 180° sapmasın: sin/cos dairesel ortalama. */
    fun circularHeadingDegrees(previous: Double?, incoming: Double, weight: Double): Double? {
        if (!incoming.isFinite() || !weight.isFinite() || weight !in 0.0..1.0) return null
        if (previous == null || !previous.isFinite()) return normalize(incoming)
        val theta1 = Math.toRadians(previous)
        val theta2 = Math.toRadians(incoming)
        val sx = (1.0 - weight) * cos(theta1) + weight * cos(theta2)
        val sy = (1.0 - weight) * sin(theta1) + weight * sin(theta2)
        return if (abs(sx) + abs(sy) < 1e-10) normalize(incoming)
            else normalize(Math.toDegrees(atan2(sy, sx)))
    }

    private fun normalize(angle: Double): Double = ((angle % 360.0) + 360.0) % 360.0
}
```

---

### 5. `app/src/main/java/app/evcockpit/ui/unified/map/MarkerIcons.kt`

```kotlin
package app.evcockpit.ui.unified.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Ağ/uzak görsel kullanılmaz. PNG/Bitmap, Android Canvas ile bellekte üretilir.
 * Aynı yoğunluk ve aynı tür için ikinci kez çizilmeyip önbellekten verilir.
 */
class MarkerIcons(context: Context) {
    private val density = context.resources.displayMetrics.density
    private val cached = mutableMapOf<String, Bitmap>()
    private val t = CockpitDesignTokens

    fun all(): Map<String, Bitmap> = mapOf(
        OverlayIds.STATION_UNKNOWN_ICON to icon(OverlayIds.STATION_UNKNOWN_ICON),
        OverlayIds.STATION_AC_ICON to icon(OverlayIds.STATION_AC_ICON),
        OverlayIds.STATION_50_ICON to icon(OverlayIds.STATION_50_ICON),
        OverlayIds.STATION_100_ICON to icon(OverlayIds.STATION_100_ICON),
        OverlayIds.STATION_150_ICON to icon(OverlayIds.STATION_150_ICON),
        OverlayIds.START_ICON to icon(OverlayIds.START_ICON),
        OverlayIds.FINISH_ICON to icon(OverlayIds.FINISH_ICON),
        OverlayIds.WEATHER_ICON to icon(OverlayIds.WEATHER_ICON),
        OverlayIds.VEHICLE_ICON to icon(OverlayIds.VEHICLE_ICON),
        OverlayIds.VEHICLE_UNKNOWN_ICON to icon(OverlayIds.VEHICLE_UNKNOWN_ICON),
    )

    private fun icon(id: String): Bitmap = cached.getOrPut(id) {
        val dp = when (id) {
            OverlayIds.VEHICLE_ICON -> t.vehicleMarkerSize
            OverlayIds.START_ICON, OverlayIds.FINISH_ICON -> t.endpointMarkerSize
            OverlayIds.WEATHER_ICON -> t.weatherMarkerSize
            else -> t.stationMarkerSize
        }
        val size = max(18, (dp.value * density).roundToInt())
        val picture = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(picture)
        val brush = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = size / 2f
        val radius = center * 0.80f
        val color = when (id) {
            OverlayIds.STATION_AC_ICON -> t.stationAcColor
            OverlayIds.STATION_50_ICON -> t.stationDc50Color
            OverlayIds.STATION_100_ICON -> t.stationDc100Color
            OverlayIds.STATION_150_ICON -> t.stationDc150Color
            OverlayIds.START_ICON -> t.startPinColor
            OverlayIds.FINISH_ICON -> t.finishPinColor
            OverlayIds.WEATHER_ICON -> t.weatherIconColor
            OverlayIds.VEHICLE_ICON, OverlayIds.VEHICLE_UNKNOWN_ICON -> t.accent
            else -> t.neutralTrack
        }
        if (id == OverlayIds.VEHICLE_ICON) {
            drawVehicle(canvas, brush, size, color)
        } else {
            brush.color = Color.White.toArgb()
            canvas.drawCircle(center, center, radius, brush)
            brush.color = color.toArgb()
            canvas.drawCircle(center, center, radius * 0.80f, brush)
            brush.color = Color.White.toArgb()
            when (id) {
                OverlayIds.WEATHER_ICON -> {
                    brush.style = Paint.Style.STROKE
                    brush.strokeWidth = max(2f, size * 0.075f)
                    val cloudY = center - radius * 0.12f
                    canvas.drawArc(
                        center - radius * 0.43f, cloudY - radius * 0.26f,
                        center + radius * 0.43f, cloudY + radius * 0.32f,
                        180f, 180f, false, brush,
                    )
                    brush.style = Paint.Style.FILL
                    canvas.drawCircle(center - radius * 0.16f,
                        center + radius * 0.40f, size * 0.07f, brush)
                    canvas.drawCircle(center + radius * 0.17f,
                        center + radius * 0.40f, size * 0.07f, brush)
                }
                OverlayIds.START_ICON, OverlayIds.FINISH_ICON ->
                    canvas.drawCircle(center, center, radius * 0.25f, brush)
                else -> {
                    val path = Path().apply {
                        moveTo(center + radius * 0.01f, center - radius * 0.60f)
                        lineTo(center - radius * 0.34f, center + radius * 0.06f)
                        lineTo(center - radius * 0.04f, center + radius * 0.05f)
                        lineTo(center - radius * 0.20f, center + radius * 0.60f)
                        lineTo(center + radius * 0.40f, center - radius * 0.13f)
                        lineTo(center + radius * 0.13f, center - radius * 0.12f)
                        close()
                    }
                    canvas.drawPath(path, brush)
                }
            }
        }
        picture
    }

    private fun drawVehicle(canvas: Canvas, brush: Paint, size: Int, color: Color) {
        val c = size / 2f
        val p = Path().apply {
            moveTo(c, size * 0.10f)
            lineTo(size * 0.84f, size * 0.87f)
            lineTo(c, size * 0.68f)
            lineTo(size * 0.16f, size * 0.87f)
            close()
        }
        brush.color = Color.White.toArgb()
        canvas.drawCircle(c, c, size * 0.44f, brush)
        brush.color = color.toArgb()
        canvas.drawPath(p, brush)
    }
}
```

---

### 6. `app/src/main/java/app/evcockpit/ui/unified/map/OverlayInstaller.kt`

```kotlin
package app.evcockpit.ui.unified.map

import android.content.Context
import android.os.Looper
import androidx.compose.ui.graphics.toArgb
import app.evcockpit.domain.nav.PolylineGeometry
import app.evcockpit.domain.nav.WgsPoint
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * C2: yalnız gerçek OverlayModel'i çiz. Boş model hiçbir çizgi/pin üretmez.
 * Tüm MapLibre işlemleri ana iş parçacığında gerçekleştirilir.
 * Stil yeniden yüklenince kaynaklar otomatik olarak geri kurulur.
 */
class OverlayInstaller(context: Context) {
    private val icons = MarkerIcons(context.applicationContext)
    private val t = CockpitDesignTokens

    /** Kaynak/katman zaten varsa yeniden eklemek yerine verisini günceller. */
    fun install(style: Style, ui: OverlayModel) {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "MapLibre stilleri yalnız ana iş parçacığında güncellenebilir."
        }
        icons.all().forEach { (id, bmp) -> style.addImage(id, bmp) }

        repeat(t.maxAlternativeOverlayRoutes) { index ->
            updateSource(style, OverlayIds.alternativeSource(index),
                lineCollection(ui.alternativeRoutes.getOrNull(index), ui.zoomLevel))
            if (style.getLayer(OverlayIds.alternativeLayer(index)) == null) {
                style.addLayer(LineLayer(OverlayIds.alternativeLayer(index),
                    OverlayIds.alternativeSource(index)).withProperties(
                    PropertyFactory.lineColor(t.routeAltColor.toArgb()),
                    PropertyFactory.lineWidth(t.routeAlternativeWidth.value),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                ))
            }
        }

        updateSource(style, OverlayIds.SELECTED_ROUTE_SOURCE,
            lineCollection(ui.selectedRoute, ui.zoomLevel))
        if (style.getLayer(OverlayIds.SELECTED_ROUTE_CASING) == null) {
            style.addLayer(LineLayer(OverlayIds.SELECTED_ROUTE_CASING,
                OverlayIds.SELECTED_ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(t.routeCasingColor.toArgb()),
                PropertyFactory.lineWidth(t.routeCasingWidth.value),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ))
        }
        if (style.getLayer(OverlayIds.SELECTED_ROUTE_LAYER) == null) {
            style.addLayer(LineLayer(OverlayIds.SELECTED_ROUTE_LAYER,
                OverlayIds.SELECTED_ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(t.accent.toArgb()),
                PropertyFactory.lineWidth(t.routeSelectedWidth.value),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ))
        }

        updateSource(style, OverlayIds.WEATHER_SOURCE,
            FeatureCollection.fromFeatures(ui.weatherAlerts
                .filter { it.point.valid() && it.rainExpected == true }
                .take(t.maxOverlayWeatherAlerts)
                .map { warning ->
                    Feature.fromGeometry(point(warning.point)).apply {
                        addStringProperty("icon", OverlayIds.WEATHER_ICON)
                        addStringProperty("label",
                            warning.text?.takeIf { it.isNotBlank() }
                                ?: "Yağış uyarısı var")
                    }
                }))
        if (style.getLayer(OverlayIds.WEATHER_LAYER) == null) {
            style.addLayer(SymbolLayer(OverlayIds.WEATHER_LAYER,
                OverlayIds.WEATHER_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.textField(Expression.get("label")),
                PropertyFactory.textColor(t.caution.toArgb()),
                PropertyFactory.textSize(t.mapLabelSize.value),
                PropertyFactory.textAllowOverlap(true),
            ))
        }

        updateSource(style, OverlayIds.STATIONS_SOURCE,
            FeatureCollection.fromFeatures(ui.stations
                .filter { it.point.valid() }.take(t.maxOverlayStations).map { station ->
                    Feature.fromGeometry(point(station.point)).apply {
                        addStringProperty("id", station.id)
                        addStringProperty("icon", stationIcon(station))
                        addNumberProperty("scale", if (station.selected)
                            t.mapMarkerSelectedScale else 1.0)
                    }
                }))
        if (style.getLayer(OverlayIds.STATIONS_LAYER) == null) {
            style.addLayer(SymbolLayer(OverlayIds.STATIONS_LAYER,
                OverlayIds.STATIONS_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconSize(Expression.get("scale")),
                PropertyFactory.iconAllowOverlap(true),
            ))
        }

        val ends = buildList {
            ui.origin?.takeIf { it.valid() }?.let {
                add(iconFeature(it, OverlayIds.START_ICON))
            }
            ui.destination?.takeIf { it.valid() }?.let {
                add(iconFeature(it, OverlayIds.FINISH_ICON))
            }
        }
        updateSource(style, OverlayIds.ENDPOINTS_SOURCE,
            FeatureCollection.fromFeatures(ends))
        if (style.getLayer(OverlayIds.ENDPOINTS_LAYER) == null) {
            style.addLayer(SymbolLayer(OverlayIds.ENDPOINTS_LAYER,
                OverlayIds.ENDPOINTS_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconAllowOverlap(true),
            ))
        }

        val vehicle = ui.vehicle?.takeIf { it.point.valid() }
        val accuracy = vehicle?.accuracyMeters?.takeIf {
            it.isFinite() && it > 0.0 && it <= 50_000.0
        }
        updateSource(style, OverlayIds.VEHICLE_ACCURACY_SOURCE,
            FeatureCollection.fromFeatures(if (vehicle != null && accuracy != null)
                listOf(Feature.fromGeometry(accuracyCircle(vehicle.point, accuracy)))
                else emptyList()))
        if (style.getLayer(OverlayIds.VEHICLE_ACCURACY_LAYER) == null) {
            style.addLayer(FillLayer(OverlayIds.VEHICLE_ACCURACY_LAYER,
                OverlayIds.VEHICLE_ACCURACY_SOURCE).withProperties(
                PropertyFactory.fillColor(t.vehicleAccuracyFill.toArgb()),
            ))
        }

        updateSource(style, OverlayIds.VEHICLE_SOURCE,
            FeatureCollection.fromFeatures(vehicle?.let { fix ->
                val validHeading = fix.bearingDegrees?.takeIf { it.isFinite() }
                listOf(Feature.fromGeometry(point(fix.point)).apply {
                    addStringProperty("icon",
                        if (validHeading == null) OverlayIds.VEHICLE_UNKNOWN_ICON
                        else OverlayIds.VEHICLE_ICON)
                    addNumberProperty("bearing", validHeading ?: 0.0)
                })
            } ?: emptyList()))
        if (style.getLayer(OverlayIds.VEHICLE_LAYER) == null) {
            style.addLayer(SymbolLayer(OverlayIds.VEHICLE_LAYER,
                OverlayIds.VEHICLE_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconRotate(Expression.get("bearing")),
                PropertyFactory.iconRotationAlignment(
                    Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true),
            ))
        }
    }

    private fun stationIcon(station: OverlayStation): String {
        val kw = station.powerKw?.takeIf { it.isFinite() && it >= 0.0 }
        return when {
            kw == null -> OverlayIds.STATION_UNKNOWN_ICON
            !station.dc -> OverlayIds.STATION_AC_ICON
            kw >= 150.0 -> OverlayIds.STATION_150_ICON
            kw >= 100.0 -> OverlayIds.STATION_100_ICON
            kw >= 50.0 -> OverlayIds.STATION_50_ICON
            else -> OverlayIds.STATION_AC_ICON
        }
    }

    private fun iconFeature(point: OverlayPoint, iconId: String) =
        Feature.fromGeometry(point(point)).apply { addStringProperty("icon", iconId) }

    private fun point(p: OverlayPoint): Point = Point.fromLngLat(p.longitude, p.latitude)

    private fun updateSource(style: Style, id: String, collection: FeatureCollection) {
        val current = style.getSource(id) as? GeoJsonSource
        if (current != null) current.setGeoJson(collection)
        else style.addSource(GeoJsonSource(id, collection))
    }

    private fun lineCollection(
        geometry: List<OverlayPoint>?,
        zoom: Double?,
    ): FeatureCollection {
        val points = geometry.orEmpty().filter { it.valid() }
        if (points.size < 2) return FeatureCollection.fromFeatures(emptyList())
        // Zoom eşiği yalnız sadeleştirme kopyasını etkiler; orijinal rota saklanır.
        val scale = when {
            zoom == null || !zoom.isFinite() -> 1.0
            zoom < 8.0 -> 4.0
            zoom < 12.0 -> 2.0
            else -> 1.0
        }
        val input = points.map { WgsPoint(it.latitude, it.longitude) }
        val reduced = PolylineGeometry.simplify(
            input, t.overlaySimplificationToleranceMeters * scale)
        val stride = (reduced.size / t.overlayMaxSampledRoutePoints).coerceAtLeast(1)
        val sampled = reduced.filterIndexed { i, _ ->
            i % stride == 0 || i == reduced.lastIndex
        }
        val feature = Feature.fromGeometry(LineString.fromLngLats(
            sampled.map { Point.fromLngLat(it.lon, it.lat) },
        ))
        return FeatureCollection.fromFeatures(listOf(feature))
    }

    private fun accuracyCircle(center: OverlayPoint, meters: Double): Polygon {
        val earth = PolylineGeometry.EARTH_RADIUS_M
        val angle = meters / earth
        val cosLatitude = cos(Math.toRadians(center.latitude)).coerceAtLeast(0.00001)
        val ring = (0..t.accuracyCircleVertices).map { i ->
            val theta = 2.0 * PI * i / t.accuracyCircleVertices
            Point.fromLngLat(
                center.longitude + Math.toDegrees(angle * cos(theta) / cosLatitude),
                center.latitude + Math.toDegrees(angle * sin(theta)),
            )
        }
        return Polygon.fromLngLats(listOf(ring))
    }
}
```

---

### 7. `app/src/main/java/app/evcockpit/ui/unified/map/CameraController.kt`

```kotlin
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
```

---

### 8. `app/src/main/java/app/evcockpit/ui/unified/map/MapHost.kt`

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
import android.os.SystemClock
import app.evcockpit.domain.nav.PolylineGeometry
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
    private val overlayInstaller = OverlayInstaller(appContext)
    private var overlayModel: OverlayModel = OverlayModel()
    private var lastVehicleUpdateElapsedMs: Long = 0L
    private var lastVehicle: OverlayVehicle? = null
    var cameraController: CameraController? = null
        private set

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
                cameraController = CameraController(instance)
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
                runCatching { overlayInstaller.install(style, overlayModel) }
                    .onFailure { debug("Overlay kurulamadı: ${it.javaClass.simpleName}") }
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

    /**
     * Kaynaklar yalnız gerçek ViewModel verisiyle güncellenir; boş durum çizmez.
     * Araç imleci en çok 2Hz, hız düşükken heading dondurulur.
     */
    fun updateOverlays(model: OverlayModel) {
        val now = SystemClock.elapsedRealtime()
        val incoming = model.vehicle
        val previous = lastVehicle
        val chosen = if (incoming == null) {
            lastVehicle = null
            null
        } else if (previous != null &&
            now - lastVehicleUpdateElapsedMs <
                (1_000.0 / CockpitDesignTokens.overlayMaxPositionHz).toLong()) {
            previous
        } else {
            val speed = incoming.speedMetersPerSecond
            val bearing = if (speed != null && speed.isFinite() &&
                speed < CockpitDesignTokens.overlayCarHeadingFreezeMetersPerSecond)
                previous?.bearingDegrees
            else incoming.bearingDegrees?.let { angle ->
                PolylineGeometry.circularHeadingDegrees(
                    previous?.bearingDegrees, angle, 0.35)
            }
            incoming.copy(bearingDegrees = bearing).also {
                lastVehicle = it
                lastVehicleUpdateElapsedMs = now
            }
        }
        overlayModel = model.copy(vehicle = chosen)
        readyMap?.style?.let { style ->
            overlayInstaller.install(style, overlayModel)
        }
        chosen?.let { cameraController?.follow(it) }
    }

    fun detach() {
        cameraController?.dispose()
        cameraController = null
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
            host.detach()
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

### 9. `app/src/test/java/app/evcockpit/domain/nav/PolylineGeometryTest.kt`

```kotlin
package app.evcockpit.domain.nav

import org.junit.Assert.*
import org.junit.Test

class PolylineGeometryTest {
    @Test fun duzYolAraNoktaSilinirBasVeSonKorunur() {
        val path = listOf(
            WgsPoint(39.0, 34.0),
            WgsPoint(39.0005, 34.0005),
            WgsPoint(39.0010, 34.0010),
        )
        val simplified = PolylineGeometry.simplify(path, 10.0)
        assertEquals(2, simplified.size)
        assertEquals(path.first(), simplified.first())
        assertEquals(path.last(), simplified.last())
    }

    @Test fun sertDonusToleransIcindeKaybolmaz() {
        val path = listOf(
            WgsPoint(39.0, 34.0),
            WgsPoint(39.01, 34.0),
            WgsPoint(39.01, 34.01),
        )
        val reduced = PolylineGeometry.simplify(path, 20.0)
        assertEquals(3, reduced.size)
        assertEquals(path[1], reduced[1])
    }

    @Test fun ufakVeyaGecersizToleranstaOrijinalKorunur() {
        val path = listOf(
            WgsPoint(39.0, 34.0),
            WgsPoint(39.001, 34.002),
            WgsPoint(39.004, 34.005),
        )
        assertEquals(path, PolylineGeometry.simplify(path, 0.0))
        assertEquals(path, PolylineGeometry.simplify(path, -1.0))
        assertEquals(path, PolylineGeometry.simplify(path, Double.NaN))
    }

    @Test fun kuzeyAcisi359Ve1Arasinda0aYakinKalir() {
        val heading = PolylineGeometry.circularHeadingDegrees(359.0, 1.0, 0.5)!!
        assertTrue("Dairesel ortalama kuzeye yakın olmalı: $heading",
            heading < 2.0 || heading > 358.0)
    }

    @Test fun gecersizHeadingUygulanmaz() {
        assertNull(PolylineGeometry.circularHeadingDegrees(80.0, Double.NaN, 0.5))
        assertNull(PolylineGeometry.circularHeadingDegrees(80.0, 20.0, -0.5))
        assertEquals(270.0,
            PolylineGeometry.circularHeadingDegrees(null, -90.0, 0.5)!!, 0.0001)
    }
}
```

---

### 10. `app/src/test/java/app/evcockpit/ui/unified/map/OverlayModelTest.kt`

```kotlin
package app.evcockpit.ui.unified.map

import org.junit.Assert.*
import org.junit.Test

class OverlayModelTest {
    @Test fun baslangictaSahteRotaIstasyonHavaAracUretilmez() {
        val empty = OverlayModel()
        assertFalse(empty.hasAnyGeometry)
        assertTrue(empty.selectedRoute.isEmpty())
        assertTrue(empty.alternativeRoutes.isEmpty())
        assertTrue(empty.stations.isEmpty())
        assertTrue(empty.weatherAlerts.isEmpty())
        assertNull(empty.vehicle)
    }

    @Test fun wgs84KoordinatSinirlariDogrulanir() {
        assertTrue(OverlayPoint(90.0, 180.0).valid())
        assertTrue(OverlayPoint(-90.0, -180.0).valid())
        assertFalse(OverlayPoint(91.0, 0.0).valid())
        assertFalse(OverlayPoint(Double.NaN, 34.0).valid())
    }

    @Test fun overlayKimlikleriTekilVeSiraDogrudur() {
        val ids = buildList {
            repeat(3) {
                add(OverlayIds.alternativeSource(it))
                add(OverlayIds.alternativeLayer(it))
            }
            addAll(listOf(
                OverlayIds.SELECTED_ROUTE_SOURCE, OverlayIds.SELECTED_ROUTE_CASING,
                OverlayIds.SELECTED_ROUTE_LAYER,
                OverlayIds.WEATHER_SOURCE, OverlayIds.WEATHER_LAYER,
                OverlayIds.STATIONS_SOURCE, OverlayIds.STATIONS_LAYER,
                OverlayIds.ENDPOINTS_SOURCE, OverlayIds.ENDPOINTS_LAYER,
                OverlayIds.VEHICLE_ACCURACY_SOURCE, OverlayIds.VEHICLE_ACCURACY_LAYER,
                OverlayIds.VEHICLE_SOURCE, OverlayIds.VEHICLE_LAYER,
            ))
        }
        assertEquals(ids.size, ids.distinct().size)
        assertEquals("src-route-sel", OverlayIds.SELECTED_ROUTE_SOURCE)
    }

    @Test fun gucOlcumunuTahminEtmedenKabulEder() {
        val station = OverlayStation("s1", OverlayPoint(39.0, 35.0), null, dc = true)
        assertNull(station.powerKw)
        assertFalse(station.selected)
    }
}
```
