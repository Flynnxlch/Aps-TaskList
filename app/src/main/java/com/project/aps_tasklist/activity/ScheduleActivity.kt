package com.project.aps_tasklist.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.project.aps_tasklist.R
import com.project.aps_tasklist.adapter.TaskAdapter
import com.project.aps_tasklist.model.TaskModel
import java.util.*

class ScheduleActivity : AppCompatActivity(), TaskAdapter.TaskActionListener {

    private lateinit var toolbar: Toolbar
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var rvScheduleEvents: RecyclerView
    private lateinit var placeholderNoEvents: View
    private lateinit var scheduleAdapter: TaskAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val tasksData = mutableListOf<TaskModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_schedule)

        toolbar = findViewById(R.id.toolbarSchedule)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scheduleLayout)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        calendarView = findViewById(R.id.calendarView)
        rvScheduleEvents = findViewById(R.id.rvScheduleEvents)
        placeholderNoEvents = findViewById(R.id.tvNoEvents)

        rvScheduleEvents.layoutManager = LinearLayoutManager(this)
        scheduleAdapter = TaskAdapter(emptyList(), this)
        rvScheduleEvents.adapter = scheduleAdapter

        calendarView.setOnDateChangedListener { _, date, _ ->
            showTasksForDate(date)
        }

        fetchTasksFromFirestore()
    }

    private fun fetchTasksFromFirestore() {
        firestore.collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                tasksData.clear()
                for (doc in result) {
                    val task = doc.toObject(TaskModel::class.java)
                    tasksData.add(task)
                }
                applyDateRangeHighlights()
                showTasksForDate(CalendarDay.today())
            }
    }

    private fun showTasksForDate(date: CalendarDay) {
        val cal = Calendar.getInstance().apply {
            set(date.year, date.month - 1, date.day)
        }
        val dayMillis = cal.timeInMillis

        val filtered = tasksData.filter { t ->
            t.progress < 100 && dayMillis in t.lastInteractedMillis..t.deadlineMillis
        }

        if (filtered.isEmpty()) {
            placeholderNoEvents.visibility = View.VISIBLE
            rvScheduleEvents.visibility = View.GONE
        } else {
            placeholderNoEvents.visibility = View.GONE
            rvScheduleEvents.visibility = View.VISIBLE
            scheduleAdapter.updateData(filtered)
        }
    }

    private fun applyDateRangeHighlights() {
        val palette = resources.obtainTypedArray(R.array.pill_colors).use { ta ->
            (0 until ta.length()).map { ta.getColor(it, 0) }.shuffled()
        }

        tasksData.forEachIndexed { idx, t ->
            val color = palette[idx % palette.size]

            val dLeft = getDrawable(R.drawable.pill_left)!!.mutate().apply { setTint(color) }
            val dMid = getDrawable(R.drawable.pill_middle)!!.mutate().apply { setTint(color) }
            val dRight = getDrawable(R.drawable.pill_right)!!.mutate().apply { setTint(color) }

            var cal = Calendar.getInstance().apply { timeInMillis = t.lastInteractedMillis }
            while (cal.timeInMillis <= t.deadlineMillis) {
                val day = CalendarDay.from(cal)
                val decorDrawable = when (cal.timeInMillis) {
                    t.lastInteractedMillis -> dLeft
                    t.deadlineMillis -> dRight
                    else -> dMid
                }
                calendarView.addDecorator(object : DayViewDecorator {
                    override fun shouldDecorate(d: CalendarDay) = d == day
                    override fun decorate(view: DayViewFacade) {
                        view.setSelectionDrawable(decorDrawable)
                    }
                })
                cal.add(Calendar.DATE, 1)
            }
        }
    }

    override fun onTaskClicked(task: TaskModel, position: Int) {
        startActivity(Intent(this, DetailActivity::class.java).apply {
            putExtra("task", task)
            putExtra("pos", position)
            putExtra("isGroupTask", task.usercount > 1)
        })
    }

    override fun onEdit(task: TaskModel, position: Int) {}
    override fun onDelete(task: TaskModel, position: Int) {}

    companion object {
        private fun nowOffset(daysAgo: Int): Long {
            return Calendar.getInstance().apply { add(Calendar.DATE, daysAgo) }
                .timeInMillis
        }
    }
}
