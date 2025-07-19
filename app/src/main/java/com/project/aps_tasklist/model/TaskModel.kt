package com.project.aps_tasklist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskModel(
    val id: String = "",                     // Firestore doc ID
    val title: String = "",
    val description: String = "",
    val type: String = "individual",         // "individual" atau "group"
    val groupId: String? = null,             // kode grup kalau type="group"
    val createdBy: String = "",              // UID pembuat
    val createdAt: Long = 0L,                // millis
    val lastInteractedMillis: Long = createdAt,
    val deadlineMillis: Long = createdAt,
    val progress: Int = 0,                   // 0–100
    val usercount: Int = 1                   // 1 untuk individual, >1 untuk group
) : Parcelable

