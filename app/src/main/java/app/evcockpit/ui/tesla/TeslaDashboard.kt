package app.evcockpit.ui.tesla

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.location.SpeedSelector
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.navigation.RouteProgress
import app.evcockpit.navigation.RouteUiState
import app.evcockpit.obd.SocReading
import app.evcockpit.obd.VehicleState
import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.charging.ChargerEvidence
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.ui.CockpitTheme
import app.evcockpit.ui.OnlineMap

enum class TeslaAction(val title: String, val symbol: String) {
    ROTA("Rota", "⌖"),
    PLAN("Plan", "⚡"),
    MEDYA("Medya", "♫"),
    VIDEO("Yolcu video", "▶"),
    MOLA("Mola", "M"),
    AYARLAR("Ayarlar", "⚙"),
}

object TeslaLayoutPolicy {
    fun useThreePanes(widthDp: Int, heightDp: Int): Boolean =
        widthDp >= 700 && widthDp > heightDp
}

@Composable
fun DriveModeScaffold(
    left: @Composable () -> Unit,
    center: @Composable () -> Unit,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier.fillMaxSize().background(TeslaTokens.background)
            .padding(TeslaTokens.contentPadding),
    ) {
        if (TeslaLayoutPolicy.useThreePanes(maxWidth.value.toInt(), maxHeight.value.toInt())) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(TeslaTokens.leftPaneWeight)
                    .fillMaxHeight().verticalScroll(rememberScrollState())) { left() }
                Box(Modifier.weight(TeslaTokens.mapWeight).fillMaxHeight()) { center() }
                Box(Modifier.weight(TeslaTokens.actionWeight)
                    .fillMaxHeight().verticalScroll(rememberScrollState())) { right() }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.fillMaxWidth()) { left() }
                Box(Modifier.fillMaxWidth().height(420.dp)) { center() }
                Box(Modifier.fillMaxWidth()) { right() }
            }
        }
    }
}

@Composable
fun ActionRail(
    onAction: (TeslaAction) -> Unit,
    molaAvailable: Boolean = false,
    mode: DriveMode = DriveMode.UNKNOWN,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TeslaAction.entries.forEach { action ->
            val enabled = DriveModePolicy.allowedAction(action, mode, molaAvailable)
            OutlinedButton(
                onClick = { onAction(action) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = TeslaTokens.touchTarget)
                    .semantics {
                        contentDescription = action.title + when {
                            action == TeslaAction.MOLA && !molaAvailable ->
                                " • kurulu değilse resmî kurulum rehberi"
                            action == TeslaAction.VIDEO && mode != DriveMode.PARKED ->
                                " • sürüşte kapalı"
                            else -> ""
                        }
                    },
                shape = RoundedCornerShape(TeslaTokens.cornerRadius),
                border = BorderStroke(1.dp, TeslaTokens.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TeslaTokens.text,
                    disabledContentColor = TeslaTokens.secondaryText,
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 9.dp),
            ) {
                Text(action.symbol, fontSize = 21.sp)
                Spacer(Modifier.width(5.dp))
                Text(
                    action.title,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

    }
}

@Composable
fun MapHeadlineBar(summary: MapHeadline) {
    Column(
        Modifier.fillMaxWidth().background(TeslaTokens.surface)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(summary.title, color = TeslaTokens.secondaryText,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(summary.distance, color = TeslaTokens.text,
            style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(summary.eta, color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f), maxLines = 2)
            Text(summary.arrivalSoc, color = TeslaTokens.estimate,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f), maxLines = 2)
        }
    }
}

@Composable
fun NextStopBar(stop: app.evcockpit.planner.model.PlannedStop, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(TeslaTokens.cornerRadius),
        color = TeslaTokens.surfaceRaised,
        modifier = Modifier.fillMaxWidth().heightIn(min = TeslaTokens.touchTarget),
        border = BorderStroke(1.dp, TeslaTokens.outline),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("PLANLANAN ŞARJ DURAĞI • DETAY",
                color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.labelMedium)
            Text(stop.charger.name, color = TeslaTokens.text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "~" + stop.charger.powerKw.value.toInt() + " kW • hedef %" +
                    stop.departureSocPct + " • şarj ~" +
                    stop.chargeMinutes.toInt() + " dk • TAHMİN • doluluk bilinmiyor",
                color = TeslaTokens.secondaryText, style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun MapHero(
    route: NavigationRoute?,
    destination: GeoPoint?,
    gps: GpsSnapshot,
    gpsFresh: Boolean,
    plan: PlanResult.Success?,
    weatherSummary: String,
    onSelectDestination: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
    previewMap: Boolean = false,
    allowRetry: Boolean = true,
) {
    val position = if (gpsFresh && route != null && gps.point != null) {
        RouteProgress.locate(route, gps.point)
    } else null
    val headline = MapHeadlinePolicy.create(
        route, destination != null, position, plan, System.currentTimeMillis(),
    )
    val next = MapHeadlinePolicy.nextStop(plan, route, position)
    var selectedStation by remember { mutableStateOf<ChargerCandidate?>(null) }
    var mapFailed by remember { mutableStateOf(false) }
    var mapRetry by remember { mutableIntStateOf(0) }
    Column(
        modifier.clip(RoundedCornerShape(TeslaTokens.cornerRadius))
            .background(TeslaTokens.surface),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MapHeadlineBar(headline)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (previewMap) {
                Box(
                    Modifier.fillMaxSize().background(TeslaTokens.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) { Text("HARİTA ÖNİZLEME • DEMO", color = TeslaTokens.secondaryText) }
            } else {
                key(mapRetry) {
                    OnlineMap(
                        modifier = Modifier.fillMaxSize(),
                        route = route,
                        destination = destination,
                        gps = gps,
                        weatherSummary = weatherSummary,
                        onSelectDestination = onSelectDestination,
                        stopMarkers = plan?.stops?.map { it.charger } ?: emptyList(),
                        onStopSelected = { selectedStation = it },
                        onLoadFailed = { mapFailed = true },
                        onStyleReady = { mapFailed = false },
                        canChangeLayer = allowRetry,
                    )
                }
                if (mapFailed) {
                    Column(
                        Modifier.align(Alignment.Center)
                            .background(TeslaTokens.surface)
                            .padding(TeslaTokens.contentPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Harita yüklenemedi",
                            color = TeslaTokens.error,
                            style = MaterialTheme.typography.labelLarge)
                        Text("Bağlantını kontrol et. Çevrimiçi değilken harita açılamayabilir.",
                            color = TeslaTokens.text,
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            enabled = allowRetry,
                            onClick = {
                                mapFailed = false
                                mapRetry++
                            },
                            modifier = Modifier.heightIn(min = TeslaTokens.touchTarget),
                        ) { Text("Haritayı yeniden dene") }
                    }
                }
            }
        }
        if (next != null) NextStopBar(next, onClick = {
            selectedStation = next.charger
        })
    }
    selectedStation?.let { charger ->
        AlertDialog(
            onDismissRequest = { selectedStation = null },
            title = { Text("Şarj durağı • TAHMİN") },
            text = {
                Text(ChargerEvidence.details(charger).joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = { selectedStation = null }) { Text("Kapat") }
            },
        )
    }
}

@Composable
fun TeslaDashboardScreen(
    s: VehicleState,
    soc: SocReading,
    usableKwhAtSoh: Double,
    gps: GpsSnapshot,
    gpsFresh: Boolean,
    navigation: RouteUiState,
    plan: PlanResult.Success?,
    driveMode: DriveModeSnapshot,
    weatherSummary: String,
    onSelectDestination: (GeoPoint) -> Unit,
    molaInstalled: Boolean,
    guidance: DashboardGuidance = DashboardGuidance.NONE,
    onGuidanceAction: (GuidanceAction) -> Unit = {},
    onAction: (TeslaAction) -> Unit,
) {
    val speed = SpeedSelector.select(
        obdConnected = s.connected && driveMode.obdSpeedFresh, demo = s.demo, obdKmh = s.speedKmh,
        gpsKmh = gps.speedKmh, gpsFresh = gpsFresh,
    )
    val panel = DashboardPresentation.vehicle(s, soc, speed, usableKwhAtSoh,
        powerFresh = driveMode.obdPowerFresh)
    DriveModeScaffold(
        left = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(driveMode.label, color = if (driveMode.mode == DriveMode.DRIVING)
                    TeslaTokens.warning else TeslaTokens.secondaryText,
                    style = MaterialTheme.typography.labelMedium)
                VehicleStatePanel(panel, Modifier.fillMaxWidth(),
                    compact = driveMode.mode != DriveMode.DRIVING)
            }
        },
        center = {
            MapHero(navigation.route, navigation.destination, gps, gpsFresh,
                plan, weatherSummary, onSelectDestination, Modifier.fillMaxSize(),
                allowRetry = driveMode.mode != DriveMode.DRIVING)
        },
        right = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionRail(onAction, molaAvailable = molaInstalled,
                    mode = driveMode.mode,
                    modifier = Modifier.fillMaxWidth())
                TeslaGuidanceCard(
                    guidance, onGuidanceAction,
                    enabled = driveMode.mode != DriveMode.DRIVING,
                )
            }
        },
    )
}

@Preview(name = "Tesla üçlü düzen - DEMO", widthDp = 1000, heightDp = 540,
    showBackground = true)
@Composable
private fun ThreePanePreview() {
    CockpitTheme(dark = true) {
        DriveModeScaffold(
            left = {
                VehicleStatePanel(
                    VehiclePanelUiState(speedKmh = 74.0, socPercent = 60.0,
                        source = SourceKind.DEMO, status = PanelStatus.NORMAL,
                        message = "ÖNİZLEME / DEMO"),
                    compact = true,
                )
            },
            center = {
                MapHero(null, null, GpsSnapshot(), false, null, "",
                    {}, Modifier.fillMaxSize(), previewMap = true)
            },
            right = { ActionRail(onAction = {}) },
        )
    }
}

@Preview(name = "Tesla dikey düzen - DEMO", widthDp = 400, heightDp = 850,
    showBackground = true)
@Composable
private fun VerticalDashboardPreview() {
    CockpitTheme(dark = true) {
        DriveModeScaffold(
            left = { VehicleStatePanel(VehiclePanelUiState(), compact = true) },
            center = {
                MapHero(null, null, GpsSnapshot(), false, null, "",
                    {}, Modifier.fillMaxSize(), previewMap = true)
            },
            right = { ActionRail(onAction = {}) },
        )
    }
}
