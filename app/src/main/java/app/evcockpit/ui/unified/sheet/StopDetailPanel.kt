package app.evcockpit.ui.unified.sheet

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.evcockpit.R
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.Loadable
import app.evcockpit.ui.unified.SheetMode
import app.evcockpit.ui.unified.UnifiedUiState

@Composable
fun StopDetailPanel(state: UnifiedUiState) {
    val t = CockpitDesignTokens
    val id = (state.mode as? SheetMode.StopDetail)?.stationId
    val station = when (val s = state.stations) {
        is Loadable.Ready -> s.value.firstOrNull { it.id == id }
        else -> null
    }
    Text(stringResource(R.string.a0b_unified_station),
        fontSize = t.titleText, color = t.text)
    Text(station?.name ?: stringResource(R.string.a0b_unified_station_empty),
        fontSize = t.bodyText, color = t.secondary)
    Text(stringResource(R.string.a0b_unified_charge_unknown),
        fontSize = t.bodyText, color = t.secondary)
}
