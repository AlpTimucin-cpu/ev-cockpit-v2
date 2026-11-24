package app.evcockpit.ui.unified.sheet

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.evcockpit.R
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.UnifiedUiState

/** C3 yalnız durum panelidir; manevra, TTS veya GPS izlemesi başlatmaz. */
@Composable
fun NavigationPanel(state: UnifiedUiState) {
    val t = CockpitDesignTokens
    Text(stringResource(R.string.a0b_unified_navigation),
        fontSize = t.titleText, color = t.text)
    Text(stringResource(R.string.a0b_unified_navigation_empty),
        fontSize = t.bodyText, color = t.secondary)
}
