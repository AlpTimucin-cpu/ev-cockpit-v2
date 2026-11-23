package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** İstasyon/plan henüz bağlanmadıysa bütün bilgiler — görünür. */
@Composable
fun ChargePlanCard(state: DashboardUiState, modifier: Modifier = Modifier) {
    val t = CockpitDesignTokens
    val charge = state.nextCharge
    Surface(
        modifier.fillMaxWidth(),
        color = t.panel,
        shape = t.corner,
        border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline),
    ) {
        Column(
            Modifier.padding(t.spaceM),
            verticalArrangement = Arrangement.spacedBy(t.spaceS),
        ) {
            Text("SIRADAKİ ŞARJ PLANI", color = t.text, fontSize = t.titleText)
            Text(charge.stationName.value ?: "—", color = t.accent,
                fontSize = t.bodyText)
            Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                ChargeDetail("Mesafe", DashboardFormat.number(charge.distanceKm, " km"),
                    Modifier.weight(1f))
                ChargeDetail("Süre (tahmini)",
                    DashboardFormat.number(charge.chargingMinutes, " dk"),
                    Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(t.spaceS)) {
                ChargeDetail("Hedef SoC", DashboardFormat.number(
                    charge.targetSocPercent, " %"), Modifier.weight(1f))
                ChargeDetail("Ücret (tahmini)", charge.costLabel.value ?: "—",
                    Modifier.weight(1f))
            }
            Text("Boş/eksik tarife 0 TL değildir. Gerçek plan F4'te bağlanacak.",
                color = t.secondary, fontSize = t.labelText)
        }
    }
}

@Composable
private fun ChargeDetail(label: String, value: String, modifier: Modifier = Modifier) {
    val t = CockpitDesignTokens
    Column(modifier.background(t.panelRaised, t.cornerSmall)
        .padding(t.spaceS),
        verticalArrangement = Arrangement.spacedBy(t.spaceXS)) {
        Text(label, fontSize = t.labelText, color = t.secondary)
        Text(value, fontSize = t.bodyText, color = t.text, maxLines = 2)
    }
}
