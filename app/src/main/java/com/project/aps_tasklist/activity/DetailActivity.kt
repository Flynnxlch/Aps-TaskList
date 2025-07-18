package com.project.aps_tasklist.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
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

    private var isGroupTask = false
    private lateinit var taskModel: TaskModel
    private lateinit var subtasks: MutableList<SbTaskModel>

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
        setupSubtasksSection()
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
        isGroupTask = intent.getBooleanExtra("isGroupTask", false)
        subtasks = intent
            .getParcelableArrayListExtra<SbTaskModel>("subtasks")
            ?.toMutableList()
            ?: generateDummySubtasks().toMutableList()
    }

    private fun populateTaskInfo() {
        tvDetailTitle.text = taskModel.title
        tvDetailDesc.text  = taskModel.description
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        tvDetailDeadline.text = "Due: ${sdf.format(Date(taskModel.deadlineMillis))}"

        val members = intent.getStringArrayListExtra("members") ?: arrayListOf()
        if (isGroupTask && members.isNotEmpty()) {
            tvMembersLabel.visibility  = View.VISIBLE
            memberContainer.visibility = View.VISIBLE
            memberContainer.removeAllViews()
            for (m in members) {
                val tv = TextView(this).apply {
                    textSize = 14f
                    text = "• $m"
                }
                memberContainer.addView(tv)
            }
        }
    }

    private fun setupSubtasksSection() {
        if (isGroupTask) {
            tvSubtasksProgressLabel.visibility = View.VISIBLE
            pbSubtasksProgress.visibility      = View.VISIBLE
            updateSubtasksProgress()
        }

        rvDetailSubtasks.layoutManager =
            LinearLayoutManager(this)
        // Pasang listener 'this'
        rvDetailSubtasks.adapter =
            SubTaskAdapter(subtasks, isGroupTask, this)
    }

    private fun updateSubtasksProgress() {
        val doneCount = subtasks.count { it.completedBy != null }
        val percent   = if (subtasks.isNotEmpty()) doneCount * 100 / subtasks.size else 0
        pbSubtasksProgress.progress = percent
        pbOverallProgress.progress  = percent
    }

    /** Dari interface SubtaskActionListener */
    override fun onSubtaskToggled(position: Int, isDone: Boolean, completedBy: String?) {
        // Update model
        subtasks[position].completedBy = completedBy
        updateSubtasksProgress()

        // Simpan ke TaskModel
        taskModel.progress = pbOverallProgress.progress
        taskModel.lastInteractedMillis = System.currentTimeMillis()

        // Kembalikan data ke parent
        val result = Intent().apply {
            putExtra("updatedTask", taskModel)
            putExtra("updatedPos", intent.getIntExtra("pos", -1))
        }
        setResult(RESULT_OK, result)
    }

    private fun generateDummySubtasks(): List<SbTaskModel> {
        val now = System.currentTimeMillis()
        return listOf(
            SbTaskModel("Design UI",     now + 2_000_000_000, completedBy = "alice", comment = "Looks good"),
            SbTaskModel("API Backend",   now + 5_000_000_000, completedBy = null,    comment = null),
            SbTaskModel("Testing",       now - 1_000_000_000, completedBy = "bob",   comment = "Fixed bugs")
        )
    }
}
