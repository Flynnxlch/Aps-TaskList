package com.project.aps_tasklist.model

import kotlinx.datetime.LocalTime

data class Notification(
    val title: String,
    val message: String,
    val timestamp: LocalTime,
    val colorResId: Int = 0 // Optional, default to 0 if not specified
)
