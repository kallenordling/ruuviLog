package com.nordling.ruuvilog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nordling.ruuvilog.databinding.ItemLogDetailBinding
import com.nordling.ruuvilog.db.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogDetailItem(val entry: LogEntry, val speedKmh: Float?)

class LogDetailAdapter : ListAdapter<LogDetailItem, LogDetailAdapter.ViewHolder>(Diff) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemLogDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LogDetailItem) {
            binding.textDetailTime.text = timeFormat.format(Date(item.entry.timestamp))
            binding.textDetailTemp.text = String.format(Locale.US, "%.2f °C", item.entry.temperature)

            if (item.entry.latitude != null && item.entry.longitude != null) {
                binding.textDetailGps.text = String.format(
                    Locale.US, "%.5f, %.5f", item.entry.latitude, item.entry.longitude
                )
                binding.textDetailGps.visibility = View.VISIBLE
            } else {
                binding.textDetailGps.visibility = View.GONE
            }

            if (item.speedKmh != null) {
                binding.textDetailSpeed.text = String.format(Locale.US, "%.1f km/h", item.speedKmh)
                binding.textDetailSpeed.visibility = View.VISIBLE
            } else {
                binding.textDetailSpeed.visibility = View.INVISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemLogDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<LogDetailItem>() {
        override fun areItemsTheSame(a: LogDetailItem, b: LogDetailItem) = a.entry.id == b.entry.id
        override fun areContentsTheSame(a: LogDetailItem, b: LogDetailItem) = a == b
    }
}
