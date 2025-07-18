package com.project.aps_tasklist.activity

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.project.aps_tasklist.R
import com.project.aps_tasklist.adapter.NotificationAdapter
import com.project.aps_tasklist.model.Notification
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NotificationActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)

        // Toolbar back arrow
        toolbar = findViewById(R.id.toolbarNotifications)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // RecyclerView setup
        val rv = findViewById<RecyclerView>(R.id.rvNotifications)
        rv.layoutManager = LinearLayoutManager(this)

        // Ambil waktu lokal sekarang
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .time

        // Dummy data
        val dummy = listOf(
            Notification(
                title     = "Task Deadline",
                message   = "Submit report hari ini",
                timestamp = now,
                colorResId = R.color.green
            ),
            Notification(
                title     = "New Member",
                message   = "Alice ditambahkan ke Project X",
                timestamp = LocalTime(11, 15),
                colorResId = R.color.blue
            ),
            Notification(
                title     = "Task Completed",
                message   = "Build UI selesai",
                timestamp = LocalTime(9, 5),
                colorResId = R.color.gray
            )
        )
        rv.adapter = NotificationAdapter(dummy)

        // Edge‑to‑edge padding
        val root = findViewById<View>(R.id.notificationsLayout)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }
    }
}
