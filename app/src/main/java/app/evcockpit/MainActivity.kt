package app.evcockpit

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
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
import androidx.lifecycle.lifecycleScope
import app.evcockpit.obd.ObdRepository
import app.evcockpit.obd.PidTable
import app.evcockpit.ui.DashboardScreen
import app.evcockpit.ui.ObdTestScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = ObdRepository(applicationContext, lifecycleScope)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { App(repo) }
        }
    }
}

@Composable
private fun App(repo: ObdRepository) {
    val ctx = LocalContext.current
    val cfg = remember { PidTable.load(ctx) }
    val state by repo.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(1) } // ilk açılışta test ekranı
    var devices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }

    fun refresh() {
        devices = try {
            ctx.getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices?.toList().orEmpty()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    val perms = remember {
        if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }
    LaunchedEffect(Unit) { launcher.launch(perms) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Gösterge") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("OBD Test") })
        }
        when (tab) {
            0 -> DashboardScreen(state, cfg.usableKwh)
            else -> ObdTestScreen(repo, devices, onRefresh = ::refresh)
        }
    }
}
