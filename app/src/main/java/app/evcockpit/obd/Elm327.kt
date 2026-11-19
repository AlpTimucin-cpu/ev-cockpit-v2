package app.evcockpit.obd

/** Basit ELM327 istemcisi: AT başlatma, başlık (header) yönetimi ve UDS (servis 0x22) sorguları. */
class Elm327(
    private val t: ObdTransport,
    private val onLine: (String) -> Unit = {},
) {
    private var currentHeader = ""

    suspend fun send(cmd: String, timeoutMs: Long = 2500): String {
        onLine("> $cmd")
        t.write(cmd)
        val raw = t.readUntilPrompt(timeoutMs)
        val clean = raw.replace("\r", "\n").lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("SEARCHING") }
            .joinToString("\n")
        onLine("< ${clean.replace("\n", " | ")}")
        return clean
    }

    /** true: cihaz ATI'ye yanıt verdi. Hatalı AT komutları (ucuz klonlarda '?') yok sayılır. */
    suspend fun initialize(cfg: ObdConfig): Boolean {
        send("ATZ", 4000)
        send("ATE0")
        send("ATL0")
        send("ATS0")
        send("ATH0")
        send("ATCAF1")
        val ver = send("ATI")
        send("ATSP${cfg.protocol}")
        currentHeader = ""
        setHeader(cfg.txHeader)
        return ver.isNotEmpty()
    }

    suspend fun setHeader(h: String) {
        if (h == currentHeader) return
        send("ATSH$h")
        send("ATCRA${rxFor(h)}")
        // Çok çerçeveli yanıtlar için akış kontrolü; desteklemeyen klonlarda '?' döner, sorun değil.
        send("ATFCSH$h")
        send("ATFCSD300000")
        send("ATFCSM1")
        currentHeader = h
    }

    suspend fun query(cmd: String): ByteArray? = parseUds(cmd, send(cmd, 1500))

    companion object {
        /** 17FC007B -> 17FE007B (MEB'de istek 17FC.., yanıt 17FE..) */
        fun rxFor(txHeader: String): String =
            if (txHeader.length == 8) txHeader.substring(0, 2) + "FE" + txHeader.substring(4) else txHeader

        /**
         * Olumlu yanıtı (servis+0x40 + DID) bulup sonrasındaki veri baytlarını döndürür.
         * "NO DATA", "?", "CAN ERROR" vb. için null.
         */
        fun parseUds(cmd: String, resp: String): ByteArray? {
            if (cmd.length < 6) return null
            val service = cmd.substring(0, 2).toIntOrNull(16) ?: return null
            val expected = "%02X".format(service + 0x40) + cmd.substring(2).uppercase()
            val hex = StringBuilder()
            for (line in resp.lines()) {
                val l = line.replace(" ", "").uppercase()
                if (l == "?" || l.contains("NODATA") || l.contains("ERROR") ||
                    l.contains("UNABLE") || l.contains("STOPPED") || l.contains("BUSY")
                ) return null
                hex.append(if (l.contains(':')) l.substringAfter(':') else l)
            }
            val h = hex.toString()
            val idx = h.indexOf(expected)
            if (idx < 0) return null
            var data = h.substring(idx + expected.length)
            if (data.length % 2 != 0) data = data.dropLast(1)
            if (data.isEmpty()) return null
            return data.chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
        }
    }
}
