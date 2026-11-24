package app.evcockpit.ui.unified.sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.evcockpit.R
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.UnifiedUiState

@Composable
fun RoutePreviewPanel(state: UnifiedUiState) {
    val t = CockpitDesignTokens
    Text(stringResource(R.string.a0b_unified_route),
        fontSize = t.titleText, color = t.text)
    val route = state.currentRoute
    if (route == null) {
        Text(stringResource(R.string.a0b_unified_route_empty),
            fontSize = t.bodyText, color = t.secondary)
    } else {
        Text("Mesafe: ${(route.distanceMeters / 1000.0).toInt()} km (kaynak: ${route.source})",
            color = t.text, fontSize = t.bodyText)
        Text("Süre: ${(route.durationSeconds / 60.0).toInt()} dk (tahmini)",
            color = t.secondary, fontSize = t.bodyText)
    }
}
