package com.project.aps_tasklist.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.project.aps_tasklist.R
import com.project.aps_tasklist.adapter.SubTaskAdapter
import com.project.aps_tasklist.model.SbTaskModel
import com.project.aps_tasklist.model.TaskModel
import java.text.SimpleDateFormat
import java.util.*

class DetailActivity : AppCompatActivity(),
    SubTaskAdapter.SubtaskActionListener
{

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvOverallProgressLabel: TextView
    private lateinit var pbOverallProgress: ProgressBar
    private lateinit var tvSubtasksProgressLabel: TextView
    private lateinit var pbSubtasksProgress: ProgressBar
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailDesc: TextView
    private lateinit var tvDetailDeadline: TextView
    private lateinit var tvMembersLabel: TextView
    private lateinit var memberContainer: LinearLayout
    private lateinit var tvSubtaskHeader: TextView
    private lateinit var rvDetailSubtasks: RecyclerView

    private val firestore = FirebaseFirestore.getInstance()
    private val auth      = FirebaseAuth.getInstance()

    private lateinit var taskId: String
    private var isGroupTask = false
    private lateinit var taskModel: TaskModel
    private val subtasks = mutableListOf<SbTaskModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollDetail)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        bindViews()
        setupToolbar()
        loadIntentData()
        populateTaskInfo()

        val btnMarkComplete = findViewById<MaterialButton>(R.id.btnMarkComplete)
        btnMarkComplete.setOnClickListener {
            val total = subtasks.size
            val done = subtasks.count { it.completedBy != null }
            val progress = (done * 100 / total).coerceAtMost(100)

            firestore.collection("tasks")
                .document(taskId)
                .update("progress", progress)
                .addOnSuccessListener {
                    Toast.makeText(this, "Progress updated", Toast.LENGTH_SHORT).show()
                    finish() // kembali ke MainActivity
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal update progress", Toast.LENGTH_SHORT).show()
                }
        }


        firestore.collection("tasks")
            .document(taskId)
            .collection("subtasks")
            .get()
            .addOnSuccessListener { snaps ->
                subtasks.clear()
                for (doc in snaps) {
                    val sub = doc.toObject(SbTaskModel::class.java)
                    subtasks.add(sub.copy(
                        id = doc.id
                    ))
                }
                setupSubtasksSection()
                updateSubtasksProgress()
            }
    }

    private fun bindViews() {
        toolbar                 = findViewById(R.id.toolbarDetail)
        tvOverallProgressLabel  = findViewById(R.id.tvOverallProgressLabel)
        pbOverallProgress       = findViewById(R.id.pbOverallProgress)
        tvSubtasksProgressLabel = findViewById(R.id.tvSubtasksProgressLabel)
        pbSubtasksProgress      = findViewById(R.id.pbSubtasksProgress)
        tvDetailTitle           = findViewById(R.id.tvDetailTitle)
        tvDetailDesc            = findViewById(R.id.tvDetailDesc)
        tvDetailDeadline        = findViewById(R.id.tvDetailDeadline)
        tvMembersLabel          = findViewById(R.id.tvMembersLabel)
        memberContainer         = findViewById(R.id.memberContainer)
        tvSubtaskHeader         = findViewById(R.id.tvSubtaskHeader)
        rvDetailSubtasks        = findViewById(R.id.rvDetailSubtasks)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadIntentData() {
        taskModel   = intent.getParcelableExtra("task")!!
        taskId      = taskModel.id
        isGroupTask = intent.getBooleanExtra("isGroupTask", false)
    }

    private fun populateTaskInfo() {
        // 2) Tampilkan parent fields
        tvDetailTitle.text = taskModel.title
        tvDetailDesc.text = taskModel.description
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        tvDetailDeadline.text = "Due: ${sdf.format(Date(taskModel.deadlineMillis))}"

        // 3) Fetch anggota grup (jika ada)
        if (isGroupTask) {
            firestore.collection("groups")
                .document(taskModel.groupId!!)
                .get()
                .addOnSuccessListener { grp ->
                    val members = grp.get("members") as? List<String> ?: emptyList()
                    tvMembersLabel.visibility = View.VISIBLE
                    memberContainer.visibility = View.VISIBLE
                    memberContainer.removeAllViews()
                    for (m in members) {
                        val tv = TextView(this).apply {
                            textSize = 14f; text = "• $m"
                        }
                        memberContainer.addView(tv)
                    }
                }
        }
    }

    private fun setupSubtasksSection() {
        if (isGroupTask) {
            tvSubtasksProgressLabel.visibility = View.VISIBLE
            pbSubtasksProgress.visibility      = View.VISIBLE
            updateSubtasksProgress()
        }

        rvDetailSubtasks.layoutManager   = LinearLayoutManager(this)
        rvDetailSubtasks.adapter         = SubTaskAdapter(subtasks, isGroupTask, this)
    }

    private fun updateSubtasksProgress() {
        val doneCount = subtasks.count { it.completedBy != null }
        val percent   = if (subtasks.isNotEmpty()) doneCount * 100 / subtasks.size else 0
        pbSubtasksProgress.progress = percent
        pbOverallProgress.progress  = percent
    }

    /** Dari interface SubtaskActionListener */
    override fun onSubtaskToggled(
        subId: String,
        position: Int,
        isDone: Boolean,
        completedBy: String?,
        comment: String?
    ) {
        val subDocRef = firestore.collection("tasks")
            .document(taskId)
            .collection("subtasks")
            .document(subId)

        // 1) Update subtask
        subDocRef.set(
            mapOf(
                "isDone"       to isDone,
                "completedBy"  to completedBy,
                "comment"      to comment
            ),
            SetOptions.merge()
        )

        // 2) Update local model & UI
        subtasks[position].completedBy = completedBy
        updateSubtasksProgress()

        // 3) Update parent progress & lastInteracted
        val newProgress = pbOverallProgress.progress
        val newTs       = Timestamp.now()
        firestore.collection("tasks")
            .document(taskId)
            .update(
                "progress", newProgress,
                "lastInteracted", newTs
            )
            .addOnSuccessListener {
                // 4) Kembalikan hasil ke parent jika perlu
                val intent = Intent().apply {
                    putExtra("updatedTask", taskModel.copy(
                        progress               = newProgress,
                        lastInteractedMillis   = newTs.toDate().time
                    ))
                    putExtra("updatedPos", intent.getIntExtra("pos", -1))
                }
                setResult(RESULT_OK, intent)
            }
    }

}
