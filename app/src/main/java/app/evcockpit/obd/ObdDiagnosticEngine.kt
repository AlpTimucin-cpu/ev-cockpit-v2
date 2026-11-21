package app.evcockpit.obd

import java.util.Locale

/** The profile is a communication path, NOT a verified BMS/PID scale. */
enum class DiagnosticProfile {
    NONE, STANDARD_SPEED, MEB_FULL_HEADER, MEB_SHORT_HEADER, MEB_SHORT_PADDED,
}

data class DiagnosticStep(
    val group: String,
    val command: String,
    val raw: String,
    val durationMs: Long,
    val timedOut: Boolean = false,
)

data class DiagnosticOutcome(
    val steps: List<DiagnosticStep>,
    val monitorFrames: Int,
    val monitorIds: Set<String>,
    val standardSupported: Boolean,
    val standardSpeedSupported: Boolean,
    val mebFullResponded: Boolean,
    val mebShortResponded: Boolean,
    val mebShortPaddedResponded: Boolean,
    val meb11BitResponded: Boolean,
    val proposedProfile: DiagnosticProfile,
    val summary: String,
)

/**
 * READ-ONLY diagnostic probes; there is no raw PID scaling or verified=true in this class.
 * A positive service+DID is required; OK or ELM firmware identification is NOT ECU data.
 */
object DiagnosticEvidence {
    private fun hexOnly(raw: String): String = raw.uppercase(Locale.US)
        .replace(Regex("[^0-9A-F]"), "")

    fun hasMode01(raw: String, pid: String): Boolean =
        hexOnly(raw).contains("41" + pid.uppercase(Locale.US)) &&
            !raw.uppercase(Locale.US).contains("NO DATA")

    fun hasUds(raw: String, did: String): Boolean =
        hexOnly(raw).contains("62" + did.uppercase(Locale.US)) &&
            !raw.uppercase(Locale.US).contains("NO DATA")

    fun monitorIdentifiers(raw: String): List<String> = raw.uppercase(Locale.US).lines()
        .mapNotNull { line ->
            val id = Regex("^\\s*([0-9A-F]{8}|[0-9A-F]{3})\\s+[0-9A-F]{2}(?:\\s|$)")
                .find(line)?.groupValues?.get(1)
            id
        }

    fun propose(
        standardSpeed: Boolean, full: Boolean, short: Boolean, padded: Boolean = false,
    ): DiagnosticProfile =
        when {
            full -> DiagnosticProfile.MEB_FULL_HEADER
            short -> DiagnosticProfile.MEB_SHORT_HEADER
            padded -> DiagnosticProfile.MEB_SHORT_PADDED
            standardSpeed -> DiagnosticProfile.STANDARD_SPEED
            else -> DiagnosticProfile.NONE
        }
}

/**
 * One tap = sequential bounded capture. The adapter's own '> prompt' is required
 * for every command; after a timeout one raw CR is attempted, and the session
 * is abandoned if prompt recovery fails (the owner closes and reconnects).
 */
class ObdDiagnosticEngine(
    private val io: ObdTransport,
    private val line: (String) -> Unit,
) {
    private val steps = mutableListOf<DiagnosticStep>()
    private var group = "ELM"
    private var monitorRaw = ""
    private var error: String? = null

    private fun record(command: String, raw: String, elapsedMs: Long, timedOut: Boolean = false) {
        // VIN Mode 09 PID 02 and passive CAN payloads stay IN MEMORY ONLY.
        // This diagnostic is shareable; never persist those raw bytes.
        val value = when {
            command == "0902" ->
                if (raw.contains("NO DATA", ignoreCase = true)) "NO DATA (VIN içeriği yazılmaz)"
                else "[KİMLİK YANITI GİZLENDİ]"
            command.startsWith("ATMA") ->
                "Ham CAN veri gizlendi; okunabilir çerçeve: " +
                    DiagnosticEvidence.monitorIdentifiers(raw).size
            else -> SafeTestReportText.sanitizeLine(
                raw.replace("\r", "\\r").replace("\n", " | ")
            )
        }
        steps += DiagnosticStep(group, command, value, elapsedMs, timedOut)
        line("[" + group + "] " + command + " (" + elapsedMs + " ms) => " + value)
    }

    private suspend fun cmd(command: String, timeoutMs: Long = 3_500): String {
        val start = System.nanoTime()
        try {
            io.write(command)
            val raw = io.readUntilPrompt(timeoutMs)
            record(command, raw, (System.nanoTime() - start) / 1_000_000L)
            return raw
        } catch (failure: ObdTimeoutException) {
            record(command, "TIMEOUT", (System.nanoTime() - start) / 1_000_000L, true)
            try {
                io.sendBreak()
                val aligned = io.readUntilPrompt(1_500)
                line("[RECOVERY] CR => ELM istemi geri alındı; ham veri gizli")
            } catch (_: Exception) {
                throw ObdTimeoutException("ELM istem hizalanamadi; yeniden baglan")
            }
            throw ObdTimeoutException("ELM komutu zaman asimi; tanı güvenli şekilde durduruldu")
        }
    }

    private suspend fun mode29(full: Boolean): Pair<String, Boolean> {
        group = if (full) "MEB 29-bit / 8 hane" else "MEB 29-bit / ATCP17"
        cmd("ATSP7")
        cmd("ATH1")
        cmd("ATCAF1")
        cmd("ATCRA") // clear inherited filters before next ECU selection
        if (full) cmd("ATSH17FC007B") else {
            cmd("ATCP17")
            cmd("ATSHFC007B")
        }
        cmd("ATCRA17FE007B")
        val reply = cmd("22028C")
        if (!full) {
            // ABRP MEB.json documents an explicit padded frame with CAF0.
            cmd("ATCAF0")
            cmd("ATFCSH17FC007B")
            cmd("ATFCSD300000")
            cmd("ATFCSM1")
            val padded = cmd("0322028C55555555")
            if (DiagnosticEvidence.hasUds(padded, "028C")) return Pair(padded, true)
        }
        return Pair(reply, false)
    }

    suspend fun run(): DiagnosticOutcome {
        var standard = false
        var speed = false
        var full = false
        var short = false
        var shortPadded = false
        var eleven = false
        try {
            group = "A ELM tanimi"
            for (command in listOf("ATZ", "ATI", "ATRV", "ATDP", "ATDPN")) {
                cmd(command, if (command == "ATZ") 5_000 else 3_500)
            }

            group = "B standart OBD-II otomatik"
            cmd("ATSP0")
            val pid00 = cmd("0100", 6_000)
            standard = DiagnosticEvidence.hasMode01(pid00, "00")
            cmd("0120")
            cmd("0140")
            cmd("0902", 6_000) // VIN may appear in raw diagnostic export: share deliberately.
            speed = DiagnosticEvidence.hasMode01(cmd("010D"), "0D")

            group = "C standart CAN 11-bit"
            cmd("ATSP6")
            val can11 = cmd("0100", 5_000)
            standard = standard || DiagnosticEvidence.hasMode01(can11, "00")

            group = "D pasif 29-bit CAN"
            cmd("ATSP7")
            cmd("ATH1")
            cmd("ATCRA")
            val start = System.nanoTime()
            try {
                monitorRaw = io.monitorFor(3_000)
                record("ATMA / 3000ms / CR", monitorRaw, (System.nanoTime() - start) / 1_000_000L)
            } catch (failure: ObdTimeoutException) {
                record("ATMA / 3000ms / CR", "TIMEOUT / reconnect required",
                    (System.nanoTime() - start) / 1_000_000L, true)
                throw failure
            }

            val fullResult = mode29(true)
            full = DiagnosticEvidence.hasUds(fullResult.first, "028C")
            val shortResult = mode29(false)
            shortPadded = shortResult.second
            short = !shortPadded && DiagnosticEvidence.hasUds(shortResult.first, "028C")

            group = "E 29-bit vites ECU / evDash"
            cmd("ATSP7")
            cmd("ATCRA")
            cmd("ATSH17FC0076")
            cmd("ATCRA17FE0076")
            cmd("22210E")

            group = "F 11-bit MEB / evDash/CellPulse"
            cmd("ATSP6")
            cmd("ATCRA")
            cmd("ATSH710")
            cmd("ATCRA77A")
            eleven = DiagnosticEvidence.hasUds(cmd("222AB2"), "2AB2")
            cmd("ATCRA")
            cmd("ATSH7E4")
            val other = cmd("220105")
            eleven = eleven || DiagnosticEvidence.hasUds(other, "0105")
        } catch (ex: Exception) {
            error = ex.javaClass.simpleName + ": " + (ex.message ?: "hata")
            line("TANI DURDU (sonuçlar eksik): " + error)
        }
        val ids = DiagnosticEvidence.monitorIdentifiers(monitorRaw)
        val profile = DiagnosticEvidence.propose(speed, full, short, shortPadded)
        val diagnosis = buildString {
            append("Standart 0100: ").append(if (standard) "yanıt var" else "doğrulanmadı")
            append("; 010D: ").append(if (speed) "yanıt var" else "doğrulanmadı")
            append("; MEB 8-hane: ").append(if (full) "UDS pozitif" else "doğrulanmadı")
            append("; MEB ATCP17: ").append(if (short) "UDS pozitif" else "doğrulanmadı")
            append("; MEB ATCP17/CAF0: ").append(if (shortPadded) "UDS pozitif" else "doğrulanmadı")
            append("; 11-bit MEB: ").append(if (eleven) "UDS pozitif" else "doğrulanmadı")
            append("; ATMA: ").append(ids.size).append(" okunabilen çerçeve")
            append("; öneri: ").append(profile.name)
            if (error != null) append("; EKSİK TANILAMA: ").append(error)
            append(". Ready durumu uygulama tarafından doğrulanamaz.")
        }
        line("TANI ÖZETİ: " + diagnosis)
        return DiagnosticOutcome(
            steps.toList(), ids.size, ids.toSet(), standard, speed,
            full, short, shortPadded, eleven, profile, diagnosis,
        )
    }
}
