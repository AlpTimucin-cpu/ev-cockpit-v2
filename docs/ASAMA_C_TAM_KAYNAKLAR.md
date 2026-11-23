# Aşama C — değişen 12 dosyanın TAM içeriği

Bu belge `feature/stage-c-ocm-tariffs-premium-20261123` dalında Aşama C kapsamında değişen **12 dosyanın eksiksiz sürümüdür**. Hiçbir kod bölümü atlanmamıştır. Dosyalar GitHub'a zaten doğrudan uygulanmıştır; Android telefonundan yeniden yüklemen gerekmez.

- Ana dal ve Mola değiştirilmedi.
- Open Charge Map kullanıcı API anahtarı gerektirir.
- Fiyatı bilinmeyen operatörler için `null` kullanılır, hiçbir fiyat uydurulmaz.
- Yalnızca kod/CI doğrulaması yapılabilir; POCO F5 ve araç içinde fiziksel test yapılmadı.
- Şarj Planlayıcı sekmesindeki yeni arayüz, tüm uygulamanın yeniden tasarlandığı anlamına gelmez.


---

## 1. `NOTICE`

```text
# NOTICE — third-party attribution and licensing (2026-11-20)

This repository's own original source is offered under MIT (see LICENSE).
No Mola or Fermata source was added or copied in this repository's recorded
source tree. This is a source-tree observation, NOT a complete software
composition analysis of every transitive binary in the APK.

## Runtime SDK/code dependencies (direct dependencies, verified upstream)
* MapLibre Native Android: BSD-2-Clause core; Android platform also carries
  third-party licenses. https://github.com/maplibre/maplibre-native/blob/main/LICENSE.md
  Full Android license disclosures:
  https://github.com/maplibre/maplibre-native/blob/main/platform/android/LICENSE.md
* AndroidX Activity, Lifecycle, Compose, Core, DataStore, Media3 (if installed
  later): Apache License 2.0; see https://source.android.com/docs/setup/about/licenses
  and https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt
* Kotlin and kotlinx.coroutines: Apache License 2.0.
  https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt
  https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt
* JUnit 4 (test only): Eclipse Public License 1.0.
  https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt
* org.json Java test fixture: check upstream Maven POM license metadata;
  not packaged as runtime dependency.

## Online services and map data (not bundled code)
* © OpenStreetMap contributors — OpenStreetMap geodata distributed under ODbL:
  https://www.openstreetmap.org/copyright
* OpenFreeMap tile/style service: https://openfreemap.org/tos/
  Visible runtime notice: "© OpenStreetMap katkıcıları / OpenFreeMap".
* openrouteservice by HeiGIT: use provider terms and OSM attribution:
  https://giscience.github.io/openrouteservice/frequently-asked-questions.html

GPL-3.0 Mola/Fermata application source is not imported into this project.
This does NOT imply all transitive licenses are MIT, that all transitive
dependencies were fully audited, or that proprietary source code can be copied.

* Photon by komoot: OSM tabanlı kamusal demo geocoding, fair-use limits, explicit search only. https://github.com/komoot/photon

* Jetpack Media3 (Apache License 2.0): https://developer.android.com/jetpack/androidx/releases/media3 ; https://github.com/androidx/media/blob/release/LICENSE

* Open-Meteo forecast data: © Open-Meteo; data CC BY 4.0 attribution, free hosted API subject to non-commercial use and rate limits: https://open-meteo.com/en/terms

## M8 çekirdek veri kaynakları (kopyalanmış ABRP kodu yok)
- ORS resmi hosted API: https://giscience.github.io/openrouteservice/hosted-api-restrictions ; kullanıcı-anahtarlı rota/waypoint.
- Open-Meteo Elevation: https://open-meteo.com/en/docs/elevation-api ; Copernicus/Open-Meteo atıf.
- OSM/Overpass: https://dev.overpass-api.de/overpass-doc/en/preface/commons.html ; OSM katkıcıları, ODbL veri atfı.
- Araç profilindeki tüm tahmini parametreler verified=false, sayısal kanıt: docs/M8_DOGRULAMA.md.

## B aşaması koordinat verisi ve OSRM altyapısı
* `app/src/main/assets/tr_province_district_centres.txt`, AEKHer/il-ilce-koordinat
  kaynak verisinin uyarlanmış il/ilçe merkezleri listesidir. Telif:
  Copyright (c) 2026 Abdurrahman Ebrar Koçak; MIT Lisansı.
  https://github.com/AEKHer/il-ilce-koordinat
* Kaynak koordinatlar: © OpenStreetMap contributors, Open Database License
  (ODbL 1.0), https://www.openstreetmap.org/copyright
  Yerleşim merkezi koordinatları bina kapısı ya da yol erişim noktası garantisi vermez.
* OSRM güzergâh hizmeti: © OpenStreetMap contributors / FOSSGIS,
  https://routing.openstreetmap.de/about.html
  Harita verisi ve yol süresi trafiksizdir; genel örnek servis saniyede
  en fazla bir istek ve makul kişisel kullanım için sunulur.
* Photon arama: komoot, https://github.com/komoot/photon
  Genel örnek serviste yoğun kullanım kısıtlanabilir. Kullanıcı koordinatı,
  sadece geçerli konum izni ve güncel GPS varsa arama yanlılığı için gönderilir.
* Başka bir uygulamanın kapalı API'si, fiyat verisi ya da algoritması kopyalanmadı.

## C aşaması: Open Charge Map ve tarifeler
- Open Charge Map, şarj istasyonu konumu ve bağlantı türleri:
  © Open Charge Map contributors, https://openchargemap.org
  API belgesi ve kullanım koşulları: https://openchargemap.org/develop/api
  Kullanıcının kendi ücretsiz API anahtarı gerekir; anlık doluluk bu
  veri kaynağıyla doğrulanmış sayılmaz.
- İstasyonların coğrafi temeli / rota: © OpenStreetMap contributors,
  https://www.openstreetmap.org/copyright
- `app/src/main/assets/tariffs.json`: bu uygulama için hazırlanmış
  sürümlü operatör kataloğu. Başlangıçta fiyat alanları null'dır;
  canlı/resmî fiyat olduğu iddia edilmez. Kullanıcı girişi yereldir.
- GitHub raw tarife dağıtımı: GitHub kullanımı ve bant genişliği/servis
  koşulları geçerlidir. Çevrim dışı katalog verisi telefonda tutulur.
```

---

## 2. `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "app.evcockpit"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.evcockpit"
        minSdk = 28
        targetSdk = 35
        versionCode = 46
        versionName = "0.5.0-C-test"
        val ciSha = (System.getenv("GITHUB_SHA") ?: "unknown")
            .take(40).replace(Regex("[^0-9a-f]"), "")
        buildConfigField("String", "CI_COMMIT", "\"$ciSha\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // Public TEST certificate only. Never use it for production/release signing.
    System.getenv("EV_COCKPIT_PUBLIC_DEBUG_P12")?.takeIf { it.isNotBlank() }?.let { keyPath ->
        val shared = signingConfigs.create("persistentDebug") {
            storeFile = file(keyPath)
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        buildTypes.getByName("debug").signingConfig = shared
    }
}

dependencies {
    // Hilt, uygulama ve servisler arasında aynı konum deposunu paylaşır.
    implementation("com.google.dagger:hilt-android:2.57.1")
    kapt("com.google.dagger:hilt-compiler:2.57.1")
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.maplibre.gl:android-sdk:11.13.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.media3:media3-exoplayer:1.10.0")
    implementation("androidx.media3:media3-ui:1.10.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
```

---

## 3. `app/src/main/assets/tariffs.json`

```json
{
  "schema_version": 1,
  "notice": "Bilinmeyen fiyatlar null'dır. Operatör resmî fiyatı doğrulanmadan tutar girmeyin.",
  "attribution": "Operatörlerin yayımladığı tarifelerle doğrulanması gereken kullanıcı veri kataloğu; canlı fiyat API'si değildir.",
  "operators": [
    {
      "id": "zes",
      "operator": "ZES",
      "aliases": [
        "zes"
      ],
      "ac_tl_kwh": null,
      "dc_tl_kwh": null,
      "updated_at": null,
      "source_url": null
    },
    {
      "id": "esarj",
      "operator": "Eşarj",
      "aliases": [
        "esarj"
      ],
      "ac_tl_kwh": null,
      "dc_tl_kwh": null,
      "updated_at": null,
      "source_url": null
    },
    {
      "id": "trugo",
      "operator": "Trugo",
      "aliases": [
        "trugo"
      ],
      "ac_tl_kwh": null,
      "dc_tl_kwh": null,
      "updated_at": null,
      "source_url": null
    },
    {
      "id": "voltrun",
      "operator": "Voltrun",
      "aliases": [
        "voltrun"
      ],
      "ac_tl_kwh": null,
      "dc_tl_kwh": null,
      "updated_at": null,
      "source_url": null
    },
    {
      "id": "sharz",
      "operator": "Sharz",
      "aliases": [
        "sharz"
      ],
      "ac_tl_kwh": null,
      "dc_tl_kwh": null,
      "updated_at": null,
      "source_url": null
    },
    {
      "id": "tesla",
      "operator": "Tesla",
      "aliases": [
        "tesla"
      ],
      "ac_tl_kwh": null,
      "dc_tl_kwh": null,
      "updated_at": null,
      "source_url": null
    },
    {
      "id": "powersarj",
      "operator": "Powerşarj",
      "aliases": [
        "powersarj"
      ],
      "ac_tl_kwh": null,
      "dc_tl_kwh": null,
      "updated_at": null,
      "source_url": null
    },
    {
      "id": "rhg-enerturk",
      "operator": "RHG Enertürk",
      "aliases": [
        "rhg enerturk"
      ],
      "ac_tl_kwh": null,
      "dc_tl_kwh": null,
      "updated_at": null,
      "source_url": null
    },
    {
      "id": "kilovati",
      "operator": "Kilovati",
      "aliases": [
        "kilovati"
      ],
      "ac_tl_kwh": null,
      "dc_tl_kwh": null,
      "updated_at": null,
      "source_url": null
    }
  ]
}
```

---

## 4. `app/src/main/java/app/evcockpit/charging/ocm/OcmModels.kt`

```kotlin
package app.evcockpit.charging.ocm

import app.evcockpit.navigation.GeoPoint
import app.evcockpit.planner.route.RouteDistance
import app.evcockpit.planner.route.RouteSampler
import kotlin.math.ceil
import org.json.JSONArray
import org.json.JSONObject

/**
 * Open Charge Map konum envanteri; cihazın canlı boşluk bilgisini temsil etmez.
 * Güç veya soket tanımlanmamışsa 0 kW türetilmez, bilinmiyor olarak korunur.
 */
data class OcmStation(
    val id: Long,
    val name: String,
    val operator: String,
    val point: GeoPoint,
    val powerKw: Double?,
    val hasAc: Boolean,
    val hasDc: Boolean,
    val hasCcs2: Boolean,
    val connectors: List<String>,
    val access: String,
    val reportedStatus: String?,
    val alongKm: Double = 0.0,
    val awayKm: Double = 0.0,
) {
    val powerGroup: String get() = when {
        powerKw == null -> "GÜÇ BİLİNMİYOR"
        powerKw >= 150.0 -> "ULTRA DC"
        hasDc || powerKw >= 43.0 -> "DC HIZLI"
        hasAc -> "AC"
        else -> "TÜR BİLİNMİYOR"
    }
}

object OcmParser {
    fun parse(raw: String): List<OcmStation> {
        val array = JSONArray(raw)
        require(array.length() <= 1_000) { "OCM yanıtı beklenenden büyük." }
        return (0 until array.length()).mapNotNull { index ->
            runCatching { parseItem(array.getJSONObject(index)) }.getOrNull()
        }.distinctBy { it.id }
    }

    private fun parseItem(o: JSONObject): OcmStation {
        val id = o.optLong("ID", -1L)
        require(id > 0L)
        val address = o.getJSONObject("AddressInfo")
        val point = GeoPoint(address.getDouble("Latitude"), address.getDouble("Longitude"))
        // API ülke filtresi kullanılır; ek coğrafi doğrulama da uygulanır.
        require(point.latitude in 35.0..43.0 && point.longitude in 25.0..46.0)
        val connections = o.optJSONArray("Connections") ?: JSONArray()
        var ac = false
        var dc = false
        var ccs2 = false
        val powers = mutableListOf<Double>()
        val labels = mutableListOf<String>()
        for (i in 0 until minOf(connections.length(), 40)) {
            val connection = connections.optJSONObject(i) ?: continue
            val type = connection.optJSONObject("ConnectionType")
            val typeName = type?.optString("Title", "") ?: ""
            val typeId = type?.optInt("ID", 0) ?: 0
            val currentName = connection.optJSONObject("CurrentType")?.optString("Title", "") ?: ""
            val lowered = (typeName + " " + currentName).lowercase()
            val connectorIsDc = lowered.contains("dc") || lowered.contains("chademo") ||
                lowered.contains("supercharger") || lowered.contains("combo") ||
                typeId == 33 || typeId == 2
            val connectorIsAc = lowered.contains("ac") || lowered.contains("type 2") ||
                lowered.contains("type2") || lowered.contains("mennekes") ||
                typeId == 25
            ccs2 = ccs2 || typeId == 33 || lowered.contains("ccs") ||
                lowered.contains("combo 2")
            dc = dc || connectorIsDc
            ac = ac || connectorIsAc
            if (typeName.isNotBlank()) labels.add(typeName.take(60))
            val kw = connection.optDouble("PowerKW", Double.NaN)
            if (kw.isFinite() && kw in 0.1..1000.0) powers.add(kw)
        }
        val info = o.optJSONObject("OperatorInfo")
        val status = o.optJSONObject("StatusType")
        val title = address.optString("Title", "").trim()
            .takeIf { it.isNotBlank() } ?: "İsimsiz şarj noktası"
        return OcmStation(
            id = id, name = title.take(120),
            operator = info?.optString("Title", "")?.takeIf { it.isNotBlank() }
                ?: "Operatör bilinmiyor",
            point = point,
            powerKw = powers.maxOrNull(),
            hasAc = ac, hasDc = dc, hasCcs2 = ccs2,
            connectors = labels.distinct().take(8),
            access = o.optJSONObject("UsageType")?.optString("Title", "")
                ?.takeIf { it.isNotBlank() } ?: "bilinmiyor",
            reportedStatus = status?.optString("Title", "")
                ?.takeIf { it.isNotBlank() },
        )
    }
}

/** 25–30 km aralıklı çağrılar; kapsama tamamlığı ve sorgu üst sınırı açıkça bildirilir. */
data class OcmSamplingPlan(val points: List<GeoPoint>, val incomplete: Boolean)
object OcmCorridor {
    const val MAX_CALLS = 18
    const val RADIUS_KM = 9.0

    fun sample(geometry: List<GeoPoint>): OcmSamplingPlan {
        require(geometry.size >= 2)
        val sampled = RouteSampler(26_000.0, 150).sample(geometry)
        val all = sampled.map { GeoPoint(it.lat, it.lon) }
        if (all.size <= MAX_CALLS) return OcmSamplingPlan(all, false)
        // Uzun yolda kapsama sınırını saklamadan eşit yayılı örnekler kullan.
        val keep = (0 until MAX_CALLS).map { index ->
            ((all.lastIndex.toDouble() * index) / (MAX_CALLS - 1)).toInt()
        }.distinct()
        return OcmSamplingPlan(keep.map(all::get), true)
    }

    fun project(station: OcmStation, geometry: List<GeoPoint>,
        widthKm: Double = RADIUS_KM): OcmStation? {
        if (geometry.size < 2) return null
        var nearest = Double.POSITIVE_INFINITY
        var at = 0.0
        var cumulative = 0.0
        for ((a, b) in geometry.zipWithNext()) {
            val segmentLength = RouteDistance.haversine(a, b)
            if (segmentLength <= 0.0) continue
            val ac = RouteDistance.haversine(a, station.point)
            val bc = RouteDistance.haversine(b, station.point)
            val t = ((ac * ac + segmentLength * segmentLength - bc * bc) /
                (2.0 * segmentLength * segmentLength)).coerceIn(0.0, 1.0)
            val p = GeoPoint(a.latitude + (b.latitude - a.latitude) * t,
                a.longitude + (b.longitude - a.longitude) * t)
            val d = RouteDistance.haversine(p, station.point)
            if (d < nearest) { nearest = d; at = cumulative + t * segmentLength }
            cumulative += segmentLength
        }
        return if (nearest <= widthKm * 1000.0) station.copy(
            alongKm = at / 1000.0, awayKm = nearest / 1000.0,
        ) else null
    }
}
```

---

## 5. `app/src/main/java/app/evcockpit/charging/ocm/OcmRepository.kt`

```kotlin
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
                401, 403 -> error("OCM anahtarı reddedildi; hesabındaki anahtarı kontrol et.")
                429 -> error("Open Charge Map istek sınırı aşıldı.")
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
            } catch (_: Exception) {
                // Diğer sorgular başarısız olsa da başarılı kısımları göster.
                failures++
            }
        }
        val complete = failures == 0 && !sampling.incomplete && !truncated
        val status = when {
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
```

---

## 6. `app/src/main/java/app/evcockpit/charging/tariffs/TariffCatalog.kt`

```kotlin
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
```

---

## 7. `app/src/main/java/app/evcockpit/charging/tariffs/TariffRepository.kt`

```kotlin
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
```

---

## 8. `app/src/main/java/app/evcockpit/charging/ui/ChargingExplorerScreen.kt`

```kotlin
package app.evcockpit.charging.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.charging.ocm.OcmStation
import app.evcockpit.charging.tariffs.*
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.ui.OnlineMap
import java.util.Locale

private val ink = Color(0xFF0B1120)
private val panel = Color(0xFF141E30)
private val soft = Color(0xFF94A8BD)
private val highlight = Color(0xFF31D9B5)
private val outline = Color(0xFF2B3B50)

private enum class StationFilter(val label: String) {
    ALL("Tümü"), CCS2("CCS2"), DC("DC hızlı"), AC("AC")
}

/**
 * Harita odaklı şarj keşfi: tablet yatayda harita + istasyon listesi,
 * telefonda harita üstte, filtreler ve kartlar altta görünür.
 * Mola arayüzüne ya da Mola APK'sına müdahale edilmez.
 */
@Composable
fun ChargingExplorerScreen(
    route: NavigationRoute?,
    gps: GpsSnapshot,
    onSelectDestination: (GeoPoint) -> Unit,
    onNavigateViaStops: (List<GeoPoint>, GeoPoint) -> Unit,
    vm: ChargingExplorerViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var filter by remember { mutableStateOf(StationFilter.ALL) }
    var keyInput by remember { mutableStateOf("") }
    var openKey by remember { mutableStateOf(false) }
    var openTariffs by remember { mutableStateOf(false) }
    var activeOperator by remember { mutableStateOf("zes") }
    var kind by remember { mutableStateOf(ChargingKind.DC) }
    var priceInput by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    val filtered = remember(state.stations, filter) {
        state.stations.filter { station ->
            when (filter) {
                StationFilter.ALL -> true
                StationFilter.CCS2 -> station.hasCcs2
                StationFilter.DC -> station.hasDc || (station.powerKw ?: 0.0) >= 43.0
                StationFilter.AC -> station.hasAc
            }
        }
    }
    val chosen = state.selected
    val official = state.catalog.operators.firstOrNull { it.id == activeOperator }
    val manual = state.manual[activeOperator]

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ink,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.horizontalGradient(listOf(Color(0xFF173F50), panel, ink)),
                    RoundedCornerShape(19.dp),
                ).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("ŞARJ KEŞFİ", color = highlight,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Rotandaki şarj noktaları", color = Color.White,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("OCM envanteri · şarj gücü, soket ve operatör · doluluk canlı değil",
                    color = soft, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChargingMetric("${state.stations.size}", "BULUNAN", Modifier.weight(1f))
                    ChargingMetric("${state.stations.count { it.hasCcs2 }}",
                        "CCS2", Modifier.weight(1f))
                    ChargingMetric("${state.stations.count { (it.powerKw ?: 0.0) >= 150.0 }}",
                        "150+ kW", Modifier.weight(1f))
                }
            }

            if (!state.hasOcmKey || openKey) {
                Surface(color = panel, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Open Charge Map anahtarı", color = Color.White,
                            style = MaterialTheme.typography.titleSmall)
                        Text("Ücretsiz API anahtarını profilindeki My Apps alanından al.",
                            color = soft, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = keyInput, onValueChange = { keyInput = it.take(256) },
                            label = { Text("OCM API anahtarı") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.saveKey(keyInput); keyInput = ""; openKey = false },
                                enabled = keyInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = highlight,
                                    contentColor = ink),
                            ) { Text("Şifreli kaydet") }
                            OutlinedButton(onClick = {
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://openchargemap.org/profile"))) }
                            }) { Text("OCM hesabım") }
                        }
                    }
                }
            } else {
                TextButton(onClick = { openKey = true }) {
                    Text("OCM anahtarını değiştir", color = soft)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.search(route) },
                    enabled = !state.loading && state.hasOcmKey && route != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = highlight, contentColor = ink),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(if (state.loading) "Taranıyor…" else "Rota istasyonlarını tara",
                        fontWeight = FontWeight.Bold)
                }
                if (state.loading) {
                    OutlinedButton(onClick = vm::cancel) { Text("İptal") }
                }
            }
            if (state.loading) LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(), color = highlight)
            Text(state.stationMessage, color = if (state.incomplete) Color(0xFFFFCF85) else soft,
                style = MaterialTheme.typography.bodySmall)
            if (route == null) Text("Önce Rota ekranından güzergâh oluştur.",
                color = soft, style = MaterialTheme.typography.bodySmall)

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                StationFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = highlight,
                            selectedLabelColor = ink,
                            labelColor = Color.White,
                        ),
                    )
                }
            }

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 750.dp
                val map: @Composable (Modifier) -> Unit = { modifier ->
                    OnlineMap(
                        modifier = modifier,
                        route = route,
                        destination = route?.geometry?.lastOrNull(),
                        gps = gps,
                        onSelectDestination = onSelectDestination,
                        ocmStations = filtered,
                        onOcmStationSelected = vm::select,
                        focusedOcmStation = chosen,
                    )
                }
                val entries: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (filtered.isEmpty()) {
                            Surface(color = panel, shape = RoundedCornerShape(16.dp)) {
                                Text("Henüz istasyon yok. Haritayı görüntülemek için bir rota, " +
                                    "istasyon taraması için OCM API anahtarı gerekiyor.",
                                    color = soft, modifier = Modifier.padding(15.dp))
                            }
                        }
                        filtered.take(if (wide) 35 else 10).forEach { station ->
                            StationCard(station, state.selected?.id == station.id,
                                state.catalog, state.manual,
                                onPick = { vm.select(station) },
                                onAdd = { vm.addStop(station) })
                        }
                        if (filtered.size > if (wide) 35 else 10) {
                            Text("Listede ilk ${if (wide) 35 else 10} istasyon gösteriliyor. " +
                                "Haritadaki noktalardan diğerlerine erişebilirsin.",
                                color = soft, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        map(Modifier.weight(1.8f).height(470.dp))
                        Column(Modifier.weight(1f).height(470.dp)
                            .verticalScroll(rememberScrollState())) { entries() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        map(Modifier.fillMaxWidth().height(340.dp))
                        entries()
                    }
                }
            }

            if (chosen != null) {
                StationDetails(chosen, state.catalog, state.manual,
                    onAdd = { vm.addStop(chosen) })
            }
            if (state.addedStops.isNotEmpty()) {
                Surface(color = panel, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SEÇİLEN ŞARJ DURAKLARI • ${state.addedStops.size}",
                            color = highlight, fontWeight = FontWeight.Bold)
                        state.addedStops.forEach { station ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(station.name, Modifier.weight(1f),
                                    color = Color.White, maxLines = 1)
                                TextButton(onClick = { vm.removeStop(station.id) }) {
                                    Text("Kaldır", color = Color(0xFFFFA6A6))
                                }
                            }
                        }
                        Button(
                            enabled = route?.geometry?.lastOrNull() != null,
                            onClick = {
                                val destination = route?.geometry?.lastOrNull()
                                if (destination != null) onNavigateViaStops(
                                    state.addedStops.map { it.point }, destination)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = highlight, contentColor = ink),
                        ) { Text("Duraklarla yeni rota iste") }
                        Text("Ara duraklı rota yeniden hesaplama mevcut ORS bağlantısına bağlıdır; " +
                            "GPS ve HeiGIT anahtarı gerekebilir. Duraklar otomatik şarj planı değildir.",
                            color = soft, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedButton(onClick = { openTariffs = !openTariffs },
                modifier = Modifier.fillMaxWidth()) {
                Text(if (openTariffs) "Tarife yönetimini kapat" else
                    "AC / DC fiyat kataloğunu ve manuel tarifeleri aç",
                    color = Color.White)
            }
            if (openTariffs) {
                Column(
                    Modifier.fillMaxWidth().background(panel, RoundedCornerShape(16.dp))
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Operatör tarifeleri", color = Color.White,
                        style = MaterialTheme.typography.titleMedium)
                    Text(state.tariffMessage, color = soft,
                        style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.catalog.operators.forEach { operator ->
                            FilterChip(
                                selected = activeOperator == operator.id,
                                onClick = { activeOperator = operator.id },
                                label = { Text(operator.operator) },
                            )
                        }
                    }
                    if (official != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = kind == ChargingKind.AC,
                                onClick = { kind = ChargingKind.AC },
                                label = { Text("AC") })
                            FilterChip(selected = kind == ChargingKind.DC,
                                onClick = { kind = ChargingKind.DC },
                                label = { Text("DC") })
                        }
                        val price = TariffResolver.resolve(
                            state.catalog, state.manual, official.operator, kind)
                        Text("Fiyat: " + priceLabel(price) +
                            " • ${price.source} • tarih ${price.updatedAt ?: "yok"}",
                            color = if (price.priceTlPerKwh == null) soft else highlight)
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it.take(9) },
                            label = { Text("Kendi gözlemim — TL/kWh") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val input = priceInput.replace(',', '.').toDoubleOrNull()
                                if (input != null) {
                                    vm.savePrice(official.id, kind, input)
                                    priceInput = ""
                                }
                            }, enabled = priceInput.replace(',', '.').toDoubleOrNull()
                                ?.let { it.isFinite() && it in 0.01..150.0 } == true,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = highlight, contentColor = ink)) {
                                Text("Manuel fiyatı kaydet")
                            }
                            if (manual != null) {
                                OutlinedButton(onClick = { vm.clearPrice(official.id) }) {
                                    Text("Manueli sıfırla")
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { vm.refreshTariffs(true) }) {
                        Text("GitHub tarifelerini yenile")
                    }
                    Text("Tarife dosyası GitHub raw URL'sinden alınır; 24 saat önbelleğe " +
                        "kaydedilir. İlk APK içinde fiyatlar bilinmiyor (null). " +
                        "Girdiğin fiyatlar operatörün resmî güncel fiyatı yerine geçmez.",
                        color = soft, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("© Open Charge Map • © OpenStreetMap katkıcıları • Fiyatlar canlı değildir. " +
                "İstasyon listesi eksik/eski olabilir; ödeme öncesi operatör uygulamasında doğrula.",
                color = soft, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ChargingMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Text(label, color = soft, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StationCard(
    station: OcmStation,
    active: Boolean,
    catalog: TariffCatalog,
    manual: Map<String, ManualPrice>,
    onPick: () -> Unit,
    onAdd: () -> Unit,
) {
    val price = TariffResolver.resolve(catalog, manual, station.operator,
        stationKind(station))
    val tone = when {
        station.powerKw == null -> soft
        station.powerKw >= 150 -> Color(0xFFC48EFF)
        station.hasDc || station.powerKw >= 43 -> highlight
        else -> Color(0xFF60ABFF)
    }
    Surface(
        onClick = onPick,
        shape = RoundedCornerShape(16.dp),
        color = if (active) Color(0xFF203B46) else panel,
        modifier = Modifier.fillMaxWidth().border(
            width = if (active) 2.dp else 1.dp,
            color = if (active) highlight else outline,
            shape = RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.name, color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text(station.operator, color = soft,
                        style = MaterialTheme.typography.bodySmall)
                }
                Text(station.powerKw?.let { "${it.toInt()} kW" } ?: "— kW",
                    color = tone, fontWeight = FontWeight.Bold)
            }
            Text(station.powerGroup + "   ·   " +
                (if (station.hasCcs2) "CCS2" else
                    station.connectors.joinToString(", ").ifBlank { "Soket bilinmiyor" }),
                color = tone, style = MaterialTheme.typography.labelSmall)
            Text("${String.format(Locale.US, "%.1f", station.alongKm)} km rota boyunca" +
                "  ·  ${String.format(Locale.US, "%.1f", station.awayKm)} km kuş uçuşu sapma",
                color = soft, style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(priceLabel(price), color = if (price.priceTlPerKwh == null ||
                    price.stale) soft else Color.White, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onAdd) { Text("+ Durak ekle", color = highlight) }
            }
        }
    }
}

@Composable
private fun StationDetails(
    station: OcmStation,
    catalog: TariffCatalog,
    manual: Map<String, ManualPrice>,
    onAdd: () -> Unit,
) {
    val price = TariffResolver.resolve(catalog, manual, station.operator,
        stationKind(station))
    Surface(color = panel, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("SEÇİLEN İSTASYON", color = highlight,
                style = MaterialTheme.typography.labelMedium)
            Text(station.name, color = Color.White,
                style = MaterialTheme.typography.titleLarge)
            Text("Operatör: ${station.operator}", color = soft)
            Text("Bağlantılar: ${station.connectors.joinToString(", ").ifBlank { "bilinmiyor" }}",
                color = Color.White)
            Text("Bildirilen güç: ${station.powerKw?.let { "$it kW" } ?: "bilinmiyor"}" +
                " · erişim: ${station.access}", color = Color.White)
            Text("Envanter durum etiketi: ${station.reportedStatus ?: "bilinmiyor"}" +
                " (CANLI DOLULUK DEĞİL)", color = soft)
            Text("Fiyat: " + priceLabel(price) +
                " · ${price.source} · ${price.updatedAt ?: "tarih yok"}",
                color = Color.White)
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(
                containerColor = highlight, contentColor = ink)) {
                Text("Rota durağına ekle")
            }
        }
    }
}

private fun stationKind(station: OcmStation): ChargingKind = when {
    station.hasDc -> ChargingKind.DC
    station.hasAc -> ChargingKind.AC
    else -> ChargingKind.UNKNOWN
}

private fun priceLabel(price: DisplayTariff): String =
    price.priceTlPerKwh?.let {
        val label = String.format(Locale.forLanguageTag("tr-TR"), "%.2f TL/kWh", it)
        label + if (price.stale) " • ESKİ / KONTROL ET" else " • KAYNAĞI KONTROL ET"
    } ?: "Fiyat bilinmiyor"
```

---

## 9. `app/src/main/java/app/evcockpit/charging/ui/ChargingExplorerViewModel.kt`

```kotlin
package app.evcockpit.charging.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.charging.ocm.OcmKeyStore
import app.evcockpit.charging.ocm.OcmRepository
import app.evcockpit.charging.ocm.OcmStation
import app.evcockpit.charging.tariffs.*
import app.evcockpit.navigation.NavigationRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChargingExplorerState(
    val stations: List<OcmStation> = emptyList(),
    val selected: OcmStation? = null,
    val loading: Boolean = false,
    val hasOcmKey: Boolean = false,
    val stationMessage: String = "Rota üzerinde Open Charge Map taraması yapılmadı.",
    val tariffMessage: String = "Tarifeler okunuyor…",
    val catalog: TariffCatalog = TariffCatalog(emptyList()),
    val manual: Map<String, ManualPrice> = emptyMap(),
    val incomplete: Boolean = true,
    val addedStops: List<OcmStation> = emptyList(),
)

class ChargingExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val keys = OcmKeyStore(application)
    private val repo = OcmRepository()
    private val tariffs = TariffRepository(application)
    private val mutable = MutableStateFlow(ChargingExplorerState())
    val state = mutable.asStateFlow()
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            keys.hasKey.collect { available ->
                mutable.value = mutable.value.copy(hasOcmKey = available)
            }
        }
        refreshTariffs(false)
    }

    fun refreshTariffs(force: Boolean = true) {
        viewModelScope.launch {
            val loaded = runCatching { tariffs.load(force) }.getOrNull()
            val manual = runCatching { tariffs.manualOverrides() }.getOrDefault(emptyMap())
            mutable.value = mutable.value.copy(
                catalog = loaded?.catalog ?: TariffCatalog(emptyList()),
                manual = manual,
                tariffMessage = loaded?.let { "Tarifeler: ${it.origin} • fiyatlar doğrulanmalı" }
                    ?: "Tarife kataloğu açılamadı; tüm fiyatlar bilinmiyor.",
            )
        }
    }

    fun saveKey(key: String) {
        viewModelScope.launch {
            val result = runCatching { keys.save(key) }
            mutable.value = mutable.value.copy(stationMessage =
                if (result.isSuccess) "OCM anahtarı şifreli kaydedildi."
                else "OCM anahtarı kaydedilemedi; hesabındaki değeri kontrol et.")
        }
    }

    fun clearKey() {
        viewModelScope.launch {
            keys.clear()
            mutable.value = mutable.value.copy(stations = emptyList(),
                selected = null, stationMessage = "OCM anahtarı silindi; canlı sorgu kapalı.")
        }
    }

    fun search(route: NavigationRoute?) {
        if (fetchJob?.isActive == true) return
        if (route == null || route.geometry.size < 2) {
            mutable.value = mutable.value.copy(stationMessage = "Önce gerçek rota seç.")
            return
        }
        fetchJob = viewModelScope.launch {
            mutable.value = mutable.value.copy(
                loading = true, stations = emptyList(), selected = null,
                incomplete = true, stationMessage = "Rota çevresindeki OCM kayıtları aranıyor…")
            try {
                val key = keys.read() ?: run {
                    mutable.value = mutable.value.copy(loading = false,
                        stationMessage = "Open Charge Map anahtarını bu panelde kaydet.")
                    return@launch
                }
                val result = repo.search(route.geometry, key)
                mutable.value = mutable.value.copy(
                    loading = false, stations = result.stations,
                    incomplete = !result.complete, stationMessage = result.message,
                )
            } catch (cancel: CancellationException) {
                mutable.value = mutable.value.copy(loading = false,
                    stationMessage = "İstasyon taraması iptal edildi.")
                throw cancel
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(loading = false,
                    stationMessage = "OCM isteği tamamlanamadı; internet, anahtar veya kota sorunu.")
            }
        }
    }

    fun cancel() { fetchJob?.cancel() }
    fun select(station: OcmStation?) {
        mutable.value = mutable.value.copy(selected = station)
    }
    fun addStop(station: OcmStation) {
        val current = mutable.value.addedStops
        if (current.any { it.id == station.id } || current.size >= 10) return
        mutable.value = mutable.value.copy(
            addedStops = (current + station).sortedBy { it.alongKm })
    }
    fun removeStop(id: Long) {
        mutable.value = mutable.value.copy(
            addedStops = mutable.value.addedStops.filterNot { it.id == id })
    }
    fun savePrice(id: String, kind: ChargingKind, tl: Double) {
        viewModelScope.launch {
            val success = runCatching { tariffs.saveManual(id, kind, tl) }.isSuccess
            mutable.value = mutable.value.copy(
                manual = tariffs.manualOverrides(),
                tariffMessage = if (success) "Kullanıcı tarifesi kaydedildi; resmî fiyat değil."
                else "Fiyat kaydedilemedi; 0,01–150 TL/kWh aralığı gerekli.",
            )
        }
    }
    fun clearPrice(id: String) {
        viewModelScope.launch {
            tariffs.clearManual(id)
            mutable.value = mutable.value.copy(manual = tariffs.manualOverrides())
        }
    }
}
```

---

## 10. `app/src/main/java/app/evcockpit/ui/OnlineMap.kt`

```kotlin
package app.evcockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.awaitCancellation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.annotations.IconFactory
import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.charging.ocm.OcmStation
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import kotlin.math.abs
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.RouteProgress
import app.evcockpit.ui.tesla.RoutePolylineProgress

/**
 * MapLibre Native + OpenFreeMap public online tiles. No offline tile download.
 * Initial camera shows Türkiye; it is NOT a claimed GPS location.
 * Provider availability / phone rendering have not yet been device-verified.
 */
@Composable
fun OnlineMap(
    modifier: Modifier = Modifier,
    route: NavigationRoute?,
    destination: GeoPoint?,
    gps: GpsSnapshot,
    weatherSummary: String = "",
    onSelectDestination: (GeoPoint) -> Unit,
    stopMarkers: List<ChargerCandidate> = emptyList(),
    onStopSelected: (ChargerCandidate) -> Unit = {},
    focusedStop: ChargerCandidate? = null,
    onLoadFailed: () -> Unit = {},
    onStyleReady: () -> Unit = {},
    canChangeLayer: Boolean = true,
    alternativeRoutes: List<NavigationRoute> = emptyList(),
    ocmStations: List<OcmStation> = emptyList(),
    onOcmStationSelected: (OcmStation) -> Unit = {},
    focusedOcmStation: OcmStation? = null,
) {
    val context = LocalContext.current
    val layerPreferences = remember(context) { MapLayerPreferences(context) }
    val layer by layerPreferences.selected.collectAsState(initial = MapLayer.DARK)
    val mapTilerStore = remember(context) { MapTilerKeyStore(context) }
    val mapTilerKey by mapTilerStore.key.collectAsState(initial = null)
    val styleUrl = MapStyleResolver.url(layer, mapTilerKey)
        ?: MapLayer.DARK.styleUrl
    val chosenStyleUrl = rememberUpdatedState(styleUrl)
    val scope = rememberCoroutineScope()
    val mapStyleApplied = remember { mutableStateOf("") }
    var followCamera by remember { mutableStateOf(false) }
    var tiltedCamera by remember { mutableStateOf(false) }
    var northUp by remember { mutableStateOf(false) }
    val layerMenu = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapReady = remember { mutableStateOf<MapLibreMap?>(null) }
    val chosenDestination = rememberUpdatedState(onSelectDestination)
    val chosenStop = rememberUpdatedState(onStopSelected)
    val displayedStops = rememberUpdatedState(stopMarkers)
    val displayedOcmStations = rememberUpdatedState(ocmStations)
    val chosenOcmStation = rememberUpdatedState(onOcmStationSelected)
    val mapFailureCallback = rememberUpdatedState(onLoadFailed)
    val mapReadyCallback = rememberUpdatedState(onStyleReady)
    val mapView = remember(context) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            addOnDidFailLoadingMapListener(
                MapView.OnDidFailLoadingMapListener { _ ->
                    mapFailureCallback.value()
                },
            )
            getMapAsync { map ->
                map.addOnMapLongClickListener { location ->
                    chosenDestination.value(GeoPoint(location.latitude, location.longitude))
                    true
                }
                map.setOnMarkerClickListener { marker ->
                    val found = displayedStops.value.firstOrNull {
                        abs(it.latitude - marker.position.latitude) < 0.00001 &&
                        abs(it.longitude - marker.position.longitude) < 0.00001
                    }
                    if (found != null) {
                        chosenStop.value(found)
                        true
                    } else {
                        val ocm = displayedOcmStations.value.firstOrNull {
                            abs(it.point.latitude - marker.position.latitude) < 0.00001 &&
                            abs(it.point.longitude - marker.position.longitude) < 0.00001
                        }
                        if (ocm != null) { chosenOcmStation.value(ocm); true } else false
                    }
                }
                mapStyleApplied.value = chosenStyleUrl.value
                map.setStyle(chosenStyleUrl.value) {
                    mapReady.value = map
                    mapReadyCallback.value()
                }
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.0, 35.0), 5.2))
            }
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        var destroyed = false
        fun destroyOnce() {
            if (!destroyed) {
                destroyed = true
                mapView.onDestroy()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> destroyOnce()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapReady.value = null
            if (!destroyed) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
                destroyOnce()
            }
        }
    }

    val loadedMap = mapReady.value
    LaunchedEffect(styleUrl) {
        val map = mapReady.value
        if (map != null && styleUrl != mapStyleApplied.value) {
            mapReady.value = null
            map.setStyle(styleUrl) {
                mapStyleApplied.value = styleUrl
                mapReady.value = map
                mapReadyCallback.value()
            }
        }
    }
    LaunchedEffect(loadedMap, focusedStop) {
        if(loadedMap!=null && focusedStop!=null) {
            followCamera = false
            loadedMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(focusedStop.latitude,focusedStop.longitude),12.0))
        }
    }
    LaunchedEffect(loadedMap, focusedOcmStation) {
        if (loadedMap != null && focusedOcmStation != null) {
            followCamera = false
            loadedMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(focusedOcmStation.point.latitude, focusedOcmStation.point.longitude), 13.0))
        }
    }
    val gpsFresh = rememberGpsFreshness(gps)
    // Takip kapalıyken de 2B/3B değişimi mevcut harita merkezinde uygulanır.
    LaunchedEffect(loadedMap, tiltedCamera, northUp) {
        val map = loadedMap ?: return@LaunchedEffect
        if (followCamera) return@LaunchedEffect
        val current = map.cameraPosition
        val camera = CameraPosition.Builder()
            .target(current.target)
            .zoom(current.zoom)
            .bearing(if (northUp) 0.0 else current.bearing)
            .tilt(if (tiltedCamera) 45.0 else 0.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
    }
    // Takip isteğe bağlıdır; GPS güncel değilse kamera sahte konuma gitmez.
    // GNSS course hareket yönüdür; pusula okumak gibi yorumlanmaz.
    LaunchedEffect(loadedMap, gps.point, gpsFresh, gps.courseDegrees,
        followCamera, tiltedCamera, northUp) {
        val map = loadedMap ?: return@LaunchedEffect
        val point = gps.point ?: return@LaunchedEffect
        if (!followCamera || !gpsFresh) return@LaunchedEffect
        val camera = CameraPosition.Builder()
            .target(LatLng(point.latitude, point.longitude))
            .zoom(15.0)
            .bearing(if (northUp) 0.0 else gps.courseDegrees ?: 0.0)
            .tilt(if (tiltedCamera) 45.0 else 0.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
    }
    // Rota değiştiğinde kamera sadece GERÇEK seçilen yol geometrisini çerçeveler.
    LaunchedEffect(loadedMap, route) {
        val map = loadedMap ?: return@LaunchedEffect
        val path = route?.geometry?.takeIf { it.size >= 2 } ?: return@LaunchedEffect
        if (followCamera) return@LaunchedEffect
        runCatching {
            val bounds = LatLngBounds.Builder()
            val step = (path.size / 1_000).coerceAtLeast(1)
            path.filterIndexed { index, _ -> index % step == 0 || index == path.lastIndex }
                .forEach { point -> bounds.include(LatLng(point.latitude, point.longitude)) }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 72))
        }
    }
    LaunchedEffect(loadedMap, route, alternativeRoutes, destination, stopMarkers,
        ocmStations, gps.point, gpsFresh) {
        val currentMap = loadedMap ?: return@LaunchedEffect
        var currentLine: Polyline? = null
        var passedLine: Polyline? = null
        var currentMarker: Marker? = null
        val chargingMarkers = mutableListOf<Marker>()
        val secondaryLines = mutableListOf<Polyline>()
        val ocmPins = mutableListOf<Marker>()
        try {
            // Alternatifler soluk; yalnız seçilen güzergâh canlı yeşildir.
            alternativeRoutes.filter { it != route }.take(2).forEach { alternative ->
                if (alternative.geometry.size >= 2) {
                    val step = (alternative.geometry.size / 2_500).coerceAtLeast(1)
                    val faint = alternative.geometry.filterIndexed { index, _ ->
                        index % step == 0 || index == alternative.geometry.lastIndex
                    }
                    secondaryLines += currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(faint.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(111, 129, 146))
                            .width(5f),
                    )
                }
            }
            if (route != null) {
                val position = if (gpsFresh && gps.point != null)
                    RouteProgress.locate(route, gps.point) else null
                val progress = if (position != null && route.distanceMeters > 0.0)
                    ((route.distanceMeters - position.remainingMeters) / route.distanceMeters)
                        .coerceIn(0.0, 1.0) else 0.0
                val parts = RoutePolylineProgress.split(route.geometry, progress)
                if (parts.passed.size >= 2) {
                    passedLine = currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(parts.passed.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(100, 110, 117))
                            .width(8f),
                    )
                }
                if (parts.remaining.size >= 2) {
                    currentLine = currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(parts.remaining.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(61, 220, 132))
                            .width(8f),
                    )
                }
            }
            if (destination != null) {
                currentMarker = currentMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(destination.latitude, destination.longitude))
                        .title("Seçilen hedef"),
                )
            }
            ocmStations.take(250).forEach { station ->
                // Pin renklendirmesi rapor edilen istasyon gücünü gösterir; canlı durum DEĞİL.
                val fill = when {
                    station.powerKw == null -> android.graphics.Color.GRAY
                    station.powerKw >= 150.0 -> android.graphics.Color.rgb(173, 90, 225)
                    station.hasDc || station.powerKw >= 43.0 ->
                        android.graphics.Color.rgb(17, 205, 180)
                    station.hasAc -> android.graphics.Color.rgb(65, 157, 238)
                    else -> android.graphics.Color.GRAY
                }
                val bitmap = Bitmap.createBitmap(42, 42, Bitmap.Config.ARGB_8888)
                val draw = AndroidCanvas(bitmap)
                val brush = Paint(Paint.ANTI_ALIAS_FLAG)
                brush.color = android.graphics.Color.WHITE
                draw.drawCircle(21f, 21f, 20f, brush)
                brush.color = fill
                draw.drawCircle(21f, 21f, 16f, brush)
                val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
                ocmPins += currentMap.addMarker(MarkerOptions()
                    .position(LatLng(station.point.latitude, station.point.longitude))
                    .title(station.name.take(100))
                    .snippet("${station.powerGroup} • doluluk bilinmiyor")
                    .icon(icon))
            }
            stopMarkers.forEach { station ->
                val color=PlannerMarkerPolicy.color(station)
                val bitmap=Bitmap.createBitmap(44,44,Bitmap.Config.ARGB_8888)
                val draw=AndroidCanvas(bitmap)
                val paint=Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color=android.graphics.Color.WHITE
                draw.drawCircle(22f,22f,21f,paint)
                paint.color=color
                draw.drawCircle(22f,22f,17f,paint)
                val icon=IconFactory.getInstance(context).fromBitmap(bitmap)
                chargingMarkers+=currentMap.addMarker(MarkerOptions()
                    .position(LatLng(station.latitude,station.longitude))
                    .title(PlannerMarkerPolicy.label(station).take(120))
                    .snippet("doluluk bilinmiyor • veri TAHMİN").icon(icon))
            }
            awaitCancellation()
        } finally {
            currentLine?.let { currentMap.removePolyline(it) }
            passedLine?.let { currentMap.removePolyline(it) }
            currentMarker?.let { currentMap.removeMarker(it) }
            chargingMarkers.forEach { currentMap.removeMarker(it) }
            ocmPins.forEach { currentMap.removeMarker(it) }
            secondaryLines.forEach { currentMap.removePolyline(it) }
        }
    }

    val position = if (route != null && gpsFresh &&
        gps.point != null
    ) RouteProgress.locate(route, gps.point) else null
    val maneuver = position?.nextManeuverIndex?.let { route?.maneuvers?.getOrNull(it) }
    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            OutlinedButton(onClick = { layerMenu.value = true }, enabled = canChangeLayer) {
                Text("Katman: " + layer.titleTr, fontSize = 14.sp, color = Color.White)
            }
            DropdownMenu(expanded = layerMenu.value,
                onDismissRequest = { layerMenu.value = false }) {
                MapLayer.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.titleTr + if (option == layer) " ✓" else "") },
                        enabled = MapStyleResolver.url(option, mapTilerKey) != null,
                        onClick = {
                            layerMenu.value = false
                            scope.launch { layerPreferences.choose(option) }
                        },
                    )
                }
            }
        }
        if (canChangeLayer) {
            Column(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .background(Color(0x99000000)).padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                OutlinedButton(
                    enabled = gpsFresh && gps.point != null,
                    onClick = { followCamera = !followCamera },
                ) { Text(if (followCamera) "Takibi kapat" else "Konumumu izle") }
                OutlinedButton(onClick = { tiltedCamera = !tiltedCamera }) {
                    Text(if (tiltedCamera) "2B görünüm" else "3B açı (45°)")
                }
                OutlinedButton(onClick = { northUp = !northUp }) {
                    Text(if (northUp) "Hareket yönü" else "Kuzey yukarı")
                }
                if (!gpsFresh || gps.point == null) {
                    Text("GPS güncel değil", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Text(
            when {
                route != null -> "OpenFreeMap • rota çizgisi (sahada doğrulanmadı)"
                destination != null -> "Hedef seçildi • Rota sekmesinde hesapla"
                else -> "OpenFreeMap • hedef seçmek için haritaya uzun bas"
            },
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.TopCenter)
                .background(Color(0xC9000000)).padding(8.dp),
        )
        if (weatherSummary.isNotBlank()) {
            Text(weatherSummary, color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopEnd)
                    .background(Color(0xD9000000)).padding(6.dp))
        }
        Text(MapAttribution.LABEL, color = Color.White, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomEnd)
                .background(Color(0xDD000000)).padding(horizontal = 7.dp, vertical = 4.dp))
        if (maneuver != null) {
            Text(
                "SONRAKİ MANEVRA • ${maneuver.instructionTr}\n" +
                    "Yaklaşık: ${position.distanceToManeuverMeters?.toInt() ?: "--"} m",
                color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomStart)
                    .background(Color(0xD9000000)).padding(10.dp),
            )
        }
    }
}
```

---

## 11. `app/src/main/java/app/evcockpit/ui/PlannerScreen.kt`

```kotlin
package app.evcockpit.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.obd.SocReading
import app.evcockpit.planui.*
import app.evcockpit.planner.energy.HvacMode
import app.evcockpit.planner.model.*
import app.evcockpit.storage.PlannerOptionsStore
import app.evcockpit.storage.VehicleProfileStore
import app.evcockpit.storage.SavedPlansStore
import app.evcockpit.storage.SavedPlanBuilder
import app.evcockpit.storage.StoredPlan
import app.evcockpit.planner.integration.PlanToNavigation
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.charging.ui.ChargingExplorerScreen
import app.evcockpit.planner.charging.ChargerEvidence
import app.evcockpit.planner.charging.ChargerConfidence
import android.os.SystemClock
import android.content.Intent
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PlannerScreen(
    vm:PlannerViewModel,
    navigation:NavigationRoute?,
    soc:SocReading,
    onOpenRoute:()->Unit,
    onOpenSettings:()->Unit,
    onNavigate:(List<GeoPoint>,GeoPoint)->Unit,
    gps:GpsSnapshot,
    onSelectDestination:(GeoPoint)->Unit,
) {
    val context=LocalContext.current
    val optionsStore=remember(context){PlannerOptionsStore(context.applicationContext)}
    val profileStore=remember(context){VehicleProfileStore(context.applicationContext)}
    val savedStore=remember(context){SavedPlansStore(context.applicationContext)}
    val savedPlans by savedStore.plans.collectAsState(initial=emptyList())
    val options by optionsStore.options.collectAsState(initial=PlannerOptions())
    val profile by profileStore.profile.collectAsState(initial=VehicleProfile.defaults())
    val state by vm.state.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    var showPlanMap by remember{mutableStateOf(true)}
    var showAdvancedPlanner by remember { mutableStateOf(false) }
    var highlightedStation by remember{mutableStateOf<ChargerCandidate?>(null)}
    var selectedSaved by remember{mutableStateOf<StoredPlan?>(null)}
    var saveStatus by remember{mutableStateOf("")}
    var parameters by remember{mutableStateOf(false)}
    var dataFlow by remember{mutableStateOf(false)}
    var evidenceOpen by remember{mutableStateOf(false)}
    var optionsError by remember{mutableStateOf<String?>(null)}
    val busy=state.stage in setOf(PlannerStage.ELEVATION,PlannerStage.STATIONS,
        PlannerStage.OPTIMIZING,PlannerStage.VERIFYING)
    fun persist(value:PlannerOptions) {
        val errors=value.errors()
        if(errors.isNotEmpty())optionsError=errors.first()
        else {
            optionsError=null
            scope.launch{optionsStore.save(value)}
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        // C aşamasında ana deneyim önce harita ve gerçek OCM şarj listesi.
        // Eski ayrıntılı parametre/çözücü ekranı aşağıda isteğe bağlı açılır.
        ChargingExplorerScreen(
            route = navigation,
            gps = gps,
            onSelectDestination = onSelectDestination,
            onNavigateViaStops = onNavigate,
        )
        OutlinedButton(
            onClick = { showAdvancedPlanner = !showAdvancedPlanner },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (showAdvancedPlanner) "Gelişmiş enerji ve plan ayarlarını kapat"
                else "Gelişmiş enerji ve otomatik şarj planlaması")
        }
        if (showAdvancedPlanner) {
        val success=state.plan as? PlanResult.Success
        Surface(
            modifier=Modifier.fillMaxWidth(),
            shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color=MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text("EV Rota Planlayıcı",
                    style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Text("TAHMİN • DOĞRULANMADI",
                    color=MaterialTheme.colorScheme.error,
                    style=MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    CockpitMetric("MESAFE",
                        success?.let{ "%.0f km".format(it.distanceKm) } ?: "--",
                        Modifier.weight(1f))
                    CockpitMetric("DURAK",
                        success?.stops?.size?.toString() ?: "--",
                        Modifier.weight(1f))
                    CockpitMetric("VARIŞ SoC",
                        success?.let{ "~%" + it.arrivalSocPct } ?: "--",
                        Modifier.weight(1f))
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    CockpitMetric("TOPLAM SÜRE",
                        success?.let { PlannerDisplay.totalTimeLabel(it) } ?: "--",
                        Modifier.weight(1f))
                    CockpitMetric("ŞARJ SÜRESİ",
                        success?.let { PlannerDisplay.chargingTimeLabel(it) } ?: "--",
                        Modifier.weight(1f))
                    CockpitMetric("VARIŞ ENERJİ",
                        success?.let {
                            "~%.1f kWh".format(
                                PlannerDisplay.arrivalKwh(it,profile.capacityAtSohKwh))
                        } ?: "--",
                        Modifier.weight(1f))
                }
                Text("Tüm süre/enerji değerleri model tahminidir; canlı trafik ve saha doğrulaması yok.",
                    style=MaterialTheme.typography.bodySmall)
                Text(if(success==null)"Belirsizlik: henüz hesaplanmadı — TAHMİN"
                    else PlannerDisplay.uncertainty(success),
                    style=MaterialTheme.typography.bodySmall)
                Text("SoC kaynağı: "+soc.source+" • "+
                    (soc.pct?.let{"%"+("%.1f".format(it))} ?:"değer yok"),
                    style=MaterialTheme.typography.bodySmall)
                Text("Rota: "+(navigation?.provider ?: "yok")+
                    " • trafik dahil değil / sahada DOĞRULANMADI",
                    style=MaterialTheme.typography.bodySmall)
                if(state.message.isNotBlank()) Text(state.message)
                if(state.warning.isNotBlank()) Text(state.warning,
                    style=MaterialTheme.typography.bodySmall)
                Text("İstasyonlar: "+state.stationStatus,
                    style=MaterialTheme.typography.bodySmall)
                Text("Çözücü hesap süresi: "+
                    (state.solverElapsedMs?.let{it.toString()+" ms"} ?:"ölçülmedi")+
                    " • POCO F5 ölçümü bekleniyor",
                    style=MaterialTheme.typography.bodySmall)
            }
        }
        if (success != null) {
            TripChargeCostPanel(success, state.request)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={vm.plan(navigation,soc,options,profile)},enabled=!busy,
                modifier=Modifier.weight(1f).heightIn(min=48.dp)) {
                Text("Şarj planı hesapla")
            }
            if(busy) OutlinedButton(onClick=vm::cancel,
                modifier=Modifier.heightIn(min=48.dp)){Text("İptal")}
        }
        if(navigation==null) {
            Text("Önce GPS'i açın, hedef belirleyin ve kendi HeiGIT anahtarınızla rota oluşturun.")
            OutlinedButton(onClick=onOpenRoute){Text("Rota ekranına git")}
            OutlinedButton(onClick=onOpenSettings){Text("GPS / HeiGIT anahtar ayarları")}
        }
        if(soc.pct==null || soc.source.startsWith("DEMO")) {
            OutlinedButton(onClick=onOpenSettings){Text("Manuel SoC gir / kontrol et")}
        }
        OutlinedButton(onClick={evidenceOpen=!evidenceOpen}) {
            Text(if(evidenceOpen)"Veri güveni panelini kapat" else "Veri güveni • OBD / rota / şarj")
        }
        if(evidenceOpen) {
            val audit=TripDataEvidence.evaluate(
                soc=soc,routeAvailable=navigation!=null,routeProvider=navigation?.provider,
                gpsPresent=gps.point!=null,gpsFresh=gps.isFresh(SystemClock.elapsedRealtime()),
                gpsAccuracyMeters=gps.accuracyMeters,
                stationSearchComplete=state.stationStatus!="Şarj verisi henüz aranmadı",
                stationCount=state.candidates.size,
                taggedCcs2Count=state.candidates.count{
                    it.confidence==ChargerConfidence.OSM_TAGGED_CCS2
                },planAvailable=success!=null,
            )
            Surface(
                modifier=Modifier.fillMaxWidth(),
                color=MaterialTheme.colorScheme.surfaceVariant,
                shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text("Kaynak ve güven incelemesi",
                        style=MaterialTheme.typography.titleSmall)
                    Text("Bu panel rota hesaplamasını değiştirmez; gerçek araç/istasyon " +
                        "doğruluğunu garanti etmez.",
                        style=MaterialTheme.typography.bodySmall)
                    audit.items.forEach { item->
                        val prefix=when(item.level) {
                            EvidenceLevel.INPUT_AVAILABLE -> "GİRDİ VAR"
                            EvidenceLevel.CAUTION -> "DİKKAT"
                            EvidenceLevel.MISSING -> "VERİ YOK"
                        }
                        Text(item.label+" · "+prefix+" — "+item.explanation,
                            style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        OutlinedButton(onClick={dataFlow=true}) {Text("Veri akışı • hangi hizmete ne gönderiliyor?")}
        OutlinedButton(onClick={parameters=!parameters}) {
            Text(if(parameters)"Parametre panelini kapat" else "Parametreleri düzenle")
        }
        if(parameters) {
            Text("Başlangıç GPS/adres/Ev-İş favorileri, hedef ve ara noktalar Rota ekranında seçilir.")
            OutlinedButton(onClick=onOpenRoute) {
                Text("Başlangıç, hedef ve ara noktalar için Rota ekranına git")
            }
            Text("Tüm ayarlar cihazda kalıcı; değerler model varsayımıdır.")
            PlannerNumber("Varışta asgari SoC (%)",options.minArrivalSoc){
                persist(options.copy(minArrivalSoc=it))
            }
            PlannerNumber("Durakta asgari SoC (%)",options.minStopSoc){
                persist(options.copy(minStopSoc=it))
            }
            PlannerNumber("Azami şarj SoC (%)",options.maxChargeSoc){
                persist(options.copy(maxChargeSoc=it))
            }
            PlannerNumber("Azami seyir hızı (km/sa)",options.maxCruiseKmh){
                persist(options.copy(maxCruiseKmh=it))
            }
            PlannerNumber("Ortam sıcaklığı (°C)",options.temperatureC){
                persist(options.copy(temperatureC=it))
            }
            PlannerNumber("Ek yük (kg)",options.extraLoadKg){
                persist(options.copy(extraLoadKg=it))
            }
            PlannerNumber("Tüketim düzeltmesi (%)",options.consumptionAdjust*100.0){
                persist(options.copy(consumptionAdjust=it/100.0))
            }
            PlannerNumber("Asgari istasyon gücü (kW)",options.minStationKw){
                persist(options.copy(minStationKw=it))
            }
            PlannerNumber("İstasyon yarıçapı (km)",options.searchRadiusMeters/1000.0){
                persist(options.copy(searchRadiusMeters=(it*1000.0).roundToInt()))
            }
            PlannerNumber("En çok durak",options.maxStops.toDouble()){
                persist(options.copy(maxStops=it.toInt()))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("HVAC: "+options.hvac.name,modifier=Modifier.weight(1f))
                OutlinedButton(onClick={
                    persist(options.copy(hvac=when(options.hvac){
                        HvacMode.OFF->HvacMode.AUTO
                        HvacMode.AUTO->HvacMode.HEATING
                        HvacMode.HEATING->HvacMode.OFF
                    }))
                }) {Text("Değiştir")}
            }
            Row {
                Text("Yalnız OSM CCS2 etiketli (cihaz durumu yine bilinmiyor)",
                    modifier=Modifier.weight(1f))
                Switch(checked=options.onlyTaggedCcs2,
                    onCheckedChange={persist(options.copy(onlyTaggedCcs2=it))})
            }
            Row {
                Text("Yavaş AC adaylarına izin ver",modifier=Modifier.weight(1f))
                Switch(checked=options.allowSlowCharging,
                    onCheckedChange={persist(options.copy(allowSlowCharging=it))})
            }
            var excluded by remember(options.excludedNetworks){
                mutableStateOf(options.excludedNetworks)
            }
            OutlinedTextField(value=excluded,onValueChange={excluded=it},
                label={Text("Dışlanan ağlar (virgülle)")},modifier=Modifier.fillMaxWidth())
            OutlinedButton(onClick={persist(options.copy(excludedNetworks=excluded))}){
                Text("Ağ filtresini kaydet")
            }
            var preferred by remember(options.preferredNetworks) {
                mutableStateOf(options.preferredNetworks)
            }
            OutlinedTextField(value=preferred,onValueChange={preferred=it.take(250)},
                label={Text("Tercih edilen ağlar (virgülle)")},modifier=Modifier.fillMaxWidth())
            OutlinedButton(onClick={persist(options.copy(preferredNetworks=preferred))}){
                Text("Ağ tercihlerini kaydet")
            }
            PlannerNumber("Tercih bonusu (0–5 dk, TAHMİN)",
                options.preferenceBonusMinutes) {
                persist(options.copy(preferenceBonusMinutes=it))
            }
            Text("Tercih bonusu yalnız rota sıralama ağırlığıdır; şarj süresini, " +
                "istasyonun DC/CCS2 desteğini veya doluluğunu doğrulamaz.")
            Text("Hedefte şarj bilgisi v1'de optimizasyon maliyetine dahil değil.")
            PlannerNumber("Hedefte şarj varsa kW (0 = yok)",
                options.destinationChargeKw ?: 0.0) {
                persist(options.copy(destinationChargeKw=it.takeIf{value->value>0}))
            }
            OutlinedButton(onClick={scope.launch{optionsStore.reset()}}) {
                Text("Plan ayarlarını varsayılana döndür")
            }
            optionsError?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }
        if(state.stage==PlannerStage.FAILURE && state.message.contains("anahtar",true)) {
            OutlinedButton(onClick=onOpenSettings) {Text("HeiGIT API anahtarı ayarlarını aç")}
        }
        if(state.plan is PlanResult.Failure) {
            val failed=state.plan as PlanResult.Failure
            Text("PLAN YOK: "+failed.reason, color=MaterialTheme.colorScheme.error)
            Text("En yakın tahmini ulaşılabilir nokta: "+
                ("%.1f km".format(failed.nearestReachableKm)))
        }
        if(success!=null) {
            OutlinedButton(onClick={showPlanMap=!showPlanMap}) {
                Text(if(showPlanMap)"Şarj haritasını gizle" else "Şarj duraklarını haritada göster")
            }
            if(showPlanMap) {
                OnlineMap(modifier=Modifier.fillMaxWidth().height(275.dp),
                    route=state.route,
                    destination=state.route?.geometry?.lastOrNull(),
                    gps=gps,onSelectDestination=onSelectDestination,
                    weatherSummary="İstasyon verisi TAHMİN • güncellik DOĞRULANMADI",
                    stopMarkers=success.stops.map{it.charger},
                    onStopSelected={highlightedStation=it},
                    focusedStop=highlightedStation)
                Text("Harita: yeşil OSM CCS2 etiketli / sarı olası / gri bilinmiyor. " +
                    "Renk, şarj cihazının çalıştığını veya doluluğunu doğrulamaz.")
                highlightedStation?.let {
                    Text("Seçilen istasyon: "+PlannerMarkerPolicy.label(it)+
                        " • "+it.powerKw.value.toInt()+" kW (TAHMİN) • doluluk bilinmiyor")
                }
            }
            val computedRoute=state.route
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Button(onClick={
                    if(computedRoute!=null)scope.launch {
                        savedStore.save(SavedPlanBuilder.from(
                            success,computedRoute,System.currentTimeMillis()))
                        saveStatus="Plan uygulama içinde kaydedildi (koordinatlar paylaşılmaz)."
                    }
                },enabled=computedRoute!=null){Text("Planı kaydet")}
                OutlinedButton(onClick={
                    if(computedRoute!=null) {
                        val plan=SavedPlanBuilder.from(success,computedRoute,0L)
                        val send=Intent(Intent.ACTION_SEND).apply {
                            type="text/plain"
                            putExtra(Intent.EXTRA_TEXT,SavedPlanBuilder.shareText(plan))
                        }
                        try {
                            context.startActivity(Intent.createChooser(send,"Plan özeti paylaş"))
                        } catch(_:Exception) {saveStatus="Paylaşım uygulaması açılamadı."}
                    }
                },enabled=computedRoute!=null){Text("Özeti paylaş")}
            }
            OutlinedButton(onClick={
                val dest=computedRoute?.geometry?.lastOrNull()
                if(dest!=null)onNavigate(PlanToNavigation.intermediateStops(success),dest)
            },enabled=computedRoute!=null){Text("Şarj duraklarıyla navigasyona ekle")}

            OutlinedButton(onClick={
                vm.replanFromCurrentGps(soc,options,profile)
            },enabled=!busy) {
                Text("Güncel GPS ve SoC ile yeniden planla")
            }
            Text("Yalnız açık dokunuşla yeni ORS rota isteği yapılır; güncel konum ve " +
                "manuel/OBD etiketli SoC kullanılır. Otomatik yeniden planlama YOK.",
                style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick=vm::verifySelectedStops,enabled=!busy) {
                Text("ORS ile duraklı rotayı kontrol et")
            }
            Text("Bu işlem 1–2 ek HeiGIT API isteği kullanabilir. Enerji farkı %2'yi aşarsa " +
                "en çok bir kez yeniden hesaplanır. Telefon/gerçek rota DOĞRULANMADI.",
                style=MaterialTheme.typography.bodySmall)
            state.verification?.let { checked ->
                val first=checked.firstDifferencePercent?.let{"%.2f%%".format(it)} ?: "ölçülemedi"
                val second=checked.finalDifferencePercent?.let{"%.2f%%".format(it)} ?: "ölçülemedi"
                Text("İlk enerji farkı: "+first+" • son fark: "+second+
                    " • yeniden plan: "+if(checked.replannedOnce)"EVET" else "HAYIR")
                Text(checked.warning)
                Text("İkinci geçiş saha kanıtı değildir — TAHMİN / DOĞRULANMADI")
            }

            if(saveStatus.isNotBlank())Text(saveStatus)
            Text(PlannerDisplay.overview(success),style=MaterialTheme.typography.titleMedium)
            Text("Tahmini varış enerji: "+
                ("%.1f kWh".format(success.arrivalSocPct/100.0*
                    (state.request?.car?.capacityAtSohKwh ?: 0.0))))
            Text("Fiziksel süre ve risk cezası ayrı: "+
                ("%.0f + %.0f dk".format(success.physicalMinutes,success.riskPenaltyMinutes)))
            Text("Canlı trafik, fiyat ve doluluk yok. Şarj eğrisi TAHMİN.")
            SocProfileChart(success,state.request?.startSocPct ?: 0.0,
                state.request?.minArrivalSoc ?: 10.0)
            CalibrationPanel(success,state.request)
            if(success.stops.isEmpty())Text("Menzil tahminine göre şarj durağı gerekmiyor.")
            success.stops.forEachIndexed { index,stop ->
                ElevatedCard(modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp),
                        verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text("Durak "+(index+1)+" • "+stop.charger.name,
                            fontWeight=FontWeight.Bold)
                        Text("Operatör: "+stop.charger.operator)
                        Text(PlannerDisplay.stop(stop))
                        Text("Kaynak: "+stop.charger.powerKw.source)
                        Text("Güven: "+stop.charger.confidence.name+
                            " • doluluk bilinmiyor • TAHMİN")
                        var stationEvidenceOpen by remember(stop.charger.id){
                            mutableStateOf(false)
                        }
                        OutlinedButton(onClick={stationEvidenceOpen=!stationEvidenceOpen}) {
                            Text(if(stationEvidenceOpen)"İstasyon veri ayrıntısını gizle"
                                else "İstasyon veri kaynağı / soket / erişim")
                        }
                        if(stationEvidenceOpen) {
                            ChargerEvidence.details(stop.charger).forEach {
                                Text(it,style=MaterialTheme.typography.bodySmall)
                            }
                        }
                        OutlinedButton(onClick={
                            highlightedStation=stop.charger
                            showPlanMap=true
                        }) {Text("Haritada gör")}
                        Text("Sapma ~"+("%.1f km".format(stop.charger.detourKm))+
                            " • "+("%.0f dk".format(stop.extraMinutes))+
                            " (gerçek yol DOĞRULANMADI)")
                        val backups=state.alternatives?.backups
                            ?.filter{it.originalStopId==stop.charger.id}.orEmpty()
                        if(backups.isEmpty())Text("Yedek istasyon: veri yok / DOĞRULANMADI")
                        else backups.forEach {
                            Text("Yedek istasyon (TAHMİN): "+it.candidate.name+
                                " • "+it.note)
                        }
                    }
                }
            }
            state.alternatives?.fewerStops?.let {
                Text("Daha az durak alternatifi: "+it.stops.size+" durak (TAHMİN)")
            }
            state.alternatives?.safer?.let {
                Text("Güvenli CCS2/+%5 SoC alternatifi: "+it.stops.size+" durak (TAHMİN)")
            }
            success.warnings.forEach { Text(it,style=MaterialTheme.typography.bodySmall) }
            Text("Harita, kayıt ve navigasyon özellikleri telefon üzerinde DOĞRULANMADI.",
                style=MaterialTheme.typography.bodySmall)
        }
        Text("Kaydedilmiş planlar (en çok 10, cihaz içinde)")
        if(savedPlans.isEmpty())Text("Henüz plan kaydı yok")
        savedPlans.forEach { saved->
            ElevatedCard(modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(saved.title+" • "+saved.stops.size+" durak • varış ~%"+
                        saved.arrivalSoc+" (TAHMİN)")
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick={selectedSaved=saved}){Text("Kaydı aç")}
                        OutlinedButton(onClick={
                            onNavigate(saved.stops.map{it.point},saved.destination)
                        }){Text("Navigasyona ekle")}
                        OutlinedButton(onClick={scope.launch{
                            savedStore.delete(saved.id)
                            if(selectedSaved?.id==saved.id)selectedSaved=null
                        }}){Text("Sil")}
                    }
                }
            }
        }
        selectedSaved?.let { Text(SavedPlanBuilder.shareText(it)) }
        Text("Kaydedilen varış/durak koordinatları yalnız uygulama özel DataStore alanındadır. " +
            "Paylaşım metni koordinat içermez.")
        Text("© OpenStreetMap katkıcıları / OpenFreeMap • © openrouteservice • Open-Meteo")
        Text("Sürüş öncesi kullanım. Sürüşte ekrana yoğun dokunmayın.",
            style=MaterialTheme.typography.bodySmall)
        } // Gelişmiş plan görünümü
    }
    if(dataFlow) {
        AlertDialog(onDismissRequest={dataFlow=false},
            title={Text("Veri akışı — kullanıcı isteğiyle")},
            text={
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("HeiGIT ORS: GPS başlangıç, hedef ve seçilmiş durak koordinatları; anahtar yalnız HTTPS Authorization başlığında.")
                    Text("Open-Meteo Elevation: örneklenmiş rota enlem/boylamları; plan isteğinde en çok 3 grup × 100 nokta.")
                    Text("OSM/Overpass: sadeleştirilmiş rota koridoru, 3 km yarıçap (ayar değişebilir); 7 gün önbellek.")
                    Text("Open-Meteo Forecast: rota isteğinde başlangıç/orta/hedef hava koordinatları.")
                    Text("Photon: yalnız Ara düğmesine basılan adres metni. En az 10 saniye aralık.")
                    Text("Kamu sunucularının kota/erişim ve güncellik garantisi yok. Gerçek ağ/telefon DOĞRULANMADI.")
                    Text("Veri akışı kaynağı: docs/M8_VERI_AKISI.md; OSM/OpenFreeMap/Open-Meteo/ORS atıfları.")
                }
            },
            confirmButton={TextButton(onClick={dataFlow=false}){Text("Kapat")}})
    }
}

@Composable
private fun PlannerNumber(label:String,value:Double,onSave:(Double)->Unit) {
    var entered by remember(value){mutableStateOf(value.toString())}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
        modifier=Modifier.fillMaxWidth()) {
        OutlinedTextField(value=entered,onValueChange={entered=it},label={Text(label)},
            singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
            modifier=Modifier.weight(1f))
        OutlinedButton(onClick={
            val parsed=entered.trim().replace(',','.').toDoubleOrNull()
            if(parsed!=null && parsed.isFinite())onSave(parsed)
        },modifier=Modifier.heightIn(min=48.dp)){
            Text("Kaydet")
        }
    }
}

@Composable
private fun SocProfileChart(plan:PlanResult.Success,startSoc:Double,minSoc:Double) {
    val distance=plan.distanceKm.coerceAtLeast(0.001)
    val p=mutableListOf(0.0 to startSoc)
    plan.stops.forEach { stop->
        val x=stop.charger.distanceAlongMeters/1000.0
        p+=x to stop.arrivalSocPct.toDouble()
        p+=x to stop.departureSocPct.toDouble()
    }
    p+=distance to plan.arrivalSocPct.toDouble()
    var selected by remember {mutableStateOf("")}
    Text("SoC profili — çizgi ve alt sınır (TAHMİN)")
    Canvas(Modifier.fillMaxWidth().height(164.dp)
        .semantics { contentDescription="Mesafeye göre tahmini SoC grafiği" }
        .pointerInput(p) {
            detectTapGestures { offset ->
                val x=(offset.x/size.width.toFloat()).coerceIn(0f,1f)*distance
                val closest=p.minByOrNull {kotlin.math.abs(it.first-x)}
                selected="Yaklaşık "+("%.1f km".format(x))+" • SoC "+
                    (closest?.second?.roundToInt()?.toString() ?:"--")+"% (TAHMİN)"
            }
        }) {
        fun point(item:Pair<Double,Double>)=Offset(
            (item.first/distance*size.width).toFloat().coerceIn(0f,size.width),
            (size.height*(1-item.second.coerceIn(0.0,100.0)/100.0)).toFloat())
        val refY=(size.height*(1-minSoc.coerceIn(0.0,100.0)/100.0)).toFloat()
        drawLine(Color.Red.copy(alpha=0.75f),
            Offset(0f,refY),Offset(size.width,refY),strokeWidth=2.dp.toPx())
        for((a,b) in p.zipWithNext()) {
            drawLine(Color(0xFF30CFAA),point(a),point(b),strokeWidth=3.dp.toPx())
        }
        val last=p.last()
        val optimistic=last.first to plan.optimisticArrivalSocPct
        val pessimistic=last.first to plan.pessimisticArrivalSocPct
        drawLine(Color(0xFF83B8FF),point(pessimistic),point(optimistic),
            strokeWidth=5.dp.toPx())
    }
    if(selected.isNotBlank())Text(selected)
    Text("Mavi varış aralığı: model ±%10/+%15 varsayımı; istatistiksel güven aralığı değildir.",
        style=MaterialTheme.typography.bodySmall)
}
```

---

## 12. `app/src/test/java/app/evcockpit/charging/OcmAndTariffTest.kt`

```kotlin
package app.evcockpit.charging

import app.evcockpit.charging.ocm.*
import app.evcockpit.charging.tariffs.*
import app.evcockpit.navigation.GeoPoint
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class OcmAndTariffTest {
    private val sample = """
        [{
            "ID": 18945,
            "AddressInfo": { "Title": "Örnek Şarj", "Latitude": 37.969,
                "Longitude": 34.671 },
            "OperatorInfo": { "Title": "ZES" },
            "StatusType": { "Title": "Operational" },
            "Connections": [
                { "ConnectionType": {"ID":33,"Title":"CCS (Type 2)"},
                  "CurrentType":{"Title":"DC"},
                  "PowerKW":180.0 },
                { "ConnectionType":{"ID":25,"Title":"Type 2"},
                  "CurrentType":{"Title":"AC"},"PowerKW":22.0 }
            ]
        },{
            "ID": 18946,
            "AddressInfo":{"Title":"Gücü Bilinmeyen","Latitude":38.0,"Longitude":34.6},
            "OperatorInfo":{"Title":"Bilinmiyor"},
            "Connections":[{"ConnectionType":{"Title":"Type 2"}}]
        }]
    """.trimIndent()

    @Test fun ocmKonektorGucuVeTurleriUydurulmaz() {
        val list = OcmParser.parse(sample)
        assertEquals(2, list.size)
        assertTrue(list[0].hasCcs2)
        assertTrue(list[0].hasDc)
        assertTrue(list[0].hasAc)
        assertEquals(180.0, list[0].powerKw!!, 0.001)
        assertEquals("ULTRA DC", list[0].powerGroup)
        assertNull(list[1].powerKw)
        assertEquals("GÜÇ BİLİNMİYOR", list[1].powerGroup)
    }

    @Test fun bolgeDisiKayitVeOlmayanRotaSahteSonucaDonusmez() {
        assertTrue(OcmParser.parse("""[{"ID":99,"AddressInfo":
            {"Title":"Test","Latitude":52.0,"Longitude":13.0}}]""").isEmpty())
        val route = listOf(GeoPoint(37.96,34.67),GeoPoint(37.9,34.8))
        val sampleStation = OcmParser.parse(sample)[0]
        assertNotNull(OcmCorridor.project(sampleStation, route))
        assertNull(OcmCorridor.project(sampleStation.copy(
            point = GeoPoint(40.0, 30.0)), route))
    }

    @Test fun uzunRotaOrnekleriSinirlanirVeEksikBayragiVerilir() {
        val geometry = (0..50).map { GeoPoint(36.5 + it * 0.06, 29.5 + it * 0.2) }
        val plan = OcmCorridor.sample(geometry)
        assertTrue(plan.points.size <= OcmCorridor.MAX_CALLS)
        assertTrue(plan.incomplete)
        assertEquals(geometry.first(), plan.points.first())
        assertEquals(geometry.last(), plan.points.last())
    }

    private val json = """
        {"schema_version":1,"operators":[
          {"id":"zes","operator":"ZES","aliases":["zes"],
           "ac_tl_kwh":null,"dc_tl_kwh":null,"updated_at":null,"source_url":null},
          {"id":"trugo","operator":"Trugo","aliases":["trugo"],
           "ac_tl_kwh":9.25,"dc_tl_kwh":11.50,"updated_at":"2026-11-21",
           "source_url":"https://example.org/price"}
        ]}
    """.trimIndent()

    @Test fun katalogFiyatBosIseAslaSifirFiyatDemez() {
        val catalog = TariffCatalogCodec.parse(json)
        val unknown = TariffResolver.resolve(catalog, emptyMap(), "ZES",
            ChargingKind.DC, LocalDate.parse("2026-11-23"))
        assertNull(unknown.priceTlPerKwh)
        assertEquals("Fiyat bilinmiyor", unknown.source)
        assertNull(TariffResolver.resolve(catalog, emptyMap(), "Trugo",
            ChargingKind.UNKNOWN).priceTlPerKwh)
    }

    @Test fun manuelAcDcAyrilirVeEskiFiyatUyariVerir() {
        val catalog = TariffCatalogCodec.parse(json)
        val manual = mapOf("zes" to ManualPrice(ac = 7.25, dc = 12.0,
            updatedAt = "2026-11-01"))
        val ac = TariffResolver.resolve(catalog, manual, "ZES", ChargingKind.AC,
            LocalDate.parse("2026-11-23"))
        val dc = TariffResolver.resolve(catalog, manual, "ZES", ChargingKind.DC,
            LocalDate.parse("2026-11-23"))
        assertEquals(7.25, ac.priceTlPerKwh!!, 0.01)
        assertEquals(12.0, dc.priceTlPerKwh!!, 0.01)
        assertTrue(dc.stale)
    }

    @Test fun fiyatIcinKaynakVeTarihSarttir() {
        val invalid = json.replace("\"updated_at\":\"2026-11-21\"",
            "\"updated_at\":null")
        assertThrows(IllegalArgumentException::class.java) {
            TariffCatalogCodec.parse(invalid)
        }
    }
}
```
