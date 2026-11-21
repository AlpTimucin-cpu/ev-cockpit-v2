package app.evcockpit.ui.tesla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.evcockpit.ui.CockpitTheme
import java.util.Locale

/**
 * Presentation-only data. UI-2 will map stable StateFlow snapshots onto this model.
 * The UI never reads a repository or makes a network call.
 */
data class VehiclePanelUiState(
    val speedKmh: Double? = null,
    val powerKw: Double? = null,
    val socPercent: Double? = null,
    val estimatedKwh: Double? = null,
    val estimatedRangeKm: Double? = null,
    val source: SourceKind = SourceKind.YOK,
    val speedSource: SourceKind = SourceKind.YOK,
    val status: PanelStatus = PanelStatus.EMPTY,
    val message: String? = null,
)

@Composable
fun SourceBadge(source: SourceKind, modifier: Modifier = Modifier) {
    val statusColor = when (source) {
        SourceKind.OBD -> TeslaTokens.accent
        SourceKind.GPS -> TeslaTokens.secondaryText
        SourceKind.MANUEL -> TeslaTokens.text
        SourceKind.DEMO -> TeslaTokens.warning
        SourceKind.ESKI -> TeslaTokens.warning
        SourceKind.YOK -> TeslaTokens.secondaryText
    }
    Surface(
        modifier = modifier.semantics {
            contentDescription = "Veri kaynağı: " + source.label
        },
        shape = RoundedCornerShape(TeslaTokens.cornerRadius),
        color = TeslaTokens.surfaceRaised,
        border = BorderStroke(1.dp, TeslaTokens.outline),
    ) {
        Text(
            "KAYNAK " + source.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = statusColor,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Traction to the RIGHT in white, regenerative power to the LEFT in green. */
@Composable
fun PowerBar(powerKw: Double?, modifier: Modifier = Modifier) {
    val fraction = TeslaDisplayPolicy.powerFraction(powerKw)
    val measured = powerKw?.takeIf { it.isFinite() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "GÜÇ / REJENERASYON",
            color = TeslaTokens.secondaryText,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            measured?.let { String.format(Locale.ROOT, "%+.0f kW", it) } ?: "-- kW",
            color = TeslaTokens.text,
            style = MaterialTheme.typography.titleMedium,
        )
        val powerDescription = when {
            measured == null -> "Güç verisi yok"
            measured < 0.0 -> "Rejenerasyon"
            else -> "Çekiş gücü"
        }
        Canvas(
            Modifier.fillMaxWidth().height(16.dp).semantics {
                contentDescription = powerDescription
            },
        ) {
            val center = size.width / 2f
            drawRect(TeslaTokens.outline, topLeft = Offset.Zero, size = size)
            val magnitude = center * fraction
            if (measured != null && magnitude > 0f) {
                if (measured >= 0) {
                    drawRect(Color.White, Offset(center, 0f), Size(magnitude, size.height))
                } else {
                    drawRect(
                        TeslaTokens.accent,
                        Offset(center - magnitude, 0f),
                        Size(magnitude, size.height),
                    )
                }
            }
            drawRect(
                TeslaTokens.secondaryText,
                Offset(center - 1.dp.toPx(), 0f),
                Size(2.dp.toPx(), size.height),
            )
        }
    }
}

@Composable
fun SocGauge(
    socPercent: Double?,
    estimatedKwh: Double?,
    modifier: Modifier = Modifier,
) {
    val pct = socPercent?.takeIf { it.isFinite() && it in 0.0..100.0 }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            "BATARYA / SoC",
            color = TeslaTokens.secondaryText,
            style = MaterialTheme.typography.labelMedium,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                TeslaDisplayPolicy.soc(pct),
                fontSize = TeslaTokens.batterySize,
                fontWeight = FontWeight.Light,
                color = TeslaTokens.text,
                maxLines = 1,
            )
            Text(
                "%",
                modifier = Modifier.padding(bottom = 9.dp, start = 3.dp),
                style = MaterialTheme.typography.titleLarge,
                color = TeslaTokens.secondaryText,
            )
        }
        Text(
            TeslaDisplayPolicy.estimatedEnergy(estimatedKwh),
            color = TeslaTokens.secondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
        Canvas(
            Modifier.fillMaxWidth().height(9.dp).semantics {
                contentDescription = if (pct == null) "Batarya verisi yok"
                    else "Batarya yüzde " + TeslaDisplayPolicy.soc(pct)
            },
        ) {
            drawRect(TeslaTokens.outline, Offset.Zero, size)
            if (pct != null) drawRect(
                TeslaTokens.accent,
                Offset.Zero,
                Size(size.width * (pct / 100.0).toFloat(), size.height),
            )
        }
    }
}

/** Static shell only; the UI-2 ViewModel adapter will supply live state. */
@Composable
fun VehicleStatePanel(
    state: VehiclePanelUiState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(TeslaTokens.cornerRadius),
        color = TeslaTokens.surface,
        border = BorderStroke(1.dp, TeslaTokens.outline),
    ) {
        Column(
            Modifier.padding(TeslaTokens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(TeslaTokens.spacing),
        ) {
            Text(
                "ID.4 PURE",
                color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.labelLarge,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    TeslaDisplayPolicy.speed(state.speedKmh),
                    color = TeslaTokens.text,
                    fontWeight = FontWeight.Light,
                    fontSize = if (compact) TeslaTokens.speedCompact
                        else TeslaTokens.speedLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).semantics {
                        contentDescription = "Hız: " +
                            TeslaDisplayPolicy.speed(state.speedKmh) + " kilometre saat"
                    },
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "km/sa",
                    modifier = Modifier.padding(bottom = 14.dp),
                    color = TeslaTokens.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text("HIZ VERİ KAYNAĞI", color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.labelMedium)
            SourceBadge(state.speedSource)
            PowerBar(state.powerKw)
            Text(
                if (state.powerKw == null) "GÜÇ KAYNAĞI: YOK"
                else "GÜÇ KAYNAĞI: OBD • PID DOĞRULANMADI",
                color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.labelMedium,
            )
            SocGauge(state.socPercent, state.estimatedKwh)
            Text("SoC VERİ KAYNAĞI", color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.labelMedium)
            SourceBadge(state.source)
            Text(
                TeslaDisplayPolicy.estimatedRange(state.estimatedRangeKm),
                color = TeslaTokens.text,
                style = MaterialTheme.typography.titleMedium,
            )
            val detail = state.message ?: when (state.status) {
                PanelStatus.NORMAL -> "Araç değerleri kaynağına göre görüntüleniyor"
                PanelStatus.EMPTY -> "OBD bağlı değil • Manuel SoC girebilirsin"
                PanelStatus.LOADING -> "Veri okunuyor…"
                PanelStatus.ERROR -> "Veri alınamadı • Bağlantıyı kontrol et"
            }
            Text(
                detail,
                color = when (state.status) {
                    PanelStatus.ERROR -> TeslaTokens.error
                    PanelStatus.LOADING -> TeslaTokens.warning
                    else -> TeslaTokens.secondaryText
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(name = "Normal - DEMO", widthDp = 420, heightDp = 700, showBackground = true)
@Composable
private fun VehiclePanelNormalPreview() {
    CockpitTheme(dark = true) {
        Box(Modifier.background(TeslaTokens.background).padding(16.dp)) {
            VehicleStatePanel(
                VehiclePanelUiState(
                    speedKmh = 88.0, powerKw = 24.0, socPercent = 64.0,
                    estimatedKwh = 33.3, estimatedRangeKm = 245.0,
                    source = SourceKind.DEMO, status = PanelStatus.NORMAL,
                    message = "DEMO • Gerçek araç verisi değil",
                ),
            )
        }
    }
}

@Preview(name = "Boş - YOK", widthDp = 420, heightDp = 700, showBackground = true)
@Composable
private fun VehiclePanelEmptyPreview() {
    CockpitTheme(dark = true) {
        Box(Modifier.background(TeslaTokens.background).padding(16.dp)) {
            VehicleStatePanel(VehiclePanelUiState())
        }
    }
}

@Preview(name = "Yükleniyor", widthDp = 420, heightDp = 700, showBackground = true)
@Composable
private fun VehiclePanelLoadingPreview() {
    CockpitTheme(dark = true) {
        Box(Modifier.background(TeslaTokens.background).padding(16.dp)) {
            VehicleStatePanel(VehiclePanelUiState(status = PanelStatus.LOADING))
        }
    }
}

@Preview(name = "Hata", widthDp = 420, heightDp = 700, showBackground = true)
@Composable
private fun VehiclePanelErrorPreview() {
    CockpitTheme(dark = true) {
        Box(Modifier.background(TeslaTokens.background).padding(16.dp)) {
            VehicleStatePanel(VehiclePanelUiState(status = PanelStatus.ERROR))
        }
    }
}

@Preview(name = "Güç - rejenerasyon", widthDp = 420, heightDp = 160, showBackground = true)
@Composable
private fun PowerRegenPreview() {
    CockpitTheme(dark = true) {
        Box(Modifier.background(TeslaTokens.background).padding(16.dp)) {
            PowerBar(-35.0)
        }
    }
}

@Preview(name = "Batarya - veri yok", widthDp = 420, heightDp = 220, showBackground = true)
@Composable
private fun SocUnknownPreview() {
    CockpitTheme(dark = true) {
        Box(Modifier.background(TeslaTokens.background).padding(16.dp)) {
            SocGauge(null, null)
        }
    }
}

@Preview(name = "Kaynak - eski", widthDp = 300, heightDp = 100, showBackground = true)
@Composable
private fun StaleBadgePreview() {
    CockpitTheme(dark = true) {
        Box(Modifier.background(TeslaTokens.background).padding(16.dp)) {
            SourceBadge(SourceKind.ESKI)
        }
    }
}
