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
import androidx.compose.ui.Alignment

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
                Row(
                    Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                ) {
                    InfoMetric("Batarya" + when (state.socPercent.source) {
                        DashboardOrigin.MANUAL -> " (manuel)"
                        DashboardOrigin.MODEL -> " (tahmini)"
                        DashboardOrigin.DEMO -> " (DEMO)"
                        else -> ""
                    }, DashboardFormat.number(state.socPercent, " %"),
                        Modifier.weight(1f).fillMaxHeight())
                    InfoMetric("Menzil (tahmini)",
                        DashboardFormat.number(state.estimatedRangeKm, " km"),
                        Modifier.weight(1f).fillMaxHeight())
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
                Row(
                    Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                ) {
                    InfoMetric("Tüketim (tahmini)",
                        state.consumptionKwhPer100Km.value?.takeIf { it.isFinite() }
                            ?.let { DashboardFormat.oneDecimal(it) + " kWh/100 km" }
                            ?: "— kWh/100 km", Modifier.weight(1f).fillMaxHeight())
                    val power = state.powerKw.value?.takeIf { it.isFinite() }
                    InfoMetric("Anlık güç",
                        power?.let { DashboardFormat.oneDecimal(it) + " kW" } ?: "— kW",
                        Modifier.weight(1f).fillMaxHeight(),
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
        verticalArrangement = Arrangement.spacedBy(
            t.spaceXS, Alignment.CenterVertically),
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
