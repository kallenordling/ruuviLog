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
        const val EXTRA_MAC = "extra_mac"
    }

    private lateinit var binding: ActivityMapBinding
    private val db by lazy { AppDatabase.getInstance(this) }
    private val speedAdapter = SpeedAdapter()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mac = intent.getStringExtra(EXTRA_MAC) ?: run { finish(); return }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "GPS Track"

        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.recyclerSpeed.adapter = speedAdapter

        lifecycleScope.launch {
            val entries = db.logDao().getGpsTrackByMac(mac)
            if (entries.isEmpty()) {
                binding.textNoGps.visibility = View.VISIBLE
                return@launch
            }

            val points = entries.map { GeoPoint(it.latitude!!, it.longitude!!) }

            // Polyline track
            val polyline = Polyline().apply {
                setPoints(points)
                outlinePaint.color = Color.parseColor("#0078D7")
                outlinePaint.strokeWidth = 8f
            }
            binding.mapView.overlays.add(polyline)

            // Start marker
            binding.mapView.overlays.add(Marker(binding.mapView).apply {
                position = points.first()
                title = "Start: ${timeFormat.format(Date(entries.first().timestamp))}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })

            // End marker (only if more than one point)
            if (points.size > 1) {
                binding.mapView.overlays.add(Marker(binding.mapView).apply {
                    position = points.last()
                    title = "End: ${timeFormat.format(Date(entries.last().timestamp))}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }

            // Fit map to bounding box
            val bbox = BoundingBox.fromGeoPoints(points)
            binding.mapView.post {
                binding.mapView.zoomToBoundingBox(bbox.increaseByScale(1.3f), true)
            }

            // Calculate speed segments
            val speedEntries = mutableListOf<SpeedEntry>()
            var totalDistM = 0f
            var totalTimeS = 0L
            var maxSpeedKmh = 0f

            for (i in 1 until entries.size) {
                val prev = entries[i - 1]
                val curr = entries[i]
                val results = FloatArray(1)
                Location.distanceBetween(
                    prev.latitude!!, prev.longitude!!,
                    curr.latitude!!, curr.longitude!!,
                    results
                )
                val distM = results[0]
                val timeDeltaS = (curr.timestamp - prev.timestamp) / 1000f
                if (timeDeltaS > 0) {
                    val speedKmh = (distM / timeDeltaS) * 3.6f
                    maxSpeedKmh = maxOf(maxSpeedKmh, speedKmh)
                    totalDistM += distM
                    totalTimeS += timeDeltaS.toLong()
                    speedEntries.add(
                        SpeedEntry(
                            timeLabel = timeFormat.format(Date(curr.timestamp)),
                            distanceM = distM,
                            speedKmh = speedKmh
                        )
                    )
                }
            }

            speedAdapter.submitList(speedEntries)
            val avgSpeedKmh = if (totalTimeS > 0) (totalDistM / totalTimeS) * 3.6f else 0f
            binding.textMaxSpeed.text = "%.1f".format(maxSpeedKmh)
            binding.textAvgSpeed.text = "%.1f".format(avgSpeedKmh)
            binding.textTotalDist.text = "%.2f".format(totalDistM / 1000f)

            if (speedEntries.isEmpty()) {
                binding.textNoGps.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
