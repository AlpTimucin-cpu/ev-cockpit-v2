package app.evcockpit.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
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
) {
    /** Pozitif: çekiş, negatif: rejenerasyon/şarj. */
    val powerKw: Double? get() = if (hvVolt != null && hvAmp != null) hvVolt * hvAmp / 1000.0 else null
}

class ObdRepository(private val ctx: Context, private val scope: CoroutineScope) {
    val state = MutableStateFlow(VehicleState())
    val log = MutableStateFlow<List<String>>(emptyList())
    val logFilePath = MutableStateFlow<String?>(null)

    private var job: Job? = null
    private var logFile: File? = null

    private fun addLine(s: String) {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "$stamp $s"
        log.update { (it + line).takeLast(400) }
        runCatching { logFile?.appendText(line + "\n") }
    }

    private fun newLogFile() {
        val name = "obd_log_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".txt"
        logFile = File(ctx.getExternalFilesDir(null), name)
        logFilePath.value = logFile?.absolutePath
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stop()
        newLogFile()
        job = scope.launch(Dispatchers.IO) {
            var transport: ObdTransport? = null
            try {
                addLine("Bağlanılıyor: ${device.name ?: device.address}")
                val cfg = PidTable.load(ctx)
                val t = BluetoothSppTransport(device)
                transport = t
                t.open()
                addLine("Bluetooth bağlandı")
                val elm = Elm327(t, ::addLine)
                val ok = elm.initialize(cfg)
                state.update { it.copy(connected = ok) }
                if (!ok) addLine("UYARI: ATI yanıtı yok, cihaz ELM327 değil olabilir")

                val lastRun = HashMap<String, Long>()
                while (isActive) {
                    val now = System.currentTimeMillis()
                    var didAny = false
                    for (pid in cfg.pids) {
                        if (now - (lastRun[pid.id] ?: 0L) < pid.everyMs) continue
                        lastRun[pid.id] = now
                        didAny = true
                        elm.setHeader(pid.header ?: cfg.txHeader)
                        val data = elm.query(pid.cmd)
                        if (data == null) {
                            addLine("! ${pid.id}: yanıt yok")
                            continue
                        }
                        val hexStr = data.joinToString("") { "%02X".format(it) }
                        val v = PidTable.decode(pid, data)
                        addLine("= ${pid.id} raw=$hexStr value=${v?.let { "%.2f".format(it) }} ${pid.unit}")
                        if (v != null) applyValue(pid.role, v)
                    }
                    if (!didAny) delay(50)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                addLine("HATA: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                transport?.close()
                state.value = VehicleState()
                addLine("Bağlantı kapandı")
            }
        }
    }

    private fun applyValue(role: String, v: Double) {
        state.update {
            when (role) {
                "soc" -> it.copy(socPct = v)
                "volt" -> it.copy(hvVolt = v)
                "amp" -> it.copy(hvAmp = v)
                "speed" -> it.copy(speedKmh = v)
                else -> it
            }
        }
    }

    /** Araç olmadan arayüzü denemek için sahte veri. */
    fun startDemo() {
        stop()
        job = scope.launch {
            var t = 0.0
            var soc = 72.0
            try {
                while (isActive) {
                    val speed = 60.0 + 55.0 * sin(t / 6.0)
                    val amp = 80.0 * sin(t / 6.0 + 0.6)
                    soc = (soc - 0.01).coerceAtLeast(5.0)
                    state.value = VehicleState(
                        connected = true, demo = true,
                        speedKmh = speed.coerceAtLeast(0.0), socPct = soc,
                        hvVolt = 360.0 - amp * 0.05, hvAmp = amp,
                    )
                    t += 0.1
                    delay(100)
                }
            } finally {
                state.value = VehicleState()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
