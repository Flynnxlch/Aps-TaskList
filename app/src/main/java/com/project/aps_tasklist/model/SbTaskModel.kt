package com.project.aps_tasklist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SbTaskModel(
    val id: String = "",
    val name: String = "",
    val deadlineMillis: Long = 0L,
    var completedBy: String? = null,
    var comment: String? = null,
    val hasReminder: Boolean = false,
    val reminderOffsetMs: Long = 0L,
    val assignToMe: Boolean = false,
    val isDone: Boolean = false
) : Parcelable


