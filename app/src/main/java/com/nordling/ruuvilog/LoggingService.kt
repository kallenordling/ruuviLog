package com.nordling.ruuvilog

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.nordling.ruuvilog.db.AppDatabase
import com.nordling.ruuvilog.db.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LoggingService : Service() {

    companion object {
        const val EXTRA_MAC = "extra_mac"
        const val EXTRA_INTERVAL = "extra_interval"
        var isRunning = false
            private set
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ruuvi_logging"
    }

    inner class LocalBinder : Binder() {
        fun getService(): LoggingService = this@LoggingService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val db by lazy { AppDatabase.getInstance(this) }
    private val handler = Handler(Looper.getMainLooper())

    val latestTag = MutableLiveData<RuuviTag?>()
    val lastLocation = MutableLiveData<Location?>()

    private lateinit var targetMac: String
    private var intervalSeconds = 10

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }
    private val locationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private val locationListener = LocationListener { loc -> lastLocation.postValue(loc) }

    private val logRunnable = object : Runnable {
        override fun run() {
            val temp = latestTag.value?.temperature
            if (temp != null) {
                scope.launch {
                    db.logDao().insert(
                        LogEntry(
                            mac = targetMac,
                            temperature = temp,
                            latitude = lastLocation.value?.latitude,
                            longitude = lastLocation.value?.longitude
                        )
                    )
                }
                updateNotification("%.2f °C".format(temp))
            }
            handler.postDelayed(this, intervalSeconds * 1000L)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.address != targetMac) return
            val data = result.scanRecord
                ?.getManufacturerSpecificData(RuuviParser.MANUFACTURER_ID) ?: return
            RuuviParser.parse(result.device.address, result.rssi, data)
                ?.let { latestTag.postValue(it) }
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetMac = intent?.getStringExtra(EXTRA_MAC) ?: run { stopSelf(); return START_NOT_STICKY }
        intervalSeconds = intent.getIntExtra(EXTRA_INTERVAL, 10)
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Waiting for signal…"))
        startScan()
        startLocationUpdates()
        handler.post(logRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(logRunnable)
        stopScan()
        locationManager.removeUpdates(locationListener)
        scope.cancel()
    }

    private fun startScan() {
        if (!hasBluetoothPermission()) return
        bluetoothAdapter?.bluetoothLeScanner?.startScan(
            listOf(ScanFilter.Builder()
                .setManufacturerData(RuuviParser.MANUFACTURER_ID, byteArrayOf())
                .build()),
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            scanCallback
        )
    }

    private fun stopScan() {
        if (!hasBluetoothPermission()) return
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(provider, 2000L, 0f, locationListener)
                locationManager.getLastKnownLocation(provider)?.let {
                    if (lastLocation.value == null) lastLocation.postValue(it)
                }
            }
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            android.Manifest.permission.BLUETOOTH_SCAN
        else
            android.Manifest.permission.BLUETOOTH
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "RuuviLog", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "RuuviTag background logging" }
            )
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, LoggingActivity::class.java).apply {
                putExtra(LoggingActivity.EXTRA_MAC, targetMac)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Logging $targetMac")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }
}
