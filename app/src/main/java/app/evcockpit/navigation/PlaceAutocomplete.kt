package app.evcockpit.navigation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Place names/cities: Open-Meteo Geocoding; streets/POI: fair-use Photon.
 * Neither provider is a guaranteed navigation-grade address index.
 */
enum class PlaceField { ORIGIN, DESTINATION }

object AutocompletePolicy {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 120
    const val DEBOUNCE_MS = 750L
    const val CITY_REQUEST_GAP_MS = 2_500L
    const val CACHE_MAX = 16

    fun eligible(value: String): Boolean =
        value.trim().length in MIN_LENGTH..MAX_LENGTH

    fun normalized(value: String): String = value.trim().lowercase(
        java.util.Locale.forLanguageTag("tr-TR")
    )
}

/** HTTP requests are restricted to a single fixed HTTPS host, not a user-supplied URL. */
class OpenMeteoCityGeocoder {
    suspend fun search(query: String): List<PhotonPlace> = withContext(Dispatchers.IO) {
        require(AutocompletePolicy.eligible(query))
        val name = URLEncoder.encode(query.trim(), "UTF-8")
        val conn = URL(
            "https://geocoding-api.open-meteo.com/v1/search?name=$name&count=8&language=tr&format=json"
        ).openConnection() as HttpURLConnection
        conn.connectTimeout = 6_000
        conn.readTimeout = 6_000
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("User-Agent", "ID4-Yol-Arkadasi/0.3 (personal-use)")
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext emptyList()
            val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(96_000)
                val count = reader.read(buffer)
                if (count < 1) "" else String(buffer, 0, count)
            }
            OpenMeteoCityParser.parse(raw)
        } finally {
            conn.disconnect()
        }
    }
}

object OpenMeteoCityParser {
    fun parse(raw: String): List<PhotonPlace> = runCatching {
        val cities = JSONObject(raw).optJSONArray("results") ?: return@runCatching emptyList()
        buildList {
            for (i in 0 until minOf(8, cities.length())) {
                val item = cities.optJSONObject(i) ?: continue
                val lat = item.optDouble("latitude", Double.NaN)
                val lon = item.optDouble("longitude", Double.NaN)
                val point = runCatching { GeoPoint(lat, lon) }.getOrNull() ?: continue
                val label = listOf(
                    item.optString("name"),
                    item.optString("admin1"),
                    item.optString("country"),
                ).filter { it.isNotBlank() && it != "null" }.distinct()
                    .joinToString(", ").take(140)
                if (label.isNotBlank()) add(PhotonPlace(label, point))
            }
        }
    }.getOrDefault(emptyList())
}

interface PlaceSuggestionSource {
    suspend fun suggest(query: String): List<PhotonPlace>
}

/**
 * A single-query cache and conservative server-side pacing. Never uses public
 * Nominatim's autocomplete-prohibited endpoint.
 */
class PublicPlaceSuggestionSource(
    private val cities: OpenMeteoCityGeocoder = OpenMeteoCityGeocoder(),
    private val photon: PhotonGeocoder = PhotonGeocoder(),
) : PlaceSuggestionSource {
    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, List<PhotonPlace>>()
    private var lastCityAtMs = -AutocompletePolicy.CITY_REQUEST_GAP_MS
    private var lastPhotonAtMs = -PhotonPolicy.MIN_INTERVAL_MS

    override suspend fun suggest(query: String): List<PhotonPlace> {
        if (!AutocompletePolicy.eligible(query)) return emptyList()
        val key = AutocompletePolicy.normalized(query)
        return mutex.withLock {
            cache[key]?.let { return@withLock it }
            val now = SystemClock.elapsedRealtime()
            val wait = (AutocompletePolicy.CITY_REQUEST_GAP_MS -
                (now - lastCityAtMs)).coerceAtLeast(0)
            if (wait > 0) delay(wait)
            lastCityAtMs = SystemClock.elapsedRealtime()

            // Photon is a public fair-use demo. Keep its original 10-second budget,
            // while city suggestions remain available at a slower, bounded cadence.
            val address = if (lastCityAtMs - lastPhotonAtMs >= PhotonPolicy.MIN_INTERVAL_MS) {
                lastPhotonAtMs = lastCityAtMs
                safeFetch { photon.search(query) }
            } else emptyList()
            val places = safeFetch { cities.search(query) }
            val combined = (address + places).distinctBy {
                it.label + "|" + it.point.latitude + "|" + it.point.longitude
            }.take(10)
            cache[key] = combined
            if (cache.size > AutocompletePolicy.CACHE_MAX) {
                cache.remove(cache.keys.first())
            }
            combined
        }
    }

    private suspend fun safeFetch(block: suspend () -> List<PhotonPlace>): List<PhotonPlace> =
        try { block() } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            emptyList()
        }
}

data class PlaceSuggestionState(
    val originText: String = "",
    val destinationText: String = "",
    val originResults: List<PhotonPlace> = emptyList(),
    val destinationResults: List<PhotonPlace> = emptyList(),
    val originStatus: String = "GPS konumunu kullanabilir veya başlangıç yazabilirsin.",
    val destinationStatus: String = "Şehir, semt veya adresin ilk 3 harfini yaz.",
    val searchingOrigin: Boolean = false,
    val searchingDestination: Boolean = false,
)

/** UI only reads StateFlow, no network calls within a Composable. */
class PlaceAutocompleteViewModel(
    private val source: PlaceSuggestionSource = PublicPlaceSuggestionSource(),
) : ViewModel() {
    private val mutable = MutableStateFlow(PlaceSuggestionState())
    val state = mutable.asStateFlow()
    private var originJob: Job? = null
    private var destinationJob: Job? = null

    fun onTextChanged(field: PlaceField, raw: String) {
        val text = raw.take(AutocompletePolicy.MAX_LENGTH)
        if (field == PlaceField.ORIGIN) {
            originJob?.cancel()
            mutable.value = mutable.value.copy(
                originText = text, originResults = emptyList(), searchingOrigin = false,
                originStatus = if (AutocompletePolicy.eligible(text))
                    "Adres önerileri aranacak…" else "Başlangıç için en az 3 harf yaz.",
            )
        } else {
            destinationJob?.cancel()
            mutable.value = mutable.value.copy(
                destinationText = text, destinationResults = emptyList(),
                searchingDestination = false,
                destinationStatus = if (AutocompletePolicy.eligible(text))
                    "Adres önerileri aranacak…" else "Hedef için en az 3 harf yaz.",
            )
        }
        if (!AutocompletePolicy.eligible(text)) return
        val job = viewModelScope.launch {
            delay(AutocompletePolicy.DEBOUNCE_MS)
            markLoading(field)
            try {
                val found = source.suggest(text)
                setResults(field, found, if (found.isEmpty())
                    "Öneri bulunamadı veya ücretsiz arama servisi sınırlı. Biraz sonra yeniden yaz."
                else "Öneriler • Open-Meteo şehir / Photon adres. Haritada doğrula.")
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                setResults(field, emptyList(),
                    "Adres önerileri alınamadı. İnterneti kontrol edip tekrar yaz.")
            }
        }
        if (field == PlaceField.ORIGIN) originJob = job else destinationJob = job
    }

    fun selected(field: PlaceField, place: PhotonPlace) {
        if (field == PlaceField.ORIGIN) {
            originJob?.cancel()
            mutable.value = mutable.value.copy(
                originText = place.label, originResults = emptyList(), searchingOrigin = false,
                originStatus = "Başlangıç seçildi • haritada doğrula.",
            )
        } else {
            destinationJob?.cancel()
            mutable.value = mutable.value.copy(
                destinationText = place.label, destinationResults = emptyList(),
                searchingDestination = false,
                destinationStatus = "Hedef seçildi • haritada doğrula.",
            )
        }
    }

    fun useGpsOrigin() {
        originJob?.cancel()
        mutable.value = mutable.value.copy(
            originText = "", originResults = emptyList(), searchingOrigin = false,
            originStatus = "Başlangıç: GPS (gerçek konum yoksa rota hesaplanmaz)",
        )
    }

    private fun markLoading(field: PlaceField) {
        mutable.value = if (field == PlaceField.ORIGIN)
            mutable.value.copy(searchingOrigin = true)
        else mutable.value.copy(searchingDestination = true)
    }

    private fun setResults(field: PlaceField, places: List<PhotonPlace>, status: String) {
        mutable.value = if (field == PlaceField.ORIGIN)
            mutable.value.copy(originResults = places, originStatus = status,
                searchingOrigin = false)
        else mutable.value.copy(destinationResults = places, destinationStatus = status,
            searchingDestination = false)
    }
}
