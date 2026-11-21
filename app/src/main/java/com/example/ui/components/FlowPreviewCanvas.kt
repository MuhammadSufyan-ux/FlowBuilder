package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.domain.engine.FlowGeometry
import com.example.domain.model.EdgeStyle
import com.example.domain.model.FlowGraph
import com.example.domain.model.NodeShape
import com.example.ui.theme.LocalFlowStudioColors
import com.example.ui.theme.parseHexColor
import kotlin.math.min

/**
 * Renders a crisp, auto-fitted miniature diagram preview of any FlowGraph for project and template cards.
 */
@Composable
fun FlowPreviewCanvas(
    graph: FlowGraph,
    modifier: Modifier = Modifier,
) {
    val studioColors = LocalFlowStudioColors.current

    Canvas(
        modifier = modifier.background(studioColors.canvasBackground)
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val step = 18f
        val gridColor = studioColors.canvasGrid.copy(alpha = 0.55f)
        var gx = step
        while (gx < w) {
            var gy = step
            while (gy < h) {
                drawLine(gridColor, Offset(gx - 2f, gy), Offset(gx + 2f, gy), strokeWidth = 1f)
                drawLine(gridColor, Offset(gx, gy - 2f), Offset(gx, gy + 2f), strokeWidth = 1f)
                gy += step
            }
            gx += step
        }

        if (graph.nodes.isEmpty()) return@Canvas

        val minX = graph.nodes.minOf { it.x }
        val minY = graph.nodes.minOf { it.y }
        val maxX = graph.nodes.maxOf { it.right }
        val maxY = graph.nodes.maxOf { it.bottom }
        val contentW = (maxX - minX).coerceAtLeast(100f)
        val contentH = (maxY - minY).coerceAtLeast(80f)

        val pad = 16f
        val scale = min((w - pad * 2) / contentW, (h - pad * 2) / contentH).coerceAtMost(0.55f)
        val offsetX = (w - contentW * scale) / 2f - minX * scale
        val offsetY = (h - contentH * scale) / 2f - minY * scale

        val nodeMap = graph.nodes.associateBy { it.id }
        val defaultEdgeColor = if (studioColors.isDark) Color(0xFF94A3B8) else Color(0xFF334155)

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            graph.edges.forEach { edge ->
                val from = nodeMap[edge.fromNodeId] ?: return@forEach
                val to = nodeMap[edge.toNodeId] ?: return@forEach
                val geom = FlowGeometry.computeEdgeGeometry(from, to, edge)
                val path = Path()
                if (edge.style == EdgeStyle.CURVED) {
                    val (c1, c2) = FlowGeometry.computeCubicControls(
                        geom.start, geom.end, geom.startSide, geom.endSide
                    )
                    path.moveTo(geom.start.x, geom.start.y)
                    path.cubicTo(c1.x, c1.y, c2.x, c2.y, geom.end.x, geom.end.y)
                } else {
                    val pts = geom.waypoints
                    if (pts.size >= 2) {
                        path.moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) {
                            path.lineTo(pts[i].x, pts[i].y)
                        }
                    }
                }
                drawPath(
                    path = path,
                    color = defaultEdgeColor,
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }

            graph.nodes.forEach { node ->
                val fill = if (node.shape == NodeShape.TEXT_BOX) {
                    Color.Transparent
                } else {
                    parseHexColor(node.fillColorHex, Color.White)
                }
                val border = parseHexColor(node.borderColorHex, Color(0xFF161D29))
                val stroke = Stroke(width = 4.2f, join = StrokeJoin.Round, cap = StrokeCap.Round)

                drawFlowNodeShape(
                    shape = node.shape,
                    x = node.x,
                    y = node.y,
                    w = node.width,
                    h = node.height,
                    fillColor = fill,
                    borderColor = border,
                    stroke = stroke,
                    isIconPreview = false,
                )
            }
        }
    }
}
