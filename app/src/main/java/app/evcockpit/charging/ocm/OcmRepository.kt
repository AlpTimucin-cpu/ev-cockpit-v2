package app.evcockpit.charging.ocm

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.RouteKeyCipher
import app.evcockpit.planner.data.BoundedResponseReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

private val Context.ocmSettings by preferencesDataStore("ocm_api_key_v1")

/** OCM anahtarı kaynak dosyalara veya sürüm/build ayarlarına eklenmez. */
class OcmKeyStore(context: Context) {
    private val app = context.applicationContext
    private val field = stringPreferencesKey("ciphertext")
    val hasKey = app.ocmSettings.data.map { !it[field].isNullOrEmpty() }

    suspend fun read(): String? {
        val encrypted = app.ocmSettings.data.first()[field] ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { RouteKeyCipher.decrypt(encrypted) }.getOrNull()
        }
    }

    suspend fun save(raw: String) {
        val value = raw.trim()
        require(value.length in 8..256 && value.matches(Regex("[A-Za-z0-9_-]+"))) {
            "Open Charge Map anahtar biçimi geçersiz."
        }
        val encrypted = withContext(Dispatchers.IO) { RouteKeyCipher.encrypt(value) }
        app.ocmSettings.edit { it[field] = encrypted }
    }

    suspend fun clear() { app.ocmSettings.edit { it.remove(field) } }
}

class OcmAccessException(message: String) : Exception(message)
class OcmRateLimitException(message: String) : Exception(message)

interface OcmTransport {
    suspend fun nearby(point: GeoPoint, apiKey: String, radiusKm: Double): List<OcmStation>
}

/** Genel OCM API için kullanıcı anahtarlı ve sabit HTTPS hostlu istemci. */
class OcmHttpTransport : OcmTransport {
    override suspend fun nearby(
        point: GeoPoint, apiKey: String, radiusKm: Double,
    ): List<OcmStation> = withContext(Dispatchers.IO) {
        require(radiusKm in 1.0..15.0)
        val lat = String.format(Locale.US, "%.6f", point.latitude)
        val lon = String.format(Locale.US, "%.6f", point.longitude)
        val distance = String.format(Locale.US, "%.1f", radiusKm)
        val url = URL("https://api.openchargemap.io/v3/poi/?" +
            "output=json&countrycode=TR&latitude=$lat&longitude=$lon" +
            "&distance=$distance&distanceunit=KM&maxresults=100&compact=false&verbose=false" +
            "&client=ev-cockpit-v2")
        val conn = url.openConnection() as HttpsURLConnection
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("X-API-Key", apiKey)
        conn.setRequestProperty("User-Agent",
            "EV-Cockpit-v2/0.5 (+https://github.com/AlpTimucin-cpu/ev-cockpit-v2)")
        try {
            when (val status = conn.responseCode) {
                200 -> {
                    val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use {
                        BoundedResponseReader.read(it, 1_200_000)
                    }
                    OcmParser.parse(raw)
                }
                401, 403 -> throw OcmAccessException("OCM anahtarı reddedildi; hesabındaki anahtarı kontrol et.")
                429 -> throw OcmRateLimitException("Open Charge Map istek sınırı aşıldı.")
                else -> error("OCM hizmetine erişilemiyor (HTTP $status).")
            }
        } finally { conn.disconnect() }
    }
}

data class OcmSearchResult(
    val stations: List<OcmStation>,
    val message: String,
    val complete: Boolean,
    val queriedCenters: Int,
)

class OcmRepository(
    private val transport: OcmTransport = OcmHttpTransport(),
    private val delayCall: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun search(geometry: List<GeoPoint>, key: String): OcmSearchResult {
        require(key.isNotBlank()) { "OCM API anahtarı yok." }
        val sampling = OcmCorridor.sample(geometry)
        val all = linkedMapOf<Long, OcmStation>()
        var failures = 0
        var truncated = false
        var blocked: String? = null
        for ((index, p) in sampling.points.withIndex()) {
            if (index > 0) delayCall(1_200L)
            try {
                val received = transport.nearby(p, key, OcmCorridor.RADIUS_KM)
                if (received.size >= 100) truncated = true
                received.forEach { item ->
                    val projected = OcmCorridor.project(item, geometry) ?: return@forEach
                    all[item.id] = projected
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (blockedApi: OcmAccessException) {
                failures++
                blocked = "OCM anahtarı geçersiz; devam eden istekler durduruldu."
                break
            } catch (rateLimit: OcmRateLimitException) {
                failures++
                blocked = "OCM kota sınırı; diğer istekler iptal edildi."
                break
            } catch (_: Exception) {
                // Diğer sorgular başarısız olsa da başarılı kısımları göster.
                failures++
            }
        }
        val complete = failures == 0 && !sampling.incomplete && !truncated
        val status = when {
            blocked != null -> blocked
            all.isEmpty() && failures > 0 ->
                "Şarj verisi alınamadı. API anahtarı, kota ve interneti kontrol et."
            !complete ->
                "KISMİ VERİ: ${sampling.points.size} bölge sorgulandı; " +
                    "$failures hata. Tüm istasyonların bulunduğu söylenemez."
            else ->
                "${sampling.points.size} bölgede OCM envanter taraması tamamlandı. " +
                    "Kapsama ve güncellik garanti değil."
        }
        return OcmSearchResult(
            stations = all.values.sortedBy { it.alongKm },
            message = status,
            complete = complete,
            queriedCenters = sampling.points.size,
        )
    }
}
