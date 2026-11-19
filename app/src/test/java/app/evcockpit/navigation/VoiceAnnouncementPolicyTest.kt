package app.evcockpit.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceAnnouncementPolicyTest {
    @Test fun selects200MetreCue() {
        val cue = VoiceAnnouncementPolicy.select(2, 175.0, "sağa dön", emptySet())!!
        assertEquals("2:200", cue.id)
        assertEquals("200 metre sonra sağa dön", cue.text)
    }

    @Test fun selects50MetreCue() {
        val cue = VoiceAnnouncementPolicy.select(2, 30.0, "sola dön", setOf("2:200"))!!
        assertEquals("2:50", cue.id)
    }

    @Test fun doesNotRepeatSameCue() {
        assertNull(VoiceAnnouncementPolicy.select(2, 30.0, "sola dön", setOf("2:50")))
    }

    @Test fun rejectsFarOrMissingLocation() {
        assertNull(VoiceAnnouncementPolicy.select(0, 500.0, "sağa dön", emptySet()))
        assertNull(VoiceAnnouncementPolicy.select(0, null, "sağa dön", emptySet()))
        assertNull(VoiceAnnouncementPolicy.select(0, -1.0, "sağa dön", emptySet()))
    }
}
