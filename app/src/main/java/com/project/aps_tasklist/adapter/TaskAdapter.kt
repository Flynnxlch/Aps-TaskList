package com.project.aps_tasklist.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.aps_tasklist.R
import com.project.aps_tasklist.model.TaskModel

class TaskAdapter(
    private var taskList: List<TaskModel>,
    private val listener: TaskActionListener
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    interface TaskActionListener {
        fun onTaskClicked(task: TaskModel, position: Int)
        fun onEdit(task: TaskModel, position: Int)
        fun onDelete(task: TaskModel, position: Int)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTaskTitle: TextView   = itemView.findViewById(R.id.tvTaskTitle)
        private val tvTaskDesc: TextView    = itemView.findViewById(R.id.tvTaskDesc)
        private val progressBar: ProgressBar= itemView.findViewById(R.id.pbTaskProgress)
        private val ivTaskMenu: ImageView   = itemView.findViewById(R.id.ivTaskMenu)
        private val avatar1: ImageView      = itemView.findViewById(R.id.ivAvatar1)
        private val avatar2: ImageView      = itemView.findViewById(R.id.ivAvatar2)
        private val avatar3: ImageView      = itemView.findViewById(R.id.ivAvatar3)
        private val tvMore: TextView        = itemView.findViewById(R.id.tvAvatarMore)

        fun bind(task: TaskModel, pos: Int) {
            tvTaskTitle.text     = task.title
            tvTaskDesc.text      = task.description
            progressBar.progress = task.progress

            // Avatars placeholder + "+N"
            val count = task.usercount
            val avatars = listOf(avatar1, avatar2, avatar3)
            avatars.forEachIndexed { i, iv ->
                if (i < count && i < 3) {
                    iv.setImageResource(R.drawable.ic_avatar_placeholder)
                    iv.visibility = View.VISIBLE
                } else {
                    iv.visibility = View.GONE
                }
            }
            // Show "+N" if >3
            if (count > 3) {
                tvMore.visibility = View.VISIBLE
                tvMore.text = "+${count - 3}"
            } else {
                tvMore.visibility = View.GONE
            }

            // Klik card
            itemView.setOnClickListener { listener.onTaskClicked(task, pos) }

            // Menu overflow
            ivTaskMenu.setOnClickListener { view ->
                PopupMenu(view.context, view).apply {
                    inflate(R.menu.menu_task_item)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.action_edit   -> { listener.onEdit(task, pos); true }
                            R.id.action_delete -> { listener.onDelete(task, pos); true }
                            else               -> false
                        }
                    }
                    show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TaskViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_task_card, parent, false)
        )

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(taskList[position], position)
    }

    override fun getItemCount(): Int = taskList.size

    fun updateData(newList: List<TaskModel>) {
        taskList = newList
        notifyDataSetChanged()
    }
}
