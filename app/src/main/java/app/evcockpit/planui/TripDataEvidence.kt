package app.evcockpit.planui

import app.evcockpit.obd.SocReading

/**
 * Source/quality labels for a user-triggered EV plan. No proprietary ABRP, Car Scanner
 * or Kilovati data or code is used. This audit NEVER certifies physical vehicle data.
 */
enum class EvidenceLevel { INPUT_AVAILABLE, CAUTION, MISSING }

data class EvidenceItem(
    val label: String,
    val level: EvidenceLevel,
    val explanation: String,
)

data class EvidenceSnapshot(val items: List<EvidenceItem>) {
    val missingCount: Int get() = items.count { it.level == EvidenceLevel.MISSING }
    val cautionCount: Int get() = items.count { it.level == EvidenceLevel.CAUTION }
    /** Coordinate/API-key/VIN-free diagnostic text. */
    fun safeSummary(): String = items.joinToString("\n") {
        "${it.label}: ${it.explanation}"
    }
}

object TripDataEvidence {
    fun evaluate(
        soc: SocReading,
        routeAvailable: Boolean,
        routeProvider: String?,
        gpsPresent: Boolean,
        gpsFresh: Boolean,
        gpsAccuracyMeters: Float?,
        stationSearchComplete: Boolean,
        stationCount: Int,
        taggedCcs2Count: Int,
        planAvailable: Boolean,
    ): EvidenceSnapshot {
        val validSoc = soc.pct?.takeIf { it.isFinite() && it in 0.0..100.0 }
        val socItem = when {
            validSoc == null || soc.source.contains("DEMO", ignoreCase = true) ->
                EvidenceItem("Batarya SoC", EvidenceLevel.MISSING,
                    "Gerçek SoC girişi yok veya DEMO. Ayarlar'dan manuel SoC girin.")
            soc.source.contains("OBD", ignoreCase = true) ->
                EvidenceItem("Batarya SoC", EvidenceLevel.CAUTION,
                    "OBD verisi mevcut, ancak MEB PID ölçeği DOĞRULANMADI.")
            soc.source.contains("MANUEL", ignoreCase = true) ->
                EvidenceItem("Batarya SoC", EvidenceLevel.CAUTION,
                    "Araç ekranından manuel giriş; zamanla güncelliğini kaybedebilir.")
            else -> EvidenceItem("Batarya SoC", EvidenceLevel.CAUTION,
                "SoC değeri mevcut; kaynak doğrulaması yapılmadı.")
        }
        val gpsItem = when {
            !gpsPresent -> EvidenceItem("GPS", EvidenceLevel.MISSING,
                "Konum verisi yok; GPS takibini başlatın.")
            !gpsFresh -> EvidenceItem("GPS", EvidenceLevel.CAUTION,
                "Son konum güncel değil; yeniden planlamada taze GPS gerekir.")
            gpsAccuracyMeters == null || !gpsAccuracyMeters.isFinite() ||
                gpsAccuracyMeters > 50f -> EvidenceItem("GPS", EvidenceLevel.CAUTION,
                "Konum alındı ancak hassasiyet yeterince doğrulanmadı.")
            else -> EvidenceItem("GPS", EvidenceLevel.INPUT_AVAILABLE,
                "Güncel konum örneği mevcut; fiziksel doğruluk garanti edilmez.")
        }
        val routeItem = if (!routeAvailable) EvidenceItem(
            "Sürüş rotası", EvidenceLevel.MISSING,
            "Rota yok; kullanıcı kendi anahtarıyla rota oluşturmalı.",
        ) else EvidenceItem(
            "Sürüş rotası", EvidenceLevel.CAUTION,
            "Rota kaynağı: ${normalizeProvider(routeProvider)}; canlı trafik ve saha doğrulaması yok.",
        )
        val stationItem = when {
            !stationSearchComplete -> EvidenceItem(
                "Şarj istasyonları", EvidenceLevel.MISSING,
                "Bu plan isteğinde istasyon araması henüz tamamlanmadı.",
            )
            stationCount <= 0 -> EvidenceItem(
                "Şarj istasyonları", EvidenceLevel.MISSING,
                "Kullanılabilir aday verisi yok; istasyon bulunamadığı kesin değildir.",
            )
            else -> EvidenceItem(
                "Şarj istasyonları", EvidenceLevel.CAUTION,
                "${stationCount} aday; ${taggedCcs2Count.coerceIn(0, stationCount)} OSM CCS2 etiketli. " +
                    "Bağlantı/çalışma/doluluk/fiyat anlık olarak doğrulanmıyor.",
            )
        }
        val planItem = if (planAvailable) EvidenceItem(
            "Enerji planı", EvidenceLevel.CAUTION,
            "SoC, tüketim, şarj süresi ve varış sonucu TAHMİN; araçla kalibre edilmedi.",
        ) else EvidenceItem(
            "Enerji planı", EvidenceLevel.MISSING,
            "Henüz tamamlanmış bir enerji planı yok.",
        )
        return EvidenceSnapshot(listOf(socItem, gpsItem, routeItem, stationItem, planItem))
    }

    private fun normalizeProvider(input: String?): String = when {
        input.isNullOrBlank() -> "belirtilmedi"
        input.contains("ORS", ignoreCase = true) ||
            input.contains("openrouteservice", ignoreCase = true) -> "openrouteservice"
        else -> "harici rota sağlayıcısı"
    }
}

/** Transport connected != data verified. No commands, clearing DTCs or PID changes. */
object ObdEvidence {
    fun describe(
        connected: Boolean,
        demo: Boolean,
        soc: Double?,
        speed: Double?,
    ): List<String> {
        if (demo) return listOf(
            "DEMO: araçtan alınan veri değildir.",
            "MEB PID ölçekleri ve ELM327 uyumluluğu DOĞRULANMADI.",
        )
        if (!connected) return listOf(
            "ELM327 bağlantısı yok; canlı ECU verisi sağlanamıyor.",
            "MEB PID ölçekleri ve ELM327 uyumluluğu DOĞRULANMADI.",
        )
        return listOf(
            "ELM327 iletişim oturumu açık; ECU verisinin doğruluğu garanti edilmez.",
            if (soc != null && soc.isFinite() && soc in 0.0..100.0)
                "SoC yanıtı mevcut (PID ölçeği DOĞRULANMADI)."
            else "SoC yanıtı yok / geçersiz.",
            if (speed != null && speed.isFinite() && speed >= 0.0)
                "Hız yanıtı mevcut (PID ölçeği DOĞRULANMADI)."
            else "Hız yanıtı yok / geçersiz.",
        )
    }
}
