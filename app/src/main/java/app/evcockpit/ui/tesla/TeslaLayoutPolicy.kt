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
