package app.evcockpit.charging.tariffs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.evcockpit.planner.data.BoundedResponseReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import javax.net.ssl.HttpsURLConnection

private val Context.chargeTariffPreferences by preferencesDataStore("charge_tariffs_c_v1")

data class TariffSnapshot(
    val catalog: TariffCatalog,
    val origin: String,
    val cached: Boolean,
)

/**
 * Yerel katalog her zaman yedek; ağ hatası yüzünden fiyatlar kaybolmaz.
 * GitHub'daki main sürümü kullanılır. PR aşamasında main'de dosya yoksa
 * bu dalın doğrulanabilir raw bağlantısı devreye girer.
 */
class TariffRepository(private val context: Context) {
    companion object {
        const val MAIN_URL =
            "https://raw.githubusercontent.com/AlpTimucin-cpu/ev-cockpit-v2/main/app/src/main/assets/tariffs.json"
        const val STAGING_URL =
            "https://raw.githubusercontent.com/AlpTimucin-cpu/ev-cockpit-v2/feature/stage-c-ocm-tariffs-premium-20261123/app/src/main/assets/tariffs.json"
    }
    private val prefs = context.applicationContext.chargeTariffPreferences
    private val rawField = stringPreferencesKey("remote_json")
    private val remoteTime = longPreferencesKey("remote_time")
    private val manualField = stringPreferencesKey("manual_ac_dc")
    private val maxRemoteAgeMs = 24L * 60L * 60L * 1000L

    private fun bundled(): TariffCatalog = context.assets.open("tariffs.json")
        .bufferedReader(Charsets.UTF_8).use {
            TariffCatalogCodec.parse(it.readText())
        }

    suspend fun load(forceRefresh: Boolean = false): TariffSnapshot {
        val current = prefs.data.first()
        val lastRaw = current[rawField]
        val lastTime = current[remoteTime] ?: 0L
        val age = System.currentTimeMillis() - lastTime
        val cached = lastRaw?.let {
            runCatching { TariffCatalogCodec.parse(it) }.getOrNull()
        }
        if (!forceRefresh && cached != null && age in 0..maxRemoteAgeMs) {
            return TariffSnapshot(cached, "GitHub önbelleği", true)
        }
        for (url in listOf(MAIN_URL, STAGING_URL)) {
            val received = runCatching { fetch(url) }.getOrNull() ?: continue
            val parsed = runCatching { TariffCatalogCodec.parse(received) }.getOrNull()
                ?: continue
            prefs.edit {
                it[rawField] = received
                it[remoteTime] = System.currentTimeMillis()
            }
            return TariffSnapshot(parsed, "GitHub raw", false)
        }
        return TariffSnapshot(cached ?: bundled(),
            if (cached != null) "Eski yerel önbellek" else "APK içi katalog",
            cached != null)
    }

    private suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpsURLConnection
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 8_000
        conn.readTimeout = 12_000
        conn.setRequestProperty("User-Agent",
            "EV-Cockpit-v2/0.5 (+https://github.com/AlpTimucin-cpu/ev-cockpit-v2)")
        try {
            require(conn.responseCode == 200) { "Tarife URL'si erişilemedi." }
            conn.inputStream.bufferedReader(Charsets.UTF_8).use {
                BoundedResponseReader.read(it, 80_000)
            }
        } finally { conn.disconnect() }
    }

    suspend fun manualOverrides(): Map<String, ManualPrice> {
        val raw = prefs.data.first()[manualField] ?: return emptyMap()
        return ManualPriceCodec.parse(raw)
    }

    suspend fun saveManual(id: String, kind: ChargingKind, tl: Double) {
        require(id.matches(Regex("[a-z0-9-]{2,50}")))
        require(kind != ChargingKind.UNKNOWN)
        require(tl.isFinite() && tl in 0.01..150.0)
        val current = manualOverrides()
        val old = current[id] ?: ManualPrice()
        val value = when (kind) {
            ChargingKind.AC -> old.copy(ac = tl, updatedAt = LocalDate.now().toString())
            ChargingKind.DC -> old.copy(dc = tl, updatedAt = LocalDate.now().toString())
            ChargingKind.UNKNOWN -> old
        }
        prefs.edit { it[manualField] = ManualPriceCodec.encode(current + (id to value)) }
    }

    suspend fun clearManual(id: String) {
        val current = manualOverrides()
        prefs.edit { it[manualField] = ManualPriceCodec.encode(current - id) }
    }
}

data class ManualPrice(
    val ac: Double? = null,
    val dc: Double? = null,
    val updatedAt: String = "",
)

object ManualPriceCodec {
    fun encode(prices: Map<String, ManualPrice>): String =
        JSONObject().apply {
            prices.toList().take(100).forEach { (id, value) ->
                put(id, JSONObject().apply {
                    value.ac?.let { put("ac", it) }
                    value.dc?.let { put("dc", it) }
                    put("updatedAt", value.updatedAt)
                })
            }
        }.toString()

    fun parse(raw: String): Map<String, ManualPrice> = runCatching {
        require(raw.length <= 30_000)
        val root = JSONObject(raw)
        val out = linkedMapOf<String, ManualPrice>()
        val keys = root.keys()
        while (keys.hasNext() && out.size < 100) {
            val id = keys.next()
            if (!id.matches(Regex("[a-z0-9-]{2,50}"))) continue
            val data = root.optJSONObject(id) ?: continue
            fun price(name: String): Double? {
                if (!data.has(name) || data.isNull(name)) return null
                val value = data.optDouble(name, Double.NaN)
                return value.takeIf { it.isFinite() && it in 0.01..150.0 }
            }
            val date = data.optString("updatedAt", "")
            if (runCatching { LocalDate.parse(date) }.isFailure) continue
            out[id] = ManualPrice(price("ac"), price("dc"), date)
        }
        out
    }.getOrDefault(emptyMap())
}

/** Elle girilen fiyat, operatör seçimi için yerel geçersiz kılma kaydıdır. */
data class DisplayTariff(
    val priceTlPerKwh: Double?,
    val kind: ChargingKind,
    val source: String,
    val updatedAt: String?,
    val stale: Boolean,
)

object TariffResolver {
    fun resolve(
        catalog: TariffCatalog,
        manual: Map<String, ManualPrice>,
        operator: String,
        kind: ChargingKind,
        now: LocalDate = LocalDate.now(),
    ): DisplayTariff {
        val matched = catalog.operator(operator)
        if (matched == null || kind == ChargingKind.UNKNOWN) {
            return DisplayTariff(null, kind, "Operatör veya soket bilinmiyor", null, true)
        }
        val override = manual[matched.id]
        val overridePrice = when (kind) {
            ChargingKind.AC -> override?.ac
            ChargingKind.DC -> override?.dc
            ChargingKind.UNKNOWN -> null
        }
        if (overridePrice != null) {
            val age = runCatching {
                java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(override?.updatedAt.orEmpty()), now)
            }.getOrDefault(Long.MAX_VALUE)
            return DisplayTariff(overridePrice, kind, "Manuel kullanıcı girişi",
                override?.updatedAt, age !in 0L..14L)
        }
        val price = matched.price(kind)
        return DisplayTariff(price, kind,
            if (price == null) "Fiyat bilinmiyor" else "GitHub katalog / resmî kaynak",
            matched.updatedAt?.toString(),
            TariffCatalogCodec.isStale(matched, now))
    }
}
