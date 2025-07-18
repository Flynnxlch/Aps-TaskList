package com.project.aps_tasklist.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.project.aps_tasklist.model.Notification
import com.project.aps_tasklist.R

class NotificationAdapter (
    private val items: List<Notification>
) : RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    inner class NotifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val indicator: View     = itemView.findViewById(R.id.viewIndicator)
        private val tvTitle: TextView   = itemView.findViewById(R.id.tvNotifTitle)
        private val tvTime: TextView    = itemView.findViewById(R.id.tvNotifTime)
        private val tvDesc: TextView    = itemView.findViewById(R.id.tvNotifDesc)

        fun bind(notification: Notification) {
            tvTitle.text = notification.title
            tvTime.text = notification.timestamp.toString().substring(0, 5) // Format LocalTime to HH:mm
            tvDesc.text = notification.message

             val color = ContextCompat.getColor(itemView.context, notification.colorResId)
            indicator.setBackgroundColor(color)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }
    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        holder.bind(items[position])
    }
    override fun getItemCount(): Int = items.size
}