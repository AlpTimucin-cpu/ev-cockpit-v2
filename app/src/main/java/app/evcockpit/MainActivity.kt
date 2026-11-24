package app.evcockpit

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.navigation.NavigationViewModel
import app.evcockpit.navigation.PlaceAutocompleteViewModel
import app.evcockpit.obd.ManualSocStore
import app.evcockpit.obd.ObdLogShare
import app.evcockpit.obd.ObdViewModel
import app.evcockpit.obd.SafeTestReportExporter
import app.evcockpit.planui.PlannerViewModel
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.storage.VehicleProfileStore
import app.evcockpit.ui.DisplayPreferences
import app.evcockpit.ui.ObdTestScreen
import app.evcockpit.ui.PlannerScreen
import app.evcockpit.ui.RouteScreen
import app.evcockpit.ui.RouteSettingsScreen
import app.evcockpit.ui.finalhmi.CockpitDesignTokens
import app.evcockpit.ui.finalhmi.FinalCockpitTheme
import app.evcockpit.ui.finalhmi.FinalDashboardTab
import app.evcockpit.ui.finalhmi.MapFirstDashboardScreen
import app.evcockpit.ui.finalhmi.MapFirstDashboardViewModel
import app.evcockpit.ui.tesla.DriveModeViewModel
import app.evcockpit.ui.tesla.SafeSocSelection
import app.evcockpit.weather.RouteHourlyWeatherViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A0: Tek ana Activity, NavHost başlangıcı HARİTA / F1.
 * Mola, medya ve eski PARK / ŞARJ arayüzü bu gezinmede bulunmaz.
 * OBD, GPS, rota, planlama depoları ve DataStore aynı kalır.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val displayPrefs = remember { DisplayPreferences(applicationContext) }
            val keepScreenOn by displayPrefs.keepScreenOn.collectAsStateWithLifecycle(false)
            val darkPref by displayPrefs.darkTheme.collectAsStateWithLifecycle(true)
            DisposableEffect(keepScreenOn) {
                if (keepScreenOn)
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }

            val f1: MapFirstDashboardViewModel = viewModel()
            val dashboard by f1.state.collectAsStateWithLifecycle()
            val obd: ObdViewModel = viewModel()
            val nav: NavigationViewModel = viewModel()
            val search: PlaceAutocompleteViewModel = viewModel()
            val planner: PlannerViewModel = viewModel()
            val weather: RouteHourlyWeatherViewModel = viewModel()
            val drive: DriveModeViewModel = viewModel()
            val scope = rememberCoroutineScope()

            val obdState by obd.state.collectAsStateWithLifecycle()
            val gps by obd.gps.collectAsStateWithLifecycle()
            val obdLog by obd.log.collectAsStateWithLifecycle()
            val logFilePath by obd.logFilePath.collectAsStateWithLifecycle()
            val diagnosisPath by obd.diagnosisFilePath.collectAsStateWithLifecycle()
            val diagnosisSummary by obd.diagnosisSummary.collectAsStateWithLifecycle()
            val devices by obd.devices.collectAsStateWithLifecycle()
            val obdMessage by obd.message.collectAsStateWithLifecycle()
            val driveState by drive.state.collectAsStateWithLifecycle()
            val navState by nav.state.collectAsStateWithLifecycle()
            val voiceState by nav.voice.collectAsStateWithLifecycle()
            val weatherSummary by weather.summary.collectAsStateWithLifecycle()
            val planState by planner.state.collectAsStateWithLifecycle()

            val manualStore = remember { ManualSocStore(applicationContext) }
            val manualSoc by manualStore.socPct.collectAsStateWithLifecycle(null)
            val profileStore = remember { VehicleProfileStore(applicationContext) }
            val profile by profileStore.profile.collectAsStateWithLifecycle(
                VehicleProfile.defaults())
            val socReading = SafeSocSelection.select(obdState, manualSoc, driveState)
            var diagnosisError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(navState.route) {
                // İstek yalnız mevcut ViewModel'de yapılır; ekran G/Ç işlemez.
                weather.refresh(navState.route)
            }

            // İzinler yalnız ilgili kullanıcı eylemiyle istenir; açılışta izin yağmuru yok.
            val gpsPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { result ->
                if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                ) obd.startGps() else obd.denyGps()
            }
            val bluetoothPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { _ -> obd.refreshBonded() }

            val openDiagnostics: () -> Unit = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothPermission.launch(
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                } else {
                    obd.refreshBonded()
                }
            }

            FinalCockpitTheme {
                MapFirstDashboardScreen(
                    state = dashboard,
                    onDemoChange = f1::showDemo,
                    demoAvailable = f1.demoAvailable,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    tabContent = { tab, openDiagnosticRoute, navigateToTab ->
                        when (tab) {
                            FinalDashboardTab.ROTA -> RouteScreen(
                                state = navState,
                                gps = gps,
                                socPct = socReading.pct,
                                socSource = socReading.source,
                                vehicleProfile = profile,
                                voice = voiceState,
                                onToggleVoice = nav::toggleVoice,
                                onSpeakNextManeuver = nav::testNextManeuver,
                                onCalculate = nav::calculateSelected,
                                onSelectDestination = nav::selectDestination,
                                onSelectNamedDestination = nav::selectNamedDestination,
                                onSelectOrigin = nav::selectOrigin,
                                onUseGpsOrigin = nav::useGpsOrigin,
                                onClearDestinationSelection = nav::clearDestinationSelection,
                                onSelectAlternative = nav::selectAlternative,
                                onManualSoc = { value ->
                                    scope.launch { manualStore.save(value) }
                                },
                                searchVm = search,
                                routeWeather = weatherSummary,
                                onClear = nav::clear,
                            )
                            FinalDashboardTab.SARJ -> PlannerScreen(
                                vm = planner,
                                navigation = navState.route,
                                soc = socReading,
                                gps = gps,
                                onOpenRoute = { navigateToTab(FinalDashboardTab.ROTA) },
                                onOpenSettings = { navigateToTab(FinalDashboardTab.AYARLAR) },
                                onNavigate = { stops, destination ->
                                    nav.navigateVia(stops, destination)
                                    navigateToTab(FinalDashboardTab.ROTA)
                                },
                                onSelectDestination = nav::selectDestination,
                            )
                            FinalDashboardTab.ENERJI -> A0EnergyPlaceholder()
                            FinalDashboardTab.AYARLAR -> RouteSettingsScreen(
                                gps = gps,
                                darkTheme = darkPref,
                                keepScreenOn = keepScreenOn,
                                onDarkTheme = { value ->
                                    scope.launch { displayPrefs.setDarkTheme(value) }
                                },
                                onKeepScreenOn = { value ->
                                    scope.launch { displayPrefs.setKeepScreenOn(value) }
                                },
                                manualSocPct = manualSoc,
                                onSaveManualSoc = { value ->
                                    scope.launch { manualStore.save(value) }
                                },
                                onClearManualSoc = {
                                    scope.launch { manualStore.clear() }
                                },
                                onStartGps = {
                                    gpsPermission.launch(arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ))
                                },
                                onStopGps = obd::stopGps,
                                onOpenObdTest = {
                                    openDiagnostics()
                                    openDiagnosticRoute()
                                },
                            )
                            else -> Unit // HARİTA, NavHost'un ilk hedefidir.
                        }
                    },
                    diagnosticContent = {
                        ObdTestScreen(
                            state = obdState,
                            log = obdLog,
                            logFilePath = logFilePath,
                            diagnosisFilePath = diagnosisPath,
                            diagnosisSummary = diagnosisSummary,
                            devices = devices,
                            message = diagnosisError ?: obdMessage,
                            onRefresh = obd::refreshBonded,
                            onConnect = obd::connect,
                            onDiagnose = obd::diagnose,
                            onExportTestReport = { note ->
                                diagnosisError = null
                                scope.launch {
                                    try {
                                        val file = withContext(Dispatchers.IO) {
                                            SafeTestReportExporter.create(
                                                this@MainActivity, diagnosisSummary,
                                                obdLog.takeLast(200), note)
                                        }
                                        ObdLogShare.launch(this@MainActivity, file)
                                    } catch (_: Exception) {
                                        diagnosisError = "OBD test raporu paylaşılamadı."
                                    }
                                }
                            },
                            onShareDiagnosis = {
                                if (!ObdLogShare.launch(this@MainActivity, diagnosisPath))
                                    diagnosisError = "Tanı günlüğü bulunamadı."
                            },
                            onDemo = {
                                // OBD testindeki örnek değerler hiçbir zaman üretimde açılmaz.
                                if (BuildConfig.DEBUG) obd.startDemo()
                            },
                            onStop = obd::stop,
                            onShareLog = {
                                if (!ObdLogShare.launch(this@MainActivity, logFilePath))
                                    diagnosisError = "Paylaşılacak günlük bulunamadı."
                            },
                            onOpenBluetoothSettings = {
                                try {
                                    startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                } catch (_: Exception) {
                                    diagnosisError = "Bluetooth ayarları açılamadı."
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun A0EnergyPlaceholder() {
    val t = CockpitDesignTokens
    Surface(
        color = t.panel, shape = t.corner,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            Modifier.padding(t.spaceXL),
            verticalArrangement = Arrangement.spacedBy(t.spaceM),
        ) {
            Text("Enerji", color = t.text, fontSize = t.headText)
            Text("Henüz doğrulanmış bir yolculuk enerji kaydı bulunmuyor.",
                color = t.secondary, fontSize = t.bodyText)
            Text("Mevcut enerji ve şarj hesapları Şarj sekmesindeki gelişmiş planlayıcıda korunuyor.",
                color = t.secondary, fontSize = t.bodyText)
        }
    }
}
