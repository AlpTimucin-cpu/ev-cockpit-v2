package app.evcockpit.ui

import org.junit.Assert.*
import org.junit.Test

class DisplayPreferencesTest {
    @Test fun startsInPreviousDarkModeWithoutScreenLock() {
        assertTrue(DisplayDefaults.DARK_THEME)
        assertFalse(DisplayDefaults.KEEP_SCREEN_ON)
    }
}
