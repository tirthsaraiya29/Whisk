package com.whisk.ui.screens.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.ai.LocalGemmaManager
import com.whisk.ai.RemoteAiManager
import com.whisk.data.repository.DailyNotesRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.ActivityEvent
import com.whisk.domain.model.Note
import com.whisk.domain.model.TaskItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class DailyNotesUiState(
    val dateKey: String = "",
    val dailyNote: Note? = null,
    val activityEvents: List<ActivityEvent> = emptyList(),
    val tasksToday: List<TaskItem> = emptyList(),
    val isAiThinking: Boolean = false,
    val aiSuggestion: String? = null,
    val isSaving: Boolean = false,
)

class DailyNotesViewModel @Suppress("UNUSED_PARAMETER") constructor(
    private val dailyNotesRepository: DailyNotesRepository,
    private val notesRepository: NotesRepository,
    localGemmaManager: LocalGemmaManager? = null,
    remoteAiManager: RemoteAiManager? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyNotesUiState())
    val uiState: StateFlow<DailyNotesUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        val today = dailyNotesRepository.getTodayDateKey()
        loadDate(today)
    }

    fun loadDate(dateKey: String) {
        _uiState.value = _uiState.value.copy(dateKey = dateKey, aiSuggestion = null)
        viewModelScope.launch {
            val note = dailyNotesRepository.getOrCreateDailyNote(dateKey)
            _uiState.value = _uiState.value.copy(dailyNote = note)

            // Observe activity events for date
            dailyNotesRepository.getDailyActivityEvents(dateKey).collect { events ->
                _uiState.value = _uiState.value.copy(activityEvents = events)
            }
        }
        viewModelScope.launch {
            val tasks = dailyNotesRepository.getDailyTasks()
            _uiState.value = _uiState.value.copy(tasksToday = tasks)
        }
    }

    fun goToToday() {
        loadDate(dailyNotesRepository.getTodayDateKey())
    }

    fun goToPreviousDay() {
        val cal = parseCalFromDateKey(_uiState.value.dateKey)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        loadDate(dateKeyFormat.format(cal.time))
    }

    fun goToNextDay() {
        val cal = parseCalFromDateKey(_uiState.value.dateKey)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        loadDate(dateKeyFormat.format(cal.time))
    }

    fun updateJournalContent(title: String, body: String) {
        val current = _uiState.value.dailyNote ?: return
        val updated = current.copy(title = title, body = body, updatedAt = System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(dailyNote = updated, isSaving = true)

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            notesRepository.saveNote(updated)
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    fun toggleTask(taskId: String, targetStatus: Boolean? = null) {
        viewModelScope.launch {
            notesRepository.toggleTaskStatus(taskId, targetStatus)
            val tasks = dailyNotesRepository.getDailyTasks()
            _uiState.value = _uiState.value.copy(tasksToday = tasks)
        }
    }

    fun generateDailyReflection() {
        val state = _uiState.value
        _uiState.value = state.copy(isAiThinking = true)

        viewModelScope.launch {
            delay(600.milliseconds)
            val reflection = fallbackReflection(state)
            _uiState.value = _uiState.value.copy(
                isAiThinking = false,
                aiSuggestion = reflection,
            )
        }
    }

    fun applyAiSuggestion() {
        val suggestion = _uiState.value.aiSuggestion ?: return
        val current = _uiState.value.dailyNote ?: return
        val newBody = current.body + "\n\n## AI Reflection\n$suggestion"
        updateJournalContent(current.title, newBody)
        _uiState.value = _uiState.value.copy(aiSuggestion = null)
    }

    fun dismissAiSuggestion() {
        _uiState.value = _uiState.value.copy(aiSuggestion = null)
    }

    private fun fallbackReflection(state: DailyNotesUiState): String {
        val completedCount = state.tasksToday.count { it.isCompleted }
        val totalCount = state.tasksToday.size
        return """
- Accomplished $completedCount out of $totalCount tasks today.
- Logged ${state.activityEvents.size} workspace activities on ${state.dateKey}.
- Maintain momentum on active projects and review unfinished items tomorrow.
        """.trimIndent()
    }

    private fun parseCalFromDateKey(dateKey: String): Calendar {
        val cal = Calendar.getInstance()
        try {
            val date = dateKeyFormat.parse(dateKey)
            if (date != null) cal.time = date
        } catch (_: Exception) {}
        return cal
    }
}
