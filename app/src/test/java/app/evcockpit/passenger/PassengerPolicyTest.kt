package app.evcockpit.passenger

import org.junit.Assert.*
import org.junit.Test

class PassengerPolicyTest {
    @Test fun onlySafContentAndHttpsDirectAreAllowed() {
        assertNotNull(PassengerSourcePolicy.parseLocal("content://media/external/video/23"))
        assertNull(PassengerSourcePolicy.parseLocal("file:///sdcard/private"))
        assertNull(PassengerSourcePolicy.parseDirectHttps("http://example.com/video.mp4"))
        assertNotNull(PassengerSourcePolicy.parseDirectHttps("https://example.com/video.mp4"))
        assertNull(PassengerSourcePolicy.parseDirectHttps("https://user:pass@example.com/file.mp4"))
    }
    @Test fun youtubeUsesOfficialIdentifierNotDirectStream() {
        val result = PassengerSourcePolicy.parseYouTube(
            "https://www.youtube.com/watch?v=abcde123456")
        assertEquals("abcde123456", result?.videoId)
        assertNull(PassengerSourcePolicy.parseYouTube("https://fakeyoutube.com/watch?v=abcde123456"))
        assertNull(PassengerSourcePolicy.parseDirectHttps("https://www.youtube.com/watch?v=abcde123456"))
        assertNull(PassengerSourcePolicy.parseYouTube("https://www.youtube.com/watch?v=INVALID"))
    }
    @Test fun speedLockIsOptIn() {
        assertFalse(VideoSpeedLock.DEFAULT_LOCK_ENABLED)
        assertFalse(VideoSpeedLock.shouldPause(false, 120.0))
        assertTrue(VideoSpeedLock.shouldPause(true, 50.0))
        assertFalse(VideoSpeedLock.shouldPause(true, null))
    }
    @Test fun threeLayoutsAreFixed() {
        assertEquals(listOf(PassengerLayout.FULL, PassengerLayout.SPLIT,
            PassengerLayout.FLOATING), PassengerLayout.entries)
    }
}
