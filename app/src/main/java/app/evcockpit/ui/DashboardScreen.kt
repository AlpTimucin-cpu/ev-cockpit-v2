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
