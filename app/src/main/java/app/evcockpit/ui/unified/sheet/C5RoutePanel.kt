package app.evcockpit.ui.unified.sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.R
import app.evcockpit.domain.nav.RouteModel
import app.evcockpit.charging.tariffs.*
import app.evcockpit.navigation.RouteAlternativePreview
import app.evcockpit.obd.ObdViewModel
import app.evcockpit.obd.ManualSocStore
import app.evcockpit.obd.SocReading
import app.evcockpit.planui.*
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.storage.PlannerOptionsStore
import app.evcockpit.storage.VehicleProfileStore
import app.evcockpit.ui.tesla.DriveModeViewModel
import app.evcockpit.ui.tesla.DriveMode
import app.evcockpit.ui.tesla.SafeSocSelection
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.unified.*
import app.evcockpit.weather.RouteHourlyForecastPolicy

@Composable
fun C5RoutePanel(route: RouteModel, data: C5Data, refresh: () -> Unit) {
    val t = CockpitDesignTokens
    val context = LocalContext.current.applicationContext
    val profileStore = remember(context) { VehicleProfileStore(context) }
    val optionsStore = remember(context) { PlannerOptionsStore(context) }
    val manualStore = remember(context) { ManualSocStore(context) }
    val profile by profileStore.profile.collectAsStateWithLifecycle(VehicleProfile.defaults())
    val options by optionsStore.options.collectAsStateWithLifecycle(PlannerOptions())
    val manual by manualStore.socPct.collectAsStateWithLifecycle(null)
    val obd: ObdViewModel = viewModel()
    val vehicle by obd.state.collectAsStateWithLifecycle()
    val drive: DriveModeViewModel = viewModel()
    val mode by drive.state.collectAsStateWithLifecycle()
    val reading = SafeSocSelection.select(vehicle, manual, mode)
    val soc = if (reading.source.startsWith("DEMO", true)) SocReading(null, reading.source) else reading
    val nav = remember(route) { C5Projection.navigation(route) }
    val preview = remember(nav, profile, soc, options) {
        RouteAlternativePreview.calculate(nav, options.applyTo(profile), soc.pct, options.minArrivalSoc)
    }
    val planner: PlannerViewModel = viewModel(key = "unified-c5-planner")
    val planState by planner.state.collectAsStateWithLifecycle()
    val busy = planState.stage in setOf(PlannerStage.ELEVATION, PlannerStage.STATIONS,
        PlannerStage.OPTIMIZING, PlannerStage.VERIFYING)
    DisposableEffect(route.id) { onDispose { planner.cancel() } }
    val currentPlan = planState.takeIf { it.route == nav && it.stage == PlannerStage.READY }
    val result = currentPlan?.plan as? PlanResult.Success
    val request = currentPlan?.request
    val currentData = data.takeIf { it.routeId == route.id } ?: C5Data()
    val unknown = stringResource(R.string.a0b_c5_unknown)

    Text(stringResource(R.string.a0b_c5_title), color = t.text, fontSize = t.titleText)
    Button(onClick = refresh, enabled = !currentData.stationBusy && !currentData.weatherBusy &&
        mode.mode != DriveMode.DRIVING, modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
        Text(stringResource(R.string.a0b_c5_refresh))
    }
    Text(stringResource(R.string.a0b_c5_stations), color = t.text, fontSize = t.titleText)
    when {
        currentData.stationBusy -> {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(R.string.a0b_c5_loading))
        }
        currentData.missingKey -> Text(stringResource(R.string.a0b_c5_key), color = t.caution)
        currentData.stations.isEmpty() -> Text(stringResource(R.string.a0b_c5_no_stations), color = t.secondary)
    }
    currentData.stationMessage?.let { Text(it, color = t.secondary) }
    if (!currentData.complete) Text(stringResource(R.string.a0b_c5_partial), color = t.caution)
    Text(stringResource(R.string.a0b_c5_occupancy), color = t.secondary)
    currentData.stations.forEach { station ->
        var expanded by remember(station.id) { mutableStateOf(false) }
        OutlinedButton(onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
            Text(station.name + " · " + station.operator)
        }
        if (expanded) {
            Text(stringResource(R.string.a0b_c5_station_metrics, station.alongKm, station.awayKm))
            val power = station.powerKw?.let { stringResource(R.string.a0b_c5_kw, it) } ?: unknown
            Text(stringResource(R.string.a0b_c5_station_power, power))
            val price = TariffResolver.resolve(currentData.catalog, currentData.manual,
                station.operator, ChargingKind.DC)
            val amount = price.priceTlPerKwh
            Text(if (amount != null) stringResource(R.string.a0b_c5_price,
                amount, price.source, price.updatedAt ?: unknown)
                else stringResource(R.string.a0b_c5_price_unknown))
            if (amount != null && price.stale) Text(stringResource(R.string.a0b_c5_stale), color = t.caution)
        }
    }
    Text(stringResource(R.string.a0b_c5_weather), color = t.text, fontSize = t.titleText)
    if (currentData.weatherBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
    else if (currentData.forecasts.isEmpty())
        Text(stringResource(R.string.a0b_c5_weather_failed), color = t.secondary)
    else Text(RouteHourlyForecastPolicy.summary(currentData.forecasts.map { it.value }))
    Text(stringResource(R.string.a0b_c5_weather_partial), color = t.secondary)

    Text(stringResource(R.string.a0b_c5_energy), color = t.text, fontSize = t.titleText)
    Text(stringResource(R.string.a0b_c5_energy_note), color = t.secondary)
    val start = soc.pct?.let { stringResource(R.string.a0b_c5_pct, it) } ?: unknown
    Text(stringResource(R.string.a0b_c5_soc, start, soc.source))
    if (preview != null) {
        Text(stringResource(R.string.a0b_c5_energy_use, preview.routeEnergyKwh, preview.averageKwhPer100Km))
        val arrival = preview.arrivalSocPct?.let { stringResource(R.string.a0b_c5_pct, it) } ?: unknown
        Text(stringResource(R.string.a0b_c5_arrival, arrival))
        preview.extraKwhForMinimum?.let { Text(stringResource(R.string.a0b_c5_deficit, it)) }
        if (preview.energyDeficitKwh != null) Text(stringResource(R.string.a0b_c5_insufficient), color = t.caution)
    }
    Text(stringResource(R.string.a0b_c5_plan_note), color = t.secondary)
    Button(onClick = { planner.plan(nav, soc, options, profile) },
        enabled = !busy && soc.pct != null && mode.allowComplexActions,
        modifier = Modifier.fillMaxWidth().heightIn(min = t.target)) {
        Text(stringResource(R.string.a0b_c5_plan))
    }
    if (!mode.allowComplexActions) Text(stringResource(R.string.a0b_c5_park), color = t.caution)
    if (busy) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedButton(onClick = planner::cancel) { Text(stringResource(R.string.a0b_c5_plan_cancel)) }
    }
    if (planState.route == nav && planState.stage != PlannerStage.IDLE)
        Text(planState.message, color = t.secondary)
    if (result != null && request != null) {
        Text(stringResource(R.string.a0b_c5_plan_result,
            result.stops.size, result.arrivalSocPct, result.chargingMinutes))
        result.stops.forEach { stop ->
            Text(stringResource(R.string.a0b_c5_stop, stop.charger.name, stop.charger.operator,
                stop.arrivalSocPct, stop.departureSocPct))
        }
        val cost = C5Projection.plannedCost(result, request.car.capacityAtSohKwh, currentData)
        Text(if (cost == null) stringResource(R.string.a0b_c5_cost_unknown)
            else stringResource(R.string.a0b_c5_cost, cost), color = t.text)
    }
}
