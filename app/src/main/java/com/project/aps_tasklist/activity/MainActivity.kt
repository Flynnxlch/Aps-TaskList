package com.project.aps_tasklist.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var tvUsername: MaterialTextView
    private lateinit var tvTaskCount: TextView

    private val tasksData = mutableListOf<TaskModel>()
    private lateinit var adapter: TaskAdapter
    private var hasUnreadNotifications = true
    private val firestore = FirebaseFirestore.getInstance()
    private val auth      = FirebaseAuth.getInstance()

    private val REQUEST_CODE_ADD_TASK = 100
    private val REQUEST_CODE_DETAIL = 101

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

        val uid = auth.currentUser?.uid
        if (uid != null) {
            val rtRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("username")
            rtRef.get()
                .addOnSuccessListener { snap ->
                    val name = snap.getValue(String::class.java)
                    if (!name.isNullOrBlank()) {
                        tvUsername.text = name
                    } else {
                        loadUsernameFromFirestore(uid)
                    }
                }
                .addOnFailureListener {
                    loadUsernameFromFirestore(uid)
                }
        }

        // prepare adapter & RecyclerView
        rvTasks.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(emptyList(), this)
        rvTasks.adapter = adapter

        loadTasksFromFirestore()

        // Hitung unfinished tasks
        val incompleteCount = tasksData.count { it.progress < 100 }

        // Update teks banner
        if (incompleteCount > 0) {
            tvTaskCount.text = "You have $incompleteCount unfinished tasks today."
        } else {
            tvTaskCount.text = "Santai saja bos, Lagi tidak ada tugas"
        }
    }

    private fun bindViews() {
        toolbar       = findViewById(R.id.topAppBar)
        rvTasks       = findViewById(R.id.rvTasks)
        bannerTask    = findViewById(R.id.bannerTasks)
        ivBannerClose = findViewById(R.id.ivBannerClose)
        btnAddTask    = findViewById(R.id.btnAddTask)
        btnSchedule   = findViewById(R.id.btnSchedule)
        tvSeeAll      = findViewById(R.id.tvSeeAll)
        tvUsername    = findViewById(R.id.tvUsername)
        tvTaskCount   = findViewById(R.id.tvTaskCount)
    }

    private fun loadUsernameFromFirestore(uid: String) {
        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("username") ?: "User"
                tvUsername.text = name
            }
            .addOnFailureListener {
                tvUsername.text = "User"
            }
    }

    private fun loadTasksFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        val tasksRef = firestore.collection("tasks")

        // Ambil task di mana user adalah pembuat atau anggota (group)
        tasksRef.whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener { snap1 ->
                tasksRef.whereEqualTo("createdBy", uid)
                    .get()
                    .addOnSuccessListener { snap2 ->
                        val docs = (snap1.documents + snap2.documents).distinctBy { it.id }

                        tasksData.clear()
                        for (doc in docs) {
                            val map = doc.data ?: continue
                            try {
                                val task = TaskModel(
                                    id = doc.id,
                                    title = map["title"] as? String ?: "",
                                    description = map["description"] as? String ?: "",
                                    type = map["type"] as? String ?: "individual",
                                    groupId = map["groupId"] as? String,
                                    createdBy = map["createdBy"] as? String ?: "",
                                    createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
                                    lastInteractedMillis = (map["lastInteractedMillis"] as? Number)?.toLong()
                                        ?: (map["createdAt"] as? Number)?.toLong() ?: 0L,
                                    deadlineMillis = (map["deadlineMillis"] as? Number)?.toLong()
                                        ?: (map["createdAt"] as? Number)?.toLong() ?: 0L,
                                    progress = (map["progress"] as? Number)?.toInt() ?: 0,
                                    usercount = (map["usercount"] as? Number)?.toInt() ?: 1
                                )
                                tasksData.add(task)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        showTopTasks()

                        val incomplete = tasksData.count { it.progress < 100 }
                        tvTaskCount.text = if (incomplete > 0)
                            "You have $incomplete unfinished tasks today."
                        else
                            "Lagi tidak ada tugas"
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message ?: "Gagal memuat tugas", Toast.LENGTH_SHORT).show()
            }
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
            startActivityForResult(Intent(this, AddTaskActivity::class.java), REQUEST_CODE_ADD_TASK)
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
        val newTs = System.currentTimeMillis()

        // Update lastInteracted di Firestore
        FirebaseFirestore.getInstance()
            .collection("tasks")
            .document(task.id)
            .update("lastInteracted", newTs)
            .addOnSuccessListener {

                loadTasksFromFirestore()

                startActivityForResult(Intent(this, DetailActivity::class.java).apply {
                    putExtra("task", task)
                    putExtra("pos", position)
                    putExtra("isGroupTask", task.usercount > 1)
                }, REQUEST_CODE_DETAIL)
            }
            .addOnFailureListener { ex ->
                Toast.makeText(this, "Gagal update interaction", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && (requestCode == REQUEST_CODE_ADD_TASK || requestCode == REQUEST_CODE_DETAIL)) {
            loadTasksFromFirestore()
        }
    }


}
