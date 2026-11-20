package app.evcockpit.updates

import org.junit.Assert.*
import org.junit.Test

class NightlyReleaseParserTest {
    private val installed = "a".repeat(40)
    @Test fun newerNightlyRequiresExpectedAssetAndValidCommit() {
        val json = """{"body":"Debug test APK\nCommit: bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\n",
          "assets":[{"name":"app-debug.apk","browser_download_url":
          "https://github.com/AlpTimucin-cpu/ev-cockpit-v2/releases/download/nightly/app-debug.apk"}]}"""
        val parsed = NightlyReleaseParser.parse(json, installed)
        assertTrue(parsed!!.newBuildAvailable)
        assertFalse(NightlyReleaseParser.parse(json, "b".repeat(40))!!.newBuildAvailable)
    }
    @Test fun unsupportedOrSpoofedDownloadLinksCannotBeShown() {
        val json = """{"body":"Commit: bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "assets":[{"name":"app-debug.apk","browser_download_url":"https://evil.example/app.apk"}]}"""
        assertNull(NightlyReleaseParser.parse(json, installed))
        assertNull(NightlyReleaseParser.parse(json, "unknown"))
    }
}
