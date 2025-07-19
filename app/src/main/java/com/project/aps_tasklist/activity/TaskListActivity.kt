package com.project.aps_tasklist.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.aps_tasklist.R
import com.project.aps_tasklist.adapter.TaskListAdapter
import com.project.aps_tasklist.model.TaskListItem
import com.project.aps_tasklist.model.TaskModel
import java.text.SimpleDateFormat
import java.util.*

class TaskListActivity : AppCompatActivity(),
    TaskListAdapter.TaskActionListener {
    private lateinit var rvTaskList: RecyclerView
    private val tasksData = mutableListOf<TaskModel>()
    private lateinit var listAdapter: TaskListAdapter
    private lateinit var toolbar: Toolbar

    private val firestore = FirebaseFirestore.getInstance()
    private val auth      = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_list)

        toolbar = findViewById(R.id.toolbarTaskList)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }


        // Edge‑to‑edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.taskListLayout)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        rvTaskList = findViewById(R.id.rvTaskList)
        rvTaskList.layoutManager = LinearLayoutManager(this)
        listAdapter = TaskListAdapter(emptyList(), this)
        rvTaskList.adapter = listAdapter

        loadAllTasks()
    }

    private fun loadAllTasks() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("tasks")
            .whereEqualTo("createdBy", uid)
            .get()
            .addOnSuccessListener { snaps ->
                tasksData.clear()
                snaps.documents.mapNotNullTo(tasksData) { doc ->
                    doc.toObject(TaskModel::class.java)?.copy(id = doc.id)
                }
                // setelah data siap, build & tampilkan list sectioned
                buildAndShowList()
            }
            .addOnFailureListener { ex ->
                Toast.makeText(this, ex.message, Toast.LENGTH_LONG).show()
            }
    }


    private fun buildAndShowList() {
        val dateFmt  = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val keyFmt   = SimpleDateFormat("yyyyMMdd",    Locale.getDefault())
        val todayKey = keyFmt.format(Date())

        val sorted = tasksData.sortedByDescending { it.lastInteractedMillis }
        val (incomplete, complete) = sorted.partition { it.progress < 100 }

        val items = mutableListOf<TaskListItem>()
        fun addSection(list: List<TaskModel>) {
            var lastLabel = ""
            for (t in list) {
                val dateKey = keyFmt.format(Date(t.lastInteractedMillis))
                val label = if (dateKey == todayKey) "Hari ini" else dateFmt.format(Date(t.lastInteractedMillis))
                if (label != lastLabel) {
                    items.add(TaskListItem.Header(label))
                    lastLabel = label
                }
                items.add(TaskListItem.TaskItem(t))
            }
        }

        addSection(incomplete)
        if (complete.isNotEmpty()) {
            items.add(TaskListItem.Header("Completed"))
            addSection(complete)
        }

        listAdapter.update(items)
    }

    override fun onTaskClicked(task: TaskModel, position: Int) {
        val newTs = System.currentTimeMillis()

        // 1) Update lastInteracted di Firestore
        FirebaseFirestore.getInstance()
            .collection("tasks")
            .document(task.id)
            .update("lastInteracted", newTs)
            .addOnSuccessListener {
                loadAllTasks()
                // 3) Buka Detail
                startActivity(Intent(this, DetailActivity::class.java).apply {
                    putExtra("task", task)
                    putExtra("pos", position)
                    putExtra("isGroupTask", task.usercount > 1)
                })
            }
            .addOnFailureListener { ex ->
                Toast.makeText(this, "Gagal update interaction", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onEdit(task: TaskModel, position: Int) {
        startActivity(Intent(this, AddTaskActivity::class.java).apply {
            putExtra("mode", "edit")
            putExtra("task", task)
            putExtra("pos", position)
        })
    }

    override fun onDelete(task: TaskModel, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Delete “${task.title}”?")
            .setPositiveButton("Yes") { _, _ ->
                tasksData.removeAt(position)
                buildAndShowList()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }
    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
