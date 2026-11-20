package app.evcockpit.integration

import java.net.URI

/**
 * Minimal, public navigation contract for launchers such as Mola's phone-app picker.
 *
 * This does not modify or depend on Mola bytecode, UI, private APIs, or permissions.
 * Links only select pre-existing EV Cockpit screens. No commands, media playback,
 * OBD connections, location transfers, privileged actions or URL forwarding.
 */
object MolaOpenContract {
    private const val SCHEME = "evcockpit"
    private const val HOST = "open"

    private val tabs = mapOf(
        "/dashboard" to 0,
        "/obd" to 1,
        "/route" to 2,
        "/plan" to 3,
        "/passenger" to 4,
        "/settings" to 5,
    )

    fun tabFor(url: String?): Int? {
        if (url.isNullOrBlank() || url.length > 120) return null
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!SCHEME.equals(uri.scheme, ignoreCase = true) ||
            !HOST.equals(uri.host, ignoreCase = true) ||
            uri.userInfo != null || uri.port != -1 ||
            uri.query != null || uri.fragment != null ||
            uri.rawPath != uri.path) return null
        return tabs[uri.path]
    }

    fun linkFor(tab: Int): String? =
        tabs.entries.firstOrNull { it.value == tab }?.key?.let { "$SCHEME://$HOST$it" }
}
