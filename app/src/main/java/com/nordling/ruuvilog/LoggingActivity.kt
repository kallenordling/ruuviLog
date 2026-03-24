package com.nordling.ruuvilog

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nordling.ruuvilog.databinding.ActivityLoggingBinding
import com.nordling.ruuvilog.db.AppDatabase

class LoggingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MAC = "extra_mac"
        val INTERVAL_LABELS = listOf("5 seconds", "10 seconds", "30 seconds", "1 minute", "5 minutes", "10 minutes")
        val INTERVAL_SECONDS = listOf(5, 10, 30, 60, 300, 600)
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityLoggingBinding
    private val db by lazy { AppDatabase.getInstance(this) }
    private val sessionAdapter = SessionAdapter { session ->
        startActivity(Intent(this, SessionDetailActivity::class.java).apply {
            putExtra(SessionDetailActivity.EXTRA_SESSION_ID, session.id)
            putExtra(SessionDetailActivity.EXTRA_MAC, targetMac)
        })
    }

    private lateinit var targetMac: String
    private var selectedIntervalSeconds = 10
    private var loggingService: LoggingService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            loggingService = (binder as LoggingService.LocalBinder).getService()
            isBound = true
            loggingService?.latestTag?.observe(this@LoggingActivity) { it?.let { t -> updateLiveReading(t) } }
            loggingService?.lastLocation?.observe(this@LoggingActivity) { it?.let { l -> updateGpsStatus(l) } }
            updateLoggingButton()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            loggingService = null
            isBound = false
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

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, INTERVAL_LABELS)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerInterval.adapter = spinnerAdapter
        binding.spinnerInterval.setSelection(1)
        binding.spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, pos: Int, id: Long) {
                selectedIntervalSeconds = INTERVAL_SECONDS[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.recyclerSessions.adapter = sessionAdapter
        db.sessionDao().getByMac(targetMac).observe(this) { sessions ->
            sessionAdapter.submitList(sessions)
            binding.textNoSessions.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.btnStartLog.setOnClickListener {
            if (LoggingService.isRunning) stopLogging() else startLogging()
        }


        requestLocationPermissionIfNeeded()
    }

    override fun onStart() {
        super.onStart()
        if (LoggingService.isRunning) {
            bindService(serviceIntent(), serviceConnection, 0)
        }
        updateLoggingButton()
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
            loggingService = null
        }
    }

    private fun startLogging() {
        val intent = serviceIntent()
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, 0)
        Toast.makeText(this, "Logging every $selectedIntervalSeconds s", Toast.LENGTH_SHORT).show()
        updateLoggingButton()
    }

    private fun stopLogging() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
            loggingService = null
        }
        stopService(serviceIntent())
        updateLoggingButton()
    }

    private fun updateLoggingButton() {
        val running = LoggingService.isRunning
        binding.btnStartLog.text = if (running) "Stop Logging" else "Start Logging"
        binding.spinnerInterval.isEnabled = !running
    }

    private fun serviceIntent() = Intent(this, LoggingService::class.java).apply {
        putExtra(LoggingService.EXTRA_MAC, targetMac)
        putExtra(LoggingService.EXTRA_INTERVAL, selectedIntervalSeconds)
    }

    private fun updateLiveReading(tag: RuuviTag) {
        binding.textLiveTemp.text = tag.temperature?.let { "%.2f °C".format(it) } ?: "—"
        binding.textLiveRssi.text = "RSSI: ${tag.rssi} dBm"
        binding.textLiveHumidity.text = tag.humidity?.let { "Humidity: %.1f %%".format(it) } ?: ""
    }

    private fun updateGpsStatus(location: Location) {
        binding.textLiveGps.text = "GPS: %.5f, %.5f (±%.0f m)".format(
            location.latitude, location.longitude, location.accuracy
        )
    }

    private fun requestLocationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
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
            grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            binding.textLiveGps.text = "GPS: no permission"
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
