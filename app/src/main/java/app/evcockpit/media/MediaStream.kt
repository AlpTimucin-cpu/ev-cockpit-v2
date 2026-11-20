package app.evcockpit.media

import java.net.URI

enum class StreamKind { RADIO, IPTV }

data class MediaStream(
    val id: String,
    val title: String,
    val url: String,
    val kind: StreamKind,
)

object MediaUrlPolicy {
    /** Never accept credentials in URLs or cleartext streams. */
    fun validHttps(raw: String): String? {
        val url = raw.trim()
        if (url.length !in 10..2048 || url.any { it == '\r' || it == '\n' }) return null
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (uri.scheme != "https" || uri.host.isNullOrBlank() ||
            uri.userInfo != null || uri.fragment != null) return null
        return url
    }
}

object M3uPlaylistParser {
    private const val MAX_ENTRIES = 200
    /** Local, user-selected M3U file only; remote playlists and credentials are not downloaded. */
    fun parse(raw: String): List<MediaStream> {
        if (raw.length > 512 * 1024) return emptyList()
        val results = ArrayList<MediaStream>()
        var pendingTitle: String? = null
        raw.lineSequence().forEach { line ->
            if (results.size >= MAX_ENTRIES) return@forEach
            val value = line.trim()
            when {
                value.startsWith("#EXTINF:", ignoreCase = true) -> {
                    pendingTitle = value.substringAfterLast(',', "")
                        .trim().take(100).ifBlank { null }
                }
                value.startsWith("#") || value.isBlank() -> Unit
                else -> {
                    val url = MediaUrlPolicy.validHttps(value)
                    if (url != null) {
                        results.add(MediaStream(
                            id = "iptv-${results.size}",
                            title = pendingTitle ?: "Kanal ${results.size + 1}",
                            url = url,
                            kind = StreamKind.IPTV,
                        ))
                    }
                    pendingTitle = null
                }
            }
        }
        return results
    }
}

enum class MediaVoiceAction {
    FETCH_RADIO, NEXT_RADIO, PREVIOUS_RADIO, STOP, ROUTE, PLAN, VIDEO, MEDIA, UNKNOWN
}

object TurkishMediaVoice {
    fun parse(raw: String): MediaVoiceAction {
        val value = raw.lowercase(java.util.Locale.forLanguageTag("tr-TR"))
            .replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
            .replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
        return when {
            listOf("sonraki", "ileri").any(value::contains) &&
                listOf("radyo", "istasyon", "kanal").any(value::contains) ->
                MediaVoiceAction.NEXT_RADIO
            listOf("onceki", "geri").any(value::contains) &&
                listOf("radyo", "istasyon", "kanal").any(value::contains) ->
                MediaVoiceAction.PREVIOUS_RADIO
            listOf("durdur", "kapat", "sus").any(value::contains) &&
                listOf("radyo", "medya", "muzik", "video", "oynat").any(value::contains) ->
                MediaVoiceAction.STOP
            value.contains("radyo") -> MediaVoiceAction.FETCH_RADIO
            value.contains("sarj plan") || value.contains("plan") -> MediaVoiceAction.PLAN
            value.contains("rota") || value.contains("harita") || value.contains("navigasyon") ->
                MediaVoiceAction.ROUTE
            value.contains("youtube") || value.contains("video") -> MediaVoiceAction.VIDEO
            value.contains("medya") || value.contains("televizyon") || value.contains("iptv") ->
                MediaVoiceAction.MEDIA
            else -> MediaVoiceAction.UNKNOWN
        }
    }
}
