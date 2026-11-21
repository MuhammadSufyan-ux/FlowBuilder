package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.example.domain.engine.FlowGeometry
import com.example.domain.model.AnchorSide
import com.example.domain.model.BorderStyle
import com.example.domain.model.EdgeStyle
import com.example.domain.model.EditorTool
import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowNode
import com.example.domain.model.GridStyle
import com.example.domain.model.NodeShape
import com.example.domain.model.ValidationSeverity
import com.example.ui.components.drawFlowNodeShape
import com.example.ui.theme.FlowStudioColors
import com.example.ui.theme.LocalFlowStudioColors
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.toComposeFontFamily
import com.example.viewmodel.AlignmentGuide
import com.example.viewmodel.EditorUiState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private enum class GestureMode {
    NONE,
    PAN_CANVAS,
    DRAG_NODE,
    RESIZE_NODE,
    DRAG_CONNECT,
    MARQUEE_SELECT,
    PINCH_ZOOM,
}

data class DragDropGhost(
    val shape: NodeShape,
    val screenPosition: Offset,
)

@Composable
fun FlowCanvasView(
    state: EditorUiState,
    dragDropGhost: DragDropGhost?,
    modifier: Modifier = Modifier,
    onCanvasSizeChanged: (Float, Float) -> Unit,
    onViewportChanged: (Float, Offset) -> Unit,
    onNodeTapped: (String) -> Unit,
    onNodeDoubleTapped: (String) -> Unit,
    onEdgeTapped: (String) -> Unit,
    onEdgeDoubleTapped: (String) -> Unit,
    onEmptyCanvasTapped: () -> Unit,
    onBeginDrag: () -> Unit,
    onMoveNodes: (Float, Float, String?) -> Unit,
    onEndDrag: () -> Unit,
    onResizeNode: (String, Float, Float) -> Unit,
    onEndResize: () -> Unit,
    onConnectNodes: (String, String, AnchorSide, AnchorSide) -> Unit,
    onMarqueeSelect: (Float, Float, Float, Float) -> Unit,
) {
    val studioColors = LocalFlowStudioColors.current
    val textMeasurer = rememberTextMeasurer()

    val latestState by rememberUpdatedState(state)
    val latestOnViewportChanged by rememberUpdatedState(onViewportChanged)
    val latestOnNodeTapped by rememberUpdatedState(onNodeTapped)
    val latestOnNodeDoubleTapped by rememberUpdatedState(onNodeDoubleTapped)
    val latestOnEdgeTapped by rememberUpdatedState(onEdgeTapped)
    val latestOnEdgeDoubleTapped by rememberUpdatedState(onEdgeDoubleTapped)
    val latestOnEmptyCanvasTapped by rememberUpdatedState(onEmptyCanvasTapped)
    val latestOnBeginDrag by rememberUpdatedState(onBeginDrag)
    val latestOnMoveNodes by rememberUpdatedState(onMoveNodes)
    val latestOnEndDrag by rememberUpdatedState(onEndDrag)
    val latestOnResizeNode by rememberUpdatedState(onResizeNode)
    val latestOnEndResize by rememberUpdatedState(onEndResize)
    val latestOnConnectNodes by rememberUpdatedState(onConnectNodes)
    val latestOnMarqueeSelect by rememberUpdatedState(onMarqueeSelect)

    // Live local overlay states for responsive 60fps interaction feedback
    var connectDragStartWorld by remember { mutableStateOf<Offset?>(null) }
    var connectDragCurrentWorld by remember { mutableStateOf<Offset?>(null) }
    var connectDragSourceNodeId by remember { mutableStateOf<String?>(null) }
    var connectDragSourceSide by remember { mutableStateOf(AnchorSide.AUTO) }
    var marqueeRectWorld by remember { mutableStateOf<Rect?>(null) }

    // Double-tap tracker
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapTargetId by remember { mutableStateOf<String?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(studioColors.canvasBackground)
            .testTag("flowchart_editor_canvas")
            .onSizeChanged { size ->
                onCanvasSizeChanged(size.width.toFloat(), size.height.toFloat())
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    val s0 = latestState
                    val worldDown = (downPos - s0.panOffset) / s0.zoom

                    var mode = GestureMode.NONE
                    var activeNodeId: String? = null
                    var totalDragPx = 0f
                    var lastSinglePos = downPos
                    var marqueeStartWorld = worldDown

                    // 1. Check handles on primary selected node (resize knob & 4 port handles)
                    val primarySelected = s0.primarySelectedNode
                    if (primarySelected != null && s0.activeTool != EditorTool.PAN) {
                        val handleHitRadius = 26f / s0.zoom
                        val resizeHandlePos = Offset(primarySelected.right, primarySelected.bottom)
                        if (hypot(worldDown.x - resizeHandlePos.x, worldDown.y - resizeHandlePos.y) <= handleHitRadius) {
                            mode = GestureMode.RESIZE_NODE
                            activeNodeId = primarySelected.id
                            latestOnBeginDrag()
                        } else {
                            val ports = listOf(
                                AnchorSide.TOP to FlowGeometry.getAnchorPoint(primarySelected, AnchorSide.TOP),
                                AnchorSide.RIGHT to FlowGeometry.getAnchorPoint(primarySelected, AnchorSide.RIGHT),
                                AnchorSide.BOTTOM to FlowGeometry.getAnchorPoint(primarySelected, AnchorSide.BOTTOM),
                                AnchorSide.LEFT to FlowGeometry.getAnchorPoint(primarySelected, AnchorSide.LEFT),
                            )
                            val hitPort = ports.firstOrNull { (_, pos) ->
                                hypot(worldDown.x - pos.x, worldDown.y - pos.y) <= handleHitRadius
                            }
                            if (hitPort != null) {
                                mode = GestureMode.DRAG_CONNECT
                                connectDragSourceNodeId = primarySelected.id
                                connectDragSourceSide = hitPort.first
                                connectDragStartWorld = hitPort.second
                                connectDragCurrentWorld = worldDown
                            }
                        }
                    }

                    // 2. Otherwise check hit on nodes (top-most first)
                    if (mode == GestureMode.NONE && s0.activeTool != EditorTool.PAN) {
                        val hitNode = s0.graph.nodes.lastOrNull {
                            FlowGeometry.hitTestNode(it, worldDown, padding = 10f / s0.zoom)
                        }
                        if (hitNode != null) {
                            activeNodeId = hitNode.id
                            if (s0.activeTool == EditorTool.CONNECT) {
                                // Allow drag-to-connect directly from any node in Connect mode!
                                mode = GestureMode.DRAG_CONNECT
                                connectDragSourceNodeId = hitNode.id
                                connectDragSourceSide = AnchorSide.AUTO
                                connectDragStartWorld = Offset(hitNode.centerX, hitNode.centerY)
                                connectDragCurrentWorld = worldDown
                            } else {
                                mode = GestureMode.DRAG_NODE
                            }
                        }
                    }

                    // 3. Otherwise pan or marquee select
                    if (mode == GestureMode.NONE) {
                        mode = if (s0.activeTool == EditorTool.MULTI_SELECT) {
                            marqueeStartWorld = worldDown
                            GestureMode.MARQUEE_SELECT
                        } else {
                            GestureMode.PAN_CANVAS
                        }
                    }

                    var dragStarted = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        if (pressedChanges.isEmpty()) break

                        // Multi-touch pinch-to-zoom & two-finger pan always takes priority
                        if (pressedChanges.size >= 2) {
                            if (mode == GestureMode.DRAG_NODE && dragStarted) {
                                latestOnEndDrag()
                                dragStarted = false
                            }
                            if (mode == GestureMode.RESIZE_NODE) {
                                latestOnEndResize()
                            }
                            connectDragStartWorld = null
                            connectDragCurrentWorld = null
                            marqueeRectWorld = null
                            mode = GestureMode.PINCH_ZOOM

                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = false)
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                val cur = latestState
                                val newZoom = (cur.zoom * zoomChange).coerceIn(0.3f, 2.5f)
                                val worldCentroid = (centroid - cur.panOffset) / cur.zoom
                                val newPan = centroid - worldCentroid * newZoom + panChange
                                latestOnViewportChanged(newZoom, newPan)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                            continue
                        }

                        if (mode == GestureMode.PINCH_ZOOM) {
                            // Released back to 1 finger after pinch
                            lastSinglePos = pressedChanges.first().position
                            continue
                        }

                        val change = pressedChanges.first()
                        val deltaScreen = change.position - lastSinglePos
                        lastSinglePos = change.position
                        totalDragPx += hypot(deltaScreen.x, deltaScreen.y)

                        if (deltaScreen == Offset.Zero) continue
                        val cur = latestState
                        val worldPos = (change.position - cur.panOffset) / cur.zoom

                        when (mode) {
                            GestureMode.PAN_CANVAS -> {
                                if (totalDragPx > 6f) {
                                    latestOnViewportChanged(cur.zoom, cur.panOffset + deltaScreen)
                                    change.consume()
                                }
                            }
                            GestureMode.DRAG_NODE -> {
                                if (totalDragPx > 8f) {
                                    if (!dragStarted) {
                                        dragStarted = true
                                        latestOnBeginDrag()
                                    }
                                    latestOnMoveNodes(
                                        deltaScreen.x / cur.zoom,
                                        deltaScreen.y / cur.zoom,
                                        activeNodeId,
                                    )
                                    change.consume()
                                }
                            }
                            GestureMode.RESIZE_NODE -> {
                                activeNodeId?.let { id ->
                                    latestOnResizeNode(
                                        id,
                                        deltaScreen.x / cur.zoom,
                                        deltaScreen.y / cur.zoom,
                                    )
                                    change.consume()
                                }
                            }
                            GestureMode.DRAG_CONNECT -> {
                                connectDragCurrentWorld = worldPos
                                change.consume()
                            }
                            GestureMode.MARQUEE_SELECT -> {
                                if (totalDragPx > 8f) {
                                    val left = min(marqueeStartWorld.x, worldPos.x)
                                    val top = min(marqueeStartWorld.y, worldPos.y)
                                    val right = max(marqueeStartWorld.x, worldPos.x)
                                    val bottom = max(marqueeStartWorld.y, worldPos.y)
                                    marqueeRectWorld = Rect(left, top, right, bottom)
                                    change.consume()
                                }
                            }
                            else -> Unit
                        }
                    }

                    // Gesture ended — finalize
                    val endState = latestState
                    when (mode) {
                        GestureMode.DRAG_NODE -> {
                            if (dragStarted) {
                                latestOnEndDrag()
                            } else if (totalDragPx <= 12f && activeNodeId != null) {
                                val now = System.currentTimeMillis()
                                if (lastTapTargetId == activeNodeId && now - lastTapTime < 340L) {
                                    latestOnNodeDoubleTapped(activeNodeId)
                                    lastTapTargetId = null
                                } else {
                                    latestOnNodeTapped(activeNodeId)
                                    lastTapTargetId = activeNodeId
                                    lastTapTime = now
                                }
                            }
                        }
                        GestureMode.RESIZE_NODE -> {
                            latestOnEndResize()
                        }
                        GestureMode.DRAG_CONNECT -> {
                            val srcId = connectDragSourceNodeId
                            val dropWorld = connectDragCurrentWorld
                            if (srcId != null && dropWorld != null && totalDragPx > 18f) {
                                val target = endState.graph.nodes.lastOrNull {
                                    it.id != srcId && FlowGeometry.hitTestNode(it, dropWorld, padding = 16f / endState.zoom)
                                }
                                if (target != null) {
                                    latestOnConnectNodes(srcId, target.id, connectDragSourceSide, AnchorSide.AUTO)
                                }
                            } else if (totalDragPx <= 12f && activeNodeId != null) {
                                latestOnNodeTapped(activeNodeId)
                            }
                            connectDragStartWorld = null
                            connectDragCurrentWorld = null
                            connectDragSourceNodeId = null
                        }
                        GestureMode.MARQUEE_SELECT -> {
                            val rect = marqueeRectWorld
                            if (rect != null && rect.width > 10f && rect.height > 10f) {
                                latestOnMarqueeSelect(rect.left, rect.top, rect.right, rect.bottom)
                            } else if (totalDragPx <= 12f) {
                                latestOnEmptyCanvasTapped()
                            }
                            marqueeRectWorld = null
                        }
                        GestureMode.PAN_CANVAS -> {
                            if (totalDragPx <= 12f) {
                                // Check if user tapped an edge!
                                val hitEdge = findHitEdge(endState, worldDown)
                                if (hitEdge != null) {
                                    val now = System.currentTimeMillis()
                                    if (lastTapTargetId == hitEdge.id && now - lastTapTime < 340L) {
                                        latestOnEdgeDoubleTapped(hitEdge.id)
                                        lastTapTargetId = null
                                    } else {
                                        latestOnEdgeTapped(hitEdge.id)
                                        lastTapTargetId = hitEdge.id
                                        lastTapTime = now
                                    }
                                } else {
                                    latestOnEmptyCanvasTapped()
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
    ) {
        // 1. Architectural Grid Background (drawn in screen space synced with zoom & pan)
        drawStudioGrid(
            gridStyle = state.graph.gridStyle,
            zoom = state.zoom,
            panOffset = state.panOffset,
            gridColor = studioColors.canvasGrid,
        )

        val nodeMap = state.graph.nodes.associateBy { it.id }
        val severityByNode = state.validationIssues
            .filter { it.nodeId != null }
            .groupBy { it.nodeId!! }
            .mapValues { (_, list) ->
                if (list.any { it.severity == ValidationSeverity.ERROR }) ValidationSeverity.ERROR
                else ValidationSeverity.WARNING
            }

        // 2. World-Space Diagram Rendering
        withTransform({
            translate(left = state.panOffset.x, top = state.panOffset.y)
            scale(scaleX = state.zoom, scaleY = state.zoom, pivot = Offset.Zero)
        }) {
            // Alignment guides (behind nodes)
            drawAlignmentGuides(
                guides = state.alignmentGuides,
                zoom = state.zoom,
                panOffset = state.panOffset,
                canvasSize = size,
                guideColor = studioColors.guideLine,
            )

            // Edges
            state.graph.edges.forEach { edge ->
                val from = nodeMap[edge.fromNodeId] ?: return@forEach
                val to = nodeMap[edge.toNodeId] ?: return@forEach
                val isSelected = edge.id == state.selectedEdgeId
                drawFlowEdge(
                    from = from,
                    to = to,
                    edge = edge,
                    isSelected = isSelected,
                    studioColors = studioColors,
                    textMeasurer = textMeasurer,
                )
            }

            // Live Drag-to-Connect Preview Arrow
            val dragStart = connectDragStartWorld
            val dragCurr = connectDragCurrentWorld
            if (dragStart != null && dragCurr != null) {
                drawLine(
                    color = studioColors.selectionAccent,
                    start = dragStart,
                    end = dragCurr,
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                )
                drawArrowHead(
                    from = dragStart,
                    to = dragCurr,
                    color = studioColors.selectionAccent,
                    strokeWidth = 3f,
                )
            }

            // Nodes
            state.graph.nodes.forEach { node ->
                val isSelected = node.id in state.selectedNodeIds
                val isConnectSource = node.id == state.connectSourceNodeId
                val issueSeverity = if (state.showValidationHighlights) severityByNode[node.id] else null
                drawFlowNode(
                    node = node,
                    isSelected = isSelected,
                    isPrimarySelected = isSelected && state.selectedNodeIds.size == 1,
                    isConnectSource = isConnectSource,
                    issueSeverity = issueSeverity,
                    studioColors = studioColors,
                    textMeasurer = textMeasurer,
                )
            }

            // Marquee Multi-Select Box
            marqueeRectWorld?.let { rect ->
                drawRect(
                    color = studioColors.selectionAccent.copy(alpha = 0.14f),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                )
                drawRect(
                    color = studioColors.selectionAccent,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    style = Stroke(
                        width = 2f / state.zoom,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f / state.zoom, 6f / state.zoom)),
                    ),
                )
            }

            // Drag-and-Drop Ghost Preview from Shape Palette
            if (dragDropGhost != null) {
                val worldCenter = (dragDropGhost.screenPosition - state.panOffset) / state.zoom
                val shape = dragDropGhost.shape
                val gx = FlowGeometry.snap(worldCenter.x - shape.defaultWidth / 2f, state.graph.snapToGrid)
                val gy = FlowGeometry.snap(worldCenter.y - shape.defaultHeight / 2f, state.graph.snapToGrid)
                val ghostNode = FlowNode(
                    shape = shape,
                    text = shape.defaultText,
                    x = gx,
                    y = gy,
                    width = shape.defaultWidth,
                    height = shape.defaultHeight,
                    fillColorHex = shape.defaultFillHex,
                )
                drawGhostNode(ghostNode, studioColors.selectionAccent)
            }
        }
    }
}

private fun findHitEdge(state: EditorUiState, worldPoint: Offset): FlowEdge? {
    val nodeMap = state.graph.nodes.associateBy { it.id }
    val hitTolerance = 22f / state.zoom
    var bestEdge: FlowEdge? = null
    var bestDist = hitTolerance
    state.graph.edges.forEach { edge ->
        val from = nodeMap[edge.fromNodeId] ?: return@forEach
        val to = nodeMap[edge.toNodeId] ?: return@forEach
        val geom = FlowGeometry.computeEdgeGeometry(from, to, edge)
        val dist = FlowGeometry.distanceToEdge(worldPoint, geom, edge.style)
        if (dist < bestDist) {
            bestDist = dist
            bestEdge = edge
        }
    }
    return bestEdge
}

private fun DrawScope.drawStudioGrid(
    gridStyle: GridStyle,
    zoom: Float,
    panOffset: Offset,
    gridColor: Color,
) {
    if (gridStyle == GridStyle.NONE) return
    val baseSpacing = 40f * zoom
    val spacing = when {
        baseSpacing < 16f -> baseSpacing * 2f
        baseSpacing > 96f -> baseSpacing / 2f
        else -> baseSpacing
    }.coerceAtLeast(14f)

    val startX = ((panOffset.x % spacing) + spacing) % spacing
    val startY = ((panOffset.y % spacing) + spacing) % spacing

    when (gridStyle) {
        GridStyle.PLUS -> {
            val arm = (3.5f * zoom).coerceIn(2f, 5f)
            val strokeW = (1.3f).coerceAtMost(2f)
            var x = startX
            while (x < size.width) {
                var y = startY
                while (y < size.height) {
                    drawLine(gridColor, Offset(x - arm, y), Offset(x + arm, y), strokeWidth = strokeW)
                    drawLine(gridColor, Offset(x, y - arm), Offset(x, y + arm), strokeWidth = strokeW)
                    y += spacing
                }
                x += spacing
            }
        }
        GridStyle.DOTS -> {
            val radius = (1.8f * zoom).coerceIn(1.2f, 3f)
            var x = startX
            while (x < size.width) {
                var y = startY
                while (y < size.height) {
                    drawCircle(gridColor, radius = radius, center = Offset(x, y))
                    y += spacing
                }
                x += spacing
            }
        }
        GridStyle.LINES -> {
            val lineColor = gridColor.copy(alpha = 0.55f)
            var x = startX
            while (x < size.width) {
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += spacing
            }
            var y = startY
            while (y < size.height) {
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += spacing
            }
        }
        GridStyle.NONE -> Unit
    }
}

private fun DrawScope.drawAlignmentGuides(
    guides: List<AlignmentGuide>,
    zoom: Float,
    panOffset: Offset,
    canvasSize: Size,
    guideColor: Color,
) {
    if (guides.isEmpty()) return
    val worldLeft = -panOffset.x / zoom
    val worldTop = -panOffset.y / zoom
    val worldRight = (canvasSize.width - panOffset.x) / zoom
    val worldBottom = (canvasSize.height - panOffset.y) / zoom
    val effect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

    guides.forEach { guide ->
        if (guide.isVertical) {
            drawLine(
                color = guideColor.copy(alpha = 0.85f),
                start = Offset(guide.position, worldTop),
                end = Offset(guide.position, worldBottom),
                strokeWidth = 1.6f / zoom,
                pathEffect = effect,
            )
        } else {
            drawLine(
                color = guideColor.copy(alpha = 0.85f),
                start = Offset(worldLeft, guide.position),
                end = Offset(worldRight, guide.position),
                strokeWidth = 1.6f / zoom,
                pathEffect = effect,
            )
        }
    }
}

private fun DrawScope.drawFlowEdge(
    from: FlowNode,
    to: FlowNode,
    edge: FlowEdge,
    isSelected: Boolean,
    studioColors: FlowStudioColors,
    textMeasurer: TextMeasurer,
) {
    val geom = FlowGeometry.computeEdgeGeometry(from, to, edge)
    val baseColor = if (studioColors.isDark && edge.strokeColorHex.equals("#1E293B", true)) {
        Color(0xFFCBD5E1)
    } else {
        parseHexColor(edge.strokeColorHex, Color(0xFF1E293B))
    }
    val strokeColor = if (isSelected) studioColors.selectionAccent else baseColor
    val strokeW = if (isSelected) 3.4f else 2.4f

    val path = Path()
    var lastPrev = geom.start

    if (edge.style == EdgeStyle.CURVED) {
        val (c1, c2) = FlowGeometry.computeCubicControls(
            geom.start, geom.end, geom.startSide, geom.endSide
        )
        path.moveTo(geom.start.x, geom.start.y)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, geom.end.x, geom.end.y)
        lastPrev = c2
    } else {
        val pts = geom.waypoints
        if (pts.size >= 2) {
            path.moveTo(pts[0].x, pts[0].y)
            // Subtle rounded corners on orthogonal segments
            for (i in 1 until pts.size - 1) {
                val pPrev = pts[i - 1]
                val pCurr = pts[i]
                val pNext = pts[i + 1]
                val d1 = hypot(pCurr.x - pPrev.x, pCurr.y - pPrev.y)
                val d2 = hypot(pNext.x - pCurr.x, pNext.y - pCurr.y)
                val cornerR = min(10f, min(d1, d2) / 2.2f)
                if (cornerR > 2f) {
                    val before = Offset(
                        pCurr.x - (pCurr.x - pPrev.x) / d1 * cornerR,
                        pCurr.y - (pCurr.y - pPrev.y) / d1 * cornerR,
                    )
                    val after = Offset(
                        pCurr.x + (pNext.x - pCurr.x) / d2 * cornerR,
                        pCurr.y + (pNext.y - pCurr.y) / d2 * cornerR,
                    )
                    path.lineTo(before.x, before.y)
                    path.quadraticTo(pCurr.x, pCurr.y, after.x, after.y)
                } else {
                    path.lineTo(pCurr.x, pCurr.y)
                }
            }
            path.lineTo(pts.last().x, pts.last().y)
            lastPrev = pts[pts.size - 2]
        }
    }

    // Soft highlight halo if selected
    if (isSelected) {
        drawPath(
            path = path,
            color = studioColors.selectionAccent.copy(alpha = 0.22f),
            style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }

    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(
            width = strokeW,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = if (edge.isDashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) else null,
        ),
    )

    drawArrowHead(from = lastPrev, to = geom.end, color = strokeColor, strokeWidth = strokeW)

    // Edge Label Pill
    if (edge.label.isNotBlank()) {
        val textResult = textMeasurer.measure(
            text = edge.label,
            style = TextStyle(
                fontFamily = PlusJakartaSans,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (studioColors.isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
            ),
            maxLines = 1,
        )
        val padH = 9f
        val padV = 4f
        val pillW = textResult.size.width + padH * 2
        val pillH = textResult.size.height + padV * 2
        val pillTopLeft = Offset(
            geom.labelPosition.x - pillW / 2f,
            geom.labelPosition.y - pillH / 2f,
        )
        val pillBg = if (studioColors.isDark) Color(0xFF1E293B) else Color.White
        val pillBorder = if (isSelected) {
            studioColors.selectionAccent
        } else if (studioColors.isDark) {
            Color(0xFF334155)
        } else {
            Color(0xFFCBD5E1)
        }
        drawRoundRect(
            color = pillBg,
            topLeft = pillTopLeft,
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
        )
        drawRoundRect(
            color = pillBorder,
            topLeft = pillTopLeft,
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
            style = Stroke(width = if (isSelected) 1.8f else 1.2f),
        )
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(pillTopLeft.x + padH, pillTopLeft.y + padV),
        )
    }
}

private fun DrawScope.drawArrowHead(
    from: Offset,
    to: Offset,
    color: Color,
    strokeWidth: Float,
) {
    val angle = atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())
    val len = 12f
    val spread = Math.toRadians(28.0)
    val p1 = Offset(
        to.x - len * cos(angle - spread).toFloat(),
        to.y - len * sin(angle - spread).toFloat(),
    )
    val p2 = Offset(
        to.x - len * cos(angle + spread).toFloat(),
        to.y - len * sin(angle + spread).toFloat(),
    )
    drawLine(color, to, p1, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color, to, p2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawFlowNode(
    node: FlowNode,
    isSelected: Boolean,
    isPrimarySelected: Boolean,
    isConnectSource: Boolean,
    issueSeverity: ValidationSeverity?,
    studioColors: FlowStudioColors,
    textMeasurer: TextMeasurer,
) {
    val isTextBox = node.shape == NodeShape.TEXT_BOX
    val fill = if (isTextBox) {
        if (node.fillColorHex.equals("#00FFFFFF", true)) Color.Transparent
        else parseHexColor(node.fillColorHex, Color.Transparent)
    } else {
        parseHexColor(node.fillColorHex, Color.White)
    }
    val borderColor = parseHexColor(node.borderColorHex, Color(0xFF161D29))
    val rawTextColor = parseHexColor(node.textColorHex, Color(0xFF0F172A))
    val textColor = if (isTextBox && studioColors.isDark && node.textColorHex.equals("#0F172A", true)) {
        Color(0xFFF1F5F9)
    } else {
        rawTextColor
    }

    val dashEffect = when (node.borderStyle) {
        BorderStyle.SOLID -> null
        BorderStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        BorderStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
    }

    val strokeStyle = Stroke(
        width = node.borderWidth,
        join = StrokeJoin.Round,
        cap = StrokeCap.Round,
        pathEffect = dashEffect,
    )

    // Subtle elevation shadow (skip for clean Text Box / Annotation)
    if (!isTextBox && node.shape != NodeShape.ANNOTATION) {
        val shadowOffset = Offset(0f, 4f)
        val shadowColor = if (studioColors.isDark) Color(0x55000000) else Color(0x160F172A)
        drawNodeGeometry(node, shadowColor, null, offset = shadowOffset)
    }

    // Validation issue halo
    if (issueSeverity != null) {
        val badgeColor = if (issueSeverity == ValidationSeverity.ERROR) {
            studioColors.errorColor
        } else {
            studioColors.warningColor
        }
        drawRoundRect(
            color = badgeColor.copy(alpha = 0.22f),
            topLeft = Offset(node.x - 6f, node.y - 6f),
            size = Size(node.width + 12f, node.height + 12f),
            cornerRadius = CornerRadius(18f, 18f),
            style = Stroke(width = 2.4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
        )
    }

    // Main Shape Fill & Stroke
    drawNodeGeometry(node, fill, if (isTextBox) null else borderColor, strokeStyle)

    // Centered Multi-line Text
    if (node.text.isNotBlank()) {
        val hPad = when (node.shape) {
            NodeShape.DIAMOND -> node.width * 0.22f
            NodeShape.PARALLELOGRAM, NodeShape.PREPARATION, NodeShape.DISPLAY -> 24f
            NodeShape.SUBROUTINE -> 20f
            NodeShape.CONNECTOR, NodeShape.OR_GATE, NodeShape.SUMMING_JUNCTION -> 6f
            NodeShape.TEXT_BOX -> 8f
            else -> 14f
        }
        val maxTextW = max(24, (node.width - hPad * 2).toInt())
        val maxTextH = max(20, (node.height - 10f).toInt())

        val textLayout = textMeasurer.measure(
            text = node.text,
            style = TextStyle(
                fontFamily = node.fontFamily.toComposeFontFamily(),
                fontSize = node.fontSizeSp.sp,
                fontWeight = if (node.isBold) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = (node.fontSizeSp * 1.24f).sp,
            ),
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = maxTextW, maxHeight = maxTextH),
        )

        val yBias = if (node.shape == NodeShape.DATABASE) 8f else 0f
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(
                node.centerX - textLayout.size.width / 2f,
                node.centerY + yBias - textLayout.size.height / 2f,
            ),
        )
    }

    // Validation Badge Dot (top-right corner)
    if (issueSeverity != null) {
        val badgeColor = if (issueSeverity == ValidationSeverity.ERROR) {
            studioColors.errorColor
        } else {
            studioColors.warningColor
        }
        val badgeCenter = Offset(node.right - 2f, node.y + 2f)
        drawCircle(color = badgeColor, radius = 10f, center = badgeCenter)
        drawCircle(color = Color.White, radius = 10f, center = badgeCenter, style = Stroke(width = 2f))
        // Exclamation mark inside badge
        drawLine(
            color = Color.White,
            start = Offset(badgeCenter.x, badgeCenter.y - 4.5f),
            end = Offset(badgeCenter.x, badgeCenter.y + 1f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round,
        )
        drawCircle(color = Color.White, radius = 1.3f, center = Offset(badgeCenter.x, badgeCenter.y + 4.5f))
    }

    // Selection Bounding Box + Resize Knob + 4 Connection Ports
    if (isSelected || isConnectSource) {
        val accent = if (isConnectSource) studioColors.successColor else studioColors.selectionAccent
        drawRoundRect(
            color = accent,
            topLeft = Offset(node.x - 5f, node.y - 5f),
            size = Size(node.width + 10f, node.height + 10f),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(width = 2f),
        )

        if (isPrimarySelected) {
            // 4 Connection Anchor Ports (drag from any port to connect!)
            val ports = listOf(
                FlowGeometry.getAnchorPoint(node, AnchorSide.TOP),
                FlowGeometry.getAnchorPoint(node, AnchorSide.RIGHT),
                FlowGeometry.getAnchorPoint(node, AnchorSide.BOTTOM),
                FlowGeometry.getAnchorPoint(node, AnchorSide.LEFT),
            )
            ports.forEach { port ->
                drawCircle(color = Color.White, radius = 6.5f, center = port)
                drawCircle(color = accent, radius = 6.5f, center = port, style = Stroke(width = 2.2f))
            }

            // Bottom-Right Resize Handle Knob
            val resizeCenter = Offset(node.right + 2f, node.bottom + 2f)
            drawRoundRect(
                color = accent,
                topLeft = Offset(resizeCenter.x - 7f, resizeCenter.y - 7f),
                size = Size(14f, 14f),
                cornerRadius = CornerRadius(4f, 4f),
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(resizeCenter.x - 7f, resizeCenter.y - 7f),
                size = Size(14f, 14f),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 1.8f),
            )
        }
    }
}

private fun DrawScope.drawGhostNode(node: FlowNode, accent: Color) {
    val fill = parseHexColor(node.fillColorHex, Color.White).copy(alpha = 0.55f)
    val stroke = Stroke(
        width = 2.4f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
        join = StrokeJoin.Round,
    )
    drawNodeGeometry(node, fill, accent, stroke)
}

private fun DrawScope.drawNodeGeometry(
    node: FlowNode,
    fillColor: Color,
    strokeColor: Color?,
    strokeStyle: Stroke = Stroke(width = 2.4f, join = StrokeJoin.Round, cap = StrokeCap.Round),
    offset: Offset = Offset.Zero,
) {
    drawFlowNodeShape(
        shape = node.shape,
        x = node.x + offset.x,
        y = node.y + offset.y,
        w = node.width,
        h = node.height,
        fillColor = fillColor,
        borderColor = strokeColor ?: Color.Transparent,
        stroke = strokeStyle,
        isIconPreview = false,
    )
}
