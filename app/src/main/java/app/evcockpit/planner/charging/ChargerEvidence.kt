package app.evcockpit.planner.charging

/** Surface existing OSM provenance without suggesting live availability or verified tariffs. */
object ChargerEvidence {
    private fun value(raw: String): String = raw.trim().takeIf {
        it.isNotEmpty() && it.length <= 100
    } ?: "bilinmiyor"

    fun details(charger: ChargerCandidate): List<String> = listOf(
        "Bağlantı güveni: " + when (charger.confidence) {
            ChargerConfidence.OSM_TAGGED_CCS2 -> "OSM CCS2 etiketi var (cihaz doğrulanmadı)"
            ChargerConfidence.PROBABLE_CCS2 -> "OLASI CCS2 (cihaz doğrulanmadı)"
            ChargerConfidence.UNKNOWN -> "Bağlantı tipi BİLİNMİYOR"
        },
        "Güç: ${charger.powerKw.value.toInt()} kW — OSM/yerel kaynak tahmini; " +
            "gerçek cihaz gücü doğrulanmadı",
        "Erişim: ${value(charger.access)} • Ücret: ${value(charger.fee)} " +
            "(tarife/fiyat doğrulanmadı)",
        "Saatler: ${value(charger.openingHours)} • Kimlik doğrulama: " +
            value(charger.authentication),
        "Soket bilgisi: Type 2=${value(charger.type2)} / CHAdeMO=" +
            value(charger.chademo) +
            " • kapasite=${charger.capacity?.toString() ?: "bilinmiyor"}",
        "İstasyon kaynağı: OSM/Overpass veya yerel kayıt; güncellik DOĞRULANMADI.",
        "Anlık durum: ${charger.occupancyText}; canlı fiyat, doluluk veya çalışma verisi yok.",
    )
}
