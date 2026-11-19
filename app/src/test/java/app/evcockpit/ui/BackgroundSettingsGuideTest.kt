package app.evcockpit.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundSettingsGuideTest {
    @Test fun xiaomiFamilyIsDetected() {
        assertTrue(BackgroundSettingsGuide.isXiaomiFamily("Xiaomi"))
        assertTrue(BackgroundSettingsGuide.isXiaomiFamily("POCO"))
        assertTrue(BackgroundSettingsGuide.isXiaomiFamily("redmi"))
        assertFalse(BackgroundSettingsGuide.isXiaomiFamily("Google"))
    }
    @Test fun guideDoesNotPromiseSettingsAreChanged() {
        val text = BackgroundSettingsGuide.explanation("POCO")
        assertTrue(text.contains("manuel"))
        assertTrue(text.contains("otomatik başlatma"))
    }
}
