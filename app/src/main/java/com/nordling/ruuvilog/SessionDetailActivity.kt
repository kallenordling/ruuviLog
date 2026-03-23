package com.nordling.ruuvilog

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.nordling.ruuvilog.databinding.ActivitySessionDetailBinding
import com.nordling.ruuvilog.db.AppDatabase
import com.nordling.ruuvilog.db.LogEntry
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_MAC = "extra_mac"
    }

    private lateinit var binding: ActivitySessionDetailBinding
    private val db by lazy { AppDatabase.getInstance(this) }
    private val logDetailAdapter = LogDetailAdapter()
    private var sessionId: Long = -1
    private lateinit var mac: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1)
        mac = intent.getStringExtra(EXTRA_MAC) ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Session"

        binding.recyclerLogDetail.adapter = logDetailAdapter

        lifecycleScope.launch {
            db.sessionDao().getById(sessionId)?.let { session ->
                binding.editSessionName.setText(session.name)
                supportActionBar?.title = session.name
            }
        }

        binding.btnSaveName.setOnClickListener {
            val newName = binding.editSessionName.text.toString().trim()
            if (newName.isNotEmpty()) {
                lifecycleScope.launch {
                    db.sessionDao().updateName(sessionId, newName)
                    supportActionBar?.title = newName
                    Toast.makeText(this@SessionDetailActivity, "Name saved", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnViewMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java).apply {
                putExtra(MapActivity.EXTRA_SESSION_ID, sessionId)
                putExtra(MapActivity.EXTRA_MAC, mac)
            })
        }

        binding.btnExportCsv.setOnClickListener { exportCsv() }

        loadEntries()
    }

    private fun loadEntries() {
        lifecycleScope.launch {
            val entries = db.logDao().getAllBySession(sessionId)

            val speeds = calculateSpeeds(entries)
            logDetailAdapter.submitList(entries.map { LogDetailItem(it, speeds[it.id]) })

            val count = entries.size
            val duration = if (entries.size > 1) {
                val ms = entries.last().timestamp - entries.first().timestamp
                "%dm %ds".format(ms / 60000, (ms % 60000) / 1000)
            } else "—"
            val temps = entries.map { it.temperature }
            val tempRange = if (temps.isNotEmpty())
                String.format(Locale.US, "%.1f – %.1f °C", temps.min(), temps.max())
            else "—"

            binding.textEntryCount.text = count.toString()
            binding.textDuration.text = duration
            binding.textTempRange.text = tempRange
        }
    }

    private fun calculateSpeeds(entries: List<LogEntry>): Map<Long, Float> {
        val speeds = mutableMapOf<Long, Float>()
        var prevGps: LogEntry? = null
        for (entry in entries) {
            if (entry.latitude != null && entry.longitude != null) {
                val prev = prevGps
                if (prev != null && prev.latitude != null && prev.longitude != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        prev.latitude!!, prev.longitude!!,
                        entry.latitude, entry.longitude, results
                    )
                    val dt = (entry.timestamp - prev.timestamp) / 1000f
                    if (dt > 0) speeds[entry.id] = (results[0] / dt) * 3.6f
                }
                prevGps = entry
            }
        }
        return speeds
    }

    private fun exportCsv() {
        lifecycleScope.launch {
            val entries = db.logDao().getAllBySession(sessionId)
            if (entries.isEmpty()) {
                Toast.makeText(this@SessionDetailActivity, "No data to export", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val speeds = calculateSpeeds(entries)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val sessionName = binding.editSessionName.text.toString()
                .ifEmpty { sessionId.toString() }
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val exportDir = File(cacheDir, "export").also { it.mkdirs() }
            val file = File(exportDir, "ruuvi_$sessionName.csv")
            file.bufferedWriter().use { w ->
                w.write("time,temperature_c,latitude,longitude,speed_kmh\n")
                entries.forEach { e ->
                    val time = dateFormat.format(Date(e.timestamp))
                    val temp = String.format(Locale.US, "%.2f", e.temperature)
                    val lat = e.latitude?.let { String.format(Locale.US, "%.6f", it) } ?: ""
                    val lon = e.longitude?.let { String.format(Locale.US, "%.6f", it) } ?: ""
                    val speed = speeds[e.id]?.let { String.format(Locale.US, "%.2f", it) } ?: ""
                    w.write("$time,$temp,$lat,$lon,$speed\n")
                }
            }
            val uri = FileProvider.getUriForFile(this@SessionDetailActivity, "$packageName.fileprovider", file)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Export CSV"
            ))
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
