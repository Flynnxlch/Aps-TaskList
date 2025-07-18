package com.project.aps_tasklist.model

sealed class TaskListItem {
    data class Header(val label: String) : TaskListItem()
    data class TaskItem(val task: TaskModel) : TaskListItem()
}
