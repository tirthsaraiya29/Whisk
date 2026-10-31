package com.whisk.ui.screens.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.GraphRepository
import com.whisk.domain.model.GraphNode
import com.whisk.domain.model.KnowledgeGraph
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
)

class GraphViewModel(
    private val graphRepository: GraphRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    init {
        loadGlobalGraph()
    }

    fun loadGlobalGraph() {
        viewModelScope.launch {
            val graph = graphRepository.buildGlobalGraph()
            _uiState.value = _uiState.value.copy(
                graph = graph,
                isLocalGraph = false,
                centerNoteId = null,
            )
        }
    }

    fun loadLocalGraph(centerNoteId: String) {
        viewModelScope.launch {
            val graph = graphRepository.buildLocalGraph(centerNoteId, depth = 1)
            _uiState.value = _uiState.value.copy(
                graph = graph,
                isLocalGraph = true,
                centerNoteId = centerNoteId,
            )
        }
    }

    fun selectNode(node: GraphNode?) {
        _uiState.value = _uiState.value.copy(selectedNode = node)
    }

    fun toggleShowOrphans() {
        _uiState.value = _uiState.value.copy(showOrphans = !_uiState.value.showOrphans)
    }
}
