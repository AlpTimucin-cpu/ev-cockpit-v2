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
    @Test fun lineageOnPocoDoesNotTriggerMiuiShortcut() {
        assertTrue(BackgroundSettingsGuide.isLineageOs("lineage_marble-userdebug", "lineage/marble"))
        assertFalse(BackgroundSettingsGuide.shouldTryMiuiAutostart(
            "Xiaomi", "lineage-24.0-20261117-nightly", "lineage/marble"))
        assertTrue(BackgroundSettingsGuide.explanation(
            "POCO", "lineage-24.0", "lineage/marble").contains("LineageOS"))
    }

    @Test fun stockHyperOsUsesMiuiShortcutOnlyWithRomEvidence() {
        assertTrue(BackgroundSettingsGuide.shouldTryMiuiAutostart(
            "POCO", "HyperOS 3.0", "xiaomi/marble"))
        assertTrue(BackgroundSettingsGuide.shouldTryMiuiAutostart(
            "Xiaomi", "unknown", "xiaomi/MIUI/marble"))
        assertFalse(BackgroundSettingsGuide.shouldTryMiuiAutostart(
            "POCO", "AP3A.240905.001", "xiaomi/marble"))
    }

    @Test fun otherRomNeverInvokesMiuiActivity() {
        assertFalse(BackgroundSettingsGuide.shouldTryMiuiAutostart(
            "Google", "HyperOS", "google/pixel"))
        assertFalse(BackgroundSettingsGuide.shouldTryMiuiAutostart(
            "POCO", "Evolution X", "custom/marble"))
    }

    @Test fun unknownRomHasSafeAndroidFallback() {
        val text = BackgroundSettingsGuide.explanation("POCO", "custom", "custom/marble")
        assertTrue(text.contains("ROM'a özel menü varsayılmıyor"))
        assertTrue(BackgroundSettingsGuide.romLabel("POCO", "custom", "custom/marble")
            .contains("doğrulanamadı"))
    }
}
