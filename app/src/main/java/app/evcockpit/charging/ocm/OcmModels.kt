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
