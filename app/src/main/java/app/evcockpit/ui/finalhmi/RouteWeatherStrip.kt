package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * F1 hava şeridi: Open-Meteo çağrısı yapmaz, var olan zamanlı veriyi çizer.
 * Uydurma güneş/yağmur ikonu ve trafik lejantı bulunmaz.
 */
@Composable
fun RouteWeatherStrip(state: DashboardUiState, modifier: Modifier = Modifier) {
    val t = CockpitDesignTokens
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = t.panel,
        shape = t.corner,
        border = androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.outline),
    ) {
        Column(Modifier.padding(t.spaceM),
            verticalArrangement = Arrangement.spacedBy(t.spaceS)) {
            Text(if (state.demo) "ROTA BOYUNCA HAVA • DEMO / ÖRNEK"
                else "ROTA BOYUNCA HAVA", color = t.text,
                fontSize = t.titleText)
            if (state.weatherSamples.isEmpty()) {
                Text("Sıcaklık: — • Yağış: —", color = t.secondary, fontSize = t.bodyText)
                Text("Hava verileri F4 aşamasında bağlanacak.",
                    color = t.secondary, fontSize = t.labelText)
            } else {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                ) {
                    state.weatherSamples.forEach { sample ->
                        Column(
                            Modifier.background(t.panelRaised, t.cornerSmall)
                                .widthIn(min = t.weatherSampleMinWidth)
                                .padding(t.spaceS),
                            verticalArrangement = Arrangement.spacedBy(t.spaceXS),
                        ) {
                            Text("≈" + DashboardFormat.whole(sample.distanceAlongKm) + " km",
                                color = t.accent, fontSize = t.labelText)
                            val temp = sample.temperatureC.value?.takeIf { it.isFinite() }
                                ?.let { DashboardFormat.whole(it) + " °C" } ?: "— °C"
                            Text(temp, color = t.text, fontSize = t.titleText)
                            Text(
                                rainWarningText(sample.rainExpected.value), color = if (sample.rainExpected.value == true)
                                    t.caution else t.secondary,
                                fontSize = t.labelText,
                                softWrap = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** true = uyarı VAR; eski kısa metin yanlışlıkla belirsiz kalıyordu. */
internal fun rainWarningText(value: Boolean?): String = when (value) {
    true -> "Yağış uyarısı var"
    false -> "Yağış uyarısı yok"
    null -> "Yağış: —"
}
