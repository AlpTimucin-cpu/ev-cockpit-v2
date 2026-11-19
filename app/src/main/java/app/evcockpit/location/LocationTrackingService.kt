package app.evcockpit.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.evcockpit.CockpitApplication
import app.evcockpit.MainActivity

/**
 * User-started location foreground service. No background-start workaround,
 * no fake GPS position and no ACCESS_BACKGROUND_LOCATION request.
 */
class LocationTrackingService : Service() {
    private val repo get() = (application as CockpitApplication).locationRepository
    private var manager: LocationManager? = null
    private var listener: LocationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        try {
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (!fine && !coarse) throw SecurityException("Konum izni eksik")
            ensureForeground()
            val locationManager = getSystemService(LocationManager::class.java)
                ?: throw IllegalStateException("Konum servisi bulunamadı")
            manager = locationManager
            repo.starting()
            val update = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    repo.update(location)
                }
            }
            listener = update
            var requested = false
            if (fine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1_000L, 0f, update, Looper.getMainLooper(),
                )
                requested = true
            }
            if (coarse && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 5_000L, 10f, update, Looper.getMainLooper(),
                )
                requested = true
            }
            if (!requested) throw IllegalStateException("Konum sağlayıcıları kapalı")
        } catch (error: Exception) {
            val message = when (error) {
                is SecurityException -> "Konum izni gerekli; telefon ayarlarını kontrol et"
                else -> "Konum servisi kullanılamıyor: ${error.message ?: "Bilinmeyen hata"}"
            }
            stopSelf(startId)
            // onDestroy clears the fix; publish actionable status again afterwards.
            repo.failure(message)
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun ensureForeground() {
        val channel = "nav_location"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channel, "Navigasyon konumu", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("ID.4 Yol Arkadaşı — konum")
            .setContentText("Konum alınıyor; gerçek rota henüz doğrulanmadı")
            .setOngoing(true)
            .setContentIntent(intent)
            .build()
        startForeground(1104, notification)
    }

    override fun onDestroy() {
        val update = listener
        if (update != null) runCatching { manager?.removeUpdates(update) }
        manager = null
        listener = null
        if (repo.state.value.active) repo.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "app.evcockpit.location.START"
    }
}
