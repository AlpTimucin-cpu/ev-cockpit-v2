# A0 0.7.0 — TAM KAYNAKLAR / BÖLÜM 2 (7 dosya)

**Dal:** `feature/a0-f1-product-main-20261124`  
**Bölüm 2:** Üretim veri modeli + gerçek başlangıç ViewModel'i; eski sürüş politikası uyumluluk tipi; çeviriye hazır yeni kaynaklar; **yalnız debug** DEMO sağlayıcısı ve Compose Preview'leri. Her dosyanın tam içeriği, satır atlanmadan verilmiştir.

Diğerleri: [Bölüm 1](./A0_070_TAM_KAYNAKLAR_BOLUM_1.md) · [Bölüm 3](./A0_070_TAM_KAYNAKLAR_BOLUM_3.md).


---

## 1. `app/src/main/java/app/evcockpit/ui/finalhmi/MapFirstDashboardViewModel.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A0: İlk açılış GERÇEK boş durumdur, sahte telemetri gösterilmez.
 * Demo yalnız debug kaynak setindeki ayrı sağlayıcıdan, kullanıcı açarsa gelir.
 */
class MapFirstDashboardViewModel : ViewModel() {
    private val repository = DashboardRepository()
    private val mutable = MutableStateFlow(
        RealDashboardMapper.map(repository.realInputs.value, System.currentTimeMillis()))
    val state: StateFlow<DashboardUiState> = mutable
    val demoAvailable: Boolean = BuildTypeDemoProvider.available
    private var showDemo: Boolean = false

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(15_000L)
            }
        }
    }

    fun showDemo(enabled: Boolean) {
        showDemo = demoAvailable && enabled
        refresh()
    }

    private fun refresh() {
        val now = System.currentTimeMillis()
        mutable.value = if (showDemo) {
            BuildTypeDemoProvider.state(now)
                ?: RealDashboardMapper.map(repository.realInputs.value, now)
        } else {
            RealDashboardMapper.map(repository.realInputs.value, now)
        }
    }
}
```

---

## 2. `app/src/main/java/app/evcockpit/ui/finalhmi/DashboardUiState.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** DEĞER + KAYNAK + ZAMAN. Null asla sıfır sayılmaz. */
enum class DashboardOrigin {
    NONE, SYSTEM, GPS, OPEN_METEO, OBD_VERIFIED, MANUAL, MODEL, DEMO,
}

data class StampedValue<T>(
    val value: T? = null,
    val source: DashboardOrigin = DashboardOrigin.NONE,
    val timestampEpochMs: Long? = null,
) {
    fun freshValue(nowEpochMs: Long, maxAgeMs: Long): T? {
        val time = timestampEpochMs ?: return null
        if (source == DashboardOrigin.NONE || value == null || maxAgeMs < 0L) return null
        val age = nowEpochMs - time
        return if (age in 0L..maxAgeMs) value else null
    }
}

data class RouteWeatherSample(
    val distanceAlongKm: Double,
    val temperatureC: StampedValue<Double> = StampedValue(),
    val rainExpected: StampedValue<Boolean> = StampedValue(),
)

data class NextChargeUiState(
    val stationName: StampedValue<String> = StampedValue(),
    val distanceKm: StampedValue<Double> = StampedValue(),
    val chargingMinutes: StampedValue<Double> = StampedValue(),
    val targetSocPercent: StampedValue<Double> = StampedValue(),
    val costLabel: StampedValue<String> = StampedValue(),
)

/** Bütün alanlar bilinmiyor olabilir; boş durum normal ve güvenlidir. */
data class DashboardUiState(
    val demo: Boolean = false,
    val clock: StampedValue<String> = StampedValue(),
    val outdoorTemperatureC: StampedValue<Double> = StampedValue(),
    val speedKmh: StampedValue<Double> = StampedValue(),
    val socPercent: StampedValue<Double> = StampedValue(),
    val estimatedRangeKm: StampedValue<Double> = StampedValue(),
    val consumptionKwhPer100Km: StampedValue<Double> = StampedValue(),
    val powerKw: StampedValue<Double> = StampedValue(),
    val arrivalMinutes: StampedValue<Double> = StampedValue(),
    val remainingDistanceKm: StampedValue<Double> = StampedValue(),
    val arrivalSocPercent: StampedValue<Double> = StampedValue(),
    val chargeTotalLabel: StampedValue<String> = StampedValue(),
    val maneuverText: StampedValue<String> = StampedValue(),
    val maneuverDistanceKm: StampedValue<Double> = StampedValue(),
    val weatherSamples: List<RouteWeatherSample> = emptyList(),
    val nextCharge: NextChargeUiState = NextChargeUiState(),
)

/**
 * Saf hesaplar: kapasite parametre olarak gelir; 52 kWh burada KESİNLİKLE sabitlenmez.
 * Hesap olumsuz/gerçekdışı sonuç verirse gösterilecek değer null olur.
 */
object DashboardMath {
    fun estimatedSocPercent(
        startSocPercent: Double?,
        modelEnergySpentKwh: Double?,
        usableCapacityKwh: Double?,
    ): Double? {
        val start = startSocPercent ?: return null
        val spent = modelEnergySpentKwh ?: return null
        val capacity = usableCapacityKwh ?: return null
        if (!start.isFinite() || start !in 0.0..100.0 ||
            !spent.isFinite() || spent < 0.0 ||
            !capacity.isFinite() || capacity <= 0.0) return null
        val result = start - 100.0 * spent / capacity
        return result.takeIf { it.isFinite() && it in 0.0..100.0 }
    }

    fun estimatedRangeKm(
        socPercent: Double?,
        usableCapacityKwh: Double?,
        consumptionKwhPer100Km: Double?,
    ): Double? {
        val soc = socPercent ?: return null
        val capacity = usableCapacityKwh ?: return null
        val consumption = consumptionKwhPer100Km ?: return null
        if (!soc.isFinite() || soc !in 0.0..100.0 ||
            !capacity.isFinite() || capacity <= 0.0 ||
            !consumption.isFinite() || consumption <= 0.0) return null
        val range = (soc / 100.0 * capacity) / consumption * 100.0
        return range.takeIf { it.isFinite() && it >= 0.0 }
    }

    fun speedKmhFromGpsMetersPerSecond(metersPerSecond: Double?): Double? =
        metersPerSecond?.takeIf { it.isFinite() && it in 0.0..83.333333 }
            ?.times(3.6)
}

data class PricedStop(val priceTl: Double?)

/** Bilinmeyen bedel 0 TL değil. Kısmi toplam, kesin toplam gibi gösterilmez. */
object ChargeCostSummary {
    fun label(stops: List<PricedStop>): String? {
        if (stops.isEmpty()) return null
        if (stops.any { it.priceTl != null && (!it.priceTl.isFinite() || it.priceTl < 0.0) })
            return null
        val known = stops.mapNotNull { it.priceTl }
        if (known.isEmpty()) return null
        val amount = DashboardFormat.oneDecimal(known.sum())
        val missing = stops.size - known.size
        return if (missing == 0) "$amount TL (tahmini)" else
            "en az $amount TL ($missing durakta fiyat yok)"
    }
}

/** Sadece görünüm biçimlemesi, sayısal ölçüm üretmez. */
object DashboardFormat {
    private val locale = Locale.forLanguageTag("tr-TR")
    fun oneDecimal(value: Double): String =
        String.format(locale, "%.1f", value)
    fun whole(value: Double): String =
        String.format(locale, "%.0f", value)
    fun number(value: StampedValue<Double>, suffix: String = ""): String =
        value.value?.takeIf { it.isFinite() }?.let { whole(it) + suffix }
            ?: ("—" + suffix)
    fun time(nowEpochMs: Long): String =
        DateTimeFormatter.ofPattern("HH:mm", locale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(nowEpochMs))
}

/** F1 gerçek mod girdileri BOŞ; F3–F5'te repository'lerden doldurulacak. */
data class RealDashboardInput(
    val gpsSpeedMetersPerSecond: StampedValue<Double> = StampedValue(),
    val meteoTemperatureC: StampedValue<Double> = StampedValue(),
    val obdVerifiedSocPercent: StampedValue<Double> = StampedValue(),
    val obdVerifiedPowerKw: StampedValue<Double> = StampedValue(),
    val manualStartSocPercent: StampedValue<Double> = StampedValue(),
    val modelEnergySpentKwh: StampedValue<Double> = StampedValue(),
    val modelConsumptionKwhPer100Km: StampedValue<Double> = StampedValue(),
    val vehicleUsableCapacityKwh: StampedValue<Double> = StampedValue(),
    val routeProgressReliable: Boolean = false,
)

/** DEMO verisini kesinlikle kabul ETMEYEN gerçek veri eşlemesi. */
object RealDashboardMapper {
    private const val GPS_MAX_AGE = 10_000L
    private const val WEATHER_MAX_AGE = 60L * 60L * 1000L
    private const val OBD_MAX_AGE = 5_000L
    private const val MODEL_MAX_AGE = 60_000L
    private const val MANUAL_MAX_AGE = 90L * 24L * 60L * 60L * 1000L

    private fun StampedValue<Double>.valueFrom(
        origin: DashboardOrigin, now: Long, ttl: Long,
    ): Double? = freshValue(now, ttl)?.takeIf { source == origin && it.isFinite() }

    fun map(data: RealDashboardInput, nowEpochMs: Long): DashboardUiState {
        val speed = data.gpsSpeedMetersPerSecond.valueFrom(
            DashboardOrigin.GPS, nowEpochMs, GPS_MAX_AGE)
        val meteo = data.meteoTemperatureC.valueFrom(
            DashboardOrigin.OPEN_METEO, nowEpochMs, WEATHER_MAX_AGE)
        val obd = data.obdVerifiedSocPercent.valueFrom(
            DashboardOrigin.OBD_VERIFIED, nowEpochMs, OBD_MAX_AGE)
            ?.takeIf { it in 0.0..100.0 }
        val manual = data.manualStartSocPercent.valueFrom(
            DashboardOrigin.MANUAL, nowEpochMs, MANUAL_MAX_AGE)
            ?.takeIf { it in 0.0..100.0 }
        val consumed = data.modelEnergySpentKwh.valueFrom(
            DashboardOrigin.MODEL, nowEpochMs, MODEL_MAX_AGE)
            ?.takeIf { it >= 0.0 }
        val consumption = data.modelConsumptionKwhPer100Km.valueFrom(
            DashboardOrigin.MODEL, nowEpochMs, MODEL_MAX_AGE)
            ?.takeIf { it > 0.0 }
        // Kapasite her zaman aktif araç profilinden; F1'de profil veri akışı bağlı değil.
        val capacity = data.vehicleUsableCapacityKwh.freshValue(
            nowEpochMs, MANUAL_MAX_AGE)?.takeIf {
                data.vehicleUsableCapacityKwh.source in setOf(
                    DashboardOrigin.MODEL, DashboardOrigin.MANUAL) &&
                    it.isFinite() && it > 0.0
            }
        val estimatedSoc = if (obd == null && manual != null &&
            data.routeProgressReliable && consumed != null)
            DashboardMath.estimatedSocPercent(manual, consumed, capacity) else null
        val usableSoc = obd ?: estimatedSoc ?: manual
        val socSource = when {
            obd != null -> DashboardOrigin.OBD_VERIFIED
            estimatedSoc != null -> DashboardOrigin.MODEL
            manual != null -> DashboardOrigin.MANUAL
            else -> DashboardOrigin.NONE
        }
        fun <T> value(raw: T?, source: DashboardOrigin): StampedValue<T> =
            if (raw == null) StampedValue() else StampedValue(raw, source, nowEpochMs)
        return DashboardUiState(
            clock = StampedValue(DashboardFormat.time(nowEpochMs),
                DashboardOrigin.SYSTEM, nowEpochMs),
            speedKmh = value(DashboardMath.speedKmhFromGpsMetersPerSecond(speed),
                DashboardOrigin.GPS),
            outdoorTemperatureC = value(meteo, DashboardOrigin.OPEN_METEO),
            socPercent = value(usableSoc, socSource),
            estimatedRangeKm = value(
                DashboardMath.estimatedRangeKm(usableSoc, capacity, consumption),
                DashboardOrigin.MODEL),
            consumptionKwhPer100Km = value(consumption, DashboardOrigin.MODEL),
            powerKw = value(data.obdVerifiedPowerKw.valueFrom(
                DashboardOrigin.OBD_VERIFIED, nowEpochMs, OBD_MAX_AGE),
                DashboardOrigin.OBD_VERIFIED),
        )
    }
}
```

---

## 3. `app/src/main/java/app/evcockpit/ui/tesla/TeslaLayoutPolicy.kt`

```kotlin
package app.evcockpit.ui.tesla

/**
 * Eski kokpitin yalnız JVM testlerinde kullanılan pencere sınırı.
 * Eski arayüz/eylemler A0 ile silinmiştir; ortak test kuralı korunur.
 */
/**
 * Yalnız ortak sürüş güvenliği politikasının eski tip uyumluluğu için.
 * A0 ana ekranında bu eylemler veya Mola/medya sekmesi gösterilmez.
 */
enum class TeslaAction(val title: String, val symbol: String) {
    ROTA("Rota", "⌖"),
    PLAN("Plan", "⚡"),
    MEDYA("Medya", "♫"),
    VIDEO("Yolcu video", "▶"),
    MOLA("Mola", "M"),
    AYARLAR("Ayarlar", "⚙"),
}

object TeslaLayoutPolicy {
    fun useThreePanes(widthDp: Int, heightDp: Int): Boolean =
        widthDp >= 700 && widthDp > heightDp
}
```

---

## 4. `app/src/main/res/values/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="a0_connections">Bağlantılar</string>
    <string name="a0_connections_desc">GPS ve Bluetooth bağlantı tanısı. İzinler yalnız isteğinle açılır.</string>
    <string name="a0_vehicle">Araç profili</string>
    <string name="a0_vehicle_desc">VW ID.4 Pure • 52 kWh kullanılabilir batarya • varsayımlar ve kullanıcı gözlemleri</string>
    <string name="a0_map">Harita</string>
    <string name="a0_map_desc">Harita katmanları ile kullanıcıya ait API anahtarları</string>
    <string name="a0_units">Birimler ve ekran</string>
    <string name="a0_units_desc">Mesafe km, hız km/sa, tüketim kWh/100 km, para TL</string>
    <string name="a0_about">Hakkında / Gizlilik</string>
    <string name="a0_about_desc">Kaynaklar, güncelleme ve cihazdaki veri koruması</string>
    <string name="a0_obd_open">OBD bağlantı tanısı ve günlükler</string>
    <string name="a0_obd_note">Ucuz ELM327 adaptörlerin bağlanması, batarya PID değerlerinin doğrulandığı anlamına gelmez.</string>
    <string name="a0_gps_status">GPS durumu: %1$s</string>
    <string name="a0_gps_accuracy">Konum hassasiyeti: %1$s m • %2$s</string>
    <string name="a0_location_unknown">—</string>
    <string name="a0_provider_unknown">Sağlayıcı bilinmiyor</string>
    <string name="a0_gps_start">GPS takibini başlat</string>
    <string name="a0_gps_stop">GPS takibini durdur</string>
    <string name="a0_gps_permission">GPS izni yalnız başlatma eyleminde istenir. Kullanım sırasında konum servisi bildirim gösterir.</string>
    <string name="a0_compat_header">Telefon ve ROM uyumluluğu</string>
    <string name="a0_compat_refresh">İzin / sistem durumunu yenile</string>
    <string name="a0_compat_copy">Güvenli uyumluluk özetini kopyala</string>
    <string name="a0_compat_clipboard_name">EV Cockpit güvenli uyumluluk özeti</string>
    <string name="a0_compat_copy_ok">Konum veya cihaz kimliği içermeyen özet panoya kopyalandı.</string>
    <string name="a0_compat_copy_fail">Pano kullanılamıyor.</string>
    <string name="a0_gps_precise">Hassas konum izni bulunmuyor; konumdan rota ve takip kısıtlı olabilir.</string>
    <string name="a0_battery_settings">Pil optimizasyonu ayarlarını aç</string>
    <string name="a0_autostart_settings">Otomatik başlatma / uygulama ayarlarını aç</string>
    <string name="a0_battery_opened">Pil optimizasyonu sistem sayfası açılması istendi.</string>
    <string name="a0_battery_error">Pil optimizasyonu ayarları açılamadı.</string>
    <string name="a0_autostart_opened">Uygulama sistemi ayarları açılması istendi.</string>
    <string name="a0_autostart_error">Uygulama sistemi ayarları açılamadı.</string>
    <string name="a0_sys_settings_note">Bu düğmeler sistem ayarını otomatik değiştirmez. Arka planda kesintisiz çalışma doğrulanmadı.</string>
    <string name="a0_dc_default">İhtiyatlı model DC varsayımı: %1$d kW</string>
    <string name="a0_dc_observed">Kayıtlı DC ekran gözlemi: %1$s</string>
    <string name="a0_none">yok</string>
    <string name="a0_dc_label">Araç ekranında görülen azami DC şarj gücü (kW)</string>
    <string name="a0_dc_save">Gözlemi kaydet</string>
    <string name="a0_dc_clear">Gözlemi sil</string>
    <string name="a0_dc_reset">Varsayılan araç profiline dön</string>
    <string name="a0_dc_invalid">Geçerli gözlem aralığı 1–350 kW olmalı.</string>
    <string name="a0_dc_note">Gözlem, hesaplanan şarj eğrisini ölçekler; garantili şarj hızı değildir. 125 kW araç tahrik gücüdür.</string>
    <string name="a0_manual_soc">Manuel batarya SoC</string>
    <string name="a0_manual_current">Kayıtlı manuel SoC: %1$s</string>
    <string name="a0_soc_label">Araç ekranındaki SoC (0–100)</string>
    <string name="a0_soc_save">Manuel SoC değerini kaydet</string>
    <string name="a0_soc_clear">Manuel SoC değerini sil</string>
    <string name="a0_soc_invalid">0–100 arasında geçerli bir SoC gir.</string>
    <string name="a0_soc_note">Bu değer araç ekranından kullanıcı girişi olarak saklanır; doğrulanmış OBD ölçümü değildir.</string>
    <string name="a0_layers_note">Sokak/koyu katmanlar ücretsiz; uydu/arazi için kişisel MapTiler anahtarı gerekir.</string>
    <string name="a0_maptiler_current">MapTiler anahtarı: %1$s</string>
    <string name="a0_key_saved">cihaza şifreli kaydedildi</string>
    <string name="a0_key_absent">kayıtlı değil</string>
    <string name="a0_maptiler_label">Kendi MapTiler API anahtarın</string>
    <string name="a0_maptiler_save">MapTiler anahtarını kaydet</string>
    <string name="a0_maptiler_clear">MapTiler anahtarını sil</string>
    <string name="a0_maptiler_saved">MapTiler anahtarı cihazda şifreli saklandı.</string>
    <string name="a0_maptiler_save_failed">MapTiler anahtarı kaydedilemedi.</string>
    <string name="a0_maptiler_cleared">MapTiler anahtarı silindi.</string>
    <string name="a0_maptiler_clear_failed">MapTiler anahtarı silinemedi.</string>
    <string name="a0_maptiler_note">MapTiler anahtarı karo isteklerinde sağlayıcıya gönderilir. Kota ve kullanım hakları kendi hesabına bağlıdır.</string>
    <string name="a0_ors_name">Rota sağlayıcısı: openrouteservice / HeiGIT</string>
    <string name="a0_ors_current">HeiGIT anahtarı: %1$s</string>
    <string name="a0_ors_web">HeiGIT hesap sayfasını aç</string>
    <string name="a0_ors_key_label">Kendi HeiGIT API anahtarın</string>
    <string name="a0_ors_save">HeiGIT anahtarını şifreli kaydet</string>
    <string name="a0_ors_clear">HeiGIT anahtarını sil</string>
    <string name="a0_ors_saved">HeiGIT anahtarı şifreli saklandı.</string>
    <string name="a0_ors_save_failed">HeiGIT anahtarı kaydedilemedi.</string>
    <string name="a0_ors_cleared">HeiGIT anahtarı silindi.</string>
    <string name="a0_ors_clear_failed">HeiGIT anahtarı silinemedi.</string>
    <string name="a0_ors_web_failed">HeiGIT hesap sayfası açılamadı.</string>
    <string name="a0_ors_note">Anahtar doğruluğu yalnız gerçek servis yanıtıyla bilinir. Kota ve üçüncü taraf bağlantı koşulları geçerlidir.</string>
    <string name="a0_keep_screen">Ekranı açık tut</string>
    <string name="a0_keep_screen_desc">Yalnız uygulama ön plandayken geçerlidir.</string>
    <string name="a0_theme_pref">Koyu görünüm tercihi</string>
    <string name="a0_theme_pref_desc">F1 tasarımı bu sürümde koyu sabit. Önceki tema tercihi kaydedilmeye devam eder.</string>
    <string name="a0_units_speed">Hız • km/sa</string>
    <string name="a0_units_energy">Tüketim • kWh/100 km</string>
    <string name="a0_units_cost">Şarj maliyeti • TL ve TL/kWh</string>
    <string name="a0_privacy">API anahtarları Android Keystore ile korunur; konum, günlük ve izinleri yönetebilirsin.</string>
    <string name="a0_privacy_note">Uygulama çevrim içi rota/harita isteklerini ilgili servis sağlayıcılarına gönderebilir. Play Store gizlilik belgesi A7 aşamasında hazırlanacak.</string>
    <string name="a0_credit">Harita: © OpenStreetMap katılımcıları, OpenFreeMap / MapLibre. Sağlayıcı atıfları haritada görünür.</string>
    <string name="a0_nightly">Nightly sürüm kontrolü</string>
    <string name="a0_a0_state">A0: F1 ana ekranı gerçek veri bulunmadığında — gösterir. OBD/hava/şarj bağlama sonraki aşamalardadır.</string>
    <string name="a0_data_unavailable">Bu alan henüz gerçek veri kaynağına bağlanmadı.</string>
    <string name="a0_no_diagnostic">Tanı ekranı kullanılamıyor.</string>
    <string name="a0_energy_heading">Enerji</string>
    <string name="a0_energy_empty">Henüz doğrulanmış yolculuk enerji kaydı yok.</string>
    <string name="a0_energy_note">Mevcut enerji hesabı Şarj sekmesindeki gelişmiş planlayıcıda kullanılabilir.</string>
    <string name="a0_demo_on">Demo modu: açık</string>
    <string name="a0_demo_off">Demo modu: kapalı</string>
</resources>
```

---

## 5. `app/src/debug/java/app/evcockpit/ui/finalhmi/DemoDashboardMapper.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import app.evcockpit.planner.model.VehicleProfile

/** Debug kaynak kümesinde. Release APK içinde bu örnek veriler derlenmez. */
/**
 * YALNIZCA ayrı önizleme moduna ait örnekler.
 * RealDashboardMapper bunu çağırmaz, ortak bir depo/OBD durumunu değiştirmez.
 */
object DemoDashboardMapper {
    fun map(nowEpochMs: Long): DashboardUiState {
        fun <T> demo(value: T): StampedValue<T> =
            StampedValue(value, DashboardOrigin.DEMO, nowEpochMs)
        // Demo bile mevcut tek araç profilinin varsayılanını kullanır, kapasiteyi sabitlemez.
        val capacity = VehicleProfile.defaults().capacityAtSohKwh
        val soc = 74.0
        val consumption = 17.2
        return DashboardUiState(
            demo = true,
            clock = StampedValue(DashboardFormat.time(nowEpochMs),
                DashboardOrigin.SYSTEM, nowEpochMs),
            outdoorTemperatureC = demo(18.0),
            speedKmh = demo(83.0),
            socPercent = demo(soc),
            estimatedRangeKm = DashboardMath.estimatedRangeKm(soc, capacity, consumption)
                ?.let(::demo) ?: StampedValue(),
            consumptionKwhPer100Km = demo(consumption),
            powerKw = demo(21.0),
            arrivalMinutes = demo(88.0),
            remainingDistanceKm = demo(110.0),
            arrivalSocPercent = demo(31.0),
            chargeTotalLabel = demo("en az 124,0 TL (1 durakta fiyat yok)"),
            weatherSamples = listOf(
                RouteWeatherSample(25.0, demo(17.0), demo(false)),
                RouteWeatherSample(50.0, demo(15.0), demo(true)),
                RouteWeatherSample(75.0, demo(13.0), demo(false)),
            ),
            nextCharge = NextChargeUiState(
                stationName = demo("Örnek DC istasyonu"),
                distanceKm = demo(48.0),
                chargingMinutes = demo(24.0),
                targetSocPercent = demo(75.0),
                costLabel = demo("124,0 TL (örnek, kısmi)"),
            ),
        )
    }
}
```

---

## 6. `app/src/debug/java/app/evcockpit/ui/finalhmi/BuildTypeDemoProvider.kt`

```kotlin
package app.evcockpit.ui.finalhmi

/** Debug'da demo kullanıcı tarafından AÇIKÇA açılana kadar kapalıdır. */
internal object BuildTypeDemoProvider {
    const val available = true
    fun state(nowEpochMs: Long): DashboardUiState = DemoDashboardMapper.map(nowEpochMs)
}
```

---

## 7. `app/src/debug/java/app/evcockpit/ui/finalhmi/MapFirstDashboardPreview.kt`

```kotlin
package app.evcockpit.ui.finalhmi

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Üç farklı dp pencere. Bunlar fiziksel cihaz ölçümü değildir.
 * Preview'de MapLibre native View açılmaz: sahte rota gösterilmez.
 */
@Preview(
    name = "F1 960x540 yatay",
    widthDp = 960, heightDp = 540,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Yatay960() {
    F1DemoPreview()
}

@Preview(
    name = "F1 1280x800 geniş yatay",
    widthDp = 1280, heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Yatay1280() {
    F1DemoPreview()
}

@Preview(
    name = "F1 393x873 POCO F5 dikey düzen",
    widthDp = 393, heightDp = 873,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Dikey393() {
    F1DemoPreview()
}

/**
 * G1/G4/G5 için sınır dp genişlikleri ve büyütülmüş sistem yazısı.
 * Bunlar IDE Compose önizlemeleridir, cihazda ekran görüntüsü testi değildir.
 */
@Preview(
    name = "F1 G1 360x800 dar telefon",
    widthDp = 360, heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Dikey360() {
    F1DemoPreview()
}

@Preview(
    name = "F1 G1 411x891 geniş telefon",
    widthDp = 411, heightDp = 891,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Dikey411() {
    F1DemoPreview()
}

@Preview(
    name = "F1 G4 fontscale 1.3 360x800",
    widthDp = 360, heightDp = 800,
    fontScale = 1.3f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1Dikey360BuyukYazi() {
    F1DemoPreview()
}

/**
 * H1: yalnız hava şeridi. Harita veya cihaz verisi kullanılmaz.
 * 360dp, fontScale 1.3: üç örnek eşit genişlikte çok satırlı.
 */
@Preview(
    name = "F1 H1 hava 3 örnek 360dp / yazı 1.3",
    widthDp = 360, heightDp = 250,
    fontScale = 1.3f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1HavaUcOrnekBuyukYazi() {
    FinalCockpitTheme {
        RouteWeatherStrip(
            state = DemoDashboardMapper.map(1_795_460_000_000L),
        )
    }
}

/**
 * H1: beş örnek; 393dp telefonda kaydırma ve kenar solması görünür.
 * Ek örnekler yalnız DEMO mappper'dan gelir, canlı hava verisi değildir.
 */
@Preview(
    name = "F1 H1 hava 5 örnek 393dp / kaydır",
    widthDp = 393, heightDp = 250,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun F1HavaBesOrnekKaydir() {
    val demo = DemoDashboardMapper.map(1_795_460_000_000L)
    val samples = demo.weatherSamples
    FinalCockpitTheme {
        RouteWeatherStrip(
            state = demo.copy(
                weatherSamples = samples + listOf(
                    samples[0].copy(distanceAlongKm = 100.0),
                    samples[1].copy(distanceAlongKm = 125.0),
                ),
            ),
        )
    }
}

@Composable
private fun F1DemoPreview() {
    FinalCockpitTheme {
        MapFirstDashboardScreen(
            state = DemoDashboardMapper.map(1_795_460_000_000L),
            onDemoChange = {},
        )
    }
}
```
