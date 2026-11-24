package app.evcockpit.ui.finalhmi

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize

enum class FinalDashboardTab(val titleTr: String) {
    HARITA("Harita"),
    ROTA("Rota"),
    SARJ("Şarj"),
    ENERJI("Enerji"),
    AYARLAR("Ayarlar"),
}

object F1DashboardLayoutPolicy {
    /**
     * Geniş pencere = en az 840dp ve yatayda yüksekliğinden geniş.
     * Varsayımsal piksel oranı değil, gerçek kullanılabilir dp.
     */
    fun useWideMapFirst(widthDp: Int, heightDp: Int): Boolean =
        widthDp >= 840 && widthDp > heightDp

    val tabs: List<String> = FinalDashboardTab.entries.map { it.titleTr }
}

/**
 * A0 üretim kabuğu: NavHost başlangıç hedefi HARİTA.
 * Diğer sekmeler A–C servislerinin mevcut ekranlarına bağlanabilir.
 * Kayıtlı veri bulunmazsa gerçek mapper yalnız boş değer gösterir.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MapFirstDashboardScreen(
    state: DashboardUiState,
    onDemoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    demoAvailable: Boolean = false,
    tabContent: @Composable (FinalDashboardTab, () -> Unit, (FinalDashboardTab) -> Unit) -> Unit = { tab, _, _ ->
        A0UnconnectedTab(tab)
    },
    diagnosticContent: @Composable () -> Unit = {
        Text("Tanı ekranı mevcut değil.", color = CockpitDesignTokens.secondary)
    },
) {
    val t = CockpitDesignTokens
    val controller = rememberNavController()
    val backEntry by controller.currentBackStackEntryAsState()
    val selected = FinalDashboardTab.entries.firstOrNull {
        it.name == backEntry?.destination?.route
    } ?: if (backEntry?.destination?.route == A0_DIAGNOSTIC_ROUTE)
        FinalDashboardTab.AYARLAR else FinalDashboardTab.HARITA
    val navigateTab: (FinalDashboardTab) -> Unit = { destination ->
        controller.navigate(destination.name) {
            popUpTo(controller.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Column(modifier.fillMaxSize().background(t.background)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compactHeader = maxWidth < t.compactMaxWidth
            DashboardTopStatus(state, compactHeader, onDemoChange, demoAvailable)
        }

        if (state.demo) {
            Row(
                Modifier.fillMaxWidth().background(t.demoBanner)
                    .heightIn(min = t.bannerMinHeight)
                    .padding(horizontal = t.spaceM, vertical = t.spaceXS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("DEMO — GERÇEK VERİ DEĞİL",
                    color = t.caution, fontWeight = FontWeight.Bold,
                    fontSize = t.bodyText)
            }
        }

        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth()
                .padding(horizontal = t.spaceS, vertical = t.spaceXS),
        ) {
            // Material3 WindowSizeClass, Android'in dp tabanlı kompakt/orta/geniş
            // sınırlarını kullanır. Ayrıca yüksekliği kontrol ederek katlanır
            // ekranlarda aşırı dar yatay pencereleri iki sütuna zorlamayız.
            val windowClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
            val wide = windowClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                F1DashboardLayoutPolicy.useWideMapFirst(
                    maxWidth.value.toInt(), maxHeight.value.toInt())

            NavHost(
                navController = controller,
                startDestination = FinalDashboardTab.HARITA.name,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(FinalDashboardTab.HARITA.name) {
                    if (wide) {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                        ) {
                            MapHeroPane(
                                state = state,
                                onOpenRouteTab = { navigateTab(FinalDashboardTab.ROTA) },
                                modifier = Modifier.weight(t.mapWeight).fillMaxHeight(),
                            )
                            Column(
                                Modifier.weight(t.dataWeight).fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(t.spaceS),
                            ) {
                                DashboardRightPane(state)
                            }
                        }
                    } else {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(t.spaceS),
                        ) {
                            MapHeroPane(
                                state = state,
                                onOpenRouteTab = { navigateTab(FinalDashboardTab.ROTA) },
                                modifier = Modifier.fillMaxWidth().height(t.phoneMapHeight),
                            )
                            DashboardRightPane(state)
                        }
                    }
                }
                FinalDashboardTab.entries.filter { it != FinalDashboardTab.HARITA }
                    .forEach { destination ->
                        composable(destination.name) {
                            tabContent(destination, {
                                controller.navigate(A0_DIAGNOSTIC_ROUTE) {
                                    launchSingleTop = true
                                }
                            }, navigateTab)
                        }
                    }
                composable(A0_DIAGNOSTIC_ROUTE) {
                    diagnosticContent()
                }
            }
        }

        FinalBottomBar(selected = selected, onSelect = navigateTab)
    }
}

private const val A0_DIAGNOSTIC_ROUTE = "obd_tani"

/** A0'da henüz bağlanmamış alanlarda sahte veri ya da boş etkileşim yok. */
@Composable
private fun A0UnconnectedTab(tab: FinalDashboardTab) {
    val t = CockpitDesignTokens
    Surface(color = t.panel, shape = t.corner, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(t.spaceXL), verticalArrangement = Arrangement.spacedBy(t.spaceM)) {
            Text(tab.titleTr, color = t.text, fontSize = t.headText)
            Text("Bu alan henüz gerçek veri kaynağına bağlanmadı.",
                color = t.secondary, fontSize = t.bodyText)
        }
    }
}

@Composable
private fun DashboardRightPane(state: DashboardUiState) {
    val t = CockpitDesignTokens
    TripMetricsPane(state, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(t.spaceXXS))
    RouteWeatherStrip(state)
    Spacer(Modifier.height(t.spaceXXS))
    ChargePlanCard(state)
}

@Composable
private fun DashboardTopStatus(
    state: DashboardUiState,
    compact: Boolean,
    onDemoChange: (Boolean) -> Unit,
    demoAvailable: Boolean,
) {
    val t = CockpitDesignTokens
    val time = state.clock.value ?: "—"
    val temperature = state.outdoorTemperatureC.value?.takeIf { it.isFinite() }
        ?.let { DashboardFormat.whole(it) + " °C" } ?: "— °C"
    val battery = DashboardFormat.number(state.socPercent, " %")
    val range = DashboardFormat.number(state.estimatedRangeKm, " km")
    val header = @Composable {
        StatusLabel("Saat", time)
        StatusLabel("Dış sıcaklık", temperature)
        StatusLabel(if (state.socPercent.source == DashboardOrigin.MANUAL)
            "Batarya (manuel)" else if (state.socPercent.source in setOf(
                DashboardOrigin.MODEL, DashboardOrigin.DEMO))
            "Batarya (tahmini)" else "Batarya", battery)
        StatusLabel("Menzil (tahmini)", range)
    }
    Surface(color = t.panel, shape = t.cornerSmall) {
        if (compact) {
            Column(Modifier.fillMaxWidth().padding(t.spaceXS)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusLabel("Saat", time)
                    StatusLabel("Dış sıcaklık", temperature)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusLabel("Batarya", battery)
                    StatusLabel("Menzil (tahmini)", range)
                }
                if (demoAvailable) DemoModeButton(state.demo, onDemoChange)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().heightIn(min = t.topBarMinHeight)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = t.spaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(t.spaceL),
            ) {
                header()
                Spacer(Modifier.weight(1f))
                if (demoAvailable) DemoModeButton(state.demo, onDemoChange)
            }
        }
    }
}

@Composable
private fun StatusLabel(title: String, value: String) {
    val t = CockpitDesignTokens
    Column(Modifier.padding(t.spaceXXS)) {
        Text(title, color = t.secondary, fontSize = t.labelText, maxLines = 1)
        Text(value, color = t.text, fontSize = t.bodyText,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun DemoModeButton(demo: Boolean, onChange: (Boolean) -> Unit) {
    val t = CockpitDesignTokens
    Surface(
        onClick = { onChange(!demo) },
        color = if (demo) t.accentSoft else t.panelRaised,
        shape = t.cornerSmall,
        modifier = Modifier.heightIn(min = t.target)
            .widthIn(min = t.target),
    ) {
        Box(Modifier.padding(horizontal = t.spaceM),
            contentAlignment = Alignment.Center) {
            Text(if (demo) "Demo modu: açık" else "Demo modu: kapalı", color = t.text,
                fontSize = t.bodyText)
        }
    }
}

@Composable
private fun FinalBottomBar(
    selected: FinalDashboardTab,
    onSelect: (FinalDashboardTab) -> Unit,
) {
    val t = CockpitDesignTokens
    Row(
        Modifier.fillMaxWidth().background(t.panel)
            .padding(horizontal = t.spaceXS, vertical = t.spaceXXS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(t.spaceXXS),
    ) {
        FinalDashboardTab.entries.forEach { tab ->
            Surface(
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f).heightIn(min = t.tabBarHeight),
                shape = t.cornerSmall,
                color = if (tab == selected) t.accentSoft else t.panel,
                border = if (tab == selected)
                    androidx.compose.foundation.BorderStroke(t.cardOutlineWidth, t.accent)
                else null,
            ) {
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(t.spaceXXS)) {
                    Text(tab.titleTr,
                        fontSize = t.tabText,
                        color = if (tab == selected) t.text else t.secondary,
                        fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1)
                }
            }
        }
    }
}
