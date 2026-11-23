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

@Composable
private fun F1DemoPreview() {
    FinalCockpitTheme {
        MapFirstDashboardScreen(
            state = DemoDashboardMapper.map(1_795_460_000_000L),
            onDemoChange = {},
        )
    }
}
