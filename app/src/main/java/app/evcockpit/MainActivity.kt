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
import app.evcockpit.ui.DashboardScreen
import app.evcockpit.ui.ObdTestScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val viewModel: ObdViewModel = viewModel()
                App(viewModel)
            }
        }
    }
}

@Composable
private fun App(vm: ObdViewModel) {
    val context = LocalContext.current
    val cfg = remember { PidTable.load(context) }
    val state by vm.state.collectAsStateWithLifecycle()
    val logs by vm.log.collectAsStateWithLifecycle()
    val logPath by vm.logFilePath.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
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

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Gösterge") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("OBD Test") })
        }
        when (tab) {
            0 -> DashboardScreen(state, cfg.usableKwh)
            else -> ObdTestScreen(
                state = state,
                log = logs,
                logFilePath = logPath,
                devices = devices,
                message = shareError ?: message,
                onRefresh = vm::refreshBonded,
                onConnect = vm::connect,
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
        }
    }
}
