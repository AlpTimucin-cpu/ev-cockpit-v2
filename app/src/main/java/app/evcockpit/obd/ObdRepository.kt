package app.evcockpit.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
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
    val diagnosisFilePath = MutableStateFlow<String?>(null)
    val diagnosisSummary = MutableStateFlow("Tanı henüz yapılmadı — DOĞRULANMADI.")
    private var diagnosisFile: File? = null
    private val maxLogBytes = 1_000_000L
    private var mainTruncationNoted = false
    private var diagnosisTruncationNoted = false

    private var job: Job? = null
    @Volatile private var activeTransport: ObdTransport? = null
    @Volatile private var logFile: File? = null

    private fun addLine(value: String) {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "$stamp $value"
        log.update { (it + line).takeLast(400) }
        // Caller is on Dispatchers.IO (including demo); no disk access on main thread.
        runCatching {
            val file = logFile
            if (file != null && file.length() < maxLogBytes) {
                file.appendText(line + "\n")
            } else if (file != null && !mainTruncationNoted) {
                mainTruncationNoted = true
                // Preserve bounded output: no more repeated text once the limit is reached.
            }
        }
    }

    private fun newLogFile() {
        val name = "obd_log_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".txt"
        val directory = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        logFile = File(directory, name)
        mainTruncationNoted = false
        logFilePath.value = logFile?.absolutePath
        log.value = emptyList()
    }

    /** Every diagnostic command + raw response lives in its own capped, shareable file. */
    private fun diagnosticLine(value: String) {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = stamp + " " + value
        log.update { (it + entry).takeLast(400) }
        runCatching {
            val file = diagnosisFile ?: return@runCatching
            if (file.length() < maxLogBytes) {
                file.appendText(entry.take(13_000) + "\n")
            } else if (!diagnosisTruncationNoted) {
                diagnosisTruncationNoted = true
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun diagnose(device: BluetoothDevice) {
        stop()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val directory = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        diagnosisFile = File(directory, "obd_diagnosis_" + timestamp + ".txt")
        diagnosisFilePath.value = diagnosisFile?.absolutePath
        diagnosisTruncationNoted = false
        diagnosisSummary.value = "Tanı sürüyor... araç Ready durumu kullanıcı tarafından kontrol edilmeli."
        log.value = emptyList()
        state.value = VehicleState(statusText = "Tanı modu çalışıyor; yalnızca bilgi sorgulanıyor")
        job = scope.launch(Dispatchers.IO) {
            var last: DiagnosticOutcome? = null
            try {
                for (attempt in 1..2) {
                    if (!currentCoroutineContext().isActive) break
                    val transport = BluetoothSppTransport(device)
                    activeTransport = transport
                    try {
                        diagnosticLine("Tanı bağlantısı " + attempt + "/2; Ready durumu uygulama tarafından bilinmiyor")
                        transport.open()
                        val engine = ObdDiagnosticEngine(transport, ::diagnosticLine)
                        last = engine.run()
                        if (last.steps.any { it.timedOut } && attempt == 1) {
                            diagnosticLine("İstem/ATMA zaman aşımı; socket kapanıp bir kez yeniden bağlanılıyor.")
                            continue
                        }
                        break
                    } finally {
                        transport.close()
                        if (activeTransport === transport) activeTransport = null
                    }
                }
                if (last != null) {
                    val outcome = last
                    val stored = ObdProfileStore(ctx).saveFromDiagnostic(outcome)
                    val conclusion = if (outcome.monitorFrames == 0) {
                        "ATMA: gözlenen çerçeve yok; Ready bilinmiyorsa donanım hükmü verilemez."
                    } else if (!outcome.mebFullResponded && !outcome.mebShortResponded &&
                        !outcome.mebShortPaddedResponded) {
                        "ATMA çerçevesi var, MEB UDS pozitif değil; adres/klon sınırı OLASI, kesin değil."
                    } else {
                        "Pozitif ECU yanıtı gözlendi; PID ölçekleri yine doğrulanmadı."
                    }
                    diagnosisSummary.value = outcome.summary +
                        "\n" + conclusion +
                        "\nKaydedilen iletişim profili: " + stored.name +
                        "\nTanı logu: " + (diagnosisFilePath.value ?: "yok")
                    diagnosticLine("SONUÇ: " + diagnosisSummary.value.replace("\n", " | "))
                } else diagnosisSummary.value = "Tanı tamamlanamadı — DOĞRULANMADI."
            } catch (cancel: CancellationException) {
                diagnosisSummary.value = "Tanı kullanıcı tarafından durduruldu; sonuç eksik."
                throw cancel
            } catch (error: Exception) {
                diagnosisSummary.value = "Tanı kesildi — DOĞRULANMADI: " +
                    error.javaClass.simpleName + " (yeniden dene)"
                diagnosticLine("HATA: " + diagnosisSummary.value)
            } finally {
                activeTransport?.close()
                activeTransport = null
                state.value = VehicleState(statusText = "Tanı bitti — normal OBD okumak için yeniden bağlan")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stop()
        newLogFile()
        job = scope.launch(Dispatchers.IO) {
            var failures = 0
            val noDataCounts = linkedMapOf<String, Int>()
            val config = PidTable.load(ctx)
            while (currentCoroutineContext().isActive) {
                val transport = BluetoothSppTransport(device)
                activeTransport = transport
                try {
                    state.value = VehicleState(statusText = "OBD bağlanıyor")
                    addLine("Bluetooth bağlantısı deneniyor")
                    transport.open()
                    val profile = ObdProfileStore(ctx).read()
                    addLine("İletişim profili: " + profile.name + "; PID'ler verified=false")
                    val elm = Elm327(transport, { raw ->
                        // Continuous NO DATA/command echoes are counted instead of writing thousands of duplicates.
                        if (raw != "< NO DATA" && !raw.startsWith("> 22") &&
                            raw != "> 010D") addLine(raw)
                    })
                    elm.initialize(config, profile)
                    failures = 0
                    state.value = VehicleState(connected = true, statusText = "Bağlı; ölçümler doğrulanmadı")
                    val guard = NoResponseGuard()
                    while (currentCoroutineContext().isActive) {
                        var anyPositive = false
                        if (profile == DiagnosticProfile.STANDARD_SPEED) {
                            val kmh = elm.queryStandardSpeed()
                            if (kmh != null) {
                                anyPositive = true
                                applyValue("speed", kmh)
                            } else {
                                noDataCounts["010D"] = (noDataCounts["010D"] ?: 0) + 1
                            }
                        } else {
                            for (pid in config.pids) {
                                if (!currentCoroutineContext().isActive) break
                                elm.setHeader(pid.header ?: config.txHeader, profile)
                                val data = elm.query(pid.cmd, profile)
                                if (data == null) {
                                    noDataCounts[pid.cmd] = (noDataCounts[pid.cmd] ?: 0) + 1
                                    continue
                                }
                                anyPositive = true
                                val hex = data.joinToString("") { "%02X".format(it) }
                                val value = PidTable.decode(pid, data)
                                addLine("YANIT: " + pid.cmd + " raw=" + hex +
                                    " value=" + (value?.toString() ?: "çözümlenemedi") +
                                    " verified=" + pid.verified)
                                if (value != null) applyValue(pid.role, value)
                            }
                        }
                        if (!currentCoroutineContext().isActive) break
                        if (guard.recordFullCycle(anyPositive)) {
                            val compact = noDataCounts.entries.joinToString("; ") {
                                it.key + " NO DATA x" + it.value
                            }
                            addLine("5 ardışık tam sorgu turu yanıtsız: " + compact)
                            state.value = VehicleState(
                                connected = true,
                                statusText = "Araç yanıt vermiyor, Tanı modunu çalıştır",
                            )
                            // Maintain a live Bluetooth socket, but send NO FURTHER requests.
                            while (currentCoroutineContext().isActive) delay(60_000)
                        }
                        delay(1_000)
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    if (!currentCoroutineContext().isActive) break
                    failures++
                    val pause = ReconnectBackoff.delayMs(failures)
                    val reason = when (error) {
                        is SecurityException -> "Bluetooth izni eksik"
                        is ObdTimeoutException -> "ELM327 zaman aşımı; yeniden bağlanacak"
                        is ObdProtocolException -> "Gerekli ELM327 komutu desteklenmiyor"
                        is IOException -> "Bluetooth/OBD bağlantısı koptu"
                        else -> "OBD bağlantısı başarısız"
                    }
                    addLine("HATA: " + reason + " (" + error.javaClass.simpleName + ")")
                    state.value = VehicleState(statusText =
                        if (error is SecurityException) reason else
                            reason + " — " + pause / 1_000 + " sn sonra yeniden denenecek")
                    if (error is SecurityException) return@launch
                    transport.close()
                    if (activeTransport === transport) activeTransport = null
                    delay(pause)
                } finally {
                    transport.close()
                    if (activeTransport === transport) activeTransport = null
                }
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
