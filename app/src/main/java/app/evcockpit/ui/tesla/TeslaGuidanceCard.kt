package app.evcockpit.ui.tesla

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.evcockpit.ui.CockpitTheme

@Composable
fun TeslaGuidanceCard(
    guidance: DashboardGuidance,
    onAction: (GuidanceAction) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (guidance.kind == GuidanceKind.NONE) return
    val tint = when (guidance.kind) {
        GuidanceKind.ROUTE_LOADING -> TeslaTokens.warning
        GuidanceKind.STALE, GuidanceKind.NO_CHARGER -> TeslaTokens.estimate
        GuidanceKind.NO_OBD, GuidanceKind.GPS_MISSING, GuidanceKind.ROUTE_KEY ->
            TeslaTokens.warning
        else -> TeslaTokens.secondaryText
    }
    Surface(modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TeslaTokens.cornerRadius),
        color = TeslaTokens.surface,
        border = BorderStroke(1.dp,TeslaTokens.outline)) {
        Column(Modifier.padding(TeslaTokens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(guidance.title, color = tint,
                style = MaterialTheme.typography.labelLarge)
            Text(guidance.detail, color = TeslaTokens.text,
                style = MaterialTheme.typography.bodySmall)
            if (guidance.action != GuidanceAction.NONE) {
                OutlinedButton(
                    onClick = { onAction(guidance.action) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = TeslaTokens.touchTarget),
                ) { Text(guidance.actionText, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Preview(name = "Veri yok / DEMO", widthDp = 390, heightDp = 220)
@Composable private fun NoObdGuidancePreview() {
    CockpitTheme(dark=true) {
        TeslaGuidanceCard(
            DashboardGuidance(GuidanceKind.NO_OBD,"OBD bağlı değil",
                "Araç verisi yok. Manuel SoC girebilirsin.",
                GuidanceAction.SETTINGS,"Manuel SoC gir"),
            {},
        )
    }
}
@Preview(name = "Konum yok / DEMO", widthDp = 390, heightDp = 220)
@Composable private fun GpsGuidancePreview() {
    CockpitTheme(dark=true) {
        TeslaGuidanceCard(
            DashboardGuidance(GuidanceKind.GPS_MISSING,"Konum alınamıyor",
                "GPS konum iznini kontrol et.",
                GuidanceAction.LOCATION_PERMISSION,"Konum iznini aç"),
            {},
        )
    }
}
@Preview(name = "Eski veri / DEMO", widthDp = 390, heightDp = 220)
@Composable private fun StaleGuidancePreview() {
    CockpitTheme(dark=true) {
        TeslaGuidanceCard(
            DashboardGuidance(GuidanceKind.STALE,"ESKİ",
                "Son SoC örneği güncel değil.",
                GuidanceAction.OBD_TEST,"OBD Test'e git"),
            {},
        )
    }
}
@Preview(name = "Hata / DEMO", widthDp = 390, heightDp = 220)
@Composable private fun NoChargerGuidancePreview() {
    CockpitTheme(dark=true) {
        TeslaGuidanceCard(
            DashboardGuidance(GuidanceKind.NO_CHARGER,
                "Bu rota için durak bulunamadı",
                "En yakın tahmini nokta: ~45 km • TAHMİN",
                GuidanceAction.PLAN,"Plan filtrelerine git"),
            {},
        )
    }
}
