package app.evcockpit.ui.unified.sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.evcockpit.R
import app.evcockpit.ui.finalhmi.CockpitDesignTokens

/** C3'te sahte başlangıç/rota/şarj noktası çizilmez. */
@Composable
fun IdlePanel() {
    val t = CockpitDesignTokens
    Text(stringResource(R.string.a0b_unified_idle),
        fontSize = t.titleText, color = t.text)
    Text(stringResource(R.string.a0b_unified_idle_desc),
        fontSize = t.bodyText, color = t.secondary)
    Text(stringResource(R.string.a0b_unified_source_unknown),
        fontSize = t.labelText, color = t.secondary)
}
