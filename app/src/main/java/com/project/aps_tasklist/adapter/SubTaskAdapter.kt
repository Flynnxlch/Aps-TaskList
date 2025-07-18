package com.project.aps_tasklist.adapter

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.aps_tasklist.model.SbTaskModel
import com.project.aps_tasklist.R
import java.util.*

class SubTaskAdapter(
    private val items: List<SbTaskModel>,
    private val isGroupTask: Boolean,
    private val listener: SubtaskActionListener      // ← tambahkan listener
) : RecyclerView.Adapter<SubTaskAdapter.VH>() {

    interface SubtaskActionListener {
        /**
         * Dipanggil saat subtask dicentang/di-uncentang.
         * @param position indeks subtask
         * @param isDone apakah sudah dicentang
         * @param completedBy nama yang menyelesaikan (atau null)
         */
        fun onSubtaskToggled(position: Int, isDone: Boolean, completedBy: String?)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val cbDone: CheckBox       = view.findViewById(R.id.cbSubtaskDone)
        val tvName: TextView       = view.findViewById(R.id.tvSubtaskName)
        val tvDeadline: TextView   = view.findViewById(R.id.tvSubtaskDeadline)
        val tvTimeLeft: TextView   = view.findViewById(R.id.tvSubtaskTimeLeft)
        val tvFinishedBy: TextView = view.findViewById(R.id.tvFinishedBy)
        val inputComment: View     = view.findViewById(R.id.inputLayoutComment)
        val etComment: TextView    = view.findViewById(R.id.etSubtaskComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subtask_detail, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val sub = items[pos]
        holder.tvName.text = sub.name

        // Format deadline
        val cal = Calendar.getInstance().apply { timeInMillis = sub.deadlineMillis }
        val fmt = DateFormat.format("yyyy-MM-dd HH:mm", cal).toString()
        holder.tvDeadline.text = "Due: $fmt"

        // Hitung time left
        val now = System.currentTimeMillis()
        val diff = sub.deadlineMillis - now
        holder.tvTimeLeft.text = when {
            diff <= 0                      -> "Overdue"
            diff > 24*60*60*1000L          -> "${diff/(24*60*60*1000)} days left"
            diff > 60*60*1000L             -> "${diff/(60*60*1000)} hours left"
            else                           -> "${diff/60000} minutes left"
        }

        // Reset listener sebelum setChecked
        holder.cbDone.setOnCheckedChangeListener(null)
        holder.cbDone.isChecked = (sub.completedBy != null)
        holder.inputComment.visibility = if (sub.completedBy != null) View.VISIBLE else View.GONE
        holder.etComment.text = sub.comment ?: ""

        // Finished by untuk group tasks
        if (isGroupTask && sub.completedBy != null) {
            holder.tvFinishedBy.visibility = View.VISIBLE
            holder.tvFinishedBy.text = "Finished by: ${sub.completedBy}"
        } else {
            holder.tvFinishedBy.visibility = View.GONE
        }

        // Pasang listener toggle
        holder.cbDone.setOnCheckedChangeListener { _, checked ->
            // tampilkan/menean comment container
            holder.inputComment.visibility = if (checked) View.VISIBLE else View.GONE
            // siapa yang menyelesaikan
            val completer = if (checked) "me" else null
            listener.onSubtaskToggled(pos, checked, completer)
        }
    }

    override fun getItemCount(): Int = items.size
}
