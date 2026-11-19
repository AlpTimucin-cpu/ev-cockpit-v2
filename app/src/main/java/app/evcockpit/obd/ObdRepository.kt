package app.evcockpit.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

data class VehicleState(
    val connected: Boolean = false,
    val demo: Boolean = false,
    val speedKmh: Double? = null,
    val socPct: Double? = null,
    val hvVolt: Double? = null,
    val hvAmp: Double? = null,
    val statusText: String = "Bağlı değil",
) {
    /** Positive: traction. Negative: regeneration/charging. Community PID scale unverified. */
    val powerKw: Double? get() = if (hvVolt != null && hvAmp != null) hvVolt * hvAmp / 1000.0 else null
}

/**
 * Application-scoped OBD session, independent of Activity recreation.
 * Connected-device foreground service owns when a real OBD session starts/stops.
 */
class ObdRepository(private val ctx: Context, private val scope: CoroutineScope) {
    val state = MutableStateFlow(VehicleState())
    val log = MutableStateFlow<List<String>>(emptyList())
    val logFilePath = MutableStateFlow<String?>(null)

    private var job: Job? = null
    @Volatile private var activeTransport: ObdTransport? = null
    @Volatile private var logFile: File? = null

    private fun addLine(value: String) {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "$stamp $value"
        log.update { (it + line).takeLast(400) }
        // Caller is on Dispatchers.IO (including demo); no disk access on main thread.
        runCatching { logFile?.appendText(line + "\n") }
    }

    private fun newLogFile() {
        val name = "obd_log_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".txt"
        val directory = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        logFile = File(directory, name)
        logFilePath.value = logFile?.absolutePath
        log.value = emptyList()
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stop()
        newLogFile()
        job = scope.launch(Dispatchers.IO) {
            var failureCount = 0
            while (currentCoroutineContext().isActive) {
                val transport = BluetoothSppTransport(device)
                activeTransport = transport
                try {
                    state.value = VehicleState(statusText = "OBD cihazına bağlanılıyor")
                    addLine("Bağlantı deneniyor: ${device.name ?: device.address}")
                    val cfg = PidTable.load(ctx)
                    transport.open()
                    addLine("Bluetooth bağlantısı kuruldu")
                    val elm = Elm327(transport, ::addLine)
                    elm.initialize(cfg)
                    failureCount = 0
                    state.value = VehicleState(connected = true, statusText = "Bağlı")
                    addLine("ELM327 hazır (PID ölçekleri doğrulanmadı)")
                    val lastRun = HashMap<String, Long>()

                    while (currentCoroutineContext().isActive) {
                        val now = System.currentTimeMillis()
                        var queried = false
                        for (pid in cfg.pids) {
                            if (!currentCoroutineContext().isActive) break
                            if (now - (lastRun[pid.id] ?: 0L) < pid.everyMs) continue
                            lastRun[pid.id] = now
                            queried = true
                            elm.setHeader(pid.header ?: cfg.txHeader)
                            val data = elm.query(pid.cmd)
                            if (data == null) {
                                addLine("! ${pid.id}: veri yok/yanıt reddedildi")
                                continue
                            }
                            val hex = data.joinToString("") { "%02X".format(it) }
                            val value = PidTable.decode(pid, data)
                            addLine("= ${pid.id} raw=$hex value=${value?.let { "%.2f".format(it) }} ${pid.unit} (verified=${pid.verified})")
                            if (value != null) applyValue(pid.role, value)
                        }
                        if (!queried) delay(50)
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    if (!currentCoroutineContext().isActive) break
                    failureCount += 1
                    val delayMs = ReconnectBackoff.delayMs(failureCount)
                    val reason = when (error) {
                        is SecurityException -> "Bluetooth izni eksik"
                        is ObdTimeoutException -> "ELM327 yanıt vermiyor"
                        is ObdProtocolException -> "ELM327 komut desteği yetersiz"
                        is IOException -> "Bluetooth/OBD bağlantısı koptu"
                        else -> "OBD bağlantısı kurulamadı"
                    }
                    addLine("HATA: $reason (${error.javaClass.simpleName}: ${error.message})")
                    state.value = VehicleState(statusText = "$reason; ${delayMs / 1_000} sn sonra yeniden denenecek")
                    // Missing permission cannot be repaired by automatic retries.
                    if (error is SecurityException) return@launch
                    // Release the failed socket before waiting; no open resources during backoff.
                    transport.close()
                    if (activeTransport === transport) activeTransport = null
                    delay(delayMs)
                } finally {
                    transport.close()
                    if (activeTransport === transport) activeTransport = null
                }
            }
            if (job === currentCoroutineContext()[Job]) {
                state.value = VehicleState(statusText = "Bağlı değil")
                addLine("OBD oturumu sonlandırıldı")
            }
        }
    }

    private fun applyValue(role: String, value: Double) {
        state.update {
            when (role) {
                "soc" -> it.copy(socPct = value)
                "volt" -> it.copy(hvVolt = value)
                "amp" -> it.copy(hvAmp = value)
                "speed" -> it.copy(speedKmh = value)
                else -> it
            }
        }
    }

    fun startDemo() {
        stop()
        // Mark demo immediately so a pending Service.onDestroy cannot cancel it.
        state.value = VehicleState(demo = true, statusText = "Demo başlatılıyor")
        job = scope.launch(Dispatchers.IO) {
            var time = 0.0
            var soc = 72.0
            try {
                while (currentCoroutineContext().isActive) {
                    val speed = 60.0 + 55.0 * sin(time / 6.0)
                    val amp = 80.0 * sin(time / 6.0 + 0.6)
                    soc = (soc - 0.01).coerceAtLeast(5.0)
                    state.value = VehicleState(
                        connected = true, demo = true,
                        speedKmh = speed.coerceAtLeast(0.0), socPct = soc,
                        hvVolt = 360.0 - amp * 0.05, hvAmp = amp,
                        statusText = "Demo — gerçek araç verisi değil",
                    )
                    time += 0.1
                    delay(100)
                }
            } finally {
                if (job === currentCoroutineContext()[Job]) state.value = VehicleState()
            }
        }
    }

    fun stop() {
        job?.cancel()
        activeTransport?.close() // interrupts blocked BluetoothSocket.connect()
        activeTransport = null
        job = null
        state.value = VehicleState(statusText = "Bağlantı kesildi")
    }
}
