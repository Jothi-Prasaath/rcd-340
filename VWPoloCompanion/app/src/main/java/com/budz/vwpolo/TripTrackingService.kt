package com.budz.vwpolo

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class TripTrackingService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private val store by lazy { getSharedPreferences("polo_companion", Context.MODE_PRIVATE) }
    private var lastLocation: Location? = null
    private var distanceKm: Double = 0.0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        startForeground(NOTIFICATION_ID, notification("GPS trip tracking is active"))
        if (!store.getBoolean("trip_active", false)) {
            store.edit().putBoolean("trip_active", true)
                .putLong("trip_start_ms", System.currentTimeMillis())
                .putString("trip_distance_km", "0.00").apply()
            distanceKm = 0.0
        } else {
            distanceKm = store.getString("trip_distance_km", "0.00")?.toDoubleOrNull() ?: 0.0
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 0f, this)
            }
        } catch (_: SecurityException) { stopSelf() }
        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        if (!location.hasAccuracy() || location.accuracy > 50f) return
        val previous = lastLocation
        if (previous != null) {
            val meters = previous.distanceTo(location)
            val seconds = ((location.time - previous.time).coerceAtLeast(1L)) / 1000.0
            // Ignore implausible GPS jumps instead of corrupting the trip total.
            if (meters in 1f..500f && seconds <= 30.0) distanceKm += meters / 1000.0
        }
        lastLocation = location
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6 else 0.0
        store.edit().putString("gps_speed_kmh", String.format(Locale.US, "%.1f", speedKmh))
            .putString("trip_distance_km", String.format(Locale.US, "%.2f", distanceKm))
            .putFloat("gps_accuracy_m", location.accuracy).apply()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification("GPS speed ${speedKmh.toInt()} km/h · ${String.format(Locale.US, "%.2f", distanceKm)} km"))
    }

    override fun onProviderDisabled(provider: String) {
        store.edit().putString("gps_speed_kmh", "0").apply()
    }

    override fun onDestroy() {
        try { if (::locationManager.isInitialized) locationManager.removeUpdates(this) } catch (_: Exception) {}
        if (store.getBoolean("trip_active", false)) {
            val start = store.getLong("trip_start_ms", System.currentTimeMillis())
            val seconds = ((System.currentTimeMillis() - start).coerceAtLeast(0L) / 1000L)
            val count = (store.getString("trip_count", "0")?.toIntOrNull() ?: 0) + 1
            val total = (store.getString("trip_total_km", "0.00")?.toDoubleOrNull() ?: 0.0) + distanceKm
            val summary = "${String.format(Locale.US, "%.2f", distanceKm)} km · %02d:%02d duration".format(Locale.US, seconds / 3600, (seconds % 3600) / 60)
            store.edit().putBoolean("trip_active", false).putString("trip_distance_km", String.format(Locale.US, "%.2f", distanceKm))
                .putString("trip_total_km", String.format(Locale.US, "%.2f", total)).putString("trip_count", count.toString())
                .putString("trip_last_summary", summary).putString("gps_speed_kmh", "0").apply()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Polo trip tracking", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun notification(message: String): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Polo Companion")
        .setContentText(message)
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    companion object {
        const val ACTION_START = "com.budz.vwpolo.TRIP_START"
        const val ACTION_STOP = "com.budz.vwpolo.TRIP_STOP"
        private const val CHANNEL_ID = "polo_trip_tracking"
        private const val NOTIFICATION_ID = 3301
    }
}
