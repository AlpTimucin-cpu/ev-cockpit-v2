package app.evcockpit.ui.unified

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.R
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.map.MapHostViewport
import app.evcockpit.ui.unified.search.SearchBar
import app.evcockpit.ui.unified.sheet.*

/**
 * C3 bağımsız birleşik harita/panel ekranı: henüz üretim NavHost'a geçirilmez
 * (C8). C3 ekranı rota/istasyon/sürüş sayısı icat ETMEZ.
 */
@Composable
fun UnifiedMapScreen(
    vm: UnifiedViewModel = viewModel(),
    onExit: () -> Unit,
) {
    val t = CockpitDesignTokens
    val state by vm.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    BackHandler {
        vm.dispatch(UnifiedEvent.Back)
    }
    LaunchedEffect(state.exitRequested) {
        if (state.exitRequested) {
            vm.dispatch(UnifiedEvent.ExitConsumed)
            onExit()
        }
    }
    Box(
        Modifier.fillMaxSize().background(t.background),
    ) {
        MapHostViewport(
            modifier = Modifier.fillMaxSize(),
            unavailableMessage = stringResource(R.string.a0b_unified_map_no_data),
        )

        SearchBar(
            query = searchQuery,
            onQuery = { searchQuery = it },
            onFocus = { vm.dispatch(UnifiedEvent.SetDetent(SheetDetent.FULL)) },
            modifier = Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(t.spaceM),
        )

        ThreeDetentSheet(
            detent = state.detent,
            onDetent = { vm.dispatch(UnifiedEvent.SetDetent(it)) },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (state.mode) {
                SheetMode.Idle -> IdlePanel()
                is SheetMode.RoutePreview -> RoutePreviewPanel(state)
                SheetMode.Navigating -> NavigationPanel(state)
                is SheetMode.StopDetail -> StopDetailPanel(state)
                SheetMode.Energy -> EnergyPanel(state)
            }
            state.banners.lastOrNull()?.let { banner ->
                Surface(
                    shape = t.cornerSmall,
                    color = t.panelRaised,
                ) {
                    Text(banner.message.textTr, color = t.caution,
                        modifier = Modifier.padding(t.spaceM),
                        fontSize = t.bodyText)
                }
            }
        }
    }
}
