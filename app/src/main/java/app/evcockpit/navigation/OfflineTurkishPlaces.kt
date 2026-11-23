package app.evcockpit.navigation

import java.util.Locale

/**
 * Bounded offline first-letter suggestions for Türkiye's provinces and districts.
 * Coordinates are approximate locality centres, NOT street-address matches.
 * Dataset: AEKHer/il-ilce-koordinat (MIT), sourced from OpenStreetMap (ODbL).
 */
class OfflineTurkishPlaces private constructor(
    private val records: List<Record>,
) {
    private data class Record(val province: String, val district: String, val point: GeoPoint) {
        val provinceKey = normalize(province)
        val districtKey = normalize(district)
    }

    fun suggest(query: String, maxResults: Int = 8): List<PhotonPlace> {
        val normalized = normalize(query)
        if (normalized.length < 2 || normalized.length > 120 || maxResults <= 0)
            return emptyList()

        fun rank(item: Record): Int = when {
            item.provinceKey == normalized && item.districtKey == normalized -> 0
            item.districtKey == normalized -> 1
            item.provinceKey == normalized -> 2
            item.districtKey.startsWith(normalized) -> 3
            item.provinceKey.startsWith(normalized) -> 4
            (item.provinceKey + " " + item.districtKey).startsWith(normalized) -> 5
            else -> 100
        }

        return records.asSequence()
            .map { it to rank(it) }
            .filter { it.second < 100 }
            .sortedWith(compareBy<Pair<Record, Int>> { it.second }
                .thenBy { it.first.provinceKey }
                .thenBy { it.first.districtKey })
            .take(maxResults.coerceAtMost(10))
            .map { (record, _) ->
                PhotonPlace("${record.district}, ${record.province} (il/ilçe merkezi)", record.point)
            }
            .toList()
    }

    companion object {
        private val tr = Locale.forLanguageTag("tr-TR")
        fun normalize(text: String): String = text.trim().lowercase(tr)
            .replace('ı', 'i').replace('ğ', 'g')
            .replace('ş', 's').replace('ç', 'c')
            .replace('ö', 'o').replace('ü', 'u')

        fun fromDelimited(content: String): OfflineTurkishPlaces {
            val records = content.lineSequence().take(1_500).mapNotNull { line ->
                if (line.isBlank() || line.startsWith("#") || line.length > 240)
                    return@mapNotNull null
                val parts = line.split('|')
                if (parts.size != 4) return@mapNotNull null
                val lat = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                val lon = parts[3].toDoubleOrNull() ?: return@mapNotNull null
                if (lat !in 35.0..43.0 || lon !in 25.0..46.0) return@mapNotNull null
                val province = parts[0].trim().take(80)
                val district = parts[1].trim().take(80)
                if (province.isBlank() || district.isBlank()) return@mapNotNull null
                val point = runCatching { GeoPoint(lat, lon) }.getOrNull()
                    ?: return@mapNotNull null
                Record(province, district, point)
            }.toList()
            return OfflineTurkishPlaces(records)
        }

        fun empty(): OfflineTurkishPlaces = OfflineTurkishPlaces(emptyList())
    }
}
