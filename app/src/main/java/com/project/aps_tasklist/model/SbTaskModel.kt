package com.project.aps_tasklist.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SbTaskModel(
    val name: String,
    val deadlineMillis: Long,
    var completedBy: String?,   // ← var agar bisa diubah
    var comment: String?        // ← var agar bisa diubah
) : Parcelable
