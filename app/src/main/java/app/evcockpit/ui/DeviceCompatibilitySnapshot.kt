package app.evcockpit.ui

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Local, privacy-preserving device capability snapshot for Android 17/custom ROM acceptance.
 * Never includes location coordinates, BT MAC, VIN, token, fingerprint or API keys.
 * This reports permissions/settings, not successful GPS/OBD/route communication.
 */
data class DeviceCompatibilitySnapshot(
    val androidRelease: String,
    val apiLevel: Int,
    val rom: String,
    val preciseLocationGranted: Boolean,
    val approximateLocationGranted: Boolean,
    val gpsProviderEnabled: Boolean,
    val networkProviderEnabled: Boolean,
    val bluetoothPermissionGranted: Boolean,
    val bluetoothEnabled: Boolean?,
    val notificationsEnabled: Boolean,
    val batteryOptimizationExempt: Boolean,
) {
    fun safeReport(): String = listOf(
        "ID.4 Yol Arkadaşı — telefon uyumluluk özeti",
        "Android: $androidRelease (API $apiLevel)",
        "ROM: $rom",
        "Hassas konum izni: " + yesNo(preciseLocationGranted),
        "Yaklaşık konum izni: " + yesNo(approximateLocationGranted),
        "GPS sağlayıcı: " + yesNo(gpsProviderEnabled),
        "Ağ konum sağlayıcı: " + yesNo(networkProviderEnabled),
        "Bluetooth bağlantı izni: " + yesNo(bluetoothPermissionGranted),
        "Bluetooth açık: " + (bluetoothEnabled?.let(::yesNo) ?: "İZİN YOK / BİLİNMİYOR"),
        "Bildirimler: " + yesNo(notificationsEnabled),
        "Pil optimizasyonu muafiyeti: " + yesNo(batteryOptimizationExempt),
        "Uyarı: gerçek GPS, ELM327, rota/API ve arka plan sürekliliği DOĞRULANMADI.",
        "Bu özet koordinat, Bluetooth MAC, VIN, API anahtarı veya cihaz kimliği içermez.",
    ).joinToString("\n")

    companion object {
        private fun yesNo(value: Boolean): String = if (value) "EVET" else "HAYIR"
    }
}

object DeviceCompatibilityDiagnostics {
    fun capture(context: Context): DeviceCompatibilitySnapshot {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val btPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val btEnabled = if (!btPermission) null else runCatching {
            context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled
        }.getOrNull()
        val loc = context.getSystemService(LocationManager::class.java)
        val gpsEnabled = runCatching {
            loc?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        }.getOrDefault(false)
        val networkEnabled = runCatching {
            loc?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }.getOrDefault(false)
        val notificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val notifications = notificationPermission && runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(false)
        val batteryExempt = runCatching {
            context.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(context.packageName) == true
        }.getOrDefault(false)

        return DeviceCompatibilitySnapshot(
            androidRelease = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            rom = BackgroundSettingsGuide.romLabel(
                Build.MANUFACTURER, Build.DISPLAY, Build.FINGERPRINT),
            preciseLocationGranted = fine,
            approximateLocationGranted = coarse,
            gpsProviderEnabled = gpsEnabled,
            networkProviderEnabled = networkEnabled,
            bluetoothPermissionGranted = btPermission,
            bluetoothEnabled = btEnabled,
            notificationsEnabled = notifications,
            batteryOptimizationExempt = batteryExempt,
        )
    }
}
