package app.evcockpit

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.obd.ObdLogShare
import app.evcockpit.obd.ObdViewModel
import app.evcockpit.obd.PidTable
import app.evcockpit.navigation.NavigationViewModel
import app.evcockpit.ui.RouteScreen
import app.evcockpit.ui.DashboardScreen
import app.evcockpit.ui.ObdTestScreen
import app.evcockpit.ui.RouteSettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val obdViewModel: ObdViewModel = viewModel()
                val navViewModel: NavigationViewModel = viewModel()
                App(obdViewModel, navViewModel)
            }
        }
    }
}

@Composable
private fun App(vm: ObdViewModel, navVm: NavigationViewModel) {
    val context = LocalContext.current
    val cfg = remember { PidTable.load(context) }
    val state by vm.state.collectAsStateWithLifecycle()
    val logs by vm.log.collectAsStateWithLifecycle()
    val logPath by vm.logFilePath.collectAsStateWithLifecycle()
    val diagnosisPath by vm.diagnosisFilePath.collectAsStateWithLifecycle()
    val diagnosisSummary by vm.diagnosisSummary.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val gps by vm.gps.collectAsStateWithLifecycle()
    val navState by navVm.state.collectAsStateWithLifecycle()
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
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Ayarlar") })
        }
        when (tab) {
            0 -> DashboardScreen(
                s = state, usableKwh = cfg.usableKwh, gps = gps,
                navigation = navState, onSelectDestination = navVm::selectDestination,
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
                socPct = state.socPct,
                usableKwh = cfg.usableKwh,
                voice = voiceState,
                onToggleVoice = navVm::toggleVoice,
                onSpeakNextManeuver = navVm::testNextManeuver,
                onCalculate = navVm::calculate,
                onClear = navVm::clear,
            )
            else -> RouteSettingsScreen(
                gps = gps,
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
