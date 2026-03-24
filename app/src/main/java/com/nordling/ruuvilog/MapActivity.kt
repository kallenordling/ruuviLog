package com.nordling.ruuvilog

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nordling.ruuvilog.databinding.ActivityMapBinding
import com.nordling.ruuvilog.db.AppDatabase
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_MAC = "extra_mac"
    }

    private lateinit var binding: ActivityMapBinding
    private val db by lazy { AppDatabase.getInstance(this) }
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var colorBySpeed = false
    private var sessionId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            load(this@MapActivity, getPreferences(MODE_PRIVATE))
            userAgentValue = packageName
        }

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GPS Track"

        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)

        binding.radioGroupColor.setOnCheckedChangeListener { _, checkedId ->
            colorBySpeed = checkedId == R.id.radioColorSpeed
            loadTrack()
        }

        loadTrack()
    }

    private fun loadTrack() {
        lifecycleScope.launch {
            val entries = if (sessionId > 0)
                db.logDao().getGpsTrackBySession(sessionId)
            else
                emptyList()

            binding.mapView.overlays.clear()
            binding.mapView.invalidate()

            if (entries.isEmpty()) {
                binding.textNoGps.visibility = View.VISIBLE
                return@launch
            }
            binding.textNoGps.visibility = View.GONE

            val points = entries.map { GeoPoint(it.latitude!!, it.longitude!!) }

            // Compute speed for every point (index 0 gets 0)
            val speedValues = mutableListOf(0f)
            for (i in 1 until entries.size) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    entries[i - 1].latitude!!, entries[i - 1].longitude!!,
                    entries[i].latitude!!, entries[i].longitude!!,
                    results
                )
                val dt = (entries[i].timestamp - entries[i - 1].timestamp) / 1000f
                speedValues.add(if (dt > 0) (results[0] / dt) * 3.6f else 0f)
            }

            val colorValues: List<Float> = if (colorBySpeed)
                speedValues
            else
                entries.map { it.temperature.toFloat() }

            val minVal = colorValues.minOrNull() ?: 0f
            val maxVal = colorValues.maxOrNull() ?: 0f

            // Draw colored segments
            for (i in 0 until points.size - 1) {
                binding.mapView.overlays.add(Polyline().apply {
                    setPoints(listOf(points[i], points[i + 1]))
                    outlinePaint.color = metricColor(colorValues[i], minVal, maxVal)
                    outlinePaint.strokeWidth = 10f
                })
            }

            // Start marker
            binding.mapView.overlays.add(Marker(binding.mapView).apply {
                position = points.first()
                title = "Start: ${timeFormat.format(Date(entries.first().timestamp))}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })

            // End marker
            if (points.size > 1) {
                binding.mapView.overlays.add(Marker(binding.mapView).apply {
                    position = points.last()
                    title = "End: ${timeFormat.format(Date(entries.last().timestamp))}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }

            // Fit bounds
            val bbox = BoundingBox.fromGeoPoints(points)
            binding.mapView.post { binding.mapView.zoomToBoundingBox(bbox.increaseByScale(1.3f), true) }
        }
    }

    /** Maps value in [min, max] to a color: blue (low) -> cyan -> green -> yellow -> red (high) */
    private fun metricColor(value: Float, min: Float, max: Float): Int {
        val t = if (max > min) ((value - min) / (max - min)).coerceIn(0f, 1f) else 0.5f
        val hue = (1f - t) * 240f
        return Color.HSVToColor(floatArrayOf(hue, 1f, 0.9f))
    }

    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
