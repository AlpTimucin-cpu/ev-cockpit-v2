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
