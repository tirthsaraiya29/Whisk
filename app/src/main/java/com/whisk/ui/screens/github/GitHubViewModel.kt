package com.whisk.ui.screens.github

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.local.PreferencesManager
import com.whisk.data.repository.GitHubRepository
import com.whisk.data.repository.McpRepository
import com.whisk.domain.model.GitHubCommit
import com.whisk.domain.model.GitHubIssue
import com.whisk.domain.model.GitHubPullRequest
import com.whisk.domain.model.GitHubRepo
import com.whisk.domain.model.McpTool
import com.whisk.domain.model.McpToolLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GitHubUiState(
    val searchQuery: String = "",
    val repos: List<GitHubRepo> = emptyList(),
    val selectedRepo: GitHubRepo? = null,
    val issues: List<GitHubIssue> = emptyList(),
    val pullRequests: List<GitHubPullRequest> = emptyList(),
    val commits: List<GitHubCommit> = emptyList(),
    val mcpTools: List<McpTool> = emptyList(),
    val mcpLogs: List<McpToolLog> = emptyList(),
    val isMcpConnected: Boolean = false,
    val isGitDiagramOpen: Boolean = false,
    val token: String = "",
)

class GitHubViewModel(
    private val gitHubRepository: GitHubRepository,
    private val mcpRepository: McpRepository,
    preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("android")
    private val _repos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    private val _selectedRepo = MutableStateFlow<GitHubRepo?>(null)
    private val _issues = MutableStateFlow<List<GitHubIssue>>(emptyList())
    private val _prs = MutableStateFlow<List<GitHubPullRequest>>(emptyList())
    private val _commits = MutableStateFlow<List<GitHubCommit>>(emptyList())
    private val _isGitDiagramOpen = MutableStateFlow(value = false)

    val uiState: StateFlow<GitHubUiState> = combine(
        combine(_searchQuery, _repos, _selectedRepo) { q, r, s -> Triple(q, r, s) },
        combine(mcpRepository.discoveredTools, mcpRepository.executionLogs) { tools, logs -> Pair(tools, logs) },
        preferencesManager.githubToken,
    ) { t1, t2, token ->
        GitHubUiState(
            searchQuery = t1.first,
            repos = t1.second,
            selectedRepo = t1.third,
            issues = _issues.value,
            pullRequests = _prs.value,
            commits = _commits.value,
            mcpTools = t2.first,
            mcpLogs = t2.second,
            isMcpConnected = mcpRepository.isConnected.value,
            isGitDiagramOpen = _isGitDiagramOpen.value,
            token = token,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GitHubUiState())

    init {
        searchRepositories("android")
        connectMcpServer("http://localhost:8080/mcp")
    }

    fun searchRepositories(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val token = uiState.value.token.ifBlank { null }
            val results = gitHubRepository.searchRepositories(query, token)
            _repos.value = results
        }
    }

    fun selectRepository(repo: GitHubRepo) {
        _selectedRepo.value = repo
        val token = uiState.value.token.ifBlank { null }
        viewModelScope.launch {
            _issues.value = gitHubRepository.getIssues(repo.fullName, token)
            _prs.value = gitHubRepository.getPullRequests(repo.fullName, token)
            _commits.value = gitHubRepository.getCommits(repo.fullName, token)
        }
    }

    fun connectMcpServer(endpointUrl: String) {
        viewModelScope.launch {
            mcpRepository.connectAndDiscoverTools(endpointUrl)
        }
    }

    fun invokeMcpTool(toolName: String, argsJson: String) {
        viewModelScope.launch {
            val endpoint = "http://localhost:8080/mcp"
            mcpRepository.invokeTool(endpoint, toolName, argsJson)
        }
    }

    fun toggleGitDiagram(open: Boolean) {
        _isGitDiagramOpen.value = open
    }
}
