package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

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
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val samples = state.weatherSamples
                    val mode = weatherLayoutMode(
                        availableWidthDp = maxWidth.value,
                        sampleCount = samples.size,
                        spacingDp = t.spaceS.value,
                        minCellDp = t.weatherCellMinWidth.value,
                    )
                    when (mode) {
                        WeatherLayoutMode.EQUAL_WEIGHT -> {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                            ) {
                                samples.forEach { sample ->
                                    WeatherSampleCard(sample, Modifier.weight(1f))
                                }
                            }
                        }
                        WeatherLayoutMode.SCROLL -> {
                            Box(Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                                ) {
                                    samples.forEach { sample ->
                                        WeatherSampleCard(
                                            sample,
                                            Modifier.width(t.weatherScrollCellWidth),
                                        )
                                    }
                                }
                                // Dokunma/pointerInput kullanmayan boyama katmanı.
                                // Bir sonraki kartın kısmen görünmesi ve sağ kenar
                                // solması, yatay kaydırmayı kullanıcıya anlatır.
                                Box(Modifier.matchParentSize()) {
                                    Box(
                                        Modifier.align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .width(t.weatherScrollFadeWidth)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(t.transparent, t.panel),
                                                ),
                                            ),
                                    )
                                }
                            }
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

/**
 * Ekran dp genişliği ve hücre sayısından karar; Compose/Android bağımsız saf fonksiyon.
 * Boş veya tek örnek için yatay kaydırma gerekmez.
 */
internal enum class WeatherLayoutMode { EQUAL_WEIGHT, SCROLL }

internal fun weatherLayoutMode(
    availableWidthDp: Float,
    sampleCount: Int,
    spacingDp: Float,
    minCellDp: Float,
): WeatherLayoutMode {
    if (sampleCount <= 1) return WeatherLayoutMode.EQUAL_WEIGHT
    if (!availableWidthDp.isFinite() || availableWidthDp <= 0f ||
        !spacingDp.isFinite() || spacingDp < 0f ||
        !minCellDp.isFinite() || minCellDp <= 0f) return WeatherLayoutMode.SCROLL
    val totalSpacing = (sampleCount - 1) * spacingDp
    val cellWidth = (availableWidthDp - totalSpacing) / sampleCount
    return if (cellWidth >= minCellDp) WeatherLayoutMode.EQUAL_WEIGHT
        else WeatherLayoutMode.SCROLL
}

@Composable
private fun WeatherSampleCard(
    sample: RouteWeatherSample,
    modifier: Modifier = Modifier,
) {
    val t = CockpitDesignTokens
    Column(
        modifier.background(t.panelRaised, t.cornerSmall).padding(t.spaceS),
        verticalArrangement = Arrangement.spacedBy(t.spaceXS),
    ) {
        Text(
            "≈" + DashboardFormat.whole(sample.distanceAlongKm) + " km",
            color = t.accent, fontSize = t.labelText, softWrap = true,
        )
        val temp = sample.temperatureC.value?.takeIf { it.isFinite() }
            ?.let { DashboardFormat.whole(it) + " °C" } ?: "— °C"
        Text(temp, color = t.text, fontSize = t.titleText, softWrap = true)
        Text(
            rainWarningText(sample.rainExpected.value),
            color = if (sample.rainExpected.value == true) t.caution else t.secondary,
            fontSize = t.labelText,
            softWrap = true,
        )
    }
}
