package com.indianservers.ai_stem.feature.projects

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.ai_stem.data.project.AiStemDatabase
import com.indianservers.ai_stem.data.project.LegacySceneMigration
import com.indianservers.ai_stem.data.project.ProjectEntity
import com.indianservers.ai_stem.data.project.ProjectRepository
import com.indianservers.ai_stem.data.project.ProjectSort
import com.indianservers.ai_stem.data.project.ProjectType
import com.indianservers.ai_stem.data.project.RoomProjectRepository
import com.indianservers.ai_stem.feature.subjects.SimpleHeader
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectManagerUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val deletedProjects: List<ProjectEntity> = emptyList(),
    val query: String = "",
    val sort: ProjectSort = ProjectSort.Recent,
    val showDeleted: Boolean = false,
    val migrationMessage: String? = null,
    val selectedProjectId: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AiStemDatabase.getInstance(application)
    private val repository: ProjectRepository = RoomProjectRepository(database)
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(ProjectSort.Recent)
    private val showDeleted = MutableStateFlow(false)
    private val migrationMessage = MutableStateFlow<String?>(null)
    private val selectedProjectId = MutableStateFlow<String?>(null)

    private val visibleProjects = combine(query, sort) { currentQuery, currentSort -> currentQuery to currentSort }
        .flatMapLatest { (currentQuery, currentSort) ->
            if (currentQuery.isBlank()) repository.sort(currentSort) else repository.search(currentQuery)
        }

    private val controls = combine(query, sort, showDeleted, migrationMessage, selectedProjectId) {
            currentQuery, currentSort, deletedVisible, migration, selected ->
        ProjectControls(currentQuery, currentSort, deletedVisible, migration, selected)
    }

    val uiState: StateFlow<ProjectManagerUiState> = combine(
        visibleProjects,
        repository.observeDeleted(),
        controls
    ) { projects, deleted, currentControls ->
        ProjectManagerUiState(
            projects = projects,
            deletedProjects = deleted,
            query = currentControls.query,
            sort = currentControls.sort,
            showDeleted = currentControls.showDeleted,
            migrationMessage = currentControls.migrationMessage,
            selectedProjectId = currentControls.selectedProjectId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectManagerUiState())

    init {
        viewModelScope.launch {
            val result = LegacySceneMigration(application, database).migrateIfNeeded()
            migrationMessage.value = "Legacy migration: ${result.migratedCount} migrated, ${result.skippedCount} skipped"
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSort(value: ProjectSort) {
        sort.value = value
    }

    fun toggleDeleted() {
        showDeleted.update { !it }
    }

    fun selectProject(id: String) {
        selectedProjectId.value = id
    }

    fun createProject(name: String, type: ProjectType) {
        viewModelScope.launch {
            val project = repository.create(name, type)
            selectedProjectId.value = project.id
        }
    }

    fun renameProject(project: ProjectEntity, name: String) {
        viewModelScope.launch {
            repository.update(project.copy(name = name.trim().ifBlank { project.name }))
        }
    }

    fun saveAs(project: ProjectEntity, name: String) {
        viewModelScope.launch {
            selectedProjectId.value = repository.saveAs(project.id, name)?.id
        }
    }

    fun duplicate(project: ProjectEntity) {
        viewModelScope.launch {
            selectedProjectId.value = repository.duplicate(project.id, "${project.name} Copy")?.id
        }
    }

    fun setFavourite(project: ProjectEntity) {
        viewModelScope.launch {
            repository.favourite(project.id, !project.isFavourite)
        }
    }

    fun delete(project: ProjectEntity) {
        viewModelScope.launch {
            repository.delete(project.id)
            if (selectedProjectId.value == project.id) selectedProjectId.value = null
        }
    }

    fun restore(project: ProjectEntity) {
        viewModelScope.launch {
            repository.restore(project.id)
            selectedProjectId.value = project.id
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.AndroidViewModelFactory() {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return ProjectManagerViewModel(application) as T
            }
        }
    }

    private data class ProjectControls(
        val query: String,
        val sort: ProjectSort,
        val showDeleted: Boolean,
        val migrationMessage: String?,
        val selectedProjectId: String?
    )
}

@Composable
fun ProjectManagerScreen(
    onBack: () -> Unit,
    viewModel: ProjectManagerViewModel = viewModel(factory = ProjectManagerViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    var newProjectDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProjectEntity?>(null) }
    var saveAsTarget by remember { mutableStateOf<ProjectEntity?>(null) }

    LaunchedEffect(state.projects) {
        if (state.selectedProjectId == null && state.projects.isNotEmpty()) {
            viewModel.selectProject(state.projects.first().id)
        }
    }

    Scaffold(topBar = { SimpleHeader("My Mathematics Projects", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { newProjectDialog = true }) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Text("New Project")
                }
                OutlinedButton(
                    enabled = state.selectedProjectId != null,
                    onClick = {
                        state.projects.firstOrNull { it.id == state.selectedProjectId }?.let { saveAsTarget = it }
                    }
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Text("Save As")
                }
                OutlinedButton(onClick = viewModel::toggleDeleted) {
                    Icon(Icons.Outlined.Restore, contentDescription = null)
                    Text(if (state.showDeleted) "Hide Deleted" else "Restore Deleted")
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                label = { Text("Search projects") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.sort == ProjectSort.Recent, onClick = { viewModel.setSort(ProjectSort.Recent) }, label = { Text("Recent") })
                FilterChip(selected = state.sort == ProjectSort.Name, onClick = { viewModel.setSort(ProjectSort.Name) }, label = { Text("Name") })
                state.migrationMessage?.let { AssistChip(onClick = {}, label = { Text(it) }) }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        selected = project.id == state.selectedProjectId,
                        onSelect = { viewModel.selectProject(project.id) },
                        onRename = { renameTarget = project },
                        onSaveAs = { saveAsTarget = project },
                        onDuplicate = { viewModel.duplicate(project) },
                        onFavourite = { viewModel.setFavourite(project) },
                        onDelete = { viewModel.delete(project) }
                    )
                }
                if (state.showDeleted) {
                    items(state.deletedProjects, key = { "deleted-${it.id}" }) { project ->
                        DeletedProjectCard(project = project, onRestore = { viewModel.restore(project) })
                    }
                }
            }
        }
    }

    if (newProjectDialog) {
        ProjectNameDialog(
            title = "New Project",
            confirmLabel = "Create",
            initialName = "Mathematics Project",
            onDismiss = { newProjectDialog = false },
            onConfirm = { name ->
                viewModel.createProject(name, ProjectType.Mixed)
                newProjectDialog = false
            }
        )
    }
    renameTarget?.let { project ->
        ProjectNameDialog(
            title = "Rename Project",
            confirmLabel = "Rename",
            initialName = project.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                viewModel.renameProject(project, name)
                renameTarget = null
            }
        )
    }
    saveAsTarget?.let { project ->
        ProjectNameDialog(
            title = "Save As",
            confirmLabel = "Save",
            initialName = "${project.name} Copy",
            onDismiss = { saveAsTarget = null },
            onConfirm = { name ->
                viewModel.saveAs(project, name)
                saveAsTarget = null
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onSaveAs: () -> Unit,
    onDuplicate: () -> Unit,
    onFavourite: () -> Unit,
    onDelete: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ElevatedCard(onClick = onSelect, modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = container)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${project.type} - ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(project.lastModifiedAt))}")
                }
                Row {
                    IconButton(onClick = onFavourite) {
                        Icon(if (project.isFavourite) Icons.Outlined.Star else Icons.Outlined.StarBorder, contentDescription = "Favourite")
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRename) { Text("Rename") }
                OutlinedButton(onClick = onSaveAs) { Text("Save As") }
            }
        }
    }
}

@Composable
private fun DeletedProjectCard(project: ProjectEntity, onRestore: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                Text("Deleted ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(project.deletedAt ?: project.lastModifiedAt))}")
            }
            Button(onClick = onRestore) {
                Icon(Icons.Outlined.Restore, contentDescription = null)
                Text("Restore")
            }
        }
    }
}

@Composable
private fun ProjectNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val valid = name.trim().isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = { Text("Project name") },
                singleLine = true,
                isError = !valid
            )
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(name) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
