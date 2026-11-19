package app.evcockpit.obd

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import app.evcockpit.CockpitApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ObdViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CockpitApplication
    private val repo get() = app.obdRepository

    val state = repo.state.asStateFlow()
    val log = repo.log.asStateFlow()
    val logFilePath = repo.logFilePath.asStateFlow()

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices = _devices.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private fun bluetoothAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun refreshBonded() {
        if (!bluetoothAllowed()) {
            _message.value = "Bluetooth izni gerekli. Uygulama izinlerini açın."
            _devices.value = emptyList()
            return
        }
        try {
            val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
            _devices.value = adapter?.bondedDevices?.sortedBy { it.name ?: it.address }.orEmpty()
            _message.value = if (_devices.value.isEmpty()) {
                "Önce ELM327 cihazını telefonun Bluetooth ayarlarından eşleştirin."
            } else null
        } catch (error: SecurityException) {
            _devices.value = emptyList()
            _message.value = "Bluetooth izni verilmedi."
        } catch (error: Exception) {
            _message.value = "Eşleşmiş cihazlar okunamadı: ${error.message ?: "Bilinmeyen hata"}"
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        if (!bluetoothAllowed()) {
            _message.value = "Bağlanmak için Bluetooth izni verin."
            return
        }
        try {
            val intent = Intent(app, ObdConnectionService::class.java).apply {
                action = ObdConnectionService.ACTION_CONNECT
                putExtra(ObdConnectionService.EXTRA_DEVICE_ADDRESS, device.address)
            }
            ContextCompat.startForegroundService(app, intent)
            _message.value = null
        } catch (error: Exception) {
            _message.value = "OBD servisi başlatılamadı: ${error.message ?: "Bilinmeyen hata"}"
        }
    }

    fun stop() {
        app.stopService(Intent(app, ObdConnectionService::class.java))
        repo.stop()
        _message.value = null
    }

    fun startDemo() {
        app.stopService(Intent(app, ObdConnectionService::class.java))
        repo.startDemo()
        _message.value = null
    }
}
