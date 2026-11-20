package app.evcockpit

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.obd.ObdLogShare
import app.evcockpit.obd.SafeTestReportExporter
import app.evcockpit.obd.ObdViewModel
import app.evcockpit.obd.ManualSocStore
import app.evcockpit.obd.SocSelector
import app.evcockpit.obd.PidTable
import app.evcockpit.navigation.NavigationViewModel
import app.evcockpit.weather.OpenMeteoRouteWeather
import app.evcockpit.weather.OpenMeteoWeatherLogic
import app.evcockpit.passenger.PassengerPlayerModel
import app.evcockpit.location.SpeedSelector
import app.evcockpit.ui.PassengerVideoScreen
import app.evcockpit.ui.RouteScreen
import app.evcockpit.ui.DashboardScreen
import app.evcockpit.ui.ObdTestScreen
import app.evcockpit.ui.RouteSettingsScreen
import app.evcockpit.ui.DisplayPreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
            MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
                val obdViewModel: ObdViewModel = viewModel()
                val navViewModel: NavigationViewModel = viewModel()
                val passengerModel: PassengerPlayerModel = viewModel()
                App(obdViewModel, navViewModel, passengerModel,
                    displayPreferences, darkTheme, keepScreenOn)
            }
        }
    }
}

@Composable
private fun App(vm: ObdViewModel, navVm: NavigationViewModel,
    passengerModel: PassengerPlayerModel, displayPreferences: DisplayPreferences,
    darkTheme: Boolean, keepScreenOn: Boolean) {
    val context = LocalContext.current
    val cfg = remember { PidTable.load(context) }
    val manualStore = remember(context) { ManualSocStore(context.applicationContext) }
    val manualSoc by manualStore.socPct.collectAsStateWithLifecycle(initialValue = null)
    val saveScope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle()
    val socReading = SocSelector.select(state.connected, state.demo, state.socPct, manualSoc)
    val logs by vm.log.collectAsStateWithLifecycle()
    val logPath by vm.logFilePath.collectAsStateWithLifecycle()
    val diagnosisPath by vm.diagnosisFilePath.collectAsStateWithLifecycle()
    val diagnosisSummary by vm.diagnosisSummary.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val gps by vm.gps.collectAsStateWithLifecycle()
    val navState by navVm.state.collectAsStateWithLifecycle()
    val weatherProvider = remember { OpenMeteoRouteWeather() }
    var weatherSummary by remember { mutableStateOf("Hava: rota hesaplanmadı") }
    LaunchedEffect(navState.route) {
        val route = navState.route
        weatherSummary = if (route == null) "Hava: rota hesaplanmadı" else try {
            OpenMeteoWeatherLogic.summary(weatherProvider.forecast(route))
        } catch (_: Exception) {
            "Hava: servis erişimi yok — DOĞRULANMADI"
        }
    }
    val voiceState by navVm.voice.collectAsStateWithLifecycle()
    var shareError by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(1) }

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

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Gösterge") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("OBD Test") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Rota") })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Yolcu video") })
            Tab(selected = tab == 4, onClick = { tab = 4 }, text = { Text("Ayarlar") })
        }
        when (tab) {
            0 -> DashboardScreen(
                s = state, soc = socReading, usableKwh = cfg.usableKwh, gps = gps,
                navigation = navState, weatherSummary = weatherSummary,
                onSelectDestination = navVm::selectDestination,
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
                onExportTestReport = {
                    shareError = null
                    val lastLogs = logs.takeLast(200)
                    val lastSummary = diagnosisSummary
                    saveScope.launch {
                        try {
                            val path = withContext(Dispatchers.IO) {
                                SafeTestReportExporter.create(context, lastSummary, lastLogs)
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
                usableKwh = cfg.usableKwh,
                voice = voiceState,
                onToggleVoice = navVm::toggleVoice,
                onSpeakNextManeuver = navVm::testNextManeuver,
                onCalculate = navVm::calculate,
                onSelectDestination = navVm::selectDestination,
                onClear = navVm::clear,
            )
            3 -> PassengerVideoScreen(
                vm = passengerModel,
                gps = gps,
                route = navState.route,
                destination = navState.destination,
                speedKmh = SpeedSelector.select(
                    obdConnected = state.connected, demo = state.demo,
                    obdKmh = state.speedKmh, gpsKmh = gps.speedKmh,
                    gpsFresh = gps.isFresh(android.os.SystemClock.elapsedRealtime())).kmh,
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
}
