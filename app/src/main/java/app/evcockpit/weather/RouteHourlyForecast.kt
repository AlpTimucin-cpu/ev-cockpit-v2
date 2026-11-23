package app.evcockpit.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Real forecast is sampled at *expected* route times, not current weather. */
data class RouteWeatherSample(
    val label: String,
    val location: GeoPoint,
    val expectedEpochSec: Long,
)

data class RouteHourlyWeather(
    val label: String,
    val expectedEpochSec: Long,
    val temperatureC: Double,
    val weatherCode: Int?,
    val windKmh: Double?,
    val rainProbabilityPct: Int?,
)

object RouteHourlyForecastPolicy {
    const val MAX_POINTS = 3
    const val MAX_RESPONSE_CHARS = 250_000
    const val MAX_HOURLY_GAP_SECONDS = 90L * 60
    private val zone = ZoneId.of("Europe/Istanbul")
    private val hourFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("tr-TR"))

    fun sample(
        route: NavigationRoute,
        departureEpochSec: Long,
    ): List<RouteWeatherSample> {
        if (route.geometry.isEmpty() || !route.durationSeconds.isFinite() ||
            route.durationSeconds <= 0.0) return emptyList()
        val geo = route.geometry
        val indexes = when (geo.size) {
            1 -> listOf(0)
            2 -> listOf(0, 1)
            else -> listOf(0, geo.size / 2, geo.lastIndex)
        }
        val labels = when (indexes.size) {
            1 -> listOf("Hedef")
            2 -> listOf("Başlangıç", "Hedef")
            else -> listOf("Başlangıç", "Yol ortası", "Hedef")
        }
        return indexes.mapIndexed { index, geometryIndex ->
            val fraction = if (geo.size < 2) 0.0 else {
                // Route geometry points are not necessarily spaced evenly;
                // this is only an ETA approximation, not an observed passage time.
                geometryIndex.toDouble() / geo.lastIndex
            }
            RouteWeatherSample(
                labels[index],
                geo[geometryIndex],
                departureEpochSec + (route.durationSeconds * fraction).toLong(),
            )
        }
    }

    fun parse(json: String, samples: List<RouteWeatherSample>): List<RouteHourlyWeather> =
        runCatching {
            if (samples.isEmpty()) return@runCatching emptyList()
            val raw = json.trim()
            val responseObjects = if (raw.startsWith("[")) {
                val array = JSONArray(raw)
                (0 until minOf(array.length(), MAX_POINTS)).map { array.getJSONObject(it) }
            } else listOf(JSONObject(raw))
            buildList {
                responseObjects.take(samples.size).forEachIndexed { sampleIndex, root ->
                    val sample = samples[sampleIndex]
                    val hourly = root.optJSONObject("hourly") ?: return@forEachIndexed
                    val times = hourly.optJSONArray("time") ?: return@forEachIndexed
                    val temperatures = hourly.optJSONArray("temperature_2m")
                        ?: return@forEachIndexed
                    var chosen = -1
                    var delta = Long.MAX_VALUE
                    for (index in 0 until minOf(times.length(), temperatures.length())) {
                        val epoch = times.optLong(index, Long.MIN_VALUE)
                        if (epoch == Long.MIN_VALUE) continue
                        val difference = abs(epoch - sample.expectedEpochSec)
                        if (difference < delta) {
                            chosen = index
                            delta = difference
                        }
                    }
                    if (chosen < 0 || delta > MAX_HOURLY_GAP_SECONDS)
                        return@forEachIndexed
                    val temperature = temperatures.optDouble(chosen, Double.NaN)
                    if (!temperature.isFinite()) return@forEachIndexed
                    val wind = hourly.optJSONArray("wind_speed_10m")
                        ?.optDouble(chosen, Double.NaN)
                        ?.takeIf { it.isFinite() && it >= 0.0 }
                    val rain = hourly.optJSONArray("precipitation_probability")
                        ?.optInt(chosen, -1)?.takeIf { it in 0..100 }
                    val code = hourly.optJSONArray("weather_code")
                        ?.optInt(chosen, -1)?.takeIf { it >= 0 }
                    add(RouteHourlyWeather(
                        sample.label, sample.expectedEpochSec, temperature,
                        code, wind, rain,
                    ))
                }
            }
        }.getOrDefault(emptyList())

    fun summary(points: List<RouteHourlyWeather>): String {
        if (points.isEmpty()) return "Rota hava tahmini alınamadı • DOĞRULANMADI"
        return "Open-Meteo rota havası • TAHMİN: " +
            points.joinToString("  •  ") { sample ->
                val time = Instant.ofEpochSecond(sample.expectedEpochSec)
                    .atZone(zone).format(hourFormat)
                sample.label + " ~" + time + " " +
                    String.format(Locale.forLanguageTag("tr-TR"),
                        "%.0f°C", sample.temperatureC) +
                    (sample.windKmh?.let {
                        " rüzgâr ~" + String.format(Locale.ROOT, "%.0f", it) + " km/sa"
                    } ?: "") +
                    (sample.rainProbabilityPct?.let { " yağış %" + it } ?: "")
            } + " • Trafik dahil değil"
    }
}

class OpenMeteoHourlyRouteClient {
    suspend fun forecast(
        route: NavigationRoute,
        departureEpochSec: Long,
    ): List<RouteHourlyWeather> = withContext(Dispatchers.IO) {
        val samples = RouteHourlyForecastPolicy.sample(route, departureEpochSec)
        if (samples.isEmpty()) return@withContext emptyList()
        val lats = samples.joinToString(",") {
            String.format(Locale.US, "%.5f", it.location.latitude)
        }
        val lons = samples.joinToString(",") {
            String.format(Locale.US, "%.5f", it.location.longitude)
        }
        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lats" +
            "&longitude=$lons" +
            "&hourly=temperature_2m,wind_speed_10m,precipitation_probability,weather_code" +
            "&timezone=GMT&timeformat=unixtime&forecast_days=3")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("User-Agent", "ID4-Yol-Arkadasi/0.3 (personal-use)")
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK)
                return@withContext emptyList()
            val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                // We reject oversized documents instead of parsing unlimited remote data.
                val builder = StringBuilder()
                val buffer = CharArray(8_192)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    if (builder.length + count > RouteHourlyForecastPolicy.MAX_RESPONSE_CHARS)
                        return@withContext emptyList()
                    builder.append(buffer, 0, count)
                }
                builder.toString()
            }
            RouteHourlyForecastPolicy.parse(raw, samples)
        } finally {
            conn.disconnect()
        }
    }
}

/** Route updates request forecast in ViewModel, never directly from a Composable. */
class RouteHourlyWeatherViewModel : ViewModel() {
    private val client = OpenMeteoHourlyRouteClient()
    private val mutable = MutableStateFlow("Rota hava tahmini henüz alınmadı")
    val summary = mutable.asStateFlow()
    private var job: Job? = null

    fun refresh(route: NavigationRoute?) {
        job?.cancel()
        if (route == null) {
            mutable.value = "Rota hava tahmini için önce güzergâh seç"
            return
        }
        mutable.value = "Güzergâhın geçiş saatlerindeki hava tahmini aranıyor…"
        job = viewModelScope.launch {
            try {
                val result = client.forecast(route, System.currentTimeMillis() / 1000L)
                mutable.value = RouteHourlyForecastPolicy.summary(result)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                mutable.value = "Hava tahmini alınamadı; ağ veya servis erişimi yok • DOĞRULANMADI"
            }
        }
    }
}
