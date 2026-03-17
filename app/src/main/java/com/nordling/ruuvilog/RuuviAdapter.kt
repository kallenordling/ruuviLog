package com.nordling.ruuvilog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nordling.ruuvilog.databinding.ItemRuuviBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RuuviAdapter(
    private val onTagClick: (RuuviTag) -> Unit
) : ListAdapter<RuuviTag, RuuviAdapter.ViewHolder>(Diff) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemRuuviBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: RuuviTag) {
            binding.textMac.text = tag.mac
            binding.textFormat.text = "DF${tag.dataFormat}  RSSI: ${tag.rssi} dBm  ${timeFormat.format(Date(tag.lastSeen))}"
            binding.textTemperature.text = tag.temperature?.let { "%.2f °C".format(it) } ?: "—"
            binding.textHumidity.text = tag.humidity?.let { "%.2f %%".format(it) } ?: "—"
            binding.textPressure.text = tag.pressure?.let { "%.0f Pa".format(it) } ?: "—"
            binding.textAccel.text = if (tag.accelX != null) {
                "X: ${tag.accelX}  Y: ${tag.accelY}  Z: ${tag.accelZ} mG"
            } else "—"
            binding.textBattery.text = tag.batteryVoltage?.let { "%.3f V".format(it) } ?: "—"
            binding.textMovement.text = tag.movementCounter?.toString() ?: "—"
            binding.textSequence.text = tag.sequenceNumber?.toString() ?: "—"
            binding.root.setOnClickListener { onTagClick(tag) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRuuviBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object Diff : DiffUtil.ItemCallback<RuuviTag>() {
        override fun areItemsTheSame(a: RuuviTag, b: RuuviTag) = a.mac == b.mac
        override fun areContentsTheSame(a: RuuviTag, b: RuuviTag) = a == b
    }
}
