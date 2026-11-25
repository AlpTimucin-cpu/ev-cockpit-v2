package app.evcockpit.ui.unified.sheet

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.evcockpit.R
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.UnifiedUiState
import app.evcockpit.ui.unified.SourcedOrigin

@Composable
fun EnergyPanel(state: UnifiedUiState) {
    val t = CockpitDesignTokens
    Text(stringResource(R.string.a0b_unified_energy),
        fontSize = t.titleText, color = t.text)
    Text(stringResource(R.string.a0b_unified_energy_empty),
        fontSize = t.bodyText, color = t.secondary)
    val value = state.soc.value?.takeIf { it.isFinite() && it in 0.0..100.0 }
    Text(
        if (value == null) "Batarya: — (kaynak: yok)"
        else "Batarya: ${value.toInt()} % (kaynak: ${state.soc.source.name})",
        fontSize = t.bodyText, color = t.secondary,
    )
}
