package app.evcockpit.obd

import java.io.IOException

class ObdProtocolException(message: String) : IOException(message)

/**
 * ELM327 protocol client. Unsupported optional AT commands do not abort the session.
 * Required protocol/header commands must be accepted to avoid misleading data.
 */
class Elm327(
    private val t: ObdTransport,
    private val onLine: (String) -> Unit = {},
) {
    private var currentHeader = ""
    private val unsupportedOptional = mutableSetOf<String>()

    suspend fun send(cmd: String, timeoutMs: Long = 2500): String {
        onLine("> $cmd")
        t.write(cmd)
        val raw = t.readUntilPrompt(timeoutMs)
        val clean = raw.replace("\r", "\n").lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("SEARCHING", ignoreCase = true) }
            .joinToString("\n")
        onLine("< ${clean.replace("\n", " | ")}")
        return clean
    }

    private fun rejected(answer: String): Boolean {
        val normalized = answer.uppercase()
        return normalized.isBlank() ||
            normalized.lines().any { it.trim() == "?" } ||
            listOf("ERROR", "UNABLE TO CONNECT", "BUFFER FULL", "BUS ERROR", "CAN ERROR", "STOPPED")
                .any { normalized.contains(it) }
    }

    private suspend fun required(cmd: String) {
        val answer = send(cmd)
        if (rejected(answer)) {
            throw ObdProtocolException("ELM327 gerekli komutu desteklemedi: $cmd")
        }
    }

    private suspend fun optional(cmd: String, family: String = cmd) {
        if (family in unsupportedOptional) return
        val answer = send(cmd)
        if (rejected(answer)) {
            unsupportedOptional += family
            onLine("! Opsiyonel komut desteklenmiyor: $cmd; bağlantı denenmeye devam edilecek")
        }
    }

    suspend fun initialize(cfg: ObdConfig, profile: DiagnosticProfile = DiagnosticProfile.NONE): Boolean {
        // ATI and timing features are not uniformly implemented by low-cost clones.
        optional("ATZ", "ATZ")
        optional("ATE0")
        optional("ATL0")
        optional("ATS0")
        optional("ATH0")
        optional("ATCAF1")
        optional("ATAT1")
        optional("ATST64")
        optional("ATI")
        if (profile == DiagnosticProfile.STANDARD_SPEED) {
            required("ATSP0")
            return true
        }
        required("ATSP${cfg.protocol}")
        if (profile == DiagnosticProfile.MEB_SHORT_PADDED) {
            optional("ATCAF0", "ATCAF")
        }
        currentHeader = ""
        setHeader(cfg.txHeader, profile)
        return true
    }

    suspend fun setHeader(h: String, profile: DiagnosticProfile = DiagnosticProfile.NONE) {
        if (h == currentHeader) return
        if ((profile == DiagnosticProfile.MEB_SHORT_HEADER ||
                profile == DiagnosticProfile.MEB_SHORT_PADDED) && h.length == 8) {
            required("ATCP" + h.substring(0, 2))
            required("ATSH" + h.substring(2))
        } else {
            required("ATSH$h")
        }
        optional("ATCRA${rxFor(h)}", "ATCRA")
        optional("ATFCSH$h", "ATFCSH")
        optional("ATFCSD300000", "ATFCSD")
        optional("ATFCSM1", "ATFCSM")
        currentHeader = h
    }

    suspend fun query(cmd: String, profile: DiagnosticProfile = DiagnosticProfile.NONE): ByteArray? {
        val request = if (profile == DiagnosticProfile.MEB_SHORT_PADDED && cmd.length == 6) {
            "03" + cmd + "55555555"
        } else cmd
        return parseUds(cmd, send(request, 2500))
    }

    suspend fun queryStandardSpeed(): Double? =
        parseStandardSpeed(send("010D", 3_500))

    companion object {
        /** Standard SAE J1979 Mode 01 PID 0D, 1-byte speed in km/h; no BMS data. */
        fun parseStandardSpeed(raw: String): Double? {
            if (raw.uppercase().contains("NO DATA")) return null
            val match = Regex("(?i)41\\s*0D\\s*([0-9A-F]{2})(?![0-9A-F])").find(raw)
                ?: return null
            return match.groupValues[1].toIntOrNull(16)?.toDouble()
        }

        /** Community assumption for MEB; not proven on this vehicle. */
        fun rxFor(txHeader: String): String =
            if (txHeader.length == 8) txHeader.substring(0, 2) + "FE" + txHeader.substring(4) else txHeader

        /**
         * Reads the positive UDS service + DID response. Never returns a value for
         * ELM errors, partial/garbled hexadecimal lines, or negative responses.
         * Multi-frame ISO-TP aggregation on low-cost clones remains unverified.
         */
        fun parseUds(cmd: String, resp: String): ByteArray? {
            if (cmd.length < 6 || cmd.length % 2 != 0) return null
            val service = cmd.substring(0, 2).toIntOrNull(16) ?: return null
            val positiveService = service + 0x40
            if (positiveService > 255) return null
            val expected = "%02X".format(positiveService) + cmd.substring(2).uppercase()
            val upper = resp.uppercase()
            if (listOf(
                    "NO DATA", "NODATA", "BUFFER FULL", "CAN ERROR", "BUS ERROR",
                    "UNABLE", "ERROR", "STOPPED", "BUSY", "RX ERROR",
                ).any { upper.contains(it) } ||
                upper.lines().any { it.trim() == "?" }
            ) return null

            val lines = upper.replace("\r", "\n").lines()
            val hex = buildString {
                for (raw in lines) {
                    val trimmed = raw.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("SEARCHING")) continue
                    val payload = trimmed.substringAfter(':', trimmed)
                        .replace(" ", "").replace("\t", "").replace(">", "")
                    if (payload.isNotEmpty() && payload.all { it in '0'..'9' || it in 'A'..'F' }) {
                        append(payload)
                    }
                }
            }
            val idx = hex.indexOf(expected)
            if (idx < 0) return null
            val data = hex.substring(idx + expected.length)
            if (data.isEmpty() || data.length % 2 != 0) return null
            return data.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
    }
}
