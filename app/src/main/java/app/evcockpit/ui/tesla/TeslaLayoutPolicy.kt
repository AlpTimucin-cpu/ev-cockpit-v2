package app.evcockpit.ui.tesla

/**
 * Eski kokpitin yalnız JVM testlerinde kullanılan pencere sınırı.
 * Eski arayüz/eylemler A0 ile silinmiştir; ortak test kuralı korunur.
 */
object TeslaLayoutPolicy {
    fun useThreePanes(widthDp: Int, heightDp: Int): Boolean =
        widthDp >= 700 && widthDp > heightDp
}
