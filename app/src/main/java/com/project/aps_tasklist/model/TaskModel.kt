package com.project.aps_tasklist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskModel(
    val title: String,
    val description: String,
    val usercount : Int,
    var progress: Int,
    var lastInteractedMillis: Long,
    var createdMillis: Long = lastInteractedMillis,
    var deadlineMillis: Long = lastInteractedMillis
) : Parcelable
