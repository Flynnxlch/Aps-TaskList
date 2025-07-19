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
import com.project.aps_tasklist.model.TaskListItem
import com.project.aps_tasklist.model.TaskModel

class TaskListAdapter(
    private var items: List<TaskListItem>,
    private val listener: TaskActionListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface TaskActionListener {
        fun onTaskClicked(task: TaskModel, position: Int)
        fun onEdit(task: TaskModel, position: Int)
        fun onDelete(task: TaskModel, position: Int)
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TASK   = 1
    }

    override fun getItemViewType(position: Int) =
        when (items[position]) {
            is TaskListItem.Header   -> TYPE_HEADER
            is TaskListItem.TaskItem -> TYPE_TASK
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            HeaderViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_task_card, parent, false)
            TaskViewHolder(v, listener)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        when (val item = items[pos]) {
            is TaskListItem.Header ->
                (holder as HeaderViewHolder).bind(item.label)
            is TaskListItem.TaskItem ->
                (holder as TaskViewHolder).bind(item.task, pos)
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<TaskListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(android.R.id.text1)
        fun bind(label: String) {
            tv.text = label
        }
    }

    class TaskViewHolder(
        view: View,
        private val listener: TaskActionListener
    ) : RecyclerView.ViewHolder(view) {
        private val tvTitle     = view.findViewById<TextView>(R.id.tvTaskTitle)
        private val tvDesc      = view.findViewById<TextView>(R.id.tvTaskDesc)
        private val progressBar = view.findViewById<ProgressBar>(R.id.pbTaskProgress)
        private val iv1         = view.findViewById<ImageView>(R.id.ivAvatar1)
        private val iv2         = view.findViewById<ImageView>(R.id.ivAvatar2)
        private val iv3         = view.findViewById<ImageView>(R.id.ivAvatar3)
        private val tvMore      = view.findViewById<TextView>(R.id.tvAvatarMore)
        private val ivMenu      = view.findViewById<ImageView>(R.id.ivTaskMenu)

        fun bind(task: TaskModel, position: Int) {
            // Basic info
            tvTitle.text = task.title
            tvDesc.text  = task.description
            progressBar.progress = task.progress

            // Avatar placeholders + "+N"
            val count = task.usercount
            listOf(iv1, iv2, iv3).forEachIndexed { i, iv ->
                if (i < count && i < 3) {
                    iv.setImageResource(R.drawable.ic_avatar_placeholder)
                    iv.visibility = View.VISIBLE
                } else iv.visibility = View.GONE
            }
            if (count > 3) {
                tvMore.visibility = View.VISIBLE
                tvMore.text = "+${count - 3}"
            } else {
                tvMore.visibility = View.GONE
            }

            // Card click → detail
            itemView.setOnClickListener {
                listener.onTaskClicked(task, position)
            }

            // Overflow menu
            ivMenu.setOnClickListener { v ->
                PopupMenu(v.context, v).apply {
                    inflate(R.menu.menu_task_item)
                    setOnMenuItemClickListener { mi ->
                        when (mi.itemId) {
                            R.id.action_edit   -> { listener.onEdit(task, position); true }
                            R.id.action_delete -> { listener.onDelete(task, position); true }
                            else               -> false
                        }
                    }
                    show()
                }
            }
        }
    }
}