package app.evcockpit.charging.tariffs

import app.evcockpit.planner.charging.PreferredNetworks
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Fiyatı olmayan ağlarda null kalır; 0 TL ücretsiz şarj anlamına GELMEZ.
 * AC/DC aynı şey değildir; bilinmeyen soket sınıfında fiyat tahmini yapılmaz.
 */
enum class ChargingKind { AC, DC, UNKNOWN }
data class OperatorTariff(
    val id: String,
    val operator: String,
    val aliases: List<String>,
    val acTlKwh: Double?,
    val dcTlKwh: Double?,
    val updatedAt: LocalDate?,
    val sourceUrl: String?,
) {
    fun price(kind: ChargingKind): Double? = when (kind) {
        ChargingKind.AC -> acTlKwh
        ChargingKind.DC -> dcTlKwh
        ChargingKind.UNKNOWN -> null
    }
}
data class TariffCatalog(val operators: List<OperatorTariff>) {
    fun operator(raw: String): OperatorTariff? {
        val normalized = PreferredNetworks.normalize(raw)
        if (normalized.isBlank() || normalized == "operator bilinmiyor") return null
        return operators.firstOrNull { entry ->
            (entry.aliases + entry.operator).any { alias ->
                val tag = PreferredNetworks.normalize(alias)
                tag.isNotBlank() &&
                    (normalized == tag || normalized.startsWith("$tag ") ||
                        normalized.endsWith(" $tag"))
            }
        }
    }
}

object TariffCatalogCodec {
    private const val MAX_JSON_CHARS = 80_000
    private val safeId = Regex("[a-z0-9-]{2,50}")
    fun parse(raw: String): TariffCatalog {
        require(raw.length in 20..MAX_JSON_CHARS) { "Tarife veri boyutu geçersiz." }
        val json = JSONObject(raw)
        require(json.getInt("schema_version") == 1) { "Tarife şema sürümü bilinmiyor." }
        val entries = json.getJSONArray("operators")
        require(entries.length() in 1..100) { "Operatör listesi boyutu geçersiz." }
        val items = (0 until entries.length()).map { i ->
            val entry = entries.getJSONObject(i)
            val aliasesJson = entry.optJSONArray("aliases") ?: JSONArray()
            val id = entry.getString("id")
            require(id.matches(safeId)) { "Operatör kimliği geçersiz." }
            val operator = entry.getString("operator").trim().take(100)
            require(operator.isNotEmpty())
            val aliases = (0 until minOf(aliasesJson.length(), 10)).map {
                aliasesJson.getString(it).trim().take(90)
            }.filter { it.isNotBlank() }
            fun numeric(field: String): Double? {
                if (!entry.has(field) || entry.isNull(field)) return null
                val number = entry.optDouble(field, Double.NaN)
                require(number.isFinite() && number in 0.01..150.0) {
                    "Tarife fiyatı geçersiz: $id / $field"
                }
                return number
            }
            val dateRaw = if (entry.isNull("updated_at")) ""
                else entry.optString("updated_at", "")
            val date = dateRaw.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
            val source = if (entry.isNull("source_url")) null
                else entry.optString("source_url", "").takeIf {
                    it.startsWith("https://") && it.length <= 350
                }
            val ac = numeric("ac_tl_kwh")
            val dc = numeric("dc_tl_kwh")
            // Fiyat varsa yayımlanmış kaynak ve tarih zorunludur.
            require((ac == null && dc == null) || (date != null && source != null)) {
                "Fiyat için kaynak URL ve güncelleme tarihi zorunludur: $id"
            }
            OperatorTariff(id, operator, aliases, ac, dc, date, source)
        }
        require(items.distinctBy { it.id }.size == items.size) { "Çift operatör kimliği." }
        return TariffCatalog(items)
    }

    fun isStale(tariff: OperatorTariff, now: LocalDate = LocalDate.now()): Boolean {
        val day = tariff.updatedAt ?: return true
        val age = ChronoUnit.DAYS.between(day, now)
        return age < 0 || age > 14
    }
}
