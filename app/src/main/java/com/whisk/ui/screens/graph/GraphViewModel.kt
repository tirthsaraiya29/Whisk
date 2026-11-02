package com.whisk.ui.screens.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.GraphRepository
import com.whisk.domain.model.GraphNode
import com.whisk.domain.model.KnowledgeGraph
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GraphUiState(
    val graph: KnowledgeGraph = KnowledgeGraph(),
    val selectedNode: GraphNode? = null,
    val isLocalGraph: Boolean = false,
    val centerNoteId: String? = null,
    val showOrphans: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class GraphViewModel(
    private val graphRepository: GraphRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadGlobalGraph()
    }

    fun loadGlobalGraph() {
        loadJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadJob = viewModelScope.launch(ioDispatcher) {
            try {
                val graph = graphRepository.buildGlobalGraph()
                _uiState.value = _uiState.value.copy(
                    graph = graph,
                    isLocalGraph = false,
                    centerNoteId = null,
                    isLoading = false,
                    error = null,
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to build graph",
                )
            }
        }
    }

    fun loadLocalGraph(centerNoteId: String) {
        loadJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadJob = viewModelScope.launch(ioDispatcher) {
            try {
                val graph = graphRepository.buildLocalGraph(centerNoteId, depth = 1)
                _uiState.value = _uiState.value.copy(
                    graph = graph,
                    isLocalGraph = true,
                    centerNoteId = centerNoteId,
                    isLoading = false,
                    error = null,
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to build local graph",
                )
            }
        }
    }

    fun selectNode(node: GraphNode?) {
        _uiState.value = _uiState.value.copy(selectedNode = node)
    }

    fun toggleShowOrphans() {
        _uiState.value = _uiState.value.copy(showOrphans = !_uiState.value.showOrphans)
    }
}
