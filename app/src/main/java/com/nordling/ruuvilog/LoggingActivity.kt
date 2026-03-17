package com.nordling.ruuvilog

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    }

    private lateinit var binding: ActivityLoggingBinding
    private val db by lazy { AppDatabase.getInstance(this) }
    private val logAdapter = LogAdapter()

    private lateinit var targetMac: String
    private var latestTag: RuuviTag? = null
    private var isLogging = false
    private val handler = Handler(Looper.getMainLooper())
    private var selectedIntervalSeconds = 10

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val logRunnable = object : Runnable {
        override fun run() {
            val temp = latestTag?.temperature
            if (temp != null) {
                lifecycleScope.launch {
                    db.logDao().insert(LogEntry(mac = targetMac, temperature = temp))
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

        startScan()
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
    }
}
