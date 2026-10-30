package com.whisk

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whisk.ai.LocalGemmaManager
import com.whisk.ai.RemoteAiManager
import com.whisk.data.local.PreferencesManager
import com.whisk.data.local.WhiskDatabase
import com.whisk.data.repository.CanvasRepository
import com.whisk.data.repository.ExportImportRepository
import com.whisk.data.repository.GitHubRepository
import com.whisk.data.repository.GraphRepository
import com.whisk.data.repository.McpRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Note
import com.whisk.ui.components.CommandPalette
import com.whisk.ui.components.WhiskBottomBar
import com.whisk.ui.screens.ai.AiViewModel
import com.whisk.ui.screens.ai.AiWorkspaceScreen
import com.whisk.ui.screens.canvas.CanvasViewModel
import com.whisk.ui.screens.canvas.SpatialCanvasScreen
import com.whisk.ui.screens.github.GitHubBrowserScreen
import com.whisk.ui.screens.github.GitHubViewModel
import com.whisk.ui.screens.graph.GraphViewModel
import com.whisk.ui.screens.graph.KnowledgeGraphScreen
import com.whisk.ui.screens.notes.NoteDetailScreen
import com.whisk.ui.screens.notes.NoteDetailViewModel
import com.whisk.ui.screens.notes.NotesListScreen
import com.whisk.ui.screens.notes.NotesViewModel
import com.whisk.ui.screens.search.SearchScreen
import com.whisk.ui.screens.search.SearchViewModel
import com.whisk.ui.screens.settings.SettingsScreen
import com.whisk.ui.screens.settings.SettingsViewModel
import com.whisk.ui.screens.tasks.TasksScreen
import com.whisk.ui.screens.tasks.TasksViewModel
import com.whisk.ui.theme.WhiskTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import com.whisk.ui.components.SidebarDrawer
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
inline fun <reified T : ViewModel> viewModelFactory(crossinline creator: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
    }

class MainActivity : ComponentActivity() {

    private lateinit var database: WhiskDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notesRepository: NotesRepository
    private lateinit var graphRepository: GraphRepository
    private lateinit var canvasRepository: CanvasRepository
    private lateinit var gitHubRepository: GitHubRepository
    private lateinit var mcpRepository: McpRepository
    private lateinit var exportImportRepository: ExportImportRepository
    private lateinit var localGemmaManager: LocalGemmaManager
    private lateinit var remoteAiManager: RemoteAiManager

    private var exportZipLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            contentResolver.openOutputStream(it)?.use { stream ->
                lifecycleScope.launch {
                    exportImportRepository.exportVaultToZip(stream)
                    Toast.makeText(this@MainActivity, "Vault backup exported successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private var importZipLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val count = exportImportRepository.importVaultFromZip(it)
                Toast.makeText(this@MainActivity, "Imported $count notes into vault", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Application Dependencies
        database = WhiskDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        notesRepository = NotesRepository(database.noteDao(), database.folderDao(), database.taskDao())
        graphRepository = GraphRepository(database.noteDao())
        canvasRepository = CanvasRepository(database.canvasDao())
        gitHubRepository = GitHubRepository()
        mcpRepository = McpRepository()
        exportImportRepository = ExportImportRepository(this, database.noteDao(), notesRepository)
        localGemmaManager = LocalGemmaManager(this)
        remoteAiManager = RemoteAiManager()

        handleShareIntent(intent)

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            val isFirstLaunch by preferencesManager.isFirstLaunch.collectAsStateWithLifecycle(initialValue = true)

            WhiskTheme(themeMode = themeMode) {
                WhiskApp(
                    notesRepository = notesRepository,
                    graphRepository = graphRepository,
                    canvasRepository = canvasRepository,
                    gitHubRepository = gitHubRepository,
                    mcpRepository = mcpRepository,
                    exportImportRepository = exportImportRepository,
                    localGemmaManager = localGemmaManager,
                    remoteAiManager = remoteAiManager,
                    preferencesManager = preferencesManager,
                    isFirstLaunch = isFirstLaunch,
                    onExportZip = { exportZipLauncher.launch("Whisk_Vault_Backup.zip") },
                    onImportZip = { importZipLauncher.launch(arrayOf("application/zip", "text/plain", "text/markdown")) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "Quick Capture"
            if (sharedText.isNotBlank()) {
                lifecycleScope.launch {
                    val newNote = Note(
                        title = sharedSubject.ifBlank { "Quick Capture" },
                        body = sharedText,
                        createdAt = System.currentTimeMillis()
                    )
                    notesRepository.saveNote(newNote)
                    Toast.makeText(this@MainActivity, "Captured quick note to vault", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun WhiskApp(
    notesRepository: NotesRepository,
    graphRepository: GraphRepository,
    canvasRepository: CanvasRepository,
    gitHubRepository: GitHubRepository,
    mcpRepository: McpRepository,
    exportImportRepository: ExportImportRepository,
    localGemmaManager: LocalGemmaManager,
    remoteAiManager: RemoteAiManager,
    preferencesManager: PreferencesManager,
    isFirstLaunch: Boolean,
    onExportZip: () -> Unit,
    onImportZip: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var showCommandPalette by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "notes"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val allNotes by notesRepository.allNotes.collectAsStateWithLifecycle(initialValue = emptyList())

    // Seed Welcome Note on First Launch
    LaunchedEffect(isFirstLaunch) {
        if (isFirstLaunch) {
            notesRepository.seedWelcomeNoteIfEmpty()
        }
    }

    val hintsDismissed by preferencesManager.hintsDismissed.collectAsStateWithLifecycle(initialValue = emptyMap())

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f),
        drawerContent = {
            SidebarDrawer(
                notes = allNotes,
                currentNoteId = navBackStackEntry?.arguments?.getString("noteId"),
                onSelectNote = { noteId -> navController.navigate("note_detail/$noteId") },
                onNewNote = {
                    scope.launch {
                        val newNote = Note(title = "Untitled Note", body = "")
                        notesRepository.saveNote(newNote)
                        navController.navigate("note_detail/${newNote.id}")
                    }
                },
                onNavigate = { route -> navController.navigate(route) },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "notes",
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetX = { it / 4 }
                    ) + fadeIn()
                },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        targetOffsetX = { it / 4 }
                    ) + fadeOut()
                }
            ) {
            // 1. Notes List Destination
            composable("notes") {
                val viewModel: NotesViewModel = viewModel(
                    factory = viewModelFactory { NotesViewModel(notesRepository) }
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                NotesListScreen(
                    state = state,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNoteClick = { noteId -> navController.navigate("note_detail/$noteId") },
                    onNewNoteClick = {
                        scope.launch {
                            val newId = viewModel.createNewNote()
                            navController.navigate("note_detail/$newId")
                        }
                    },
                    onFilterChange = { filter -> viewModel.setFilter(filter) },
                    onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                    showHint = isFirstLaunch && !(hintsDismissed["links"] ?: false),
                    onDismissHint = {
                        scope.launch { preferencesManager.dismissHint("links") }
                    }
                )
            }

            // 2. Note Detail / Editor Destination
            composable(
                route = "note_detail/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                val viewModel: NoteDetailViewModel = viewModel(
                    key = noteId,
                    factory = viewModelFactory { NoteDetailViewModel(notesRepository, noteId) }
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(state.isDeleted) {
                    if (state.isDeleted) navController.popBackStack()
                }

                NoteDetailScreen(
                    state = state,
                    onTitleChange = { title, body -> viewModel.updateContent(title, body) },
                    onBackClick = {
                        viewModel.flushPendingSave()
                        navController.popBackStack()
                    },
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onDeleteNote = { viewModel.deleteNote() },
                    onOpenNote = { targetIdOrTitle ->
                        scope.launch {
                            val target = notesRepository.getNoteByTitle(targetIdOrTitle) ?: notesRepository.getNoteById(targetIdOrTitle)
                            if (target != null) {
                                navController.navigate("note_detail/${target.id}")
                            }
                        }
                    },
                    onOpenAiWithNote = { navController.navigate("ai?noteId=$noteId") }
                )
            }

            // 3. Search Destination
            composable("search") {
                val viewModel: SearchViewModel = viewModel(
                    factory = viewModelFactory { SearchViewModel(notesRepository) }
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                SearchScreen(
                    state = state,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onQueryChange = { q -> viewModel.updateQuery(q) },
                    onNoteClick = { noteId -> navController.navigate("note_detail/$noteId") }
                )
            }

            // 4. Knowledge Graph Destination
            composable("graph") {
                val viewModel: GraphViewModel = viewModel(
                    factory = viewModelFactory { GraphViewModel(graphRepository) }
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                KnowledgeGraphScreen(
                    state = state,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNodeSelect = { node -> viewModel.selectNode(node) },
                    onOpenNote = { noteId -> navController.navigate("note_detail/$noteId") },
                    onResetGraph = { viewModel.loadGlobalGraph() },
                    onToggleOrphans = { viewModel.toggleShowOrphans() }
                )
            }

            // 5. Spatial Canvas Destination
            composable("canvas") {
                val viewModel: CanvasViewModel = viewModel(
                    factory = viewModelFactory { CanvasViewModel(canvasRepository, notesRepository) }
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                SpatialCanvasScreen(
                    state = state,
                    onAddTextCard = { viewModel.addTextCard("Idea Card", "Double tap or drag card around spatial canvas") },
                    onAddNoteCard = { note -> viewModel.addNoteCard(note) },
                    onUpdateCardPos = { id, x, y -> viewModel.updateCardPosition(id, x, y) },
                    onDeleteCard = { id -> viewModel.deleteCard(id) },
                    onOpenNote = { noteId -> navController.navigate("note_detail/$noteId") }
                )
            }

            // 6. Tasks Checklist Destination
            composable("tasks") {
                val viewModel: TasksViewModel = viewModel(
                    factory = viewModelFactory { TasksViewModel(notesRepository) }
                )
                val tasks by viewModel.tasks.collectAsStateWithLifecycle(initialValue = emptyList())

                TasksScreen(
                    tasks = tasks,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onToggleTask = { taskId, status -> viewModel.toggleTask(taskId, status) },
                    onOpenNote = { noteId -> navController.navigate("note_detail/$noteId") }
                )
            }

            // 7. AI Workspace Destination
            composable(
                route = "ai?noteId={noteId}",
                arguments = listOf(navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val context = androidx.compose.ui.platform.LocalContext.current
                val noteId = backStackEntry.arguments?.getString("noteId")
                val viewModel: AiViewModel = viewModel(
                    key = noteId ?: "ai_global",
                    factory = viewModelFactory {
                        AiViewModel(localGemmaManager, remoteAiManager, preferencesManager).apply {
                            if (!noteId.isNullOrBlank()) {
                                loadNoteContext(notesRepository, noteId)
                            }
                        }
                    }
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                AiWorkspaceScreen(
                    state = state,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSendMessage = { text -> viewModel.sendMessage(text) },
                    onStopGeneration = { viewModel.stopGeneration() },
                    onDownloadModel = { viewModel.downloadLocalModel() },
                    onSwitchProvider = { provider -> viewModel.switchProvider(provider) },
                    onClearHistory = { viewModel.clearHistory() },
                    onStartNewSession = { viewModel.startNewSession() },
                    onLoadSession = { session -> viewModel.loadSession(session) },
                    onDeleteSession = { id -> viewModel.deleteSession(id) },
                    onApplyEdit = { edit ->
                        scope.launch {
                            val currentNote = state.contextPayload.activeNote
                            if (currentNote != null) {
                                val cleanEdit = edit.lines()
                                    .filterNot { it.trim().startsWith("```") }
                                    .joinToString("\n")
                                    .trim()
                                val updatedText = if (cleanEdit.isNotBlank()) cleanEdit else edit
                                val updated = currentNote.copy(body = currentNote.body + "\n\n" + updatedText)
                                notesRepository.saveNote(updated)
                                Toast.makeText(context, "Applied edit to '${currentNote.title}'", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.dismissEditPreview()
                        }
                    },
                    onDismissEdit = { viewModel.dismissEditPreview() },
                    onOpenNote = { targetNoteIdOrTitle ->
                        scope.launch {
                            val target = notesRepository.getNoteByTitle(targetNoteIdOrTitle) ?: notesRepository.getNoteById(targetNoteIdOrTitle)
                            if (target != null) {
                                navController.navigate("note_detail/${target.id}")
                            }
                        }
                    }
                )
            }

            // 8. GitHub & MCP Destination
            composable("github") {
                val viewModel: GitHubViewModel = viewModel(
                    factory = viewModelFactory { GitHubViewModel(gitHubRepository, mcpRepository, preferencesManager) }
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                GitHubBrowserScreen(
                    state = state,
                    onSearchQueryChange = { q -> viewModel.searchRepositories(q) },
                    onSelectRepo = { repo -> viewModel.selectRepository(repo) },
                    onInvokeMcpTool = { name, args -> viewModel.invokeMcpTool(name, args) },
                    onOpenGitDiagram = { _ -> viewModel.toggleGitDiagram(true) }
                )
            }

            // 9. Settings Destination
            composable("settings") {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory { SettingsViewModel(preferencesManager, exportImportRepository) }
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                SettingsScreen(
                    state = state,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onThemeChange = { mode -> viewModel.setThemeMode(mode) },
                    onSaveAiConfig = { config -> viewModel.saveAiConfig(config) },
                    onSaveGitHubToken = { token -> viewModel.saveGitHubToken(token) },
                    onResetOnboarding = { viewModel.resetOnboarding() },
                    onExportBackup = onExportZip,
                    onImportBackup = onImportZip
                )
            }
        }

        // Brutalist Bottom Navigation Bar
        val isBottomBarVisible = (currentRoute in listOf("notes", "search", "graph", "settings") || currentRoute.startsWith("ai")) && drawerState.isClosed
        if (isBottomBarVisible) {
            val selectedRoute = if (currentRoute.startsWith("ai")) "ai" else currentRoute
            WhiskBottomBar(
                currentRoute = selectedRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onQuickAdd = {
                    scope.launch {
                        val newNote = Note(title = "Untitled Note", body = "")
                        notesRepository.saveNote(newNote)
                        navController.navigate("note_detail/${newNote.id}")
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Command Palette Modal
        if (showCommandPalette) {
            CommandPalette(
                onDismiss = { showCommandPalette = false },
                onNavigate = { route -> navController.navigate(route) },
                onNewNote = {
                    scope.launch {
                        val newNote = Note(title = "Untitled Note", body = "")
                        notesRepository.saveNote(newNote)
                        navController.navigate("note_detail/${newNote.id}")
                    }
                },
                onExportBackup = onExportZip,
                onCreateTemplateNote = { templateNote ->
                    scope.launch {
                        notesRepository.saveNote(templateNote)
                        navController.navigate("note_detail/${templateNote.id}")
                    }
                }
            )
        }
    }
}
}
