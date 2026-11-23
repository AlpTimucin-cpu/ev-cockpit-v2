package app.evcockpit

import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.obd.ObdLogShare
import app.evcockpit.obd.SafeTestReportExporter
import app.evcockpit.obd.ObdViewModel
import app.evcockpit.obd.ManualSocStore
import app.evcockpit.navigation.NavigationViewModel
import app.evcockpit.navigation.PlaceAutocompleteViewModel
import app.evcockpit.weather.RouteHourlyWeatherViewModel
import app.evcockpit.passenger.PassengerPlayerModel
import app.evcockpit.location.SpeedSelector
import app.evcockpit.integration.MolaOpenContract
import kotlinx.coroutines.flow.MutableStateFlow
import app.evcockpit.ui.PassengerVideoScreen
import app.evcockpit.ui.RouteScreen
import app.evcockpit.ui.PlannerScreen
import app.evcockpit.planui.PlannerViewModel
import app.evcockpit.ui.tesla.TeslaDashboardScreen
import app.evcockpit.ui.tesla.TeslaAction
import app.evcockpit.ui.tesla.DriveMode
import app.evcockpit.ui.tesla.DriveModeViewModel
import app.evcockpit.ui.tesla.DriveModePolicy
import app.evcockpit.integration.MolaLinkViewModel
import app.evcockpit.integration.MolaStatus
import app.evcockpit.ui.tesla.MolaLaunchCard
import app.evcockpit.ui.tesla.DashboardGuidancePolicy
import app.evcockpit.ui.tesla.GuidanceAction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.evcockpit.ui.tesla.SafeSocSelection
import app.evcockpit.storage.VehicleProfileStore
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.ui.ObdTestScreen
import app.evcockpit.ui.RouteSettingsScreen
import app.evcockpit.ui.CockpitTheme
import app.evcockpit.ui.CockpitTabs
import app.evcockpit.ui.DisplayPreferences
import kotlinx.coroutines.launch

private data class ExternalOpenRequest(val generation: Int = 0, val tab: Int? = null)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val externalOpenRequest = MutableStateFlow(ExternalOpenRequest())

    private fun acceptOpenIntent(value: Intent?) {
        if (value?.action != Intent.ACTION_VIEW) return
        val target = MolaOpenContract.tabFor(value.dataString) ?: return
        val previous = externalOpenRequest.value
        externalOpenRequest.value = ExternalOpenRequest(previous.generation + 1, target)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptOpenIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptOpenIntent(intent)
        setContent {
            val openRequest by externalOpenRequest.collectAsState()
            val displayPreferences = remember { DisplayPreferences(applicationContext) }
            val darkTheme by displayPreferences.darkTheme.collectAsState(
                initial = true,
            )
            val keepScreenOn by displayPreferences.keepScreenOn.collectAsState(
                initial = false,
            )
            DisposableEffect(keepScreenOn) {
                if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose { /* next effect handles updates; Activity teardown clears the flag */ }
            }
            CockpitTheme(dark = darkTheme) {
                val obdViewModel: ObdViewModel = viewModel()
                val navViewModel: NavigationViewModel = viewModel()
                val placeSearchModel: PlaceAutocompleteViewModel = viewModel()
                val routeWeatherModel: RouteHourlyWeatherViewModel = viewModel()
                val passengerModel: PassengerPlayerModel = viewModel()
                val plannerModel: PlannerViewModel = viewModel()
                val driveModel: DriveModeViewModel = viewModel()
                val molaModel: MolaLinkViewModel = viewModel()
                App(obdViewModel, navViewModel, placeSearchModel, routeWeatherModel, passengerModel, plannerModel,
                    driveModel, molaModel, displayPreferences, darkTheme, keepScreenOn, openRequest)
            }
        }
    }
}

@Composable
private fun App(vm: ObdViewModel, navVm: NavigationViewModel,
    placeSearchModel: PlaceAutocompleteViewModel,
    routeWeatherModel: RouteHourlyWeatherViewModel,
    passengerModel: PassengerPlayerModel, plannerModel: PlannerViewModel,
    driveModel: DriveModeViewModel,
    molaModel: MolaLinkViewModel,
    displayPreferences: DisplayPreferences,
    darkTheme: Boolean, keepScreenOn: Boolean,
    openRequest: ExternalOpenRequest) {
    val context = LocalContext.current
    val molaState by molaModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, molaModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) molaModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            molaModel.refresh()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val vehicleProfileStore = remember(context) { VehicleProfileStore(context.applicationContext) }
    val vehicleProfile by vehicleProfileStore.profile.collectAsStateWithLifecycle(
        initialValue = VehicleProfile.defaults())
    val planUi by plannerModel.state.collectAsStateWithLifecycle()
    val manualStore = remember(context) { ManualSocStore(context.applicationContext) }
    val manualSoc by manualStore.socPct.collectAsStateWithLifecycle(initialValue = null)
    val saveScope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle()
    val drive by driveModel.state.collectAsStateWithLifecycle()
    val socReading = SafeSocSelection.select(state, manualSoc, drive)
    val logs by vm.log.collectAsStateWithLifecycle()
    val logPath by vm.logFilePath.collectAsStateWithLifecycle()
    val diagnosisPath by vm.diagnosisFilePath.collectAsStateWithLifecycle()
    val diagnosisSummary by vm.diagnosisSummary.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val gps by vm.gps.collectAsStateWithLifecycle()
    val gpsFresh = drive.gpsFresh
    val navState by navVm.state.collectAsStateWithLifecycle()
    val weatherSummary by routeWeatherModel.summary.collectAsStateWithLifecycle()
    LaunchedEffect(navState.route) {
        // Only ViewModel/repository makes the network call, not this Composable.
        routeWeatherModel.refresh(navState.route)
    }
    val voiceState by navVm.voice.collectAsStateWithLifecycle()
    var shareError by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var showMolaCard by remember { mutableStateOf(false) }
    LaunchedEffect(drive.mode) {
        if (drive.mode != DriveMode.PARKED) showMolaCard = false
    }
    LaunchedEffect(openRequest.generation) {
        openRequest.tab?.let { requested ->
            if (DriveModePolicy.allowedTab(requested, drive.mode)) tab = requested
        }
    }
    LaunchedEffect(drive.mode, tab) {
        val mustClose = (drive.mode == DriveMode.DRIVING && tab in setOf(3,4,5)) ||
            (drive.mode != DriveMode.PARKED && tab == 4)
        if (mustClose) {
            passengerModel.pause()
            tab = 0
        }
    }

    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { vm.refreshBonded() }
    LaunchedEffect(vm) { launcher.launch(permissions) }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) vm.startGps() else vm.denyGps()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)) {
        if (tab != 0) CockpitTabs(selected = tab, onSelect = { selected ->
            if (DriveModePolicy.allowedTab(selected, drive.mode)) tab = selected
        })
        when (tab) {
            0 -> TeslaDashboardScreen(
                s = state,
                soc = socReading,
                usableKwhAtSoh = vehicleProfile.capacityAtSohKwh,
                gps = gps,
                gpsFresh = gpsFresh,
                navigation = navState,
                plan = (planUi.plan as? PlanResult.Success).takeIf {
                    planUi.route == navState.route
                },
                driveMode = drive,
                weatherSummary = weatherSummary,
                onSelectDestination = navVm::selectDestination,
                molaInstalled = molaState.status == MolaStatus.READY ||
                    molaState.status == MolaStatus.CANNOT_LAUNCH,
                guidance = DashboardGuidancePolicy.evaluate(
                    state, socReading, gps, gpsFresh, navState,
                    planUi.plan.takeIf { planUi.route == navState.route }),
                onGuidanceAction = { action ->
                    if (drive.mode != DriveMode.DRIVING) when (action) {
                        GuidanceAction.NONE -> Unit
                        GuidanceAction.SETTINGS -> tab = 5
                        GuidanceAction.LOCATION_PERMISSION -> gpsPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION))
                        GuidanceAction.OBD_TEST -> tab = 1
                        GuidanceAction.ROUTE -> tab = 2
                        GuidanceAction.PLAN -> tab = 3
                    }
                },
                onAction = { action ->
                    when (action) {
                        TeslaAction.ROTA -> tab = 2
                        TeslaAction.PLAN -> tab = 3
                        TeslaAction.MEDYA, TeslaAction.VIDEO -> tab = 4
                        TeslaAction.MOLA -> {
                            if (drive.mode == DriveMode.PARKED) {
                                molaModel.refresh()
                                showMolaCard = true
                            }
                        }
                        TeslaAction.AYARLAR -> tab = 5
                    }
                },
            )
            1 -> ObdTestScreen(
                state = state,
                log = logs,
                logFilePath = logPath,
                diagnosisFilePath = diagnosisPath,
                diagnosisSummary = diagnosisSummary,
                devices = devices,
                message = shareError ?: message,
                onRefresh = vm::refreshBonded,
                onConnect = vm::connect,
                onDiagnose = vm::diagnose,
                onExportTestReport = { carScannerNote ->
                    shareError = null
                    val lastLogs = logs.takeLast(200)
                    val lastSummary = diagnosisSummary
                    saveScope.launch {
                        try {
                            val path = withContext(Dispatchers.IO) {
                                SafeTestReportExporter.create(context, lastSummary, lastLogs, carScannerNote)
                            }
                            ObdLogShare.launch(context, path)
                        } catch (error: Exception) {
                            shareError = "Test raporu dışa aktarılamadı: " +
                                error.javaClass.simpleName
                        }
                    }
                },
                onShareDiagnosis = {
                    shareError = null
                    try {
                        if (!ObdLogShare.launch(context, diagnosisPath)) {
                            shareError = "Tanı logu bulunamadı."
                        }
                    } catch (error: Exception) {
                        shareError = "Tanı logu paylaşılamadı: " +
                            error.javaClass.simpleName
                    }
                },
                onDemo = vm::startDemo,
                onStop = vm::stop,
                onShareLog = {
                    shareError = null
                    try {
                        if (!ObdLogShare.launch(context, logPath)) {
                            shareError = "Paylaşılacak kayıt henüz oluşturulmadı."
                        }
                    } catch (error: Exception) {
                        shareError = "Kayıt paylaşılamadı: ${error.message ?: "Bilinmeyen hata"}"
                    }
                },
                onOpenBluetoothSettings = {
                    shareError = null
                    try {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    } catch (error: Exception) {
                        shareError = "Bluetooth ayarları açılamadı: ${error.message ?: "Bilinmeyen hata"}"
                    }
                },
            )
            2 -> RouteScreen(
                state = navState,
                gps = gps,
                socPct = socReading.pct,
                socSource = socReading.source,
                usableKwh = vehicleProfile.capacityAtSohKwh,
                voice = voiceState,
                onToggleVoice = navVm::toggleVoice,
                onSpeakNextManeuver = navVm::testNextManeuver,
                onCalculate = navVm::calculate,
                onSelectDestination = navVm::selectDestination,
                onSelectNamedDestination = navVm::selectNamedDestination,
                onSelectOrigin = navVm::selectOrigin,
                onUseGpsOrigin = navVm::useGpsOrigin,
                onClearDestinationSelection = navVm::clearDestinationSelection,
                onSelectAlternative = navVm::selectAlternative,
                searchVm = placeSearchModel,
                routeWeather = weatherSummary,
                onClear = navVm::clear,
            )
            3 -> PlannerScreen(
                vm = plannerModel, navigation = navState.route, soc = socReading,
                onOpenRoute = { tab = 2 }, onOpenSettings = { tab = 5 },
                onNavigate = { stops, destination ->
                    navVm.navigateVia(stops,destination)
                    tab = 2
                },
                gps = gps,
                onSelectDestination = navVm::selectDestination,
            )
            4 -> PassengerVideoScreen(
                vm = passengerModel,
                gps = gps,
                route = navState.route,
                destination = navState.destination,
                speedKmh = SpeedSelector.select(
                    obdConnected = state.connected && drive.obdSpeedFresh, demo = state.demo,
                    obdKmh = state.speedKmh, gpsKmh = gps.speedKmh,
                    gpsFresh = gpsFresh).kmh,
                onSelectDestination = navVm::selectDestination,
            )
            else -> RouteSettingsScreen(
                gps = gps,
                darkTheme = darkTheme,
                keepScreenOn = keepScreenOn,
                onDarkTheme = { value -> saveScope.launch { displayPreferences.setDarkTheme(value) } },
                onKeepScreenOn = { value -> saveScope.launch { displayPreferences.setKeepScreenOn(value) } },
                manualSocPct = manualSoc,
                onSaveManualSoc = { value -> saveScope.launch { manualStore.save(value) } },
                onClearManualSoc = { saveScope.launch { manualStore.clear() } },
                onStartGps = {
                    gpsPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
                onStopGps = vm::stopGps,
            )
        }
    }
    if (showMolaCard && drive.mode == DriveMode.PARKED) {
        MolaLaunchCard(
            state = molaState,
            onOpen = molaModel::openMola,
            onInstallGuide = molaModel::openInstallGuide,
            onAppSettings = molaModel::openSettings,
            onRefresh = molaModel::refresh,
            onDismiss = { showMolaCard = false },
        )
    }

}
