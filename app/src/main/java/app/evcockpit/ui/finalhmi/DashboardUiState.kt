package app.evcockpit.ui.finalhmi

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** DEĞER + KAYNAK + ZAMAN. Null asla sıfır sayılmaz. */
enum class DashboardOrigin {
    NONE, SYSTEM, GPS, OPEN_METEO, OBD_VERIFIED, OBD_UNVERIFIED, MANUAL, MODEL, DEMO,
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
