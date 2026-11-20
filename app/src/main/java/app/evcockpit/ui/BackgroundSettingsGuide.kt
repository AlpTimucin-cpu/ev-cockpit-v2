package app.evcockpit.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * System settings shortcuts only. A POCO device can run a non-Xiaomi ROM;
 * manufacturer name alone is not evidence that MIUI/HyperOS activities exist.
 */
object BackgroundSettingsGuide {
    fun isXiaomiFamily(manufacturer: String): Boolean =
        listOf("xiaomi", "redmi", "poco").any { manufacturer.contains(it, ignoreCase = true) }

    fun isLineageOs(display: String, fingerprint: String): Boolean =
        listOf(display, fingerprint).any { it.contains("lineage", ignoreCase = true) }

    fun hasMiuiSkin(display: String, fingerprint: String): Boolean =
        listOf(display, fingerprint).any {
            it.contains("miui", ignoreCase = true) || it.contains("hyperos", ignoreCase = true)
        }

    /** Only try OEM-specific activities when the running ROM actually looks like that OEM. */
    fun shouldTryMiuiAutostart(
        manufacturer: String, display: String, fingerprint: String,
    ): Boolean = isXiaomiFamily(manufacturer) &&
        !isLineageOs(display, fingerprint) && hasMiuiSkin(display, fingerprint)

    fun romLabel(manufacturer: String, display: String, fingerprint: String): String = when {
        isLineageOs(display, fingerprint) -> "LineageOS (ROM işareti bulundu)"
        shouldTryMiuiAutostart(manufacturer, display, fingerprint) ->
            "MIUI/HyperOS (ROM işareti bulundu)"
        else -> "Android / ROM türü doğrulanamadı"
    }

    fun explanation(
        manufacturer: String, display: String = "", fingerprint: String = "",
    ): String = when {
        isLineageOs(display, fingerprint) ->
            "LineageOS: Uygulama bilgileri > Pil bölümünden arka plan kısıtlarını " +
                "manuel kontrol edin. Xiaomi otomatik başlatma ekranı bu ROM'da bulunmayabilir."
        shouldTryMiuiAutostart(manufacturer, display, fingerprint) ->
            "POCO/HyperOS: Ayarlar > Uygulamalar > Arka planda otomatik başlatma. " +
                "Pil kullanımında bu uygulama için kısıtlamayı manuel kontrol edin."
        else ->
            "Android: Uygulama bilgileri > Pil / pil optimizasyonu ve otomatik başlatma " +
                "ayarlarını manuel kontrol edin. ROM'a özel menü varsayılmıyor."
    }

    fun openBatteryOptimizationSettings(context: Context): Boolean = runCatching {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    fun openAutostartOrAppDetails(context: Context): Boolean {
        if (shouldTryMiuiAutostart(Build.MANUFACTURER, Build.DISPLAY, Build.FINGERPRINT)) {
            val miui = Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(miui); true }.getOrDefault(false)) return true
        }
        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }
}
