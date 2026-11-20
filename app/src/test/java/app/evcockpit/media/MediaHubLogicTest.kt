package app.evcockpit.media

import org.junit.Assert.*
import org.junit.Test

class MediaHubLogicTest {
    @Test fun localM3uOnlyIncludesSecureUrlsAndNamedStations() {
        val list = """
            #EXTM3U
            #EXTINF:-1 tvg-id="news",Haber TV
            https://example.org/live/stream.m3u8
            #EXTINF:-1,Cleartext Blocked
            http://example.org/video.m3u8
            #EXTINF:-1,Credential Blocked
            https://user:pass@example.org/live.m3u8
            #EXTINF:-1,Kültür Radyo
            https://example.org/radio.aac
        """.trimIndent()
        val stations = M3uPlaylistParser.parse(list)
        assertEquals(2, stations.size)
        assertEquals("Haber TV", stations[0].title)
        assertEquals("Kültür Radyo", stations[1].title)
        assertTrue(stations.all { it.kind == StreamKind.IPTV })
    }

    @Test fun malformedOrOversizedPlaylistFailsClosed() {
        assertTrue(M3uPlaylistParser.parse("x".repeat(512 * 1024 + 1)).isEmpty())
        assertTrue(M3uPlaylistParser.parse("#EXTM3U\nfile:///sdcard/a.mp4").isEmpty())
    }

    @Test fun rejectUserInfoAndHttpAndFragments() {
        assertNull(MediaUrlPolicy.validHttps("http://example.org/song.mp3"))
        assertNull(MediaUrlPolicy.validHttps("https://username:pass@example.org/song.mp3"))
        assertNull(MediaUrlPolicy.validHttps("https://example.org/song.mp3#secret"))
        assertNotNull(MediaUrlPolicy.validHttps("https://example.org/song.mp3"))
    }

    @Test fun turkishCommandsHandleVoiceVariants() {
        assertEquals(MediaVoiceAction.NEXT_RADIO,
            TurkishMediaVoice.parse("Sonraki radyo istasyonu"))
        assertEquals(MediaVoiceAction.PREVIOUS_RADIO,
            TurkishMediaVoice.parse("Önceki istasyon"))
        assertEquals(MediaVoiceAction.STOP,
            TurkishMediaVoice.parse("Radyoyu durdur"))
        assertEquals(MediaVoiceAction.ROUTE,
            TurkishMediaVoice.parse("Navigasyon aç"))
        assertEquals(MediaVoiceAction.PLAN,
            TurkishMediaVoice.parse("Şarj planını aç"))
        assertEquals(MediaVoiceAction.VIDEO,
            TurkishMediaVoice.parse("YouTube aç"))
        assertEquals(MediaVoiceAction.UNKNOWN,
            TurkishMediaVoice.parse("tamamen ilgisiz bir ifade"))
    }

    @Test fun radioBrowserFiltersNonTlsDuplicateAndInvalidStreams() {
        val json = """
            [
              {"stationuuid":"11111111-1111-1111-1111-111111111111",
               "name":"Test Radio","url_resolved":"https://radio.example.org/stream"},
              {"stationuuid":"11111111-1111-1111-1111-111111111111",
               "name":"Duplicate","url_resolved":"https://radio2.example.org/stream"},
              {"stationuuid":"22222222-2222-2222-2222-222222222222",
               "name":"No TLS","url_resolved":"http://radio.example.org/stream"}
            ]
        """.trimIndent()
        val radios = RadioBrowserClient().parseStations(json)
        assertEquals(1, radios.size)
        assertEquals("Test Radio", radios[0].title)
        assertEquals(StreamKind.RADIO, radios[0].kind)
    }
}
