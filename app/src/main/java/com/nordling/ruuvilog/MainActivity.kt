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
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nordling.ruuvilog.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: RuuviViewModel by viewModels()
    private val adapter = RuuviAdapter { tag ->
        startActivity(Intent(this, LoggingActivity::class.java).apply {
            putExtra(LoggingActivity.EXTRA_MAC, tag.mac)
        })
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val mfrData = result.scanRecord?.getManufacturerSpecificData(RuuviParser.MANUFACTURER_ID)
                ?: return
            val tag = RuuviParser.parse(result.device.address, result.rssi, mfrData) ?: return
            viewModel.updateTag(tag)
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Scan failed: error $errorCode", Toast.LENGTH_SHORT).show()
            }
            viewModel.setScanning(false)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            startScan()
        } else {
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.adapter = adapter

        viewModel.tags.observe(this) { tags ->
            adapter.submitList(tags)
            binding.textEmpty.visibility =
                if (tags.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.scanning.observe(this) { scanning ->
            binding.btnScan.text = if (scanning) "Stop Scan" else "Start Scan"
        }

        binding.btnScan.setOnClickListener {
            if (viewModel.scanning.value == true) stopScan() else checkPermissionsAndScan()
        }
    }

    private fun checkPermissionsAndScan() {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startScan()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show()
            return
        }

        val filter = ScanFilter.Builder()
            .setManufacturerData(RuuviParser.MANUFACTURER_ID, byteArrayOf())
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
        viewModel.setScanning(true)
        Toast.makeText(this, "Scanning for RuuviTags…", Toast.LENGTH_SHORT).show()
    }

    private fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        viewModel.setScanning(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
    }
}
