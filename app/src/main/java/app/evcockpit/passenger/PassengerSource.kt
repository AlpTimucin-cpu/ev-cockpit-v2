package app.evcockpit.passenger

import java.net.URI

/** No DRM scraping, platform extraction or cleartext HTTP transport. */
sealed class PassengerSource {
    data class DirectHttps(val url: String) : PassengerSource()
    data class LocalDocument(val uri: String) : PassengerSource()
    data class YouTubeEmbed(val videoId: String) : PassengerSource()
}

object PassengerSourcePolicy {
    private val idPattern = Regex("^[A-Za-z0-9_-]{11}$")
    fun parseLocal(uri: String): PassengerSource.LocalDocument? =
        if (uri.startsWith("content://") && uri.length <= 4_096)
            PassengerSource.LocalDocument(uri) else null

    fun parseDirectHttps(url: String): PassengerSource.DirectHttps? {
        val parsed = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        if (!parsed.scheme.equals("https", true) || parsed.host.isNullOrBlank() ||
            parsed.userInfo != null || url.length > 2_048 ||
            parsed.host.lowercase() in setOf("youtube.com", "www.youtube.com",
                "youtu.be", "m.youtube.com", "netflix.com", "www.netflix.com")) return null
        return PassengerSource.DirectHttps(url.trim())
    }

    /** Official YouTube iframe only: never returns a direct stream media URL. */
    fun parseYouTube(url: String): PassengerSource.YouTubeEmbed? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", true) || uri.userInfo != null) return null
        val host = uri.host?.lowercase() ?: return null
        val id = when (host) {
            "youtu.be" -> uri.path.trimStart('/').substringBefore('/')
            "youtube.com", "www.youtube.com", "m.youtube.com", "youtube-nocookie.com",
            "www.youtube-nocookie.com" -> when {
                uri.path == "/watch" -> uri.rawQuery.orEmpty().split('&')
                    .firstOrNull { it.startsWith("v=") }?.substringAfter("v=")
                uri.path.startsWith("/embed/") -> uri.path.substringAfter("/embed/").substringBefore('/')
                uri.path.startsWith("/shorts/") -> uri.path.substringAfter("/shorts/").substringBefore('/')
                else -> null
            }
            else -> null
        } ?: return null
        if (!idPattern.matches(id)) return null
        return PassengerSource.YouTubeEmbed(id)
    }
}

enum class PassengerLayout { FULL, SPLIT, FLOATING }

object VideoSpeedLock {
    const val DEFAULT_LOCK_ENABLED = false
    fun shouldPause(enabled: Boolean, currentSpeedKmh: Double?): Boolean =
        enabled && currentSpeedKmh != null && currentSpeedKmh.isFinite() &&
            currentSpeedKmh > 0.5
}
