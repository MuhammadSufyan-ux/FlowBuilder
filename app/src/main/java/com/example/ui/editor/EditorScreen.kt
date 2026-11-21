package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.export.FlowExportManager
import com.example.domain.model.EdgeStyle
import com.example.domain.model.EditorTool
import com.example.domain.model.NodeShape
import com.example.domain.model.ValidationSeverity
import com.example.ui.theme.LocalFlowStudioColors
import com.example.viewmodel.EditorToast
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    autoSaveEnabled: Boolean,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val studioColors = LocalFlowStudioColors.current

    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    var hasInitialFitted by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var pendingDarkExport by remember { mutableStateOf(false) }

    LaunchedEffect(autoSaveEnabled) {
        viewModel.updateAutoSaveSetting(autoSaveEnabled)
    }

    val savePngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = FlowExportManager.renderToPngBytes(state.graph, darkBackground = pendingDarkExport)
                FlowExportManager.writeBytesToUri(context, uri, bytes)
                    .onSuccess { snackbarHostState.showSnackbar("Saved PNG diagram") }
                    .onFailure { snackbarHostState.showSnackbar("Could not save PNG") }
            }
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val title = state.project?.title ?: "Flowchart"
                val bytes = FlowExportManager.renderToPdfBytes(state.graph, title, darkBackground = pendingDarkExport)
                FlowExportManager.writeBytesToUri(context, uri, bytes)
                    .onSuccess { snackbarHostState.showSnackbar("Saved PDF document") }
                    .onFailure { snackbarHostState.showSnackbar("Could not save PDF") }
            }
        }
    }

    val saveHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val title = state.project?.title ?: "Flowchart"
                val bytes = FlowExportManager.renderToHtmlBytes(state.graph, title, darkBackground = pendingDarkExport)
                FlowExportManager.writeBytesToUri(context, uri, bytes)
                    .onSuccess { snackbarHostState.showSnackbar("Saved HTML webpage (.html)") }
                    .onFailure { snackbarHostState.showSnackbar("Could not save HTML file") }
            }
        }
    }

    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val jsonBytes = viewModel.generatePrettyJson().toByteArray(Charsets.UTF_8)
                FlowExportManager.writeBytesToUri(context, uri, jsonBytes)
                    .onSuccess { snackbarHostState.showSnackbar("Saved JSON project") }
                    .onFailure { snackbarHostState.showSnackbar("Could not save JSON") }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toasts.collect { toast ->
            when (toast) {
                is EditorToast.Info -> snackbarHostState.showSnackbar(toast.message)
            }
        }
    }

    LaunchedEffect(state.isLoading, canvasWidth, canvasHeight) {
        if (!state.isLoading && !hasInitialFitted && canvasWidth > 0f && canvasHeight > 0f) {
            hasInitialFitted = true
            viewModel.fitToScreen(canvasWidth, canvasHeight)
        }
    }

    DisposableEffect(autoSaveEnabled) {
        onDispose {
            if (autoSaveEnabled) {
                viewModel.saveNow(showFeedback = false)
            }
        }
    }

    BackHandler {
        if (autoSaveEnabled) {
            viewModel.saveNow(showFeedback = false)
        }
        onNavigateBack()
    }

    Scaffold(
        containerColor = studioColors.canvasBackground,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
    ) { _ ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 720.dp

            FlowCanvasView(
                state = state,
                dragDropGhost = null,
                modifier = Modifier.fillMaxSize(),
                onCanvasSizeChanged = { w, h ->
                    canvasWidth = w
                    canvasHeight = h
                },
                onViewportChanged = viewModel::updateViewport,
                onNodeTapped = viewModel::selectSingleNode,
                onNodeDoubleTapped = { nodeId ->
                    viewModel.selectSingleNode(nodeId)
                    viewModel.openTextEditor(nodeId)
                },
                onEdgeTapped = viewModel::selectEdge,
                onEdgeDoubleTapped = { edgeId ->
                    viewModel.selectEdge(edgeId)
                    viewModel.openEdgeEditor(edgeId)
                },
                onEmptyCanvasTapped = viewModel::clearSelection,
                onBeginDrag = viewModel::beginDragInteraction,
                onMoveNodes = viewModel::moveSelectedNodesBy,
                onEndDrag = viewModel::endDragInteraction,
                onResizeNode = viewModel::resizeNode,
                onEndResize = viewModel::endResizeInteraction,
                onConnectNodes = viewModel::createEdge,
                onMarqueeSelect = viewModel::selectNodesInRect,
            )

            // Top Header + Floating Mode Pill
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EditorTopBar(
                    title = state.project?.title ?: "Flowchart",
                    saveStatus = state.saveStatus,
                    autoSaveEnabled = state.autoSaveEnabled,
                    issueCount = state.validationIssues.count { it.severity != ValidationSeverity.INFO },
                    hasErrors = state.validationIssues.any { it.severity == ValidationSeverity.ERROR },
                    showCodePanel = state.showCodePanel,
                    onBack = {
                        if (autoSaveEnabled) viewModel.saveNow(showFeedback = false)
                        onNavigateBack()
                    },
                    onRenameClick = { showRenameDialog = true },
                    onManualSave = { viewModel.saveNow(showFeedback = true) },
                    onToggleCodePanel = { viewModel.toggleCodePanel() },
                    onOpenAiAssistant = { viewModel.toggleAiSheet(true) },
                    onOpenValidation = { viewModel.toggleValidationSheet(true) },
                    onOpenAutoLayout = { viewModel.toggleLayoutSheet(true) },
                    onOpenExport = { viewModel.toggleExportSheet(true) },
                )

                FloatingModePillToolbar(
                    activeTool = state.activeTool,
                    showShapePalette = state.showShapePalette,
                    onSelectTool = { tool ->
                        viewModel.toggleShapePalette(false)
                        viewModel.setTool(tool)
                    },
                    onAddTextBox = {
                        val centerScreen = Offset(canvasWidth / 2f, canvasHeight / 2f)
                        val worldCenter = (centerScreen - state.panOffset) / state.zoom
                        viewModel.addNode(NodeShape.TEXT_BOX, worldCenter.x, worldCenter.y)
                    },
                    onToggleShapePalette = { viewModel.toggleShapePalette() },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                if (state.activeTool == EditorTool.CONNECT || state.activeTool == EditorTool.MULTI_SELECT) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = when (state.activeTool) {
                                EditorTool.CONNECT -> if (state.connectSourceNodeId == null) {
                                    "Drag from a shape or tap two shapes to connect an arrow"
                                } else {
                                    "Now tap the target shape to complete the arrow"
                                }
                                EditorTool.MULTI_SELECT -> "Tap shapes or drag a box to multi-select"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            // Slide-in Live Pseudocode / Flow DSL / JSON Inspector Panel
            AnimatedVisibility(
                visible = state.showCodePanel,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it }),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, top = 124.dp, bottom = 76.dp)
                    .width(if (isWide) 336.dp else 296.dp)
                    .fillMaxHeight(),
            ) {
                DiagramCodeInspectorPanel(
                    pseudocode = remember(state.graph, state.project?.title) { viewModel.generatePseudocode() },
                    mermaidCode = remember(state.graph) { viewModel.generateMermaidCode() },
                    jsonCode = remember(state.graph, state.project?.title) { viewModel.generatePrettyJson() },
                    onClose = { viewModel.toggleCodePanel(false) },
                    onCopied = {
                        scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                    },
                )
            }

            // Bottom Floating Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SelectionContextToolbar(
                    state = state,
                    onEditText = {
                        state.primarySelectedNode?.let { viewModel.openTextEditor(it.id) }
                    },
                    onToggleStylePopover = { viewModel.toggleStylePopover() },
                    onConnectFromNode = viewModel::startConnectFromSelected,
                    onCopy = viewModel::copySelected,
                    onDuplicate = viewModel::duplicateSelected,
                    onDelete = viewModel::deleteSelected,
                    onUpdateFillColor = { hex -> viewModel.updateSelectedNodesStyle(fillColorHex = hex) },
                    onUpdateBorderColor = { hex -> viewModel.updateSelectedNodesStyle(borderColorHex = hex) },
                    onUpdateBorderStyle = { borderStyle -> viewModel.updateSelectedNodesStyle(borderStyle = borderStyle) },
                    onChangeShape = { shape -> viewModel.updateSelectedNodesStyle(shape = shape) },
                    onChangeFontFamily = { font -> viewModel.updateSelectedNodesStyle(fontFamily = font) },
                    onAdjustFontSize = { delta -> viewModel.updateSelectedNodesStyle(fontSizeDelta = delta) },
                    onEditEdge = {
                        state.selectedEdgeId?.let { viewModel.openEdgeEditor(it) }
                    },
                    onCycleEdgeStyle = {
                        state.selectedEdge?.let { edge ->
                            val styles = EdgeStyle.entries
                            val next = styles[(styles.indexOf(edge.style) + 1) % styles.size]
                            viewModel.updateEdge(edge.id, style = next)
                        }
                    },
                    onToggleEdgeDashed = {
                        state.selectedEdge?.let { edge ->
                            viewModel.updateEdge(edge.id, isDashed = !edge.isDashed)
                        }
                    },
                    onReverseEdge = viewModel::reverseSelectedEdge,
                )

                BottomZoomAndHistoryBar(
                    zoom = state.zoom,
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    hasClipboard = state.hasClipboard,
                    snapToGrid = state.graph.snapToGrid,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onPaste = viewModel::pasteClipboard,
                    onZoomOut = { viewModel.zoomBy(0.84f, canvasWidth, canvasHeight) },
                    onZoomIn = { viewModel.zoomBy(1.18f, canvasWidth, canvasHeight) },
                    onFitToScreen = { viewModel.fitToScreen(canvasWidth, canvasHeight) },
                    onToggleSnap = viewModel::toggleSnapToGrid,
                    onCycleGridStyle = viewModel::cycleGridStyle,
                )
            }
        }
    }

    // --- Bottom Sheets & Dialogs ---

    if (state.showShapePalette) {
        ShapesPickerBottomSheet(
            onSelectShape = { shape ->
                val centerScreen = Offset(canvasWidth / 2f, canvasHeight / 2f)
                val worldCenter = (centerScreen - state.panOffset) / state.zoom
                viewModel.addNode(shape, worldCenter.x, worldCenter.y)
            },
            onSelectFlowlineTool = {
                viewModel.setTool(EditorTool.CONNECT)
            },
            onDismiss = { viewModel.toggleShapePalette(false) },
        )
    }

    state.editingTextNodeId?.let { editId ->
        val node = state.graph.nodes.firstOrNull { it.id == editId }
        if (node != null) {
            EditNodeTextDialog(
                node = node,
                onDismiss = { viewModel.openTextEditor(null) },
                onConfirm = { text, fontSize, isBold, colorHex, fontFamily ->
                    viewModel.updateNodeTextAndStyle(node.id, text, fontSize, isBold, colorHex, fontFamily)
                },
            )
        }
    }

    state.editingEdgeId?.let { edgeId ->
        val edge = state.graph.edges.firstOrNull { it.id == edgeId }
        if (edge != null) {
            EditEdgeDialog(
                edge = edge,
                onDismiss = { viewModel.openEdgeEditor(null) },
                onConfirm = { label, style, colorHex, dashed ->
                    viewModel.updateEdge(edge.id, label, style, colorHex, dashed)
                    viewModel.openEdgeEditor(null)
                },
            )
        }
    }

    if (showRenameDialog) {
        var titleText by remember(state.project?.title) { mutableStateOf(state.project?.title ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    singleLine = true,
                    label = { Text("Flowchart name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameProject(titleText)
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (state.showLayoutSheet) {
        AutoLayoutBottomSheet(
            onDismiss = { viewModel.toggleLayoutSheet(false) },
            onSelectLayout = { dir ->
                viewModel.applyAutoLayout(dir)
                viewModel.fitToScreen(canvasWidth, canvasHeight)
            },
        )
    }

    if (state.showValidationSheet) {
        SmartValidationBottomSheet(
            issues = state.validationIssues,
            showHighlights = state.showValidationHighlights,
            onToggleHighlights = viewModel::toggleValidationHighlights,
            onFocusNode = { nodeId -> viewModel.focusOnNode(nodeId, canvasWidth, canvasHeight) },
            onExplainErrorsWithAi = viewModel::runExplainWorkflowErrors,
            onDismiss = { viewModel.toggleValidationSheet(false) },
        )
    }

    if (state.showAiSheet) {
        AiWorkflowAssistantBottomSheet(
            aiConfig = state.aiConfig,
            focusedNodeText = state.primarySelectedNode?.text,
            issueCount = state.validationIssues.count { it.severity != ValidationSeverity.INFO },
            isBusy = state.isAiBusy,
            outputTitle = state.aiOutputTitle,
            outputText = state.aiOutputText,
            errorMessage = state.aiErrorMessage,
            onSuggestImprovements = viewModel::runSuggestWorkflowImprovements,
            onExplainErrors = viewModel::runExplainWorkflowErrors,
            onExplainNodesAndConnections = viewModel::runExplainNodesAndConnections,
            onGenerateSteps = viewModel::runGenerateWorkflowSteps,
            onCopied = {
                scope.launch { snackbarHostState.showSnackbar("AI response copied") }
            },
            onDismiss = { viewModel.toggleAiSheet(false) },
        )
    }

    if (state.showExportSheet) {
        val safeBaseName = (state.project?.title ?: "flowchart")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "flowchart" }

        ExportAndShareBottomSheet(
            projectTitle = state.project?.title ?: "Flowchart",
            defaultDarkExport = studioColors.isDark,
            onDismiss = { viewModel.toggleExportSheet(false) },
            onSavePng = { dark ->
                pendingDarkExport = dark
                viewModel.toggleExportSheet(false)
                savePngLauncher.launch("$safeBaseName.png")
            },
            onSharePng = { dark ->
                viewModel.toggleExportSheet(false)
                scope.launch {
                    val bytes = FlowExportManager.renderToPngBytes(state.graph, darkBackground = dark)
                    FlowExportManager.createShareIntent(context, "$safeBaseName.png", "image/png", bytes)
                        .onSuccess { context.startActivity(it) }
                        .onFailure { snackbarHostState.showSnackbar("Unable to share PNG") }
                }
            },
            onSavePdf = { dark ->
                pendingDarkExport = dark
                viewModel.toggleExportSheet(false)
                savePdfLauncher.launch("$safeBaseName.pdf")
            },
            onSharePdf = { dark ->
                viewModel.toggleExportSheet(false)
                scope.launch {
                    val title = state.project?.title ?: "Flowchart"
                    val bytes = FlowExportManager.renderToPdfBytes(state.graph, title, darkBackground = dark)
                    FlowExportManager.createShareIntent(context, "$safeBaseName.pdf", "application/pdf", bytes)
                        .onSuccess { context.startActivity(it) }
                        .onFailure { snackbarHostState.showSnackbar("Unable to share PDF") }
                }
            },
            onSaveHtml = { dark ->
                pendingDarkExport = dark
                viewModel.toggleExportSheet(false)
                saveHtmlLauncher.launch("$safeBaseName.html")
            },
            onShareHtml = { dark ->
                viewModel.toggleExportSheet(false)
                scope.launch {
                    val title = state.project?.title ?: "Flowchart"
                    val bytes = FlowExportManager.renderToHtmlBytes(state.graph, title, darkBackground = dark)
                    FlowExportManager.createShareIntent(context, "$safeBaseName.html", "text/html", bytes)
                        .onSuccess { context.startActivity(it) }
                        .onFailure { snackbarHostState.showSnackbar("Unable to share HTML") }
                }
            },
            onSaveJson = {
                viewModel.toggleExportSheet(false)
                saveJsonLauncher.launch("$safeBaseName.json")
            },
            onShareJson = {
                viewModel.toggleExportSheet(false)
                scope.launch {
                    val bytes = viewModel.generatePrettyJson().toByteArray(Charsets.UTF_8)
                    FlowExportManager.createShareIntent(context, "$safeBaseName.json", "application/json", bytes)
                        .onSuccess { context.startActivity(it) }
                        .onFailure { snackbarHostState.showSnackbar("Unable to share JSON") }
                }
            },
        )
    }
}
