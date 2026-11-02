package com.whisk.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.local.ChatSessionEntity
import com.whisk.data.repository.ChatRepository
import com.whisk.data.repository.GitHubRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.data.repository.ObjectTypeRepository
import com.whisk.data.repository.PropertyRepository
import com.whisk.data.repository.RelationshipRepository
import com.whisk.domain.model.GitHubIssue
import com.whisk.domain.model.GitHubPullRequest
import com.whisk.domain.model.GitHubRepo
import com.whisk.domain.model.Note
import com.whisk.domain.model.NotePropertyItem
import com.whisk.domain.model.ObjectTypeDefinition
import com.whisk.domain.model.RelationshipDetail
import com.whisk.domain.model.RelationshipType
import com.whisk.domain.model.TaskItem
import com.whisk.ui.screens.ai.ChatSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ProjectWorkspaceUiState(
    val project: Note? = null,
    val tasks: List<TaskItem> = emptyList(),
    val progress: Float = 0f,
    val relationships: List<RelationshipDetail> = emptyList(),
    val objectTypes: List<ObjectTypeDefinition> = emptyList(),
    val propertyItems: List<NotePropertyItem> = emptyList(),
    val aiSessions: List<ChatSessionEntity> = emptyList(),
    val linkedRepo: GitHubRepo? = null,
    val repoIssues: List<GitHubIssue> = emptyList(),
    val repoPullRequests: List<GitHubPullRequest> = emptyList(),
    val isLoadingRepo: Boolean = false,
)

class ProjectWorkspaceViewModel(
    private val projectId: String,
    private val notesRepository: NotesRepository,
    private val propertyRepository: PropertyRepository,
    private val relationshipRepository: RelationshipRepository,
    private val objectTypeRepository: ObjectTypeRepository,
    private val chatRepository: ChatRepository,
    private val gitHubRepository: GitHubRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectWorkspaceUiState())
    val uiState: StateFlow<ProjectWorkspaceUiState> = _uiState.asStateFlow()

    init {
        loadProjectData()
        observeData()
    }

    private fun loadProjectData() {
        viewModelScope.launch {
            val note = notesRepository.getNoteById(projectId)
            if (note != null) {
                _uiState.value = _uiState.value.copy(project = note)
                if (!note.repoRef.isNullOrBlank()) {
                    fetchRepoDetails(note.repoRef)
                }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            notesRepository.allTasks.collect { allTasks ->
                val projectTasks = allTasks.filter { it.noteId == projectId }
                val completedCount = projectTasks.count { it.isCompleted }
                val totalCount = projectTasks.size
                val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                _uiState.value = _uiState.value.copy(
                    tasks = projectTasks,
                    progress = progress,
                )
            }
        }

        viewModelScope.launch {
            relationshipRepository.getRelationshipsForObject(projectId).collect { rels ->
                _uiState.value = _uiState.value.copy(relationships = rels)
            }
        }

        viewModelScope.launch {
            objectTypeRepository.nonArchivedObjectTypeDefinitions.collect { types ->
                _uiState.value = _uiState.value.copy(objectTypes = types)
            }
        }

        viewModelScope.launch {
            propertyRepository.getPropertyItemsForNote(projectId).collect { props ->
                _uiState.value = _uiState.value.copy(propertyItems = props)
            }
        }

        viewModelScope.launch {
            chatRepository.allSessions.collect { sessions ->
                val projectSessions = sessions.filter { it.contextRef == projectId }
                _uiState.value = _uiState.value.copy(aiSessions = projectSessions)
            }
        }
    }

    fun updateProjectContent(title: String, description: String, body: String) {
        val current = _uiState.value.project ?: return
        val updated = current.copy(
            title = title,
            description = description,
            body = body,
            updatedAt = System.currentTimeMillis(),
        )
        _uiState.value = _uiState.value.copy(project = updated)
        viewModelScope.launch {
            notesRepository.saveNote(updated)
        }
    }

    fun updateStatus(status: String) {
        val current = _uiState.value.project ?: return
        val updated = current.copy(status = status, updatedAt = System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(project = updated)
        viewModelScope.launch {
            notesRepository.saveNote(updated)
        }
    }

    fun updateRepoRef(repoRef: String) {
        val current = _uiState.value.project ?: return
        val updated = current.copy(repoRef = repoRef, updatedAt = System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(project = updated)
        viewModelScope.launch {
            notesRepository.saveNote(updated)
            if (repoRef.isNotBlank()) {
                fetchRepoDetails(repoRef)
            } else {
                _uiState.value = _uiState.value.copy(linkedRepo = null, repoIssues = emptyList(), repoPullRequests = emptyList())
            }
        }
    }

    private suspend fun fetchRepoDetails(repoRef: String) {
        val parts = repoRef.split("/")
        if (parts.size != 2) return
        val owner = parts[0].trim()
        val name = parts[1].trim()

        _uiState.value = _uiState.value.copy(isLoadingRepo = true)
        try {
            val repo = gitHubRepository.getRepository(owner, name).getOrNull()
            val issues = gitHubRepository.getIssues("$owner/$name").getOrNull() ?: emptyList()
            val prs = gitHubRepository.getPullRequests("$owner/$name").getOrNull() ?: emptyList()
            _uiState.value = _uiState.value.copy(
                linkedRepo = repo,
                repoIssues = issues,
                repoPullRequests = prs,
                isLoadingRepo = false,
            )
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(isLoadingRepo = false)
        }
    }

    fun createProjectTask(taskTitle: String) {
        val project = _uiState.value.project ?: return
        if (taskTitle.isBlank()) return

        val newBody = if (project.body.isBlank()) {
            "- [ ] $taskTitle"
        } else {
            "${project.body}\n- [ ] $taskTitle"
        }
        val updated = project.copy(body = newBody, updatedAt = System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(project = updated)
        viewModelScope.launch {
            notesRepository.saveNote(updated)
        }
    }

    fun toggleTask(taskId: String, status: Boolean) {
        viewModelScope.launch {
            notesRepository.toggleTaskStatus(taskId, status)
        }
    }

    fun addRelationship(targetObjectId: String, relationshipType: RelationshipType) {
        viewModelScope.launch {
            relationshipRepository.addRelationship(projectId, targetObjectId, relationshipType)
        }
    }

    fun removeRelationship(relationshipId: String) {
        viewModelScope.launch {
            relationshipRepository.removeRelationship(relationshipId)
        }
    }

    suspend fun queryObjectsForPicker(query: String, typeId: String?): List<Note> {
        return notesRepository.getObjectsForPicker(query, typeId)
    }

    fun startNewAiSession(onSessionCreated: (String) -> Unit) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            val newSession = ChatSession(
                id = UUID.randomUUID().toString(),
                title = "Copilot for ${project.title}",
                timestamp = System.currentTimeMillis(),
                messages = emptyList(),
            )
            chatRepository.saveSession(newSession, contextRef = projectId)
            onSessionCreated(newSession.id)
        }
    }
}
