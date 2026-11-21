package app.evcockpit.planner.charging

import app.evcockpit.planner.model.SourcedValue

enum class ChargerConfidence { OSM_TAGGED_CCS2, PROBABLE_CCS2, UNKNOWN }

data class ChargerCandidate(
    val id: String, val name: String, val operator: String,
    val latitude: Double, val longitude: Double,
    val powerKw: SourcedValue, val confidence: ChargerConfidence,
    val source: String = "OSM/Overpass — güncellik DOĞRULANMADI",
    val access: String = "bilinmiyor",
    val fee: String = "bilinmiyor",
    val openingHours: String = "bilinmiyor",
    val authentication: String = "bilinmiyor",
    val capacity: Int? = null,
    val chademo: String = "bilinmiyor",
    val type2: String = "bilinmiyor",
    val distanceAlongMeters: Double = 0.0,
    val detourKm: Double = 0.0,
    val detourMinutes: Double = 0.0,
) {
    init { require(latitude.isFinite() && longitude.isFinite());require(powerKw.value > 0) }
    val occupancyText get() = "doluluk bilinmiyor"
}
/** User-entered preferences are routing weights, not proof of a charger's power or access. */
object PreferredNetworks {
    fun normalize(raw:String):String = raw.trim().lowercase(java.util.Locale.ROOT)
        .replace('ş','s').replace('ğ','g').replace('ü','u')
        .replace('ö','o').replace('ç','c').replace('ı','i')
        .replace(Regex("\\s+"), " ")
    fun parse(raw:String):Set<String> = raw.split(',').map(::normalize)
        .filter { it.isNotBlank() }.take(12).toSet()
    fun matches(operator:String, choices:Set<String>):Boolean =
        normalize(operator) in choices.map(::normalize)
}

object OperatorHeuristics {
    /** An operator offering DC somewhere NEVER proves this individual OSM station has DC. */
    val dcNetworks = mapOf(
        "zes" to "https://zes.net/tr/sarj-istasyonlari",
        "esarj" to "https://trugo.com.tr/individual/news/trugo-ve-esarj-is-birligiyle-hizli-ve-kesintisiz-sarj-81-ilde-hizmetinizde",
        "eşarj" to "https://trugo.com.tr/individual/news/trugo-ve-esarj-is-birligiyle-hizli-ve-kesintisiz-sarj-81-ilde-hizmetinizde",
        "trugo" to "https://trugo.com.tr/individual/news/12-aydir-kesintisiz-pazar-lideriyiz",
        "tesla" to "https://www.tesla.com/tr_tr/support/charging/supercharging-other-evs",
        "voltrun" to "https://www.voltrun.com/tr/sss",
        "aksa şarj" to "https://www.aksasarj.com.tr/tr/haberler",
        "aksa sarj" to "https://www.aksasarj.com.tr/tr/haberler",
        "powerşarj" to "https://powersarj.com/fiyatlandirma/",
        "powersarj" to "https://powersarj.com/fiyatlandirma/",
    )
    fun source(operator:String):String? = dcNetworks[operator.trim().lowercase()]
}
data class ChargerFilter(
    val onlyOsmTaggedCcs2:Boolean = false,
    val minKw:Double = 0.0,
    val excludedNetworks:Set<String> = emptySet(),
    val acceptPrivate:Boolean = false,
)
object ChargerFiltering {
    fun apply(candidates:List<ChargerCandidate>,filter:ChargerFilter):List<ChargerCandidate> {
        require(filter.minKw>=0 && filter.minKw.isFinite())
        return candidates.filter { item ->
            (!filter.onlyOsmTaggedCcs2 || item.confidence==ChargerConfidence.OSM_TAGGED_CCS2) &&
                item.powerKw.value>=filter.minKw &&
                item.operator.lowercase() !in filter.excludedNetworks.map { it.lowercase() } &&
                (filter.acceptPrivate || item.access.lowercase() !in setOf("private","no"))
        }
    }
}
