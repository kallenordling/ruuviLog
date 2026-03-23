package com.nordling.ruuvilog

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nordling.ruuvilog.databinding.ItemSpeedBinding

data class SpeedEntry(
    val timeLabel: String,
    val distanceM: Float,
    val speedKmh: Float
)

class SpeedAdapter : ListAdapter<SpeedEntry, SpeedAdapter.ViewHolder>(Diff) {

    inner class ViewHolder(private val binding: ItemSpeedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: SpeedEntry) {
            binding.textSpeedTime.text = entry.timeLabel
            binding.textSpeedDist.text = "%.0f m".format(entry.distanceM)
            binding.textSpeedValue.text = "%.1f km/h".format(entry.speedKmh)
            binding.textSpeedValue.setTextColor(speedColor(entry.speedKmh))
        }

        private fun speedColor(kmh: Float) = when {
            kmh < 10f  -> Color.parseColor("#4CAF50")   // green: walking
            kmh < 50f  -> Color.parseColor("#FF9800")   // orange: cycling / slow car
            else       -> Color.parseColor("#F44336")   // red: fast
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSpeedBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<SpeedEntry>() {
        override fun areItemsTheSame(a: SpeedEntry, b: SpeedEntry) = a.timeLabel == b.timeLabel
        override fun areContentsTheSame(a: SpeedEntry, b: SpeedEntry) = a == b
    }
}
