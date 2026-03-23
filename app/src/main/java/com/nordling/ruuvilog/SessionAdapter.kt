package com.nordling.ruuvilog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nordling.ruuvilog.databinding.ItemSessionBinding
import com.nordling.ruuvilog.db.Session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter(
    private val onViewMap: (Session) -> Unit
) : ListAdapter<Session, SessionAdapter.ViewHolder>(Diff) {

    private val dateFormat = SimpleDateFormat("MMM d yyyy  HH:mm", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(session: Session) {
            binding.textSessionName.text = session.name
            binding.textSessionDate.text = dateFormat.format(Date(session.startTime))
            binding.btnSessionMap.setOnClickListener { onViewMap(session) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(a: Session, b: Session) = a.id == b.id
        override fun areContentsTheSame(a: Session, b: Session) = a == b
    }
}
