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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 * 5 sekmeli F1 ekranı; yalnız HARİTA açık. Diğer sekmeler F2'ye kadar
 * açıklamalı yer tutucudur. A–C arka plan işlevleri eski ekranlarda korunur.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MapFirstDashboardScreen(
    state: DashboardUiState,
    onDemoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = CockpitDesignTokens
    var selected by rememberSaveable { mutableStateOf(FinalDashboardTab.HARITA) }
    Column(modifier.fillMaxSize().background(t.background)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compactHeader = maxWidth < t.compactMaxWidth
            DashboardTopStatus(state, compactHeader, onDemoChange)
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

            if (selected == FinalDashboardTab.HARITA) {
                if (wide) {
                    Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(t.spaceS),
                    ) {
                        MapHeroPane(
                            state = state,
                            onOpenRouteTab = { selected = FinalDashboardTab.ROTA },
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
                            onOpenRouteTab = { selected = FinalDashboardTab.ROTA },
                            modifier = Modifier.fillMaxWidth().height(t.phoneMapHeight),
                        )
                        DashboardRightPane(state)
                    }
                }
            } else {
                Surface(
                    color = t.panel, shape = t.corner,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        Modifier.padding(t.spaceXL),
                        verticalArrangement = Arrangement.spacedBy(t.spaceM),
                    ) {
                        Text(selected.titleTr, fontSize = t.headText,
                            color = t.text)
                        Text("Bu sekme F2'de bağlanacak.",
                            color = t.secondary, fontSize = t.bodyText)
                        Text("F1 yalnız ana ekran iskeletini ve bağımsız veri " +
                            "gösterim kurallarını doğrular. A–C sekmeleri eski " +
                            "uygulamada korunmuştur.", color = t.secondary,
                            fontSize = t.bodyText)
                    }
                }
            }
        }

        FinalBottomBar(selected = selected, onSelect = { selected = it })
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
                DemoModeButton(state.demo, onDemoChange)
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
                DemoModeButton(state.demo, onDemoChange)
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
            Text(if (demo) "Boş veri" else "DEMO aç", color = t.text,
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
