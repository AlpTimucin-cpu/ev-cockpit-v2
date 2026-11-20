package app.evcockpit.passenger

import org.junit.Assert.*
import org.junit.Test

class PassengerLayoutTest {
    @Test fun startsFullscreenWithSpeedLockOff() {
        assertEquals(PassengerLayout.FULL, PassengerUiDefaults.LAYOUT)
        assertFalse(PassengerUiDefaults.SPEED_LOCK)
    }
    @Test fun invalidSavedLayoutFallsBackSafely() {
        assertEquals(PassengerLayout.FULL, PassengerUiDefaults.parseLayout("not-a-layout"))
        assertEquals(PassengerLayout.FLOATING,
            PassengerUiDefaults.parseLayout("FLOATING"))
    }
    @Test fun onlyOfficialIframeScriptWithSafeId() {
        val html = YouTubeIframeHtml.render("abcde123456")
        assertTrue(html.contains("https://www.youtube.com/iframe_api"))
        assertTrue(html.contains("videoId:'abcde123456'"))
        assertFalse(html.contains("get_video_info"))
    }
    @Test(expected = IllegalArgumentException::class)
    fun invalidYouTubeIdIsRejectedBeforeWebView() {
        YouTubeIframeHtml.render("</script>")
    }
}
