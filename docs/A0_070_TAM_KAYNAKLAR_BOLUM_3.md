# A0 0.7.0 — TAM KAYNAKLAR / BÖLÜM 3 (6 güncel, 4 silinen dosya)

**Dal:** `feature/a0-f1-product-main-20261124`  
**Bölüm 3:** release DEMO izolasyonu, debug/release/açılış JVM testleri ve silinmiş dört eski ekran dosyasının **A0 öncesindeki tam içerikleri** (geri almayı kolaylaştırmak için). Silinen dosyaların A0 sonrası içeriği yoktur. 
Diğer tam kaynaklar: [Bölüm 1](./A0_070_TAM_KAYNAKLAR_BOLUM_1.md) · [Bölüm 2](./A0_070_TAM_KAYNAKLAR_BOLUM_2.md).


---

## Güncel 1. `app/src/release/java/app/evcockpit/ui/finalhmi/BuildTypeDemoProvider.kt`

```kotlin
package app.evcockpit.ui.finalhmi

/** Release üretiminde DEMO mapper veya örnek veriye hiçbir referans yoktur. */
internal object BuildTypeDemoProvider {
    const val available = false
    fun state(@Suppress("UNUSED_PARAMETER") nowEpochMs: Long): DashboardUiState? = null
}
```

---

## Güncel 2. `app/src/testDebug/java/app/evcockpit/ui/finalhmi/DebugDemoMapperIsolationTest.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class DebugDemoMapperIsolationTest {
    private val now = 1_795_460_000_000L

    @Test fun demoModeliYalnizAyrikDemoMapperUretir() {
        val demo = DemoDashboardMapper.map(now)
        assertTrue(demo.demo)
        assertEquals(DashboardOrigin.DEMO, demo.socPercent.source)
        assertEquals(DashboardOrigin.DEMO, demo.speedKmh.source)
        assertTrue(demo.weatherSamples.isNotEmpty())
        val real = RealDashboardMapper.map(
            RealDashboardInput(
                gpsSpeedMetersPerSecond = StampedValue(
                    25.0, DashboardOrigin.DEMO, now),
                obdVerifiedSocPercent = StampedValue(
                    75.0, DashboardOrigin.DEMO, now),
                modelConsumptionKwhPer100Km = StampedValue(
                    17.0, DashboardOrigin.DEMO, now),
            ), now)
        assertFalse(real.demo)
        assertNull(real.speedKmh.value)
        assertNull(real.socPercent.value)
        assertNull(real.consumptionKwhPer100Km.value)
    }

}
```

---

## Güncel 3. `app/src/testDebug/java/app/evcockpit/ui/finalhmi/A0DebugModeTest.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class A0DebugModeTest {
    @Test fun demoKullaniciAcanaKadarGercekModBosKalir() {
        assertTrue(BuildTypeDemoProvider.available)
        val real = RealDashboardMapper.map(RealDashboardInput(), 1_795_460_000_000L)
        assertFalse(real.demo)
        assertNull(real.socPercent.value)
        assertNull(real.speedKmh.value)
    }

    @Test fun debugOrnegiAyriKaynakVeAcikDemoEtiketlidir() {
        val state = BuildTypeDemoProvider.state(1_795_460_000_000L)
        assertTrue(state.demo)
        assertEquals(DashboardOrigin.DEMO, state.socPercent.source)
    }
}
```

---

## Güncel 4. `app/src/testRelease/java/app/evcockpit/ui/finalhmi/A0ReleaseDemoExclusionTest.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class A0ReleaseDemoExclusionTest {
    @Test fun releaseDemoModunuAslaSunmaz() {
        assertFalse(BuildTypeDemoProvider.available)
        assertNull(BuildTypeDemoProvider.state(1_795_460_000_000L))
    }
}
```

---

## Güncel 5. `app/src/test/java/app/evcockpit/ui/finalhmi/DashboardMapperAndLayoutTest.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class DashboardMapperAndLayoutTest {
    private val now = 1_795_460_000_000L

    @Test fun gercekModBosKalirDemoGorunmez() {
        val empty = RealDashboardMapper.map(RealDashboardInput(), now)
        assertFalse(empty.demo)
        assertNull(empty.speedKmh.value)
        assertNull(empty.socPercent.value)
        assertNull(empty.estimatedRangeKm.value)
        assertNull(empty.chargeTotalLabel.value)
        assertTrue(empty.weatherSamples.isEmpty())
    }

    @Test fun hizYalnizTazeGpsKaynaklidir() {
        val valid = RealDashboardMapper.map(RealDashboardInput(
            gpsSpeedMetersPerSecond = StampedValue(
                25.0, DashboardOrigin.GPS, now - 2_000L),
        ), now)
        assertEquals(90.0, valid.speedKmh.value!!, 0.001)
        assertEquals(DashboardOrigin.GPS, valid.speedKmh.source)
        val stale = RealDashboardMapper.map(RealDashboardInput(
            gpsSpeedMetersPerSecond = StampedValue(
                25.0, DashboardOrigin.GPS, now - 20_000L),
        ), now)
        assertNull(stale.speedKmh.value)
        val wrong = RealDashboardMapper.map(RealDashboardInput(
            gpsSpeedMetersPerSecond = StampedValue(
                25.0, DashboardOrigin.OBD_VERIFIED, now),
        ), now)
        assertNull(wrong.speedKmh.value)
    }

    @Test fun manuelSocGuzergahIlerlemesiYokkenAyniKalir() {
        val data = RealDashboardInput(
            manualStartSocPercent = StampedValue(80.0, DashboardOrigin.MANUAL, now),
            modelEnergySpentKwh = StampedValue(10.0, DashboardOrigin.MODEL, now),
            vehicleUsableCapacityKwh = StampedValue(52.0, DashboardOrigin.MODEL, now),
            modelConsumptionKwhPer100Km = StampedValue(16.0, DashboardOrigin.MODEL, now),
            routeProgressReliable = false,
        )
        val manual = RealDashboardMapper.map(data, now)
        assertEquals(80.0, manual.socPercent.value!!, 0.001)
        assertEquals(DashboardOrigin.MANUAL, manual.socPercent.source)
        val progressed = RealDashboardMapper.map(
            data.copy(routeProgressReliable = true), now)
        assertEquals(60.769230769, progressed.socPercent.value!!, 0.00001)
        assertEquals(DashboardOrigin.MODEL, progressed.socPercent.source)
        assertEquals(197.5, progressed.estimatedRangeKm.value!!, 0.01)
    }

    @Test fun veriEskidiyseUzerineTahminYapilmaz() {
        val data = RealDashboardInput(
            manualStartSocPercent = StampedValue(78.0, DashboardOrigin.MANUAL, now),
            modelEnergySpentKwh = StampedValue(6.0, DashboardOrigin.MODEL, now - 300_000L),
            modelConsumptionKwhPer100Km = StampedValue(16.0, DashboardOrigin.MODEL, now - 300_000L),
            vehicleUsableCapacityKwh = StampedValue(52.0, DashboardOrigin.MODEL, now),
            routeProgressReliable = true,
        )
        val actual = RealDashboardMapper.map(data, now)
        assertEquals(78.0, actual.socPercent.value!!, 0.001)
        assertEquals(DashboardOrigin.MANUAL, actual.socPercent.source)
        assertNull(actual.estimatedRangeKm.value)
    }

    @Test fun pencereSiniflariVeAltBesSekme() {
        assertFalse(F1DashboardLayoutPolicy.useWideMapFirst(393, 873))
        assertFalse(F1DashboardLayoutPolicy.useWideMapFirst(700, 400))
        assertTrue(F1DashboardLayoutPolicy.useWideMapFirst(960, 540))
        assertTrue(F1DashboardLayoutPolicy.useWideMapFirst(1280, 800))
        assertEquals(listOf("Harita", "Rota", "Şarj", "Enerji", "Ayarlar"),
            F1DashboardLayoutPolicy.tabs)
        assertEquals(5, FinalDashboardTab.entries.size)
    }

    @Test fun tiklamaHedefiEnAz56Dp() {
        assertTrue(CockpitDesignTokens.target.value >= 56f)
        assertTrue(CockpitDesignTokens.tabBarHeight.value >= 56f)
    }
}
```

---

## Güncel 6. `app/src/test/java/app/evcockpit/ui/finalhmi/A0NavigationStructureTest.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import org.junit.Assert.*
import org.junit.Test

class A0NavigationStructureTest {
    @Test fun anaSekmeHaritaVeSadeceBesSekmeVardir() {
        assertEquals(FinalDashboardTab.HARITA, FinalDashboardTab.entries.first())
        assertEquals(5, FinalDashboardTab.entries.size)
        assertEquals(listOf("Harita","Rota","Şarj","Enerji","Ayarlar"),
            FinalDashboardTab.entries.map { it.titleTr })
        assertEquals(FinalDashboardTab.entries.size,
            FinalDashboardTab.entries.map { it.name }.distinct().size)
    }
}
```

---

## A0 İLE SİLİNEN DOSYALAR — Eski tam sürümler

Bu bölümdeki kodlar yalnız tarihsel/referans amaçlıdır; A0 APK'sına dahil edilmez. Mola entegrasyon modülleri ve Android Auto kaynakları değiştirilmedi.


### SİLİNDİ 1. `app/src/main/java/app/evcockpit/ui/finalhmi/MapFirstDashboardActivity.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue

/**
 * F1 bağımsız ekranı: eski ana Activity ve A–C sekmeleri aynen yerinde.
 * Sadece Ayarlar'daki açık kullanıcı dokunuşuyla açılır.
 * Varsayılan DEMO; "Boş veri" düğmesi gerçek modun veri-yok davranışını gösterir.
 */
class MapFirstDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MapFirstDashboardViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            FinalCockpitTheme {
                MapFirstDashboardScreen(
                    state = state,
                    onDemoChange = vm::showDemo,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                )
            }
        }
    }
}
```

### SİLİNDİ 2. `app/src/main/java/app/evcockpit/ui/finalhmi/MapFirstDashboardPreview.kt`

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

/**
 * H1: yalnız hava şeridi. Harita veya cihaz verisi kullanılmaz.
 * 360dp, fontScale 1.3: üç örnek eşit genişlikte çok satırlı.
 */
@Preview(
    name = "F1 H1 hava 3 örnek 360dp / yazı 1.3",
    widthDp = 360, heightDp = 250,
    fontScale = 1.3f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1HavaUcOrnekBuyukYazi() {
    FinalCockpitTheme {
        RouteWeatherStrip(
            state = DemoDashboardMapper.map(1_795_460_000_000L),
        )
    }
}

/**
 * H1: beş örnek; 393dp telefonda kaydırma ve kenar solması görünür.
 * Ek örnekler yalnız DEMO mappper'dan gelir, canlı hava verisi değildir.
 */
@Preview(
    name = "F1 H1 hava 5 örnek 393dp / kaydır",
    widthDp = 393, heightDp = 250,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1HavaBesOrnekKaydir() {
    val demo = DemoDashboardMapper.map(1_795_460_000_000L)
    val samples = demo.weatherSamples
    FinalCockpitTheme {
        RouteWeatherStrip(
            state = demo.copy(
                weatherSamples = samples + listOf(
                    samples[0].copy(distanceAlongKm = 100.0),
                    samples[1].copy(distanceAlongKm = 125.0),
                ),
            ),
        )
    }
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

### SİLİNDİ 3. `app/src/main/java/app/evcockpit/ui/DashboardScreen.kt`

```kotlin
package app.evcockpit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.evcockpit.obd.VehicleState
import app.evcockpit.obd.SocReading
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.location.SpeedSelector
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.RouteUiState
import kotlin.math.abs

/**
 * Landscape-first cockpit; no placeholder values are represented as vehicle readings.
 * The existing MapLibre route and map attribution remain unchanged.
 */
@Composable
fun DashboardScreen(
    s: VehicleState,
    soc: SocReading,
    usableKwh: Double,
    gps: GpsSnapshot,
    navigation: RouteUiState,
    weatherSummary: String,
    onSelectDestination: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gpsFresh = rememberGpsFreshness(gps)
    val speed = SpeedSelector.select(
        obdConnected = s.connected,
        demo = s.demo,
        obdKmh = s.speedKmh,
        gpsKmh = gps.speedKmh,
        gpsFresh = gpsFresh,
    )
    BoxWithConstraints(
        modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(10.dp),
    ) {
        val wide = CockpitLayoutPolicy.useTwoColumns(maxWidth.value.toInt())
        val compact = CockpitLayoutPolicy.compactDashboard(maxHeight.value.toInt())
        if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardReadouts(
                    speed.kmh, speed.source, s, soc, usableKwh, compact,
                    Modifier.weight(0.36f).fillMaxHeight(),
                )
                OnlineMap(
                    modifier = Modifier.weight(0.64f).fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp)),
                    route = navigation.route,
                    destination = navigation.destination,
                    gps = gps,
                    weatherSummary = weatherSummary,
                    onSelectDestination = onSelectDestination,
                )
            }
        } else {
            // Split screen and large font settings still get a scrollable view.
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DashboardReadouts(
                    speed.kmh, speed.source, s, soc, usableKwh, true,
                    Modifier.fillMaxWidth().heightIn(min = 260.dp),
                )
                OnlineMap(
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    route = navigation.route,
                    destination = navigation.destination,
                    gps = gps,
                    weatherSummary = weatherSummary,
                    onSelectDestination = onSelectDestination,
                )
            }
        }
    }
}

@Composable
private fun DashboardReadouts(
    speedKmh: Double?,
    speedSource: String,
    s: VehicleState,
    soc: SocReading,
    usableKwh: Double,
    compact: Boolean,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape)
            .padding(if (compact) 12.dp else 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "CANLI GÖSTERGE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    CockpitLayoutPolicy.speedLabel(speedKmh),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = if (compact) 59.sp else 78.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                )
                Text(
                    "km/sa",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 6.dp, bottom = 13.dp),
                )
            }
            Text(
                "Hız kaynağı: $speedSource",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
            PowerBar(s.powerKw, compact)
            BatteryBlock(soc, usableKwh, compact)
            Text(
                when {
                    s.demo -> "DEMO • Gerçek araç verisi değil"
                    s.connected -> "OBD bağlı • PID verisi doğrulanmadı"
                    else -> "OBD bağlı değil • Veri yoksa -- gösterilir"
                },
                color = when {
                    s.demo -> CockpitColors.amber
                    s.connected -> CockpitColors.green
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 11.sp,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun PowerBar(kw: Double?, compact: Boolean, maxKw: Double = 125.0) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val midpointColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "GÜÇ / REJENERASYON",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
        )
        Text(
            kw?.let { "%.0f kW".format(it) } ?: "-- kW",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 18.sp else 20.sp,
            fontWeight = FontWeight.Medium,
        )
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val mid = size.width / 2f
            drawRoundRect(trackColor, cornerRadius = CornerRadius(6.dp.toPx()))
            if (kw != null && kw.isFinite()) {
                val width = mid * (abs(kw) / maxKw).coerceIn(0.0, 1.0).toFloat()
                if (kw >= 0) {
                    drawRect(CockpitColors.accent, Offset(mid, 0f), Size(width, size.height))
                } else {
                    drawRect(CockpitColors.green, Offset(mid - width, 0f), Size(width, size.height))
                }
            }
            drawRect(midpointColor, Offset(mid - 1f, 0f), Size(2f, size.height))
        }
    }
}

@Composable
private fun BatteryBlock(soc: SocReading, usableKwh: Double, compact: Boolean) {
    val pct = soc.pct?.takeIf { it.isFinite() }?.coerceIn(0.0, 100.0)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val levelColor = when {
        pct == null -> MaterialTheme.colorScheme.onSurfaceVariant
        pct > 20 -> CockpitColors.green
        pct > 10 -> CockpitColors.amber
        else -> Color(0xFFFF6B75)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("BATARYA / SoC", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                pct?.let { "%.0f".format(it) } ?: "--",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = if (compact) 35.sp else 44.sp,
                fontWeight = FontWeight.Light,
            )
            Text(
                "%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 17.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            if (pct != null) {
                Text(
                    "≈ %.1f kWh • TAHMİN".format(pct / 100.0 * usableKwh),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
        }
        Canvas(Modifier.fillMaxWidth().height(7.dp)) {
            drawRoundRect(trackColor, cornerRadius = CornerRadius(5.dp.toPx()))
            if (pct != null) {
                drawRoundRect(
                    levelColor,
                    size = Size(size.width * (pct / 100.0).toFloat(), size.height),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
            }
        }
        Text(
            "Kaynak: ${soc.source}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
```

### SİLİNDİ 4. `app/src/main/java/app/evcockpit/ui/tesla/TeslaDashboard.kt`

```kotlin
package app.evcockpit.ui.tesla

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.location.SpeedSelector
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.navigation.RouteProgress
import app.evcockpit.navigation.RouteUiState
import app.evcockpit.obd.SocReading
import app.evcockpit.obd.VehicleState
import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.charging.ChargerEvidence
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.ui.CockpitTheme
import app.evcockpit.ui.OnlineMap

enum class TeslaAction(val title: String, val symbol: String) {
    ROTA("Rota", "⌖"),
    PLAN("Plan", "⚡"),
    MEDYA("Medya", "♫"),
    VIDEO("Yolcu video", "▶"),
    MOLA("Mola", "M"),
    AYARLAR("Ayarlar", "⚙"),
}

object TeslaLayoutPolicy {
    fun useThreePanes(widthDp: Int, heightDp: Int): Boolean =
        widthDp >= 700 && widthDp > heightDp
}

@Composable
fun DriveModeScaffold(
    left: @Composable () -> Unit,
    center: @Composable () -> Unit,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier.fillMaxSize().background(TeslaTokens.background)
            .padding(TeslaTokens.contentPadding),
    ) {
        if (TeslaLayoutPolicy.useThreePanes(maxWidth.value.toInt(), maxHeight.value.toInt())) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(TeslaTokens.leftPaneWeight)
                    .fillMaxHeight().verticalScroll(rememberScrollState())) { left() }
                Box(Modifier.weight(TeslaTokens.mapWeight).fillMaxHeight()) { center() }
                Box(Modifier.weight(TeslaTokens.actionWeight)
                    .fillMaxHeight().verticalScroll(rememberScrollState())) { right() }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.fillMaxWidth()) { left() }
                Box(Modifier.fillMaxWidth().height(420.dp)) { center() }
                Box(Modifier.fillMaxWidth()) { right() }
            }
        }
    }
}

@Composable
fun ActionRail(
    onAction: (TeslaAction) -> Unit,
    molaAvailable: Boolean = false,
    mode: DriveMode = DriveMode.UNKNOWN,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TeslaAction.entries.forEach { action ->
            val enabled = DriveModePolicy.allowedAction(action, mode, molaAvailable)
            OutlinedButton(
                onClick = { onAction(action) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = TeslaTokens.touchTarget)
                    .semantics {
                        contentDescription = action.title + when {
                            action == TeslaAction.MOLA && !molaAvailable ->
                                " • kurulu değilse resmî kurulum rehberi"
                            action == TeslaAction.VIDEO && mode != DriveMode.PARKED ->
                                " • sürüşte kapalı"
                            else -> ""
                        }
                    },
                shape = RoundedCornerShape(TeslaTokens.cornerRadius),
                border = BorderStroke(1.dp, TeslaTokens.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TeslaTokens.text,
                    disabledContentColor = TeslaTokens.secondaryText,
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 9.dp),
            ) {
                Text(action.symbol, fontSize = 21.sp)
                Spacer(Modifier.width(5.dp))
                Text(
                    action.title,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

    }
}

@Composable
fun MapHeadlineBar(summary: MapHeadline) {
    Column(
        Modifier.fillMaxWidth().background(TeslaTokens.surface)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(summary.title, color = TeslaTokens.secondaryText,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(summary.distance, color = TeslaTokens.text,
            style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(summary.eta, color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f), maxLines = 2)
            Text(summary.arrivalSoc, color = TeslaTokens.estimate,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f), maxLines = 2)
        }
    }
}

@Composable
fun NextStopBar(stop: app.evcockpit.planner.model.PlannedStop, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(TeslaTokens.cornerRadius),
        color = TeslaTokens.surfaceRaised,
        modifier = Modifier.fillMaxWidth().heightIn(min = TeslaTokens.touchTarget),
        border = BorderStroke(1.dp, TeslaTokens.outline),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("PLANLANAN ŞARJ DURAĞI • DETAY",
                color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.labelMedium)
            Text(stop.charger.name, color = TeslaTokens.text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "~" + stop.charger.powerKw.value.toInt() + " kW • hedef %" +
                    stop.departureSocPct + " • şarj ~" +
                    stop.chargeMinutes.toInt() + " dk • TAHMİN • doluluk bilinmiyor",
                color = TeslaTokens.secondaryText, style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun MapHero(
    route: NavigationRoute?,
    destination: GeoPoint?,
    gps: GpsSnapshot,
    gpsFresh: Boolean,
    plan: PlanResult.Success?,
    weatherSummary: String,
    onSelectDestination: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
    previewMap: Boolean = false,
    allowRetry: Boolean = true,
    alternatives: List<NavigationRoute> = emptyList(),
) {
    val position = if (gpsFresh && route != null && gps.point != null) {
        RouteProgress.locate(route, gps.point)
    } else null
    val headline = MapHeadlinePolicy.create(
        route, destination != null, position, plan, System.currentTimeMillis(),
    )
    val next = MapHeadlinePolicy.nextStop(plan, route, position)
    var selectedStation by remember { mutableStateOf<ChargerCandidate?>(null) }
    var mapFailed by remember { mutableStateOf(false) }
    var mapRetry by remember { mutableIntStateOf(0) }
    Column(
        modifier.clip(RoundedCornerShape(TeslaTokens.cornerRadius))
            .background(TeslaTokens.surface),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MapHeadlineBar(headline)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (previewMap) {
                Box(
                    Modifier.fillMaxSize().background(TeslaTokens.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) { Text("HARİTA ÖNİZLEME • DEMO", color = TeslaTokens.secondaryText) }
            } else {
                key(mapRetry) {
                    OnlineMap(
                        modifier = Modifier.fillMaxSize(),
                        route = route,
                        destination = destination,
                        gps = gps,
                        weatherSummary = weatherSummary,
                        onSelectDestination = onSelectDestination,
                        stopMarkers = plan?.stops?.map { it.charger } ?: emptyList(),
                        onStopSelected = { selectedStation = it },
                        onLoadFailed = { mapFailed = true },
                        onStyleReady = { mapFailed = false },
                        canChangeLayer = allowRetry,
                        alternativeRoutes = alternatives,
                    )
                }
                if (mapFailed) {
                    Column(
                        Modifier.align(Alignment.Center)
                            .background(TeslaTokens.surface)
                            .padding(TeslaTokens.contentPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Harita yüklenemedi",
                            color = TeslaTokens.error,
                            style = MaterialTheme.typography.labelLarge)
                        Text("Bağlantını kontrol et. Çevrimiçi değilken harita açılamayabilir.",
                            color = TeslaTokens.text,
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            enabled = allowRetry,
                            onClick = {
                                mapFailed = false
                                mapRetry++
                            },
                            modifier = Modifier.heightIn(min = TeslaTokens.touchTarget),
                        ) { Text("Haritayı yeniden dene") }
                    }
                }
            }
        }
        if (next != null) NextStopBar(next, onClick = {
            selectedStation = next.charger
        })
    }
    selectedStation?.let { charger ->
        AlertDialog(
            onDismissRequest = { selectedStation = null },
            title = { Text("Şarj durağı • TAHMİN") },
            text = {
                Text(ChargerEvidence.details(charger).joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = { selectedStation = null }) { Text("Kapat") }
            },
        )
    }
}

@Composable
fun TeslaDashboardScreen(
    s: VehicleState,
    soc: SocReading,
    usableKwhAtSoh: Double,
    gps: GpsSnapshot,
    gpsFresh: Boolean,
    navigation: RouteUiState,
    plan: PlanResult.Success?,
    driveMode: DriveModeSnapshot,
    weatherSummary: String,
    onSelectDestination: (GeoPoint) -> Unit,
    molaInstalled: Boolean,
    guidance: DashboardGuidance = DashboardGuidance.NONE,
    onGuidanceAction: (GuidanceAction) -> Unit = {},
    onAction: (TeslaAction) -> Unit,
) {
    val speed = SpeedSelector.select(
        obdConnected = s.connected && driveMode.obdSpeedFresh, demo = s.demo, obdKmh = s.speedKmh,
        gpsKmh = gps.speedKmh, gpsFresh = gpsFresh,
    )
    val panel = DashboardPresentation.vehicle(s, soc, speed, usableKwhAtSoh,
        powerFresh = driveMode.obdPowerFresh)
    DriveModeScaffold(
        left = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(driveMode.label, color = if (driveMode.mode == DriveMode.DRIVING)
                    TeslaTokens.warning else TeslaTokens.secondaryText,
                    style = MaterialTheme.typography.labelMedium)
                VehicleStatePanel(panel, Modifier.fillMaxWidth(),
                    compact = driveMode.mode != DriveMode.DRIVING)
            }
        },
        center = {
            MapHero(navigation.route, navigation.destination, gps, gpsFresh,
                plan, weatherSummary, onSelectDestination, Modifier.fillMaxSize(),
                allowRetry = driveMode.mode != DriveMode.DRIVING,
                alternatives = navigation.alternatives)
        },
        right = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionRail(onAction, molaAvailable = molaInstalled,
                    mode = driveMode.mode,
                    modifier = Modifier.fillMaxWidth())
                TeslaGuidanceCard(
                    guidance, onGuidanceAction,
                    enabled = driveMode.mode != DriveMode.DRIVING,
                )
            }
        },
    )
}

@Preview(name = "Tesla üçlü düzen - DEMO", widthDp = 1000, heightDp = 540,
    showBackground = true)
@Composable
private fun ThreePanePreview() {
    CockpitTheme(dark = true) {
        DriveModeScaffold(
            left = {
                VehicleStatePanel(
                    VehiclePanelUiState(speedKmh = 74.0, socPercent = 60.0,
                        source = SourceKind.DEMO, status = PanelStatus.NORMAL,
                        message = "ÖNİZLEME / DEMO"),
                    compact = true,
                )
            },
            center = {
                MapHero(null, null, GpsSnapshot(), false, null, "",
                    {}, Modifier.fillMaxSize(), previewMap = true)
            },
            right = { ActionRail(onAction = {}) },
        )
    }
}

@Preview(name = "Tesla dikey düzen - DEMO", widthDp = 400, heightDp = 850,
    showBackground = true)
@Composable
private fun VerticalDashboardPreview() {
    CockpitTheme(dark = true) {
        DriveModeScaffold(
            left = { VehicleStatePanel(VehiclePanelUiState(), compact = true) },
            center = {
                MapHero(null, null, GpsSnapshot(), false, null, "",
                    {}, Modifier.fillMaxSize(), previewMap = true)
            },
            right = { ActionRail(onAction = {}) },
        )
    }
}
```
