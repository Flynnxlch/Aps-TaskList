package com.project.aps_tasklist.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.aps_tasklist.R
import com.project.aps_tasklist.adapter.TaskAdapter
import com.project.aps_tasklist.adapter.TaskListAdapter
import com.project.aps_tasklist.model.TaskListItem
import com.project.aps_tasklist.model.TaskModel
import java.text.SimpleDateFormat
import java.util.*

class TaskListActivity : AppCompatActivity(),
    TaskAdapter.TaskActionListener {

    private lateinit var rvTaskList: RecyclerView
    private val tasksData = mutableListOf<TaskModel>()
    private lateinit var listAdapter: TaskListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.taskListLayout)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        rvTaskList = findViewById(R.id.rvTaskList)
        rvTaskList.layoutManager = LinearLayoutManager(this)
        listAdapter = TaskListAdapter(emptyList(), this)
        rvTaskList.adapter = listAdapter

        loadDummyTasks()
        buildAndShowList()
    }

    private fun loadDummyTasks() {
        val now = System.currentTimeMillis()
        tasksData.apply {
            add(TaskModel("Buy groceries", "Milk, eggs, bread", 2, 40, now - 0 * DAY_MS))
            add(TaskModel("Write report", "Annual financials", 1, 100, now - 1 * DAY_MS))
            add(TaskModel("Team meeting", "Discuss roadmap", 3, 60, now - 2 * DAY_MS))
            add(TaskModel("Read book", "Chapter 5–6", 1, 100, now - 5 * DAY_MS))
        }
    }

    private fun buildAndShowList() {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayKey = todayFormat.format(Date())

        val sorted = tasksData.sortedByDescending { it.lastInteractedMillis }
        val (incomplete, complete) = sorted.partition { it.progress < 100 }

        val items = mutableListOf<TaskListItem>()

        fun addSection(tasks: List<TaskModel>) {
            var lastLabel = ""
            for (task in tasks) {
                val taskDate = Date(task.lastInteractedMillis)
                val taskKey = todayFormat.format(taskDate)
                val label = if (taskKey == todayKey) {
                    "Hari ini"
                } else {
                    dateFormat.format(taskDate)
                }

                if (label != lastLabel) {
                    items.add(TaskListItem.Header(label))
                    lastLabel = label
                }
                items.add(TaskListItem.TaskItem(task))
            }
        }

        // add incomplete first
        addSection(incomplete)

        // add completed with header
        if (complete.isNotEmpty()) {
            items.add(TaskListItem.Header("Completed"))
            addSection(complete)
        }

        listAdapter.update(items)
    }

    override fun onEdit(task: TaskModel, position: Int) {
        val intent = Intent(this, AddTaskActivity::class.java).apply {
            putExtra("mode", "edit")
            putExtra("task", task)
            putExtra("pos", position)
        }
        startActivity(intent)
    }

    override fun onDelete(task: TaskModel, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Delete \"${task.title}\"?")
            .setPositiveButton("Yes") { _, _ ->
                tasksData.remove(task)
                buildAndShowList()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onTaskClicked(task: TaskModel, position: Int) {
        // Update interaksi terakhir
        task.lastInteractedMillis = System.currentTimeMillis()
        buildAndShowList()
    }

    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
