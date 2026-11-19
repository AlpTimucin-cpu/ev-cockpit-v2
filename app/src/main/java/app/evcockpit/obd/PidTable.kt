package app.evcockpit.obd

import android.content.Context
import org.json.JSONObject

data class Pid(
    val id: String,
    val role: String,          // soc | volt | amp | speed | log
    val cmd: String,           // örn. 22028C
    val header: String?,       // null ise ObdConfig.txHeader
    val length: Int,           // veri baytı sayısı (yanıt önekinden sonra)
    val dataOffset: Int,
    val scale: Double,
    val offset: Double,
    val signed: Boolean,
    val unit: String,
    val everyMs: Long,
    val verified: Boolean,
    val note: String,
)

data class ObdConfig(
    val protocol: String,
    val txHeader: String,
    val usableKwh: Double,
    val pids: List<Pid>,
)

object PidTable {
    fun load(ctx: Context): ObdConfig {
        val json = ctx.assets.open("pids_meb.json").bufferedReader().use { it.readText() }
        return parse(json)
    }

    fun parse(json: String): ObdConfig {
        val o = JSONObject(json)
        val arr = o.getJSONArray("pids")
        val pids = (0 until arr.length()).map { i ->
            val p = arr.getJSONObject(i)
            Pid(
                id = p.getString("id"),
                role = p.optString("role", "log"),
                cmd = p.getString("cmd").uppercase(),
                header = if (p.has("header")) p.getString("header").uppercase() else null,
                length = p.getInt("length"),
                dataOffset = p.optInt("dataOffset", 0),
                scale = p.optDouble("scale", 1.0),
                offset = p.optDouble("offset", 0.0),
                signed = p.optBoolean("signed", false),
                unit = p.optString("unit", ""),
                everyMs = p.optLong("everyMs", 1000),
                verified = p.optBoolean("verified", false),
                note = p.optString("note", ""),
            )
        }
        return ObdConfig(
            protocol = o.optString("protocol", "7"),
            txHeader = o.getString("txHeader").uppercase(),
            usableKwh = o.optDouble("usableKwh", 52.0),
            pids = pids,
        )
    }

    /** Ham baytları (büyük-endian) ölçekleyip döndürür. */
    fun decode(pid: Pid, data: ByteArray): Double? {
        if (data.size < pid.dataOffset + pid.length) return null
        var v = 0L
        for (i in 0 until pid.length) {
            v = (v shl 8) or (data[pid.dataOffset + i].toLong() and 0xFF)
        }
        if (pid.signed) {
            val bits = pid.length * 8
            if (v and (1L shl (bits - 1)) != 0L) v -= (1L shl bits)
        }
        return v * pid.scale + pid.offset
    }
}
