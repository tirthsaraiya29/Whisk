package com.whisk.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.TaskItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel(
    private val notesRepository: NotesRepository,
) : ViewModel() {

    val tasks: StateFlow<List<TaskItem>> = notesRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTask(taskId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            notesRepository.toggleTaskStatus(taskId, !currentStatus)
        }
    }
}
