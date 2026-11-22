package app.evcockpit.ui.tesla

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.evcockpit.obd.VehicleState
import app.evcockpit.planner.model.PlanResult
import java.util.Locale

/**
 * Professional cockpit shell: presentation only. All data comes from existing
 * validated StateFlows, not simulated telemetry or new background requests.
 * Original Mola binary/menus are not touched by this UI.
 */
data class ProStatus(
    val driveLabel: String,
    val obdLabel: String,
    val gpsLabel: String,
    val socLabel: String,
)

object ProCockpitPolicy {
    fun status(
        mode: DriveMode,
        vehicle: VehicleState,
        drive: DriveModeSnapshot,
        source: SourceKind,
    ): ProStatus {
        val driveLabel = when (mode) {
            DriveMode.PARKED -> "PARK • TELEFON"
            DriveMode.DRIVING -> "SÜRÜŞ • GÜVENLİ"
            DriveMode.UNKNOWN -> "HIZ BELİRSİZ"
        }
        val obd = when {
            vehicle.demo -> "OBD • DEMO"
            !vehicle.connected -> "OBD • BAĞLI DEĞİL"
            drive.obdSocFresh -> "OBD • PID DOĞRULANMADI"
            else -> "OBD • VERİ ESKİ/YOK"
        }
        val gps = if (drive.gpsFresh) "GPS • GÜNCEL" else "GPS • YOK/ESKİ"
        val soc = when (source) {
            SourceKind.OBD -> "SoC • OBD DOĞRULANMADI"
            SourceKind.MANUEL -> "SoC • MANUEL"
            SourceKind.DEMO -> "SoC • DEMO"
            SourceKind.ESKI -> "SoC • ESKİ"
            else -> "SoC • YOK"
        }
        return ProStatus(driveLabel, obd, gps, soc)
    }

    fun batteryPercent(value: Double?): Float =
        value?.takeIf { it.isFinite() && it in 0.0..100.0 }
            ?.div(100.0)?.toFloat() ?: 0f

    fun actionSubtitle(action: TeslaAction): String = when (action) {
        TeslaAction.ROTA -> "Hedef ve navigasyon"
        TeslaAction.PLAN -> "Şarj durakları"
        TeslaAction.MEDYA -> "Telefon medya ekranı"
        TeslaAction.VIDEO -> "Yalnız parkta"
        TeslaAction.MOLA -> "Orijinal uygulamayı aç"
        TeslaAction.AYARLAR -> "Araç, OBD ve bağlantı"
    }

    fun planLabel(plan: PlanResult.Success?): String = plan?.let {
        "ŞARJ PLANI • ${it.stops.size} DURAK • ~${it.chargingMinutes.toInt()} DK"
    } ?: "ŞARJ PLANI • HENÜZ YOK"
}

@Composable
private fun ProStatusPill(value: String, isWarning: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = if (isWarning) TeslaTokens.surfaceRaised else TeslaTokens.surface,
        border = BorderStroke(1.dp, if (isWarning) TeslaTokens.warning.copy(alpha = 0.45f)
            else TeslaTokens.outline),
    ) {
        Text(
            value, Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (isWarning) TeslaTokens.warning else TeslaTokens.secondaryText,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ProCockpitHeader(status: ProStatus, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TeslaTokens.surfaceRaised,
        border = BorderStroke(0.5.dp, TeslaTokens.outline),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            val compact = maxWidth < 780.dp
            if (!compact) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("EV COCKPIT", color = TeslaTokens.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("VOLKSWAGEN ID.4 PURE • YOL ARKADAŞI",
                            color = TeslaTokens.secondaryText,
                            style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    ProStatusPill(status.driveLabel, status.driveLabel == "HIZ BELİRSİZ")
                    ProStatusPill(status.obdLabel)
                    ProStatusPill(status.gpsLabel)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("EV COCKPIT  /  ID.4 PURE",
                            style = MaterialTheme.typography.titleMedium,
                            color = TeslaTokens.text, fontWeight = FontWeight.Bold)
                        ProStatusPill(status.driveLabel,
                            status.driveLabel == "HIZ BELİRSİZ")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f)) { ProStatusPill(status.obdLabel) }
                        Box(Modifier.weight(1f)) { ProStatusPill(status.gpsLabel) }
                    }
                }
            }
        }
    }
}

@Composable
fun ProVehiclePanel(state: VehiclePanelUiState, modifier: Modifier = Modifier) {
    val battery = state.socPercent?.takeIf { it.isFinite() && it in 0.0..100.0 }
    val speed = TeslaDisplayPolicy.speed(state.speedKmh)
    Surface(
        modifier = modifier,
        color = TeslaTokens.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, TeslaTokens.outline),
    ) {
        Column(Modifier.background(Brush.verticalGradient(
            listOf(TeslaTokens.surfaceRaised, TeslaTokens.surface)))
            .padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ID.4 PURE", color = TeslaTokens.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("125 kW / 170 PS", color = TeslaTokens.secondaryText,
                    style = MaterialTheme.typography.labelSmall)
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 260.dp
                Column {
                    Text("ANLIK HIZ", color = TeslaTokens.secondaryText,
                        style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(speed, color = TeslaTokens.text,
                            fontSize = if (compact) 72.sp else 96.sp,
                            fontWeight = FontWeight.Light,
                            lineHeight = if (compact) 82.sp else 104.sp,
                            maxLines = 1, softWrap = false,
                            modifier = Modifier.semantics {
                                contentDescription = "Hız: $speed kilometre/saat"
                            })
                        Text("km/sa", color = TeslaTokens.secondaryText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 5.dp, bottom = 14.dp))
                    }
                }
            }
            Text("Hız kaynağı • ${state.speedSource.label}",
                color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.labelSmall)
            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawRect(TeslaTokens.outline)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BATARYA • SoC", color = TeslaTokens.secondaryText,
                        style = MaterialTheme.typography.labelMedium)
                    Text(TeslaDisplayPolicy.soc(battery) + "%",
                        color = TeslaTokens.text,
                        fontSize = 42.sp, fontWeight = FontWeight.Light,
                        lineHeight = 48.sp)
                    Text(state.source.label + " • " +
                        (if (state.source == SourceKind.OBD) "DOĞRULANMADI"
                            else "KAYNAK"),
                        color = TeslaTokens.secondaryText,
                        style = MaterialTheme.typography.labelSmall)
                }
                ProBatteryArc(battery,
                    Modifier.size(94.dp))
            }
            Text(TeslaDisplayPolicy.estimatedEnergy(state.estimatedKwh),
                color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.bodySmall)
            PowerBar(state.powerKw)
            Text(
                if (state.powerKw == null) "GÜÇ • VERİ YOK"
                else "GÜÇ • OBD / PID DOĞRULANMADI",
                color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(TeslaDisplayPolicy.estimatedRange(state.estimatedRangeKm),
                color = TeslaTokens.text,
                style = MaterialTheme.typography.labelLarge)
            state.message?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = if (state.status == PanelStatus.ERROR)
                    TeslaTokens.error else TeslaTokens.secondaryText,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProBatteryArc(percent: Double?, modifier: Modifier = Modifier) {
    val fraction = ProCockpitPolicy.batteryPercent(percent)
    Box(modifier.semantics {
        contentDescription = if (percent == null) "Batarya doluluk verisi yok"
            else "Batarya doluluk oranı ${TeslaDisplayPolicy.soc(percent)} yüzde"
    }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(90.dp)) {
            val stroke = 8.dp.toPx()
            val radius = Size(size.width - stroke, size.height - stroke)
            val top = Offset(stroke / 2, stroke / 2)
            drawArc(TeslaTokens.outline, startAngle = 135f, sweepAngle = 270f,
                useCenter = false, topLeft = top, size = radius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            if (percent != null && fraction > 0f) {
                drawArc(TeslaTokens.accent, startAngle = 135f,
                    sweepAngle = 270f * fraction,
                    useCenter = false, topLeft = top, size = radius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }
        Text(
            if (percent == null) "--" else
                String.format(Locale.ROOT, "%.0f%%", percent),
            color = TeslaTokens.text, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ProActionRail(
    onAction: (TeslaAction) -> Unit,
    mode: DriveMode,
    molaAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("KONTROL MERKEZİ",
            color = TeslaTokens.secondaryText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp))
        TeslaAction.entries.forEach { action ->
            val enabled = DriveModePolicy.allowedAction(action, mode, molaAvailable)
            val primary = action == TeslaAction.ROTA || action == TeslaAction.PLAN
            val tint = if (primary) TeslaTokens.accent else TeslaTokens.text
            OutlinedButton(
                onClick = { if (enabled) onAction(action) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
                    .semantics {
                        contentDescription = action.title + " • " +
                            if (enabled) ProCockpitPolicy.actionSubtitle(action)
                            else "Sürüş sırasında kapalı"
                    },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = TeslaTokens.surfaceRaised,
                    contentColor = tint,
                    disabledContainerColor = TeslaTokens.surface,
                    disabledContentColor = TeslaTokens.secondaryText,
                ),
                border = BorderStroke(1.dp,
                    if (enabled && primary) TeslaTokens.accent.copy(alpha = 0.5f)
                    else TeslaTokens.outline),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 10.dp, vertical = 8.dp),
            ) {
                Column(Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(action.symbol, color = if (enabled) tint
                            else TeslaTokens.secondaryText,
                            fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(action.title, style = MaterialTheme.typography.labelLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        if (enabled) ProCockpitPolicy.actionSubtitle(action)
                        else "Hareket / konum durumu nedeniyle kapalı",
                        style = MaterialTheme.typography.labelSmall,
                        color = TeslaTokens.secondaryText,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
fun ProRouteRibbon(plan: PlanResult.Success?, weatherSummary: String) {
    Surface(color = TeslaTokens.surfaceRaised,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(ProCockpitPolicy.planLabel(plan),
                style = MaterialTheme.typography.labelMedium,
                color = if (plan == null) TeslaTokens.secondaryText else TeslaTokens.accent,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(weatherSummary.ifBlank { "Hava durumu verisi yok" },
                color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
