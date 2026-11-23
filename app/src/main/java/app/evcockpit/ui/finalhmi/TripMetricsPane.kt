package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
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
                LinearProgressIndicator(
                    progress = { ((soc ?: 0.0) / 100.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(t.spaceXS),
                    color = if (soc == null) t.neutralTrack else t.accent,
                    trackColor = t.neutralTrack,
                )
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
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = t.secondary, fontSize = t.labelText, maxLines = 2)
        Text(value, color = valueColor,
            fontSize = t.titleText, maxLines = 2, fontWeight = FontWeight.SemiBold)
    }
}
