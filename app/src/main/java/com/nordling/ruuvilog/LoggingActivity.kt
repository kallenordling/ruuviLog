package com.nordling.ruuvilog

import android.Manifest
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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nordling.ruuvilog.databinding.ActivityLoggingBinding
import com.nordling.ruuvilog.db.AppDatabase
import com.nordling.ruuvilog.db.LogEntry
import kotlinx.coroutines.launch

class LoggingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MAC = "extra_mac"
        val INTERVAL_LABELS = listOf("5 seconds", "10 seconds", "30 seconds", "1 minute", "5 minutes", "10 minutes")
        val INTERVAL_SECONDS = listOf(5, 10, 30, 60, 300, 600)
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityLoggingBinding
    private val db by lazy { AppDatabase.getInstance(this) }
    private val logAdapter = LogAdapter()

    private lateinit var targetMac: String
    private var latestTag: RuuviTag? = null
    private var lastLocation: Location? = null
    private var isLogging = false
    private val handler = Handler(Looper.getMainLooper())
    private var selectedIntervalSeconds = 10

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val locationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private val locationListener = LocationListener { location ->
        lastLocation = location
        updateGpsStatus(location)
    }

    private val logRunnable = object : Runnable {
        override fun run() {
            val temp = latestTag?.temperature
            if (temp != null) {
                val lat = lastLocation?.latitude
                val lon = lastLocation?.longitude
                lifecycleScope.launch {
                    db.logDao().insert(
                        LogEntry(
                            mac = targetMac,
                            temperature = temp,
                            latitude = lat,
                            longitude = lon
                        )
                    )
                }
            }
            handler.postDelayed(this, selectedIntervalSeconds * 1000L)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.address != targetMac) return
            val mfrData = result.scanRecord
                ?.getManufacturerSpecificData(RuuviParser.MANUFACTURER_ID) ?: return
            val tag = RuuviParser.parse(result.device.address, result.rssi, mfrData) ?: return
            latestTag = tag
            runOnUiThread { updateLiveReading(tag) }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                Toast.makeText(this@LoggingActivity, "Scan error: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoggingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetMac = intent.getStringExtra(EXTRA_MAC) ?: run { finish(); return }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = targetMac

        // Interval spinner
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, INTERVAL_LABELS)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerInterval.adapter = spinnerAdapter
        binding.spinnerInterval.setSelection(1) // default 10 s

        binding.spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, pos: Int, id: Long) {
                selectedIntervalSeconds = INTERVAL_SECONDS[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Log list
        binding.recyclerLog.adapter = logAdapter
        db.logDao().getByMac(targetMac).observe(this) { entries ->
            logAdapter.submitList(entries)
            binding.textLogCount.text = "${entries.size} entries"
        }

        binding.btnStartLog.setOnClickListener {
            if (isLogging) stopLogging() else startLogging()
        }

        binding.btnClearLog.setOnClickListener {
            lifecycleScope.launch { db.logDao().deleteByMac(targetMac) }
        }

        binding.btnViewMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java).apply {
                putExtra(MapActivity.EXTRA_MAC, targetMac)
            })
        }

        startScan()
        requestLocationPermissionAndStart()
    }

    private fun requestLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            binding.textLiveGps.text = "GPS: no permission"
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        providers.forEach { provider ->
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(provider, 2000L, 0f, locationListener)
                locationManager.getLastKnownLocation(provider)?.let { loc ->
                    if (lastLocation == null) {
                        lastLocation = loc
                        updateGpsStatus(loc)
                    }
                }
            }
        }
        if (!providers.any { locationManager.isProviderEnabled(it) }) {
            binding.textLiveGps.text = "GPS: disabled"
        }
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }

    private fun updateGpsStatus(location: Location) {
        runOnUiThread {
            binding.textLiveGps.text = "GPS: %.5f, %.5f (±%.0f m)".format(
                location.latitude, location.longitude, location.accuracy
            )
        }
    }

    private fun startLogging() {
        isLogging = true
        binding.btnStartLog.text = "Stop Logging"
        binding.spinnerInterval.isEnabled = false
        handler.post(logRunnable)
        Toast.makeText(this, "Logging every $selectedIntervalSeconds s", Toast.LENGTH_SHORT).show()
    }

    private fun stopLogging() {
        isLogging = false
        binding.btnStartLog.text = "Start Logging"
        binding.spinnerInterval.isEnabled = true
        handler.removeCallbacks(logRunnable)
    }

    private fun updateLiveReading(tag: RuuviTag) {
        binding.textLiveTemp.text = tag.temperature?.let { "%.2f °C".format(it) } ?: "—"
        binding.textLiveRssi.text = "RSSI: ${tag.rssi} dBm"
        binding.textLiveHumidity.text = tag.humidity?.let { "Humidity: %.1f %%".format(it) } ?: ""
    }

    private fun startScan() {
        if (!hasBluetoothPermission()) return
        val filter = ScanFilter.Builder()
            .setManufacturerData(RuuviParser.MANUFACTURER_ID, byteArrayOf())
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private fun stopScan() {
        if (!hasBluetoothPermission()) return
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun hasBluetoothPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Manifest.permission.BLUETOOTH_SCAN
        else
            Manifest.permission.BLUETOOTH
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
        stopLogging()
        stopLocationUpdates()
    }
}
