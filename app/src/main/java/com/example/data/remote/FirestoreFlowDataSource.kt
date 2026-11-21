package com.example.data.remote

import com.example.domain.model.AnchorSide
import com.example.domain.model.AppFontFamily
import com.example.domain.model.BorderStyle
import com.example.domain.model.EdgeStyle
import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowGraph
import com.example.domain.model.FlowNode
import com.example.domain.model.FlowProject
import com.example.domain.model.GridStyle
import com.example.domain.model.NodeShape
import com.example.domain.model.UserProfileData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreFlowDataSource(
    private val db: FirebaseFirestore,
) {
    private fun userDocRef(userId: String) = db.collection("users").document(userId)

    private fun projectsCollection(userId: String) =
        userDocRef(userId).collection("projects")

    fun observeProfile(userId: String): Flow<UserProfileData?> = callbackFlow {
        val path = "users/$userId"
        val registration = userDocRef(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toUserProfileData(userId))
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun ensureProfileInitialized(
        userId: String,
        displayName: String,
        subtitle: String,
        email: String?,
        photoUrl: String?,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val docRef = userDocRef(userId)
            val existing = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "FlowBuilder User" }.take(80)
            val safeSubtitle = subtitle.trim().ifBlank { "Flowchart Creator" }.take(120)
            if (!existing.exists()) {
                val payload = buildMap<String, Any> {
                    put("userId", userId)
                    put("displayName", safeName)
                    put("subtitle", safeSubtitle)
                    if (!email.isNullOrBlank()) put("email", email.trim().take(160))
                    if (!photoUrl.isNullOrBlank()) put("photoUrl", photoUrl.trim().take(600))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val updates = buildMap<String, Any> {
                    if (!email.isNullOrBlank() && existing.getString("email").isNullOrBlank()) {
                        put("email", email.trim().take(160))
                    }
                    if (!photoUrl.isNullOrBlank() && existing.getString("photoUrl").isNullOrBlank()) {
                        put("photoUrl", photoUrl.trim().take(600))
                    }
                    if (isNotEmpty()) {
                        put("updatedAt", FieldValue.serverTimestamp())
                    }
                }
                if (updates.isNotEmpty()) {
                    docRef.update(updates).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        subtitle: String,
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val docRef = userDocRef(userId)
            val safeName = displayName.trim().ifBlank { "FlowBuilder User" }.take(80)
            val safeSubtitle = subtitle.trim().take(120)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "subtitle" to safeSubtitle,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "displayName" to safeName,
                        "subtitle" to safeSubtitle,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeProjects(userId: String): Flow<List<FlowProject>> = callbackFlow {
        val path = "users/$userId/projects"
        val registration = projectsCollection(userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshots?.documents?.mapNotNull { doc ->
                    runCatching { doc.toFlowProject() }.getOrNull()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveProject(userId: String, project: FlowProject): Result<Unit> {
        val path = "users/$userId/projects/${project.id}"
        return try {
            val docRef = projectsCollection(userId).document(project.id)
            val existing = docRef.get().await()
            val safeTitle = project.title.trim().ifBlank { "Untitled Flowchart" }.take(120)
            val safeDesc = project.description.trim().take(500)
            val safeCat = project.category.trim().ifBlank { "General" }.take(50)
            val nodesList = project.graph.nodes.take(500).map { it.toFirestoreMap() }
            val edgesList = project.graph.edges.take(1000).map { it.toFirestoreMap() }

            if (existing.exists()) {
                val updates = mapOf(
                    "title" to safeTitle,
                    "description" to safeDesc,
                    "category" to safeCat,
                    "favorite" to project.isFavorite,
                    "gridStyle" to project.graph.gridStyle.name,
                    "snapToGrid" to project.graph.snapToGrid,
                    "nodes" to nodesList,
                    "edges" to edgesList,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to userId,
                    "title" to safeTitle,
                    "description" to safeDesc,
                    "category" to safeCat,
                    "favorite" to project.isFavorite,
                    "gridStyle" to project.graph.gridStyle.name,
                    "snapToGrid" to project.graph.snapToGrid,
                    "nodes" to nodesList,
                    "edges" to edgesList,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectTitle(userId: String, projectId: String, newTitle: String): Result<Unit> {
        val path = "users/$userId/projects/$projectId"
        return try {
            val safeTitle = newTitle.trim().ifBlank { "Untitled Flowchart" }.take(120)
            projectsCollection(userId).document(projectId).update(
                mapOf(
                    "title" to safeTitle,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProjectFavorite(userId: String, projectId: String, isFavorite: Boolean): Result<Unit> {
        val path = "users/$userId/projects/$projectId"
        return try {
            projectsCollection(userId).document(projectId).update(
                mapOf(
                    "favorite" to isFavorite,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        val path = "users/$userId/projects/$projectId"
        return try {
            projectsCollection(userId).document(projectId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toUserProfileData(fallbackUserId: String): UserProfileData {
        return UserProfileData(
            userId = getString("userId") ?: fallbackUserId,
            displayName = getString("displayName") ?: "FlowBuilder User",
            subtitle = getString("subtitle") ?: "Flowchart Creator",
            email = getString("email") ?: "",
            photoUrl = getString("photoUrl"),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toFlowProject(): FlowProject {
        val createdMs = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: System.currentTimeMillis()
        val updatedMs = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: createdMs

        val gridStyle = getString("gridStyle")?.let {
            runCatching { GridStyle.valueOf(it) }.getOrNull()
        } ?: GridStyle.PLUS

        val snapToGrid = getBoolean("snapToGrid") ?: true

        val rawNodes = get("nodes") as? List<Map<String, Any?>> ?: emptyList()
        val nodes = rawNodes.mapNotNull { it.toFlowNode() }

        val rawEdges = get("edges") as? List<Map<String, Any?>> ?: emptyList()
        val edges = rawEdges.mapNotNull { it.toFlowEdge() }

        return FlowProject(
            id = id,
            title = getString("title") ?: "Untitled Flowchart",
            description = getString("description") ?: "",
            category = getString("category") ?: "General",
            graph = FlowGraph(
                nodes = nodes,
                edges = edges,
                gridStyle = gridStyle,
                snapToGrid = snapToGrid,
            ),
            createdAt = createdMs,
            updatedAt = updatedMs,
            isFavorite = getBoolean("favorite") ?: false,
        )
    }

    private fun FlowNode.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "shape" to shape.name,
        "text" to text,
        "x" to x.toDouble(),
        "y" to y.toDouble(),
        "width" to width.toDouble(),
        "height" to height.toDouble(),
        "fillColorHex" to fillColorHex,
        "borderColorHex" to borderColorHex,
        "textColorHex" to textColorHex,
        "borderWidth" to borderWidth.toDouble(),
        "borderStyle" to borderStyle.name,
        "fontSizeSp" to fontSizeSp.toDouble(),
        "isBold" to isBold,
        "fontFamily" to fontFamily.name,
    )

    private fun FlowEdge.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "fromNodeId" to fromNodeId,
        "toNodeId" to toNodeId,
        "label" to label,
        "fromSide" to fromSide.name,
        "toSide" to toSide.name,
        "style" to style.name,
        "strokeColorHex" to strokeColorHex,
        "isDashed" to isDashed,
    )

    private fun Map<String, Any?>.toFlowNode(): FlowNode? {
        val nodeId = this["id"] as? String ?: return null
        val shape = (this["shape"] as? String)?.let {
            runCatching { NodeShape.valueOf(it) }.getOrNull()
        } ?: NodeShape.RECTANGLE
        val borderStyle = (this["borderStyle"] as? String)?.let {
            runCatching { BorderStyle.valueOf(it) }.getOrNull()
        } ?: BorderStyle.SOLID
        val fontFamily = (this["fontFamily"] as? String)?.let {
            runCatching { AppFontFamily.valueOf(it) }.getOrNull()
        } ?: AppFontFamily.PLUS_JAKARTA_SANS

        return FlowNode(
            id = nodeId,
            shape = shape,
            text = (this["text"] as? String) ?: shape.defaultText,
            x = (this["x"] as? Number)?.toFloat() ?: 0f,
            y = (this["y"] as? Number)?.toFloat() ?: 0f,
            width = (this["width"] as? Number)?.toFloat() ?: shape.defaultWidth,
            height = (this["height"] as? Number)?.toFloat() ?: shape.defaultHeight,
            fillColorHex = (this["fillColorHex"] as? String) ?: shape.defaultFillHex,
            borderColorHex = (this["borderColorHex"] as? String) ?: "#161D29",
            textColorHex = (this["textColorHex"] as? String) ?: "#0F172A",
            borderWidth = (this["borderWidth"] as? Number)?.toFloat() ?: 2.4f,
            borderStyle = borderStyle,
            fontSizeSp = (this["fontSizeSp"] as? Number)?.toFloat() ?: 14f,
            isBold = (this["isBold"] as? Boolean) ?: true,
            fontFamily = fontFamily,
        )
    }

    private fun Map<String, Any?>.toFlowEdge(): FlowEdge? {
        val edgeId = this["id"] as? String ?: return null
        val fromId = this["fromNodeId"] as? String ?: return null
        val toId = this["toNodeId"] as? String ?: return null
        val fromSide = (this["fromSide"] as? String)?.let {
            runCatching { AnchorSide.valueOf(it) }.getOrNull()
        } ?: AnchorSide.AUTO
        val toSide = (this["toSide"] as? String)?.let {
            runCatching { AnchorSide.valueOf(it) }.getOrNull()
        } ?: AnchorSide.AUTO
        val style = (this["style"] as? String)?.let {
            runCatching { EdgeStyle.valueOf(it) }.getOrNull()
        } ?: EdgeStyle.ORTHOGONAL

        return FlowEdge(
            id = edgeId,
            fromNodeId = fromId,
            toNodeId = toId,
            label = (this["label"] as? String) ?: "",
            fromSide = fromSide,
            toSide = toSide,
            style = style,
            strokeColorHex = (this["strokeColorHex"] as? String) ?: "#1E293B",
            isDashed = (this["isDashed"] as? Boolean) ?: false,
        )
    }
}
