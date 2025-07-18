package com.project.aps_tasklist.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.appbar.MaterialToolbar
import com.project.aps_tasklist.model.TaskModel
import com.project.aps_tasklist.R
import com.project.aps_tasklist.adapter.TaskAdapter

class MainActivity : AppCompatActivity(),
    TaskAdapter.TaskActionListener {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvTasks: RecyclerView
    private lateinit var bannerTask: View
    private lateinit var ivBannerClose: ImageView
    private lateinit var btnAddTask: MaterialButton
    private lateinit var btnSchedule: MaterialButton
    private lateinit var tvSeeAll: MaterialTextView

    private val tasksData = mutableListOf<TaskModel>()
    private lateinit var adapter: TaskAdapter
    private var hasUnreadNotifications = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainactlayout)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        bindViews()
        setupToolbar()
        setupQuickActions()
        setupDismissibleBanner()

        // prepare adapter & RecyclerView
        rvTasks.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(emptyList(), this)
        rvTasks.adapter = adapter

        loadTasks()
        showTopTasks()
    }

    private fun bindViews() {
        toolbar       = findViewById(R.id.topAppBar)
        rvTasks       = findViewById(R.id.rvTasks)
        bannerTask    = findViewById(R.id.bannerTasks)
        ivBannerClose = findViewById(R.id.ivBannerClose)
        btnAddTask    = findViewById(R.id.btnAddTask)
        btnSchedule   = findViewById(R.id.btnSchedule)
        tvSeeAll      = findViewById(R.id.tvSeeAll)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
            hasUnreadNotifications = false
            updateNotificationIcon()
        }
        updateNotificationIcon()
    }

    private fun updateNotificationIcon() {
        val iconRes = if (hasUnreadNotifications)
            R.drawable.ic_notifications_badged else R.drawable.ic_notifications
        toolbar.navigationIcon = getDrawable(iconRes)
    }

    private fun setupQuickActions() {
        btnAddTask.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
        btnSchedule.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
        tvSeeAll.setOnClickListener {
            startActivity(Intent(this, TaskListActivity::class.java))
        }
    }

    private fun setupDismissibleBanner() {
        ivBannerClose.setOnClickListener {
            bannerTask.animate()
                .translationX(-bannerTask.width.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction { bannerTask.visibility = View.GONE }
                .start()
        }
        bannerTask.setOnClickListener {
            startActivity(Intent(this, TaskListActivity::class.java))
        }
    }

    private fun loadTasks() {
        // TODO: Ganti dengan data nyata dari DB/API
        val now = System.currentTimeMillis()
        tasksData.clear()
        tasksData.add(TaskModel("Task 1", "Description for Task 1", 2, 50, lastInteractedMillis = now))
        tasksData.add(TaskModel("Task 2", "Description for Task 2", 3, 75, lastInteractedMillis = now - 10_000))
        tasksData.add(TaskModel("Task 3", "Description for Task 3", 1, 25, lastInteractedMillis = now - 5_000))
        tasksData.add(TaskModel("Task 4", "Finished task",        1, 100, lastInteractedMillis = now - 20_000))
        tasksData.add(TaskModel("Task 5", "Another in progress",  2, 80, lastInteractedMillis = now - 2_000))
    }

    private fun showTopTasks() {
        // Urut berdasarkan interaksi terakhir
        val sorted = tasksData.sortedByDescending { it.lastInteractedMillis }
        val incomplete = sorted.filter { it.progress < 100 }
        val complete   = sorted.filter { it.progress >= 100 }

        val toShow = if (incomplete.isNotEmpty()) {
            incomplete.take(4)
        } else {
            complete.take(4)
        }
        adapter.updateData(toShow)
    }

    // --- TaskAdapter.TaskActionListener ---

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
            .setMessage("Delete “${task.title}”?")
            .setPositiveButton("Yes") { _, _ ->
                tasksData.removeAt(position)
                showTopTasks()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onTaskClicked(task: TaskModel, position: Int) {
        // update lastInteracted, lalu refresh
        task.lastInteractedMillis = System.currentTimeMillis()
        showTopTasks()
        // buka detail
        startActivity(Intent(this, DetailActivity::class.java).putExtra("task", task))
    }
}
