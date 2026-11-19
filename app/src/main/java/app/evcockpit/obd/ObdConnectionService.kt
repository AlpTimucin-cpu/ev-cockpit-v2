package app.evcockpit.obd

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.evcockpit.CockpitApplication
import app.evcockpit.MainActivity

/**
 * The service is started only by a foreground user tap on a paired device.
 * Navigation can use a separate location FGS when phase 2 is approved.
 */
class ObdConnectionService : Service() {
    private val repo: ObdRepository
        get() = (application as CockpitApplication).obdRepository

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_CONNECT) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        try {
            ensureForeground()
            val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                ?: throw IllegalArgumentException("Bluetooth cihaz adresi eksik")
            val device = getSystemService(BluetoothManager::class.java)
                ?.adapter?.getRemoteDevice(address)
                ?: throw IllegalStateException("Bluetooth adaptörü bulunamadı")
            repo.connect(device)
        } catch (error: Exception) {
            repo.stop()
            repo.state.value = VehicleState(statusText = when (error) {
                is SecurityException -> "Bluetooth veya bağlı cihaz servisi izni eksik"
                else -> "OBD servisi başlatılamadı: ${error.message ?: "Bilinmeyen hata"}"
            })
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun ensureForeground() {
        val channelId = "obd_connection"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "OBD bağlantısı", NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("ID.4 Yol Arkadaşı")
            .setContentText("OBD bağlantısı etkin; araç verileri doğrulanmadı")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        if (!repo.state.value.demo) repo.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "app.evcockpit.obd.CONNECT"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        private const val NOTIFICATION_ID = 1042
    }
}
