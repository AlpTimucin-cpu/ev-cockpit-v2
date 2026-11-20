package app.evcockpit.updates

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class NightlyStatus(val commit: String, val newBuildAvailable: Boolean)

object NightlyReleaseParser {
    const val EXPECTED_URL =
        "https://github.com/AlpTimucin-cpu/ev-cockpit-v2/releases/download/nightly/app-debug.apk"
    fun parse(json: String, installedCommit: String): NightlyStatus? {
        if (!Regex("^[a-f0-9]{40}$").matches(installedCommit)) return null
        val doc = JSONObject(json)
        val match = Regex("(?m)^Commit: ([a-f0-9]{40})$").find(doc.optString("body"))
            ?: return null
        val assets = doc.getJSONArray("assets")
        val found = (0 until assets.length()).any { i ->
            val a = assets.getJSONObject(i)
            a.optString("name") == "app-debug.apk" &&
                a.optString("browser_download_url") == EXPECTED_URL
        }
        if (!found) return null
        val sha = match.groupValues[1]
        return NightlyStatus(sha, sha != installedCommit)
    }
}

/** Public API, no GitHub token, never installs an APK. */
class NightlyUpdateChecker {
    private var lastCheckedAt: Long = -900_000L
    private var cached: NightlyStatus? = null
    suspend fun check(installedCommit: String, force: Boolean = false): NightlyStatus? =
        withContext(Dispatchers.IO) {
            val now = SystemClock.elapsedRealtime()
            if (!force && now - lastCheckedAt < 900_000) return@withContext cached
            lastCheckedAt = now
            val connection = URL(
                "https://api.github.com/repos/AlpTimucin-cpu/ev-cockpit-v2/releases/tags/nightly"
            ).openConnection() as HttpURLConnection
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "EV-Cockpit-v2-NightlyChecker")
            try {
                if (connection.responseCode != 200) return@withContext null
                val payload = connection.inputStream.bufferedReader().use { it.readText() }
                NightlyReleaseParser.parse(payload, installedCommit).also { cached = it }
            } finally {
                connection.disconnect()
            }
        }
}
