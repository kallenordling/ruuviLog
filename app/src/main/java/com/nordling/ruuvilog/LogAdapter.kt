package com.nordling.ruuvilog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nordling.ruuvilog.databinding.ItemLogBinding
import com.nordling.ruuvilog.db.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter : ListAdapter<LogEntry, LogAdapter.ViewHolder>(Diff) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LogEntry) {
            binding.textTimestamp.text = dateFormat.format(Date(entry.timestamp))
            binding.textTemperature.text = "%.2f °C".format(entry.temperature)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(a: LogEntry, b: LogEntry) = a.id == b.id
        override fun areContentsTheSame(a: LogEntry, b: LogEntry) = a == b
    }
}
