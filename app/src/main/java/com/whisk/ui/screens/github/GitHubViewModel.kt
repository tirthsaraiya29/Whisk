package com.whisk.ui.screens.github

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.local.PreferencesManager
import com.whisk.data.repository.GitHubRepository
import com.whisk.data.repository.McpRepository
import com.whisk.domain.model.GitHubCommit
import com.whisk.domain.model.GitHubError
import com.whisk.domain.model.GitHubIssue
import com.whisk.domain.model.GitHubPullRequest
import com.whisk.domain.model.GitHubRepo
import com.whisk.domain.model.GitHubResult
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
    val isLoadingRepos: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val repoError: GitHubError? = null,
    val detailsError: GitHubError? = null,
    val mcpTools: List<McpTool> = emptyList(),
    val mcpLogs: List<McpToolLog> = emptyList(),
    val isMcpConnected: Boolean = false,
    val isGitDiagramOpen: Boolean = false,
    val token: String = "",
)

class GitHubViewModel(
    private val gitHubRepository: GitHubRepository,
    private val mcpRepository: McpRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("android")
    private val _repos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    private val _selectedRepo = MutableStateFlow<GitHubRepo?>(null)
    private val _issues = MutableStateFlow<List<GitHubIssue>>(emptyList())
    private val _prs = MutableStateFlow<List<GitHubPullRequest>>(emptyList())
    private val _commits = MutableStateFlow<List<GitHubCommit>>(emptyList())
    private val _isLoadingRepos = MutableStateFlow(false)
    private val _isLoadingDetails = MutableStateFlow(false)
    private val _repoError = MutableStateFlow<GitHubError?>(null)
    private val _detailsError = MutableStateFlow<GitHubError?>(null)
    private val _isGitDiagramOpen = MutableStateFlow(value = false)

    val uiState: StateFlow<GitHubUiState> = combine(
        combine(_searchQuery, _repos, _selectedRepo, _isLoadingRepos, _isLoadingDetails) { q, r, s, lr, ld -> Tuple5(q, r, s, lr, ld) },
        combine(_repoError, _detailsError, mcpRepository.discoveredTools, mcpRepository.executionLogs) { re, de, tools, logs -> Tuple4(re, de, tools, logs) },
        preferencesManager.githubToken,
    ) { t1, t2, token ->
        GitHubUiState(
            searchQuery = t1.v1,
            repos = t1.v2,
            selectedRepo = t1.v3,
            isLoadingRepos = t1.v4,
            isLoadingDetails = t1.v5,
            repoError = t2.v1,
            detailsError = t2.v2,
            issues = _issues.value,
            pullRequests = _prs.value,
            commits = _commits.value,
            mcpTools = t2.v3,
            mcpLogs = t2.v4,
            isMcpConnected = mcpRepository.isConnected.value,
            isGitDiagramOpen = _isGitDiagramOpen.value,
            token = token,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GitHubUiState())

    init {
        searchRepositories("android")
        viewModelScope.launch {
            preferencesManager.mcpEndpoint.collect { endpoint ->
                if (endpoint.isNotBlank()) {
                    connectMcpServer(endpoint)
                }
            }
        }
    }

    fun searchRepositories(query: String) {
        _searchQuery.value = query
        _isLoadingRepos.value = true
        _repoError.value = null
        viewModelScope.launch {
            val token = uiState.value.token.ifBlank { null }
            when (val res = gitHubRepository.searchRepositories(query, token)) {
                is GitHubResult.Success -> {
                    _repos.value = res.data
                    _repoError.value = null
                }
                is GitHubResult.Error -> {
                    _repoError.value = res.error
                }
            }
            _isLoadingRepos.value = false
        }
    }

    fun selectRepository(repo: GitHubRepo) {
        _selectedRepo.value = repo
        _isLoadingDetails.value = true
        _detailsError.value = null
        val token = uiState.value.token.ifBlank { null }
        viewModelScope.launch {
            val issuesRes = gitHubRepository.getIssues(repo.fullName, token)
            val prsRes = gitHubRepository.getPullRequests(repo.fullName, token)
            val commitsRes = gitHubRepository.getCommits(repo.fullName, token)

            var firstErr: GitHubError? = null
            if (issuesRes is GitHubResult.Success) {
                _issues.value = issuesRes.data
            } else if (issuesRes is GitHubResult.Error) {
                firstErr = issuesRes.error
            }

            if (prsRes is GitHubResult.Success) {
                _prs.value = prsRes.data
            } else if (prsRes is GitHubResult.Error && firstErr == null) {
                firstErr = prsRes.error
            }

            if (commitsRes is GitHubResult.Success) {
                _commits.value = commitsRes.data
            } else if (commitsRes is GitHubResult.Error && firstErr == null) {
                firstErr = commitsRes.error
            }

            _detailsError.value = firstErr
            _isLoadingDetails.value = false
        }
    }

    fun connectMcpServer(endpointUrl: String) {
        viewModelScope.launch {
            mcpRepository.connectAndDiscoverTools(endpointUrl)
        }
    }

    fun invokeMcpTool(toolName: String, argsJson: String) {
        viewModelScope.launch {
            val tool = uiState.value.mcpTools.firstOrNull { it.name == toolName }
            val endpoint = tool?.serverEndpoint
                ?.ifBlank { null }
                ?: mcpRepository.activeServer.value?.endpointUrl
                ?: "http://localhost:8080/mcp"

            mcpRepository.invokeTool(endpoint, toolName, argsJson)
        }
    }

    fun toggleGitDiagram(open: Boolean) {
        _isGitDiagramOpen.value = open
    }
}

private data class Tuple5<A, B, C, D, E>(
    val v1: A,
    val v2: B,
    val v3: C,
    val v4: D,
    val v5: E,
)

private data class Tuple4<A, B, C, D>(
    val v1: A,
    val v2: B,
    val v3: C,
    val v4: D,
)
