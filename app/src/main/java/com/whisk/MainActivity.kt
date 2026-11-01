package com.whisk

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
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
import com.whisk.data.repository.ChatRepository
import com.whisk.data.repository.DailyNotesRepository
import com.whisk.data.repository.DatabaseViewRepository
import com.whisk.data.repository.ExportImportRepository
import com.whisk.data.repository.GitHubRepository
import com.whisk.data.repository.GraphRepository
import com.whisk.data.repository.McpRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.data.repository.ObjectTypeRepository
import com.whisk.data.repository.PropertyRepository
import com.whisk.data.repository.RelationshipRepository
import com.whisk.domain.model.Note
import com.whisk.ui.components.CommandPalette
import com.whisk.ui.components.SidebarDrawer
import com.whisk.ui.components.WhiskBottomBar
import com.whisk.ui.screens.ai.AiViewModel
import com.whisk.ui.screens.ai.AiWorkspaceScreen
import com.whisk.ui.screens.canvas.CanvasViewModel
import com.whisk.ui.screens.canvas.SpatialCanvasScreen
import com.whisk.ui.screens.daily.DailyNotesScreen
import com.whisk.ui.screens.daily.DailyNotesViewModel
import com.whisk.ui.screens.github.GitHubBrowserScreen
import com.whisk.ui.screens.github.GitHubViewModel
import com.whisk.ui.screens.graph.GraphViewModel
import com.whisk.ui.screens.graph.KnowledgeGraphScreen
import com.whisk.ui.screens.notes.NoteDetailScreen
import com.whisk.ui.screens.notes.NoteDetailViewModel
import com.whisk.ui.screens.notes.NotesListScreen
import com.whisk.ui.screens.notes.NotesViewModel
import com.whisk.ui.screens.projects.ProjectWorkspaceScreen
import com.whisk.ui.screens.projects.ProjectWorkspaceViewModel
import com.whisk.ui.screens.properties.PropertyManagementScreen
import com.whisk.ui.screens.properties.PropertyManagementViewModel
import com.whisk.ui.screens.search.SearchScreen
import com.whisk.ui.screens.search.SearchViewModel
import com.whisk.ui.screens.settings.SettingsScreen
import com.whisk.ui.screens.settings.SettingsViewModel
import com.whisk.ui.screens.tasks.TasksScreen
import com.whisk.ui.screens.tasks.TasksViewModel
import com.whisk.ui.screens.types.ObjectTypeManagementScreen
import com.whisk.ui.screens.types.ObjectTypeManagementViewModel
import com.whisk.ui.screens.views.DatabaseViewsScreen
import com.whisk.ui.screens.views.DatabaseViewsViewModel
import com.whisk.ui.theme.WhiskTheme
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
    private lateinit var propertyRepository: PropertyRepository
    private lateinit var objectTypeRepository: ObjectTypeRepository
    private lateinit var relationshipRepository: RelationshipRepository
    private lateinit var graphRepository: GraphRepository
    private lateinit var canvasRepository: CanvasRepository
    private lateinit var gitHubRepository: GitHubRepository
    private lateinit var mcpRepository: McpRepository
    private lateinit var exportImportRepository: ExportImportRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var databaseViewRepository: DatabaseViewRepository
    private lateinit var dailyNotesRepository: DailyNotesRepository
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
        propertyRepository = PropertyRepository(database.propertyDao(), database.noteDao())
        objectTypeRepository = ObjectTypeRepository(database.objectTypeDao(), database.noteDao(), database.propertyDao())
        relationshipRepository = RelationshipRepository(database.relationshipDao(), database.noteDao(), database.objectTypeDao())
        graphRepository = GraphRepository(database.noteDao(), database.relationshipDao())
        canvasRepository = CanvasRepository(database.canvasDao())
        gitHubRepository = GitHubRepository()
        mcpRepository = McpRepository()
        exportImportRepository = ExportImportRepository(this, database, notesRepository)
        chatRepository = ChatRepository(database.chatDao())
        databaseViewRepository = DatabaseViewRepository(database.databaseViewDao())
        dailyNotesRepository = DailyNotesRepository(database, database.dailyNoteDao(), database.activityEventDao(), database.noteDao(), database.taskDao())
        localGemmaManager = LocalGemmaManager(this)
        remoteAiManager = RemoteAiManager()

        handleShareIntent(intent)

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            val isFirstLaunch by preferencesManager.isFirstLaunch.collectAsStateWithLifecycle(initialValue = true)

            WhiskTheme(themeMode = themeMode) {
                WhiskApp(
                    notesRepository = notesRepository,
                    propertyRepository = propertyRepository,
                    objectTypeRepository = objectTypeRepository,
                    relationshipRepository = relationshipRepository,
                    graphRepository = graphRepository,
                    canvasRepository = canvasRepository,
                    gitHubRepository = gitHubRepository,
                    mcpRepository = mcpRepository,
                    exportImportRepository = exportImportRepository,
                    chatRepository = chatRepository,
                    databaseViewRepository = databaseViewRepository,
                    dailyNotesRepository = dailyNotesRepository,
                    localGemmaManager = localGemmaManager,
                    remoteAiManager = remoteAiManager,
                    preferencesManager = preferencesManager,
                    isFirstLaunch = isFirstLaunch,
                    onExportZip = { exportZipLauncher.launch("Whisk_Vault_Backup.zip") },
                ) {
                    importZipLauncher.launch(arrayOf("application/zip", "text/plain", "text/markdown"))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if ((intent?.action == Intent.ACTION_SEND) && (intent.type == "text/plain")) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "Quick Capture"
            if (sharedText.isNotBlank()) {
                lifecycleScope.launch {
                    val newNote = Note(
                        title = sharedSubject.ifBlank { "Quick Capture" },
                        body = sharedText,
                        createdAt = System.currentTimeMillis(),
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
    propertyRepository: PropertyRepository,
    objectTypeRepository: ObjectTypeRepository,
    relationshipRepository: RelationshipRepository,
    graphRepository: GraphRepository,
    canvasRepository: CanvasRepository,
    gitHubRepository: GitHubRepository,
    mcpRepository: McpRepository,
    exportImportRepository: ExportImportRepository,
    chatRepository: ChatRepository,
    databaseViewRepository: DatabaseViewRepository,
    dailyNotesRepository: DailyNotesRepository,
    localGemmaManager: LocalGemmaManager,
    remoteAiManager: RemoteAiManager,
    preferencesManager: PreferencesManager,
    isFirstLaunch: Boolean,
    onExportZip: () -> Unit,
    onImportZip: () -> Unit,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var showCommandPalette by remember { mutableStateOf(value = false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "notes"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val allNotes by notesRepository.allNotes.collectAsStateWithLifecycle(initialValue = emptyList())

    // Seed Welcome Note & Object Types
    LaunchedEffect(Unit) {
        objectTypeRepository.seedBuiltInObjectTypes()
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
                onCloseDrawer = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "notes",
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    ) { it / 4 } + fadeIn()
                },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    ) { it / 4 } + fadeOut()
                },
            ) {
            // 1. Notes List Destination
            composable("notes") {
                val viewModel: NotesViewModel = viewModel(
                    factory = viewModelFactory { NotesViewModel(notesRepository) },
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
                    },
                )
            }

            // 2. Note Detail / Editor Destination
            composable(
                route = "note_detail/{noteId}",
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                val viewModel: NoteDetailViewModel = viewModel(
                    key = noteId,
                    factory = viewModelFactory {
                        NoteDetailViewModel(
                            notesRepository = notesRepository,
                            propertyRepository = propertyRepository,
                            objectTypeRepository = objectTypeRepository,
                            relationshipRepository = relationshipRepository,
                            noteId = noteId,
                        )
                    },
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
                            target?.let { (id) -> navController.navigate("note_detail/$id") }
                        }
                    },
                    onOpenAiWithNote = { navController.navigate("ai?noteId=$noteId") },
                    onUpdatePropertyValue = { defId, textVal, numVal, boolVal, listVal ->
                        viewModel.updatePropertyValue(defId, textVal, numVal, boolVal, listVal)
                    },
                    onDeletePropertyValue = { defId -> viewModel.deletePropertyValue(defId) },
                    onManageProperties = { navController.navigate("properties") },
                    onChangeObjectType = { newTypeId -> viewModel.changeObjectType(newTypeId) },
                    onAddRelationship = { targetId, relType -> viewModel.addRelationship(targetId, relType) },
                    onRemoveRelationship = { relId -> viewModel.removeRelationship(relId) },
                    onQueryObjects = { query, typeId -> viewModel.queryObjectsForPicker(query, typeId) },
                    onOpenProjectWorkspace = { projectId -> navController.navigate("project_workspace/$projectId") },
                    onConvertUnlinkedMention = { matchedText, targetTitle -> viewModel.convertUnlinkedMention(matchedText, targetTitle) },
                    onCreateNoteFromBrokenLink = { targetTitle ->
                        viewModel.createNoteFromBrokenLink(targetTitle) { createdId ->
                            navController.navigate("note_detail/$createdId")
                        }
                    },
                    allNotes = allNotes,
                )
            }

            // 3. Search Destination
            composable("search") {
                val viewModel: SearchViewModel = viewModel(
                    factory = viewModelFactory { SearchViewModel(notesRepository, propertyRepository, objectTypeRepository) },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                SearchScreen(
                    state = state,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onQueryChange = { q -> viewModel.updateQuery(q) },
                    onSetPropertyFilter = { filter -> viewModel.setPropertyFilter(filter) },
                    onSelectType = { typeId -> viewModel.selectTypeId(typeId) },
                    onNoteClick = { noteId -> navController.navigate("note_detail/$noteId") },
                )
            }

            // 4. Knowledge Graph Destination
            composable("graph") {
                val viewModel: GraphViewModel = viewModel(
                    factory = viewModelFactory { GraphViewModel(graphRepository) },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                KnowledgeGraphScreen(
                    state = state,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNodeSelect = { node -> viewModel.selectNode(node) },
                    onOpenNote = { noteId -> navController.navigate("note_detail/$noteId") },
                    onResetGraph = { viewModel.loadGlobalGraph() },
                    onToggleOrphans = { viewModel.toggleShowOrphans() },
                )
            }

            // 5. Spatial Canvas Destination
            composable("canvas") {
                val viewModel: CanvasViewModel = viewModel(
                    factory = viewModelFactory { CanvasViewModel(canvasRepository, notesRepository) },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                SpatialCanvasScreen(
                    state = state,
                    onAddTextCard = { viewModel.addTextCard("Idea Card", "Double tap or drag card around spatial canvas") },
                    onAddNoteCard = { note -> viewModel.addNoteCard(note) },
                    onUpdateCardPos = { id, x, y -> viewModel.updateCardPosition(id, x, y) },
                    onDeleteCard = { id -> viewModel.deleteCard(id) },
                    onCreateCanvas = { viewModel.createNewCanvas() },
                    onSelectCanvas = { canvasId -> viewModel.selectCanvas(canvasId) },
                    onRetryLoad = { viewModel.loadCanvases() },
                ) { noteId ->
                    navController.navigate("note_detail/$noteId")
                }
            }

            // 6. Tasks Checklist Destination
            composable("tasks") {
                val viewModel: TasksViewModel = viewModel(
                    factory = viewModelFactory { TasksViewModel(notesRepository) },
                )
                val tasks by viewModel.tasks.collectAsStateWithLifecycle(initialValue = emptyList())

                TasksScreen(
                    tasks = tasks,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onToggleTask = { taskId, status -> viewModel.toggleTask(taskId, status) },
                    onOpenNote = { noteId -> navController.navigate("note_detail/$noteId") },
                )
            }

            // 7. AI Workspace Destination
            composable(
                route = "ai?noteId={noteId}",
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                val context = androidx.compose.ui.platform.LocalContext.current
                val noteId = backStackEntry.arguments?.getString("noteId")
                val viewModel: AiViewModel = viewModel(
                    key = noteId ?: "ai_global",
                    factory = viewModelFactory {
                        AiViewModel(localGemmaManager, remoteAiManager, preferencesManager, chatRepository).apply {
                            if (!noteId.isNullOrBlank()) {
                                loadNoteContext(notesRepository, noteId)
                            }
                        }
                    },
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
                                val cleanEdit = edit.lineSequence()
                                    .filterNot { it.trim().startsWith("```") }
                                    .joinToString("\n")
                                    .trim()
                                val updatedText = cleanEdit.ifBlank { edit }
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
                            target?.let { (id) -> navController.navigate("note_detail/$id") }
                        }
                    },
                )
            }

            // 8. GitHub & MCP Destination
            composable("github") {
                val viewModel: GitHubViewModel = viewModel(
                    factory = viewModelFactory { GitHubViewModel(gitHubRepository, mcpRepository, preferencesManager) },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                GitHubBrowserScreen(
                    state = state,
                    onSearchQueryChange = { q -> viewModel.searchRepositories(q) },
                    onSelectRepo = { repo -> viewModel.selectRepository(repo) },
                    onInvokeMcpTool = { name, args -> viewModel.invokeMcpTool(name, args) },
                ) {
                    viewModel.toggleGitDiagram(open = true)
                }
            }

            // 9. Settings Destination
            composable("settings") {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory { SettingsViewModel(preferencesManager, exportImportRepository) },
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
                    onImportBackup = onImportZip,
                    onNavigateToProperties = { navController.navigate("properties") },
                    onNavigateToTypes = { navController.navigate("object_types") },
                )
            }

            // 10. Properties Management Destination
            composable("properties") {
                val viewModel: PropertyManagementViewModel = viewModel(
                    factory = viewModelFactory { PropertyManagementViewModel(propertyRepository) },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                PropertyManagementScreen(
                    state = state,
                    onBackClick = { navController.popBackStack() },
                    onOpenCreateDialog = { viewModel.openCreateDialog() },
                    onCloseCreateDialog = { viewModel.closeCreateDialog() },
                    onCreateProperty = { name, type, desc, options -> viewModel.createProperty(name, type, desc, options) },
                    onOpenEditDialog = { prop -> viewModel.openEditDialog(prop) },
                    onCloseEditDialog = { viewModel.closeEditDialog() },
                    onUpdateProperty = { id, name, type, desc, options, archived -> viewModel.updateProperty(id, name, type, desc, options, archived) },
                    onOpenDeleteConfirmation = { prop -> viewModel.openDeleteConfirmation(prop) },
                    onCloseDeleteConfirmation = { viewModel.closeDeleteConfirmation() },
                    onDeleteProperty = { id -> viewModel.deleteProperty(id) },
                    onToggleArchive = { prop -> viewModel.toggleArchive(prop) },
                )
            }

            // 11. Object Type Registry Destination
            composable("object_types") {
                val viewModel: ObjectTypeManagementViewModel = viewModel(
                    factory = viewModelFactory { ObjectTypeManagementViewModel(objectTypeRepository) },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                ObjectTypeManagementScreen(
                    state = state,
                    onBackClick = { navController.popBackStack() },
                    onOpenCreateDialog = { viewModel.openCreateDialog() },
                    onCloseCreateDialog = { viewModel.closeCreateDialog() },
                    onCreateCustomType = { name, icon, desc -> viewModel.createCustomType(name, icon, desc) },
                    onOpenDeleteConfirmation = { typeDef -> viewModel.openDeleteConfirmation(typeDef) },
                    onCloseDeleteConfirmation = { viewModel.closeDeleteConfirmation() },
                    onDeleteCustomType = { typeId, action, reassignId -> viewModel.deleteCustomType(typeId, action, reassignId) },
                )
            }

            // 12. Project Workspace Destination
            composable(
                route = "project_workspace/{projectId}",
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                val viewModel: ProjectWorkspaceViewModel = viewModel(
                    key = projectId,
                    factory = viewModelFactory {
                        ProjectWorkspaceViewModel(
                            projectId = projectId,
                            notesRepository = notesRepository,
                            propertyRepository = propertyRepository,
                            relationshipRepository = relationshipRepository,
                            objectTypeRepository = objectTypeRepository,
                            chatRepository = chatRepository,
                            gitHubRepository = gitHubRepository,
                        )
                    },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                ProjectWorkspaceScreen(
                    state = state,
                    onBackClick = { navController.popBackStack() },
                    onUpdateContent = { title, desc, body -> viewModel.updateProjectContent(title, desc, body) },
                    onUpdateStatus = { status -> viewModel.updateStatus(status) },
                    onUpdateRepoRef = { repoRef -> viewModel.updateRepoRef(repoRef) },
                    onCreateTask = { taskTitle -> viewModel.createProjectTask(taskTitle) },
                    onToggleTask = { taskId, status -> viewModel.toggleTask(taskId, status) },
                    onAddRelationship = { targetId, relType -> viewModel.addRelationship(targetId, relType) },
                    onRemoveRelationship = { relId -> viewModel.removeRelationship(relId) },
                    onQueryObjects = { query, typeId -> viewModel.queryObjectsForPicker(query, typeId) },
                    onOpenObject = { id -> navController.navigate("note_detail/$id") },
                    onStartAiSession = { callback -> viewModel.startNewAiSession(callback) },
                    onOpenAiSession = { sessionId -> navController.navigate("ai?noteId=$projectId") },
                )
            }

            // 13. Database Views Destination
            composable("database_views") {
                val viewModel: DatabaseViewsViewModel = viewModel(
                    factory = viewModelFactory {
                        DatabaseViewsViewModel(databaseViewRepository, notesRepository, propertyRepository)
                    },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                DatabaseViewsScreen(
                    state = state,
                    onSelectView = { view -> viewModel.selectView(view) },
                    onCreateView = { name, viewType -> viewModel.createView(name, viewType) },
                    onDuplicateView = { viewId -> viewModel.duplicateView(viewId) },
                    onDeleteView = { viewId -> viewModel.deleteView(viewId) },
                    onOpenNote = { id -> navController.navigate("note_detail/$id") },
                    onUpdatePropertyValue = { noteId, defId, textVal, numVal, boolVal, listVal ->
                        viewModel.updatePropertyValue(noteId, defId, textVal, numVal, boolVal, listVal)
                    },
                    onMoveNoteToGroup = { noteId, newGroup -> viewModel.updateNoteGroupStatus(noteId, newGroup) },
                    onCreateInGroup = { group -> viewModel.createNoteInGroup(group) },
                    onCreateOnDate = { dateKey -> viewModel.createNoteOnDate(dateKey) },
                    onMenuClick = { scope.launch { drawerState.open() } },
                )
            }

            // 14. Daily Notes Destination
            composable("daily_notes") {
                val viewModel: DailyNotesViewModel = viewModel(
                    factory = viewModelFactory {
                        DailyNotesViewModel(dailyNotesRepository, notesRepository, localGemmaManager, remoteAiManager)
                    },
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                DailyNotesScreen(
                    state = state,
                    onDateSelect = { dateKey -> viewModel.loadDate(dateKey) },
                    onPrevDay = { viewModel.goToPreviousDay() },
                    onNextDay = { viewModel.goToNextDay() },
                    onGoToToday = { viewModel.goToToday() },
                    onUpdateJournal = { title, body -> viewModel.updateJournalContent(title, body) },
                    onToggleTask = { taskId, chk -> viewModel.toggleTask(taskId, chk) },
                    onGenerateAiReflection = { viewModel.generateDailyReflection() },
                    onApplyAiSuggestion = { viewModel.applyAiSuggestion() },
                    onDismissAiSuggestion = { viewModel.dismissAiSuggestion() },
                    onOpenNote = { noteId -> navController.navigate("note_detail/$noteId") },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    allNotes = allNotes,
                )
            }
        }

        // Brutalist Bottom Navigation Bar
        val isBottomBarVisible = ((currentRoute in listOf("notes", "search", "graph", "settings", "database_views", "daily_notes")) || currentRoute.startsWith("ai")) && drawerState.isClosed
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
                modifier = Modifier.align(Alignment.BottomCenter),
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
            ) { templateNote ->
                scope.launch {
                    notesRepository.saveNote(templateNote)
                    navController.navigate("note_detail/${templateNote.id}")
                }
            }
        }
    }
}
}
