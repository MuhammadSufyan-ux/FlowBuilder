package com.example.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowAiRepository
import com.example.data.repository.FlowProjectRepository
import com.example.domain.engine.FlowDslGenerator
import com.example.domain.engine.FlowGeometry
import com.example.domain.engine.FlowLayoutEngine
import com.example.domain.engine.FlowValidator
import com.example.domain.model.AiConfigurationState
import com.example.domain.model.AnchorSide
import com.example.domain.model.AppFontFamily
import com.example.domain.model.BorderStyle
import com.example.domain.model.EdgeStyle
import com.example.domain.model.EditorTool
import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowGraph
import com.example.domain.model.FlowNode
import com.example.domain.model.FlowProject
import com.example.domain.model.GridStyle
import com.example.domain.model.LayoutDirection
import com.example.domain.model.NodeShape
import com.example.domain.model.ValidationIssue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class SaveStatus {
    SAVED,
    SAVING,
    UNSAVED,
}

data class AlignmentGuide(
    val isVertical: Boolean,
    val position: Float,
)

data class EditorUiState(
    val isLoading: Boolean = true,
    val project: FlowProject? = null,
    val graph: FlowGraph = FlowGraph(),
    val saveStatus: SaveStatus = SaveStatus.SAVED,
    val autoSaveEnabled: Boolean = true,
    val activeTool: EditorTool = EditorTool.SELECT,
    val selectedNodeIds: Set<String> = emptySet(),
    val selectedEdgeId: String? = null,
    val connectSourceNodeId: String? = null,
    val zoom: Float = 1f,
    val panOffset: Offset = Offset.Zero,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val hasClipboard: Boolean = false,
    val validationIssues: List<ValidationIssue> = emptyList(),
    val issueNodeIds: Set<String> = emptySet(),
    val showValidationHighlights: Boolean = true,
    val alignmentGuides: List<AlignmentGuide> = emptyList(),
    val showShapePalette: Boolean = false,
    val showCodePanel: Boolean = false,
    val showValidationSheet: Boolean = false,
    val showExportSheet: Boolean = false,
    val showLayoutSheet: Boolean = false,
    val showAiSheet: Boolean = false,
    val isAiBusy: Boolean = false,
    val aiOutputTitle: String = "",
    val aiOutputText: String = "",
    val aiErrorMessage: String? = null,
    val aiConfig: AiConfigurationState = AiConfigurationState(),
    val editingTextNodeId: String? = null,
    val editingEdgeId: String? = null,
    val showStylePopover: Boolean = false,
    val showAlignmentGuidesEnabled: Boolean = true,
) {
    val primarySelectedNode: FlowNode?
        get() = if (selectedNodeIds.size == 1) {
            val id = selectedNodeIds.first()
            graph.nodes.firstOrNull { it.id == id }
        } else null

    val selectedEdge: FlowEdge?
        get() = selectedEdgeId?.let { id -> graph.edges.firstOrNull { it.id == id } }
}

sealed interface EditorToast {
    data class Info(val message: String) : EditorToast
}

class EditorViewModel(
    private val userId: String,
    private val projectId: String,
    private val repository: FlowProjectRepository,
    private val aiRepository: FlowAiRepository,
    private val initialShowGuides: Boolean = true,
    private val initialShowValidation: Boolean = true,
    private val initialAutoSaveEnabled: Boolean = true,
    private val defaultFontFamily: AppFontFamily = AppFontFamily.PLUS_JAKARTA_SANS,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EditorUiState(
            showAlignmentGuidesEnabled = initialShowGuides,
            showValidationHighlights = initialShowValidation,
            autoSaveEnabled = initialAutoSaveEnabled,
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _toasts = MutableSharedFlow<EditorToast>()
    val toasts = _toasts.asSharedFlow()

    private val undoStack = ArrayDeque<FlowGraph>()
    private val redoStack = ArrayDeque<FlowGraph>()
    private val maxHistory = 50

    private var clipboardNodes: List<FlowNode> = emptyList()
    private var clipboardEdges: List<FlowEdge> = emptyList()
    private var pasteCount = 0

    private var dragStartSnapshot: FlowGraph? = null
    private var autoSaveJob: Job? = null

    init {
        loadProject()
        viewModelScope.launch {
            aiRepository.observeConfiguration(userId).collect { config ->
                _uiState.update { it.copy(aiConfig = config) }
            }
        }
    }

    private fun loadProject() {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId)
            if (project != null) {
                val issues = FlowValidator.validate(project.graph)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        project = project,
                        graph = project.graph,
                        validationIssues = issues,
                        issueNodeIds = issues.mapNotNull { issue -> issue.nodeId }.toSet(),
                        saveStatus = SaveStatus.SAVED,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateAutoSaveSetting(enabled: Boolean) {
        _uiState.update { it.copy(autoSaveEnabled = enabled) }
        if (enabled && _uiState.value.saveStatus == SaveStatus.UNSAVED) {
            scheduleAutoSave()
        }
    }

    private fun commitGraphMutation(
        newGraph: FlowGraph,
        recordUndo: Boolean = true,
        customUndoSnapshot: FlowGraph? = null,
    ) {
        val prev = customUndoSnapshot ?: _uiState.value.graph
        if (recordUndo && prev != newGraph) {
            undoStack.addLast(prev)
            if (undoStack.size > maxHistory) undoStack.removeFirst()
            redoStack.clear()
        }

        val issues = FlowValidator.validate(newGraph)
        val updatedProject = _uiState.value.project?.copy(
            graph = newGraph,
            updatedAt = System.currentTimeMillis(),
        )
        val nextSaveStatus = if (_uiState.value.autoSaveEnabled) SaveStatus.SAVING else SaveStatus.UNSAVED
        _uiState.update {
            it.copy(
                project = updatedProject,
                graph = newGraph,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                validationIssues = issues,
                issueNodeIds = issues.mapNotNull { issue -> issue.nodeId }.toSet(),
                saveStatus = nextSaveStatus,
            )
        }
        if (_uiState.value.autoSaveEnabled) {
            scheduleAutoSave()
        }
    }

    private fun scheduleAutoSave() {
        if (!_uiState.value.autoSaveEnabled) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500L)
            persistNow(showFeedback = false)
        }
    }

    fun saveNow(showFeedback: Boolean = false) {
        autoSaveJob?.cancel()
        _uiState.update { it.copy(saveStatus = SaveStatus.SAVING) }
        viewModelScope.launch { persistNow(showFeedback = showFeedback) }
    }

    private suspend fun persistNow(showFeedback: Boolean) {
        val currentProject = _uiState.value.project ?: return
        repository.saveProject(userId, currentProject.copy(graph = _uiState.value.graph))
        _uiState.update { it.copy(saveStatus = SaveStatus.SAVED) }
        if (showFeedback) {
            emitToast("Saved to cloud & device")
        }
    }

    fun renameProject(newTitle: String) {
        val cleaned = newTitle.trim().ifBlank { return }
        val current = _uiState.value.project ?: return
        val updated = current.copy(title = cleaned, updatedAt = System.currentTimeMillis())
        _uiState.update {
            it.copy(
                project = updated,
                saveStatus = if (it.autoSaveEnabled) SaveStatus.SAVING else SaveStatus.UNSAVED,
            )
        }
        if (_uiState.value.autoSaveEnabled) scheduleAutoSave()
    }

    // --- Tool & Panel Controls ---

    fun setTool(tool: EditorTool) {
        _uiState.update {
            it.copy(
                activeTool = tool,
                connectSourceNodeId = null,
                showStylePopover = false,
            )
        }
    }

    fun toggleShapePalette(show: Boolean? = null) {
        _uiState.update { it.copy(showShapePalette = show ?: !it.showShapePalette, showStylePopover = false) }
    }

    fun toggleCodePanel(show: Boolean? = null) {
        _uiState.update { it.copy(showCodePanel = show ?: !it.showCodePanel) }
    }

    fun toggleValidationSheet(show: Boolean? = null) {
        _uiState.update { it.copy(showValidationSheet = show ?: !it.showValidationSheet) }
    }

    fun toggleExportSheet(show: Boolean? = null) {
        _uiState.update { it.copy(showExportSheet = show ?: !it.showExportSheet) }
    }

    fun toggleLayoutSheet(show: Boolean? = null) {
        _uiState.update { it.copy(showLayoutSheet = show ?: !it.showLayoutSheet) }
    }

    fun toggleAiSheet(show: Boolean? = null) {
        _uiState.update {
            val next = show ?: !it.showAiSheet
            it.copy(
                showAiSheet = next,
                showStylePopover = false,
                aiErrorMessage = if (next) null else it.aiErrorMessage,
            )
        }
    }

    fun clearAiError() {
        _uiState.update { it.copy(aiErrorMessage = null) }
    }

    fun runSuggestWorkflowImprovements() {
        if (_uiState.value.isAiBusy) return
        val state = _uiState.value
        val title = state.project?.title ?: "Flowchart"
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showAiSheet = true,
                    isAiBusy = true,
                    aiErrorMessage = null,
                    aiOutputTitle = "Workflow Improvement Suggestions",
                )
            }
            val result = aiRepository.suggestWorkflowImprovements(
                userId = userId,
                title = title,
                graph = state.graph,
            )
            result.onSuccess { reply ->
                _uiState.update { it.copy(isAiBusy = false, aiOutputText = reply) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAiBusy = false,
                        aiErrorMessage = err.message ?: "Could not analyze workflow.",
                    )
                }
            }
        }
    }

    fun runExplainWorkflowErrors() {
        if (_uiState.value.isAiBusy) return
        val state = _uiState.value
        val title = state.project?.title ?: "Flowchart"
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showValidationSheet = false,
                    showAiSheet = true,
                    isAiBusy = true,
                    aiErrorMessage = null,
                    aiOutputTitle = "Workflow Error Diagnosis",
                )
            }
            val result = aiRepository.explainWorkflowErrors(
                userId = userId,
                title = title,
                graph = state.graph,
                issues = state.validationIssues,
            )
            result.onSuccess { reply ->
                _uiState.update { it.copy(isAiBusy = false, aiOutputText = reply) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAiBusy = false,
                        aiErrorMessage = err.message ?: "Could not explain workflow errors.",
                    )
                }
            }
        }
    }

    fun runExplainNodesAndConnections() {
        if (_uiState.value.isAiBusy) return
        val state = _uiState.value
        val title = state.project?.title ?: "Flowchart"
        val focusedNode = state.primarySelectedNode
        val heading = if (focusedNode != null) {
            "Understanding \"${focusedNode.text}\""
        } else {
            "Nodes & Connections Guide"
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showAiSheet = true,
                    isAiBusy = true,
                    aiErrorMessage = null,
                    aiOutputTitle = heading,
                )
            }
            val result = aiRepository.explainNodesAndConnections(
                userId = userId,
                title = title,
                graph = state.graph,
                focusedNodeId = focusedNode?.id,
            )
            result.onSuccess { reply ->
                _uiState.update { it.copy(isAiBusy = false, aiOutputText = reply) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAiBusy = false,
                        aiErrorMessage = err.message ?: "Could not explain diagram.",
                    )
                }
            }
        }
    }

    fun runGenerateWorkflowSteps(instruction: String) {
        if (_uiState.value.isAiBusy) return
        val clean = instruction.trim()
        if (clean.isBlank()) {
            _uiState.update { it.copy(aiErrorMessage = "Describe the workflow steps you want to add.") }
            return
        }
        val state = _uiState.value
        val title = state.project?.title ?: "Flowchart"
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showAiSheet = true,
                    isAiBusy = true,
                    aiErrorMessage = null,
                    aiOutputTitle = "Generated Workflow Steps",
                )
            }
            val result = aiRepository.generateWorkflowSteps(
                userId = userId,
                title = title,
                graph = state.graph,
                instruction = clean,
                defaultFont = defaultFontFamily,
            )
            result.onSuccess { payload ->
                val currentGraph = _uiState.value.graph
                val merged = currentGraph.copy(
                    nodes = currentGraph.nodes + payload.addedNodes,
                    edges = currentGraph.edges + payload.addedEdges,
                )
                commitGraphMutation(merged)
                _uiState.update {
                    it.copy(
                        isAiBusy = false,
                        selectedNodeIds = payload.addedNodes.map { n -> n.id }.toSet(),
                        selectedEdgeId = null,
                        aiOutputText = "${payload.summary}\n\nAdded ${payload.addedNodes.size} shape(s) and ${payload.addedEdges.size} connection(s) to your canvas. You can undo anytime.",
                    )
                }
                emitToast("Added ${payload.addedNodes.size} AI workflow step(s)")
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAiBusy = false,
                        aiErrorMessage = err.message ?: "Could not generate workflow steps.",
                    )
                }
            }
        }
    }

    fun toggleStylePopover(show: Boolean? = null) {
        _uiState.update { it.copy(showStylePopover = show ?: !it.showStylePopover) }
    }

    fun openTextEditor(nodeId: String?) {
        _uiState.update { it.copy(editingTextNodeId = nodeId, showStylePopover = false) }
    }

    fun openEdgeEditor(edgeId: String?) {
        _uiState.update { it.copy(editingEdgeId = edgeId, showStylePopover = false) }
    }

    fun toggleValidationHighlights() {
        _uiState.update { it.copy(showValidationHighlights = !it.showValidationHighlights) }
    }

    // --- Viewport & Camera ---

    fun updateViewport(zoom: Float, panOffset: Offset) {
        _uiState.update {
            it.copy(
                zoom = zoom.coerceIn(0.3f, 2.5f),
                panOffset = panOffset,
            )
        }
    }

    fun zoomBy(factor: Float, canvasWidth: Float, canvasHeight: Float) {
        val state = _uiState.value
        val newZoom = (state.zoom * factor).coerceIn(0.3f, 2.5f)
        if (newZoom == state.zoom) return
        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
        val worldCenter = (center - state.panOffset) / state.zoom
        val newPan = center - worldCenter * newZoom
        _uiState.update { it.copy(zoom = newZoom, panOffset = newPan) }
    }

    fun fitToScreen(canvasWidth: Float, canvasHeight: Float) {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return
        val nodes = _uiState.value.graph.nodes
        if (nodes.isEmpty()) {
            _uiState.update { it.copy(zoom = 1f, panOffset = Offset(canvasWidth * 0.15f, canvasHeight * 0.15f)) }
            return
        }
        val minX = nodes.minOf { it.x }
        val minY = nodes.minOf { it.y }
        val maxX = nodes.maxOf { it.right }
        val maxY = nodes.maxOf { it.bottom }
        val contentW = (maxX - minX).coerceAtLeast(160f)
        val contentH = (maxY - minY).coerceAtLeast(160f)
        val pad = 80f
        val scaleX = (canvasWidth - pad * 2) / contentW
        val scaleY = (canvasHeight - pad * 2) / contentH
        val targetZoom = minOf(scaleX, scaleY).coerceIn(0.38f, 1.35f)
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val panX = canvasWidth / 2f - centerX * targetZoom
        val panY = canvasHeight / 2f - centerY * targetZoom
        _uiState.update { it.copy(zoom = targetZoom, panOffset = Offset(panX, panY)) }
    }

    fun focusOnNode(nodeId: String, canvasWidth: Float, canvasHeight: Float) {
        val node = _uiState.value.graph.nodes.firstOrNull { it.id == nodeId } ?: return
        val targetZoom = _uiState.value.zoom.coerceIn(0.85f, 1.3f)
        val panX = canvasWidth / 2f - node.centerX * targetZoom
        val panY = canvasHeight / 2f - node.centerY * targetZoom
        _uiState.update {
            it.copy(
                zoom = targetZoom,
                panOffset = Offset(panX, panY),
                selectedNodeIds = setOf(nodeId),
                selectedEdgeId = null,
                showValidationSheet = false,
            )
        }
    }

    // --- Selection ---

    fun selectSingleNode(nodeId: String) {
        val state = _uiState.value
        if (state.activeTool == EditorTool.CONNECT) {
            handleConnectTap(nodeId)
            return
        }
        if (state.activeTool == EditorTool.MULTI_SELECT) {
            val toggled = if (nodeId in state.selectedNodeIds) {
                state.selectedNodeIds - nodeId
            } else {
                state.selectedNodeIds + nodeId
            }
            _uiState.update { it.copy(selectedNodeIds = toggled, selectedEdgeId = null, showStylePopover = false) }
            return
        }
        _uiState.update {
            it.copy(
                selectedNodeIds = setOf(nodeId),
                selectedEdgeId = null,
                showStylePopover = false,
            )
        }
    }

    fun selectNodesInRect(worldLeft: Float, worldTop: Float, worldRight: Float, worldBottom: Float) {
        val hits = _uiState.value.graph.nodes.filter { n ->
            n.right >= worldLeft && n.x <= worldRight && n.bottom >= worldTop && n.y <= worldBottom
        }.map { it.id }.toSet()
        _uiState.update { it.copy(selectedNodeIds = hits, selectedEdgeId = null, showStylePopover = false) }
    }

    fun selectEdge(edgeId: String) {
        _uiState.update {
            it.copy(
                selectedEdgeId = edgeId,
                selectedNodeIds = emptySet(),
                connectSourceNodeId = null,
                showStylePopover = false,
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedNodeIds = emptySet(),
                selectedEdgeId = null,
                connectSourceNodeId = null,
                showStylePopover = false,
                showShapePalette = false,
            )
        }
    }

    // --- Node Creation & Manipulation ---

    fun addNode(shape: NodeShape, worldCenterX: Float, worldCenterY: Float) {
        val graph = _uiState.value.graph
        val rawX = worldCenterX - shape.defaultWidth / 2f
        val rawY = worldCenterY - shape.defaultHeight / 2f
        val snappedX = FlowGeometry.snap(rawX, graph.snapToGrid)
        val snappedY = FlowGeometry.snap(rawY, graph.snapToGrid)

        val defaultText = when {
            shape == NodeShape.OVAL && graph.nodes.any { it.shape == NodeShape.OVAL && it.text.equals("Start", true) } -> "End"
            else -> shape.defaultText
        }
        val defaultFill = if (shape == NodeShape.OVAL && defaultText == "End") "#FA93B2" else shape.defaultFillHex

        val newNode = FlowNode(
            shape = shape,
            text = defaultText,
            x = snappedX,
            y = snappedY,
            width = shape.defaultWidth,
            height = shape.defaultHeight,
            fillColorHex = defaultFill,
            fontFamily = defaultFontFamily,
            isBold = shape != NodeShape.TEXT_BOX && shape != NodeShape.ANNOTATION,
        )
        commitGraphMutation(graph.copy(nodes = graph.nodes + newNode))
        _uiState.update {
            it.copy(
                selectedNodeIds = setOf(newNode.id),
                selectedEdgeId = null,
                showShapePalette = false,
                editingTextNodeId = if (shape == NodeShape.TEXT_BOX) newNode.id else null,
            )
        }
    }

    fun beginDragInteraction() {
        dragStartSnapshot = _uiState.value.graph
    }

    fun moveSelectedNodesBy(deltaWorldX: Float, deltaWorldY: Float, activeNodeId: String? = null) {
        val state = _uiState.value
        val targets = when {
            activeNodeId != null && activeNodeId in state.selectedNodeIds -> state.selectedNodeIds
            activeNodeId != null -> setOf(activeNodeId)
            else -> state.selectedNodeIds
        }
        if (targets.isEmpty()) return

        val otherNodes = state.graph.nodes.filter { it.id !in targets }
        val guides = mutableListOf<AlignmentGuide>()

        var movedNodes = state.graph.nodes.map { node ->
            if (node.id in targets) {
                node.copy(x = node.x + deltaWorldX, y = node.y + deltaWorldY)
            } else {
                node
            }
        }

        if (state.showAlignmentGuidesEnabled && targets.size == 1) {
            val dragged = movedNodes.firstOrNull { it.id == targets.first() }
            if (dragged != null) {
                val threshold = 7f
                var adjustX = 0f
                var adjustY = 0f
                for (other in otherNodes) {
                    if (kotlin.math.abs(dragged.centerX - other.centerX) < threshold && adjustX == 0f) {
                        adjustX = other.centerX - dragged.centerX
                        guides.add(AlignmentGuide(isVertical = true, position = other.centerX))
                    } else if (kotlin.math.abs(dragged.x - other.x) < threshold && adjustX == 0f) {
                        adjustX = other.x - dragged.x
                        guides.add(AlignmentGuide(isVertical = true, position = other.x))
                    }
                    if (kotlin.math.abs(dragged.centerY - other.centerY) < threshold && adjustY == 0f) {
                        adjustY = other.centerY - dragged.centerY
                        guides.add(AlignmentGuide(isVertical = false, position = other.centerY))
                    } else if (kotlin.math.abs(dragged.y - other.y) < threshold && adjustY == 0f) {
                        adjustY = other.y - dragged.y
                        guides.add(AlignmentGuide(isVertical = false, position = other.y))
                    }
                }
                if (adjustX != 0f || adjustY != 0f) {
                    movedNodes = movedNodes.map { n ->
                        if (n.id == dragged.id) n.copy(x = n.x + adjustX, y = n.y + adjustY) else n
                    }
                }
            }
        }

        _uiState.update {
            it.copy(
                graph = it.graph.copy(nodes = movedNodes),
                selectedNodeIds = targets,
                alignmentGuides = guides,
                saveStatus = if (it.autoSaveEnabled) SaveStatus.SAVING else SaveStatus.UNSAVED,
            )
        }
    }

    fun endDragInteraction() {
        val state = _uiState.value
        val snap = state.graph.snapToGrid
        val snappedNodes = if (snap) {
            state.graph.nodes.map { n ->
                if (n.id in state.selectedNodeIds) {
                    n.copy(
                        x = FlowGeometry.snap(n.x, true),
                        y = FlowGeometry.snap(n.y, true),
                    )
                } else n
            }
        } else {
            state.graph.nodes
        }
        val finalGraph = state.graph.copy(nodes = snappedNodes)
        val snapshot = dragStartSnapshot
        dragStartSnapshot = null
        _uiState.update { it.copy(alignmentGuides = emptyList()) }
        if (snapshot != null && snapshot != finalGraph) {
            commitGraphMutation(finalGraph, recordUndo = true, customUndoSnapshot = snapshot)
        } else if (state.autoSaveEnabled) {
            scheduleAutoSave()
        }
    }

    fun resizeNode(nodeId: String, deltaWidth: Float, deltaHeight: Float) {
        val state = _uiState.value
        val updated = state.graph.nodes.map { n ->
            if (n.id == nodeId) {
                val isCircle = n.shape in setOf(NodeShape.CONNECTOR, NodeShape.OR_GATE, NodeShape.SUMMING_JUNCTION)
                val minW = if (isCircle) 48f else 84f
                val minH = 44f
                val newW = (n.width + deltaWidth).coerceIn(minW, 440f)
                val newH = if (isCircle) newW else (n.height + deltaHeight).coerceIn(minH, 300f)
                n.copy(width = newW, height = newH)
            } else n
        }
        _uiState.update {
            it.copy(
                graph = it.graph.copy(nodes = updated),
                saveStatus = if (it.autoSaveEnabled) SaveStatus.SAVING else SaveStatus.UNSAVED,
            )
        }
    }

    fun endResizeInteraction() {
        val snapshot = dragStartSnapshot
        dragStartSnapshot = null
        val currentGraph = _uiState.value.graph
        if (snapshot != null && snapshot != currentGraph) {
            commitGraphMutation(currentGraph, recordUndo = true, customUndoSnapshot = snapshot)
        }
    }

    fun updateNodeTextAndStyle(
        nodeId: String,
        text: String,
        fontSizeSp: Float,
        isBold: Boolean,
        textColorHex: String,
        fontFamily: AppFontFamily,
    ) {
        val updatedNodes = _uiState.value.graph.nodes.map { n ->
            if (n.id == nodeId) {
                n.copy(
                    text = text.trim().ifEmpty { n.shape.defaultText },
                    fontSizeSp = fontSizeSp.coerceIn(10f, 32f),
                    isBold = isBold,
                    textColorHex = textColorHex,
                    fontFamily = fontFamily,
                )
            } else n
        }
        commitGraphMutation(_uiState.value.graph.copy(nodes = updatedNodes))
        _uiState.update { it.copy(editingTextNodeId = null) }
    }

    fun updateSelectedNodesStyle(
        fillColorHex: String? = null,
        borderColorHex: String? = null,
        textColorHex: String? = null,
        borderWidth: Float? = null,
        borderStyle: BorderStyle? = null,
        shape: NodeShape? = null,
        fontFamily: AppFontFamily? = null,
        fontSizeDelta: Float? = null,
    ) {
        val targets = _uiState.value.selectedNodeIds
        if (targets.isEmpty()) return
        val updatedNodes = _uiState.value.graph.nodes.map { n ->
            if (n.id in targets) {
                val targetShape = shape ?: n.shape
                val newFontSize = if (fontSizeDelta != null) {
                    (n.fontSizeSp + fontSizeDelta).coerceIn(10f, 32f)
                } else {
                    n.fontSizeSp
                }
                n.copy(
                    shape = targetShape,
                    fillColorHex = fillColorHex ?: n.fillColorHex,
                    borderColorHex = borderColorHex ?: n.borderColorHex,
                    textColorHex = textColorHex ?: n.textColorHex,
                    borderWidth = borderWidth ?: n.borderWidth,
                    borderStyle = borderStyle ?: n.borderStyle,
                    fontFamily = fontFamily ?: n.fontFamily,
                    fontSizeSp = newFontSize,
                )
            } else n
        }
        commitGraphMutation(_uiState.value.graph.copy(nodes = updatedNodes))
    }

    // --- Edges & Connections ---

    private fun handleConnectTap(nodeId: String) {
        val sourceId = _uiState.value.connectSourceNodeId
        if (sourceId == null) {
            _uiState.update {
                it.copy(
                    connectSourceNodeId = nodeId,
                    selectedNodeIds = setOf(nodeId),
                    selectedEdgeId = null,
                )
            }
            emitToast("Tap target shape to connect arrow")
        } else if (sourceId == nodeId) {
            _uiState.update { it.copy(connectSourceNodeId = null) }
        } else {
            createEdge(sourceId, nodeId)
            _uiState.update { it.copy(connectSourceNodeId = null) }
        }
    }

    fun startConnectFromSelected() {
        val source = _uiState.value.selectedNodeIds.firstOrNull() ?: return
        _uiState.update {
            it.copy(
                activeTool = EditorTool.CONNECT,
                connectSourceNodeId = source,
                showStylePopover = false,
            )
        }
        emitToast("Tap another shape to connect")
    }

    fun createEdge(
        fromNodeId: String,
        toNodeId: String,
        fromSide: AnchorSide = AnchorSide.AUTO,
        toSide: AnchorSide = AnchorSide.AUTO,
    ) {
        if (fromNodeId == toNodeId) return
        val graph = _uiState.value.graph
        val alreadyExists = graph.edges.any { it.fromNodeId == fromNodeId && it.toNodeId == toNodeId }
        if (alreadyExists) {
            emitToast("Connection already exists")
            return
        }
        val fromNode = graph.nodes.firstOrNull { it.id == fromNodeId }
        val existingFromCount = graph.edges.count { it.fromNodeId == fromNodeId }
        val smartLabel = if (fromNode?.shape == NodeShape.DIAMOND) {
            when (existingFromCount) {
                0 -> "Yes"
                1 -> "No"
                else -> ""
            }
        } else ""

        val newEdge = FlowEdge(
            fromNodeId = fromNodeId,
            toNodeId = toNodeId,
            label = smartLabel,
            fromSide = fromSide,
            toSide = toSide,
        )
        commitGraphMutation(graph.copy(edges = graph.edges + newEdge))
        _uiState.update {
            it.copy(
                selectedEdgeId = newEdge.id,
                selectedNodeIds = emptySet(),
            )
        }
    }

    fun updateEdge(
        edgeId: String,
        label: String? = null,
        style: EdgeStyle? = null,
        strokeColorHex: String? = null,
        isDashed: Boolean? = null,
    ) {
        val updated = _uiState.value.graph.edges.map { e ->
            if (e.id == edgeId) {
                e.copy(
                    label = label ?: e.label,
                    style = style ?: e.style,
                    strokeColorHex = strokeColorHex ?: e.strokeColorHex,
                    isDashed = isDashed ?: e.isDashed,
                )
            } else e
        }
        commitGraphMutation(_uiState.value.graph.copy(edges = updated))
    }

    fun reverseSelectedEdge() {
        val edgeId = _uiState.value.selectedEdgeId ?: return
        val updated = _uiState.value.graph.edges.map { e ->
            if (e.id == edgeId) {
                e.copy(
                    fromNodeId = e.toNodeId,
                    toNodeId = e.fromNodeId,
                    fromSide = e.toSide,
                    toSide = e.fromSide,
                )
            } else e
        }
        commitGraphMutation(_uiState.value.graph.copy(edges = updated))
    }

    // --- Delete, Copy, Paste, Duplicate ---

    fun deleteSelected() {
        val state = _uiState.value
        val graph = state.graph
        if (state.selectedEdgeId != null) {
            val remainingEdges = graph.edges.filterNot { it.id == state.selectedEdgeId }
            commitGraphMutation(graph.copy(edges = remainingEdges))
            _uiState.update { it.copy(selectedEdgeId = null, editingEdgeId = null) }
            return
        }
        if (state.selectedNodeIds.isNotEmpty()) {
            val remainingNodes = graph.nodes.filterNot { it.id in state.selectedNodeIds }
            val remainingEdges = graph.edges.filterNot {
                it.fromNodeId in state.selectedNodeIds || it.toNodeId in state.selectedNodeIds
            }
            commitGraphMutation(graph.copy(nodes = remainingNodes, edges = remainingEdges))
            _uiState.update {
                it.copy(
                    selectedNodeIds = emptySet(),
                    showStylePopover = false,
                    editingTextNodeId = null,
                )
            }
        }
    }

    fun copySelected() {
        val state = _uiState.value
        if (state.selectedNodeIds.isEmpty()) return
        clipboardNodes = state.graph.nodes.filter { it.id in state.selectedNodeIds }
        clipboardEdges = state.graph.edges.filter {
            it.fromNodeId in state.selectedNodeIds && it.toNodeId in state.selectedNodeIds
        }
        pasteCount = 0
        _uiState.update { it.copy(hasClipboard = clipboardNodes.isNotEmpty()) }
        emitToast("Copied ${clipboardNodes.size} shape${if (clipboardNodes.size > 1) "s" else ""}")
    }

    fun pasteClipboard() {
        if (clipboardNodes.isEmpty()) return
        pasteCount++
        val offset = 36f * pasteCount
        insertClonedSelection(clipboardNodes, clipboardEdges, offset, offset)
    }

    fun duplicateSelected() {
        val state = _uiState.value
        if (state.selectedNodeIds.isEmpty()) return
        val nodes = state.graph.nodes.filter { it.id in state.selectedNodeIds }
        val edges = state.graph.edges.filter {
            it.fromNodeId in state.selectedNodeIds && it.toNodeId in state.selectedNodeIds
        }
        insertClonedSelection(nodes, edges, 40f, 40f)
    }

    private fun insertClonedSelection(
        sourceNodes: List<FlowNode>,
        sourceEdges: List<FlowEdge>,
        offsetX: Float,
        offsetY: Float,
    ) {
        val graph = _uiState.value.graph
        val idMapping = mutableMapOf<String, String>()
        val clonedNodes = sourceNodes.map { node ->
            val newId = UUID.randomUUID().toString()
            idMapping[node.id] = newId
            node.copy(
                id = newId,
                x = FlowGeometry.snap(node.x + offsetX, graph.snapToGrid),
                y = FlowGeometry.snap(node.y + offsetY, graph.snapToGrid),
            )
        }
        val clonedEdges = sourceEdges.mapNotNull { edge ->
            val newFrom = idMapping[edge.fromNodeId] ?: return@mapNotNull null
            val newTo = idMapping[edge.toNodeId] ?: return@mapNotNull null
            edge.copy(
                id = UUID.randomUUID().toString(),
                fromNodeId = newFrom,
                toNodeId = newTo,
            )
        }
        commitGraphMutation(
            graph.copy(
                nodes = graph.nodes + clonedNodes,
                edges = graph.edges + clonedEdges,
            )
        )
        _uiState.update {
            it.copy(
                selectedNodeIds = clonedNodes.map { n -> n.id }.toSet(),
                selectedEdgeId = null,
            )
        }
    }

    // --- Undo & Redo ---

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeLast()
        redoStack.addLast(_uiState.value.graph)
        val issues = FlowValidator.validate(previous)
        _uiState.update {
            it.copy(
                graph = previous,
                project = it.project?.copy(graph = previous),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                selectedNodeIds = it.selectedNodeIds.filter { id -> previous.nodes.any { n -> n.id == id } }.toSet(),
                selectedEdgeId = it.selectedEdgeId?.takeIf { id -> previous.edges.any { e -> e.id == id } },
                validationIssues = issues,
                issueNodeIds = issues.mapNotNull { issue -> issue.nodeId }.toSet(),
                saveStatus = if (it.autoSaveEnabled) SaveStatus.SAVING else SaveStatus.UNSAVED,
            )
        }
        if (_uiState.value.autoSaveEnabled) scheduleAutoSave()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(_uiState.value.graph)
        val issues = FlowValidator.validate(next)
        _uiState.update {
            it.copy(
                graph = next,
                project = it.project?.copy(graph = next),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                validationIssues = issues,
                issueNodeIds = issues.mapNotNull { issue -> issue.nodeId }.toSet(),
                saveStatus = if (it.autoSaveEnabled) SaveStatus.SAVING else SaveStatus.UNSAVED,
            )
        }
        if (_uiState.value.autoSaveEnabled) scheduleAutoSave()
    }

    // --- Smart Layout & Grid Settings ---

    fun applyAutoLayout(direction: LayoutDirection) {
        val arranged = FlowLayoutEngine.autoArrange(_uiState.value.graph, direction)
        commitGraphMutation(arranged)
        _uiState.update { it.copy(showLayoutSheet = false) }
        emitToast("Applied ${direction.label} layout")
    }

    fun toggleSnapToGrid() {
        val current = _uiState.value.graph
        val updated = current.copy(snapToGrid = !current.snapToGrid)
        commitGraphMutation(updated, recordUndo = false)
        emitToast(if (updated.snapToGrid) "Snap-to-grid ON" else "Snap-to-grid OFF")
    }

    fun cycleGridStyle() {
        val current = _uiState.value.graph
        val values = GridStyle.entries
        val next = values[(values.indexOf(current.gridStyle) + 1) % values.size]
        commitGraphMutation(current.copy(gridStyle = next), recordUndo = false)
        emitToast("Grid: ${next.label}")
    }

    fun generatePseudocode(): String = FlowDslGenerator.generatePseudocode(
        graph = _uiState.value.graph,
        title = _uiState.value.project?.title ?: "Algorithm",
    )

    fun generateMermaidCode(): String = FlowDslGenerator.generateMermaidDsl(_uiState.value.graph)

    fun generatePrettyJson(): String {
        val project = _uiState.value.project
        return if (project != null) {
            repository.exportToJson(project.copy(graph = _uiState.value.graph))
        } else {
            repository.encodeGraphPretty(_uiState.value.graph)
        }
    }

    private fun emitToast(message: String) {
        viewModelScope.launch { _toasts.emit(EditorToast.Info(message)) }
    }

    class Factory(
        private val userId: String,
        private val projectId: String,
        private val repository: FlowProjectRepository,
        private val aiRepository: FlowAiRepository,
        private val initialShowGuides: Boolean,
        private val initialShowValidation: Boolean,
        private val initialAutoSaveEnabled: Boolean,
        private val defaultFontFamily: AppFontFamily,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(
                userId = userId,
                projectId = projectId,
                repository = repository,
                aiRepository = aiRepository,
                initialShowGuides = initialShowGuides,
                initialShowValidation = initialShowValidation,
                initialAutoSaveEnabled = initialAutoSaveEnabled,
                defaultFontFamily = defaultFontFamily,
            ) as T
        }
    }
}
