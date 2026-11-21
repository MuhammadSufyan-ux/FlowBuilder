package com.example.data.repository

import com.example.data.local.FlowGraphSerializer
import com.example.data.local.FlowProjectDao
import com.example.data.local.FlowProjectEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.FirestoreFlowDataSource
import com.example.domain.model.FlowGraph
import com.example.domain.model.FlowNode
import com.example.domain.model.FlowProject
import com.example.domain.model.FlowTemplate
import com.example.domain.model.GridStyle
import com.example.domain.model.NodeShape
import com.example.domain.model.UserProfileData
import com.example.domain.templates.FlowTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.UUID

class FlowProjectRepository(
    private val dao: FlowProjectDao,
    private val serializer: FlowGraphSerializer,
    private val preferences: UserPreferencesRepository,
    private val remote: FirestoreFlowDataSource,
) {
    val localProjects: Flow<List<FlowProject>> = dao.observeAllProjects().map { entities ->
        entities.map { it.toDomain() }
    }

    fun observeProjects(userId: String): Flow<List<FlowProject>> {
        return remote.observeProjects(userId).onEach { remoteProjects ->
            withContext(Dispatchers.IO) {
                remoteProjects.forEach { project ->
                    dao.upsertProject(project.toEntity())
                }
            }
        }
    }

    fun observeProfile(userId: String): Flow<UserProfileData?> = remote.observeProfile(userId)

    suspend fun ensureProfileInitialized(
        userId: String,
        displayName: String,
        subtitle: String,
        email: String?,
        photoUrl: String?,
    ): Result<Unit> = remote.ensureProfileInitialized(userId, displayName, subtitle, email, photoUrl)

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        subtitle: String,
    ): Result<Unit> = remote.updateProfile(userId, displayName, subtitle)

    suspend fun ensureStarterProjectsSeeded(userId: String) = withContext(Dispatchers.IO) {
        if (!preferences.hasSeededStarter()) {
            val now = System.currentTimeMillis()
            val loginTemplate = FlowTemplates.allTemplates.first()
            val starterProject = FlowProject(
                id = UUID.randomUUID().toString(),
                title = "User Auth Flow",
                description = "Sample flowchart — tap any node to edit or open Shapes for 24+ symbols.",
                category = loginTemplate.category,
                graph = loginTemplate.graph,
                createdAt = now,
                updatedAt = now,
                isFavorite = true,
            )
            dao.upsertProject(starterProject.toEntity())
            remote.saveProject(userId, starterProject)
            preferences.markStarterSeeded()
        }
    }

    suspend fun getProjectById(id: String): FlowProject? = withContext(Dispatchers.IO) {
        dao.getProjectById(id)?.toDomain()
    }

    suspend fun createBlankProject(
        userId: String,
        title: String,
        gridStyle: GridStyle,
        snapToGrid: Boolean,
    ): FlowProject = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val initialNode = FlowNode(
            shape = NodeShape.OVAL,
            text = "Start",
            x = 220f,
            y = 140f,
            width = 148f,
            height = 64f,
            fillColorHex = "#BEF6AC",
        )
        val project = FlowProject(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "Untitled Flowchart" },
            description = "Created in FlowBuilder",
            category = "General",
            graph = FlowGraph(
                nodes = listOf(initialNode),
                edges = emptyList(),
                gridStyle = gridStyle,
                snapToGrid = snapToGrid,
            ),
            createdAt = now,
            updatedAt = now,
        )
        dao.upsertProject(project.toEntity())
        remote.saveProject(userId, project)
        project
    }

    suspend fun createProjectWithGraph(
        userId: String,
        title: String,
        description: String,
        category: String,
        graph: FlowGraph,
    ): FlowProject = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val project = FlowProject(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "AI Flowchart" },
            description = description.take(500),
            category = category.ifBlank { "AI Generated" },
            graph = graph,
            createdAt = now,
            updatedAt = now,
        )
        dao.upsertProject(project.toEntity())
        remote.saveProject(userId, project)
        project
    }

    suspend fun createFromTemplate(
        userId: String,
        template: FlowTemplate,
        gridStyle: GridStyle,
        snapToGrid: Boolean,
    ): FlowProject = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val project = FlowProject(
            id = UUID.randomUUID().toString(),
            title = template.title,
            description = template.description,
            category = template.category,
            graph = template.graph.copy(gridStyle = gridStyle, snapToGrid = snapToGrid),
            createdAt = now,
            updatedAt = now,
        )
        dao.upsertProject(project.toEntity())
        remote.saveProject(userId, project)
        project
    }

    suspend fun saveProject(userId: String, project: FlowProject): Result<Unit> = withContext(Dispatchers.IO) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        dao.upsertProject(updated.toEntity())
        remote.saveProject(userId, updated)
    }

    suspend fun renameProject(userId: String, id: String, newTitle: String) = withContext(Dispatchers.IO) {
        val cleaned = newTitle.trim().ifBlank { "Untitled Flowchart" }
        dao.updateTitle(id, cleaned, System.currentTimeMillis())
        remote.updateProjectTitle(userId, id, cleaned)
    }

    suspend fun toggleFavorite(userId: String, id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(id, isFavorite)
        remote.updateProjectFavorite(userId, id, isFavorite)
    }

    suspend fun duplicateProject(userId: String, project: FlowProject): FlowProject = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val copy = project.copy(
            id = UUID.randomUUID().toString(),
            title = "${project.title} (Copy)".take(120),
            createdAt = now,
            updatedAt = now,
            isFavorite = false,
        )
        dao.upsertProject(copy.toEntity())
        remote.saveProject(userId, copy)
        copy
    }

    suspend fun deleteProject(userId: String, id: String) = withContext(Dispatchers.IO) {
        dao.deleteProjectById(id)
        remote.deleteProject(userId, id)
    }

    suspend fun importFromJson(userId: String, json: String): Result<FlowProject> = withContext(Dispatchers.IO) {
        serializer.decodeProjectExport(json).map { payload ->
            val now = System.currentTimeMillis()
            val imported = FlowProject(
                id = UUID.randomUUID().toString(),
                title = payload.title.take(120),
                description = payload.description.ifBlank { "Imported from JSON" }.take(500),
                category = payload.category.ifBlank { "General" }.take(50),
                graph = payload.graph,
                createdAt = now,
                updatedAt = now,
            )
            dao.upsertProject(imported.toEntity())
            remote.saveProject(userId, imported)
            imported
        }
    }

    fun exportToJson(project: FlowProject): String = serializer.encodeProjectExport(project)

    fun encodeGraphPretty(graph: FlowGraph): String = serializer.encodeGraphPretty(graph)

    private fun FlowProjectEntity.toDomain(): FlowProject {
        return FlowProject(
            id = id,
            title = title,
            description = description,
            category = category,
            graph = serializer.decodeGraph(graphJson),
            createdAt = createdAt,
            updatedAt = updatedAt,
            isFavorite = isFavorite,
        )
    }

    private fun FlowProject.toEntity(): FlowProjectEntity {
        return FlowProjectEntity(
            id = id,
            title = title,
            description = description,
            category = category,
            graphJson = serializer.encodeGraph(graph),
            createdAt = createdAt,
            updatedAt = updatedAt,
            isFavorite = isFavorite,
        )
    }
}
