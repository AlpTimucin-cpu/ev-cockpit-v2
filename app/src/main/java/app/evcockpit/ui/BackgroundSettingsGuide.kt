package app.evcockpit.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Opens system settings only. It does NOT automatically change permissions or exemptions. */
object BackgroundSettingsGuide {
    fun isXiaomiFamily(manufacturer: String): Boolean =
        listOf("xiaomi", "redmi", "poco").any { manufacturer.contains(it, ignoreCase = true) }

    fun explanation(manufacturer: String): String =
        if (isXiaomiFamily(manufacturer)) {
            "POCO/HyperOS: Ayarlar > Uygulamalar > Arka planda otomatik başlatma. " +
                "Pil kullanımında bu uygulama için kısıtlamayı manuel kaldırın. " +
                "LineageOS üzerinde menüler farklı olabilir."
        } else {
            "Android: Uygulama bilgileri > Pil / pil optimizasyonu. " +
                "Sistem izinlerini yalnız siz değiştirebilirsiniz."
        }

    fun openBatteryOptimizationSettings(context: Context): Boolean = runCatching {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    fun openAutostartOrAppDetails(context: Context): Boolean {
        if (isXiaomiFamily(Build.MANUFACTURER)) {
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
