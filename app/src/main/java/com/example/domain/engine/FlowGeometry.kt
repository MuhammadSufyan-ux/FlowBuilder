package com.example.domain.engine

import androidx.compose.ui.geometry.Offset
import com.example.domain.model.AnchorSide
import com.example.domain.model.EdgeStyle
import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowNode
import com.example.domain.model.NodeShape
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class EdgeGeometry(
    val start: Offset,
    val end: Offset,
    val startSide: AnchorSide,
    val endSide: AnchorSide,
    val waypoints: List<Offset>,
    val labelPosition: Offset,
)

object FlowGeometry {

    const val GRID_SIZE = 20f

    fun snap(value: Float, enabled: Boolean): Float {
        if (!enabled) return value
        return kotlin.math.round(value / GRID_SIZE) * GRID_SIZE
    }

    fun getAnchorPoint(node: FlowNode, side: AnchorSide): Offset {
        return when (side) {
            AnchorSide.TOP -> Offset(node.centerX, node.y)
            AnchorSide.BOTTOM -> Offset(node.centerX, node.bottom)
            AnchorSide.LEFT -> Offset(node.x, node.centerY)
            AnchorSide.RIGHT -> Offset(node.right, node.centerY)
            AnchorSide.AUTO -> Offset(node.centerX, node.centerY)
        }
    }

    fun resolveAnchorSides(from: FlowNode, to: FlowNode, edge: FlowEdge): Pair<AnchorSide, AnchorSide> {
        val dx = to.centerX - from.centerX
        val dy = to.centerY - from.centerY

        val resolvedFrom = if (edge.fromSide != AnchorSide.AUTO) {
            edge.fromSide
        } else {
            if (abs(dy) >= abs(dx) * 0.72f) {
                if (dy >= 0) AnchorSide.BOTTOM else AnchorSide.TOP
            } else {
                if (dx >= 0) AnchorSide.RIGHT else AnchorSide.LEFT
            }
        }

        val resolvedTo = if (edge.toSide != AnchorSide.AUTO) {
            edge.toSide
        } else {
            if (abs(dy) >= abs(dx) * 0.72f) {
                if (dy >= 0) AnchorSide.TOP else AnchorSide.BOTTOM
            } else {
                if (dx >= 0) AnchorSide.LEFT else AnchorSide.RIGHT
            }
        }

        return resolvedFrom to resolvedTo
    }

    fun computeEdgeGeometry(from: FlowNode, to: FlowNode, edge: FlowEdge): EdgeGeometry {
        val (fromSide, toSide) = resolveAnchorSides(from, to, edge)
        val start = getAnchorPoint(from, fromSide)
        val end = getAnchorPoint(to, toSide)

        if (from.id == to.id) {
            // Self-loop cleanly outside right side
            val p1 = Offset(from.right, from.centerY)
            val p2 = Offset(from.right + 44f, from.centerY)
            val p3 = Offset(from.right + 44f, from.y - 32f)
            val p4 = Offset(from.centerX, from.y - 32f)
            val p5 = Offset(from.centerX, from.y)
            return EdgeGeometry(
                start = p1,
                end = p5,
                startSide = AnchorSide.RIGHT,
                endSide = AnchorSide.TOP,
                waypoints = listOf(p1, p2, p3, p4, p5),
                labelPosition = Offset(from.right + 44f, from.y - 10f),
            )
        }

        val waypoints = when (edge.style) {
            EdgeStyle.STRAIGHT -> listOf(start, end)
            EdgeStyle.CURVED -> listOf(start, end)
            EdgeStyle.ORTHOGONAL -> computeOrthogonalWaypoints(start, end, fromSide, toSide)
        }

        val labelPosition = computeLabelPosition(waypoints, edge.style, start, end, fromSide, toSide)
        return EdgeGeometry(
            start = start,
            end = end,
            startSide = fromSide,
            endSide = toSide,
            waypoints = waypoints,
            labelPosition = labelPosition,
        )
    }

    private fun computeOrthogonalWaypoints(
        start: Offset,
        end: Offset,
        fromSide: AnchorSide,
        toSide: AnchorSide,
    ): List<Offset> {
        val stub = 24f
        val startOut = offsetBySide(start, fromSide, stub)
        val endOut = offsetBySide(end, toSide, stub)

        val isStartVertical = fromSide == AnchorSide.TOP || fromSide == AnchorSide.BOTTOM
        val isEndVertical = toSide == AnchorSide.TOP || toSide == AnchorSide.BOTTOM

        return when {
            isStartVertical && isEndVertical -> {
                if (abs(start.x - end.x) < 4f) {
                    listOf(start, end)
                } else {
                    val midY = if (
                        (fromSide == AnchorSide.BOTTOM && end.y > start.y + 18f) ||
                        (fromSide == AnchorSide.TOP && end.y < start.y - 18f)
                    ) {
                        (start.y + end.y) / 2f
                    } else {
                        startOut.y
                    }
                    listOf(
                        start,
                        Offset(start.x, midY),
                        Offset(end.x, midY),
                        end,
                    )
                }
            }
            !isStartVertical && !isEndVertical -> {
                if (abs(start.y - end.y) < 4f) {
                    listOf(start, end)
                } else {
                    val midX = if (
                        (fromSide == AnchorSide.RIGHT && end.x > start.x + 18f) ||
                        (fromSide == AnchorSide.LEFT && end.x < start.x - 18f)
                    ) {
                        (start.x + end.x) / 2f
                    } else {
                        // Loop-back route smoothly outside
                        val outX = if (fromSide == AnchorSide.RIGHT) {
                            max(start.x, end.x) + 46f
                        } else {
                            min(start.x, end.x) - 46f
                        }
                        outX
                    }
                    listOf(
                        start,
                        Offset(midX, start.y),
                        Offset(midX, end.y),
                        end,
                    )
                }
            }
            isStartVertical && !isEndVertical -> {
                val corner = Offset(start.x, end.y)
                if ((fromSide == AnchorSide.BOTTOM && end.y >= start.y + 8f) ||
                    (fromSide == AnchorSide.TOP && end.y <= start.y - 8f)
                ) {
                    listOf(start, corner, end)
                } else {
                    listOf(start, startOut, Offset(endOut.x, startOut.y), endOut, end)
                }
            }
            else -> {
                val corner = Offset(end.x, start.y)
                if ((fromSide == AnchorSide.RIGHT && end.x >= start.x + 8f) ||
                    (fromSide == AnchorSide.LEFT && end.x <= start.x - 8f)
                ) {
                    listOf(start, corner, end)
                } else {
                    listOf(start, startOut, Offset(startOut.x, endOut.y), endOut, end)
                }
            }
        }
    }

    fun offsetBySide(point: Offset, side: AnchorSide, distance: Float): Offset = when (side) {
        AnchorSide.TOP -> Offset(point.x, point.y - distance)
        AnchorSide.BOTTOM -> Offset(point.x, point.y + distance)
        AnchorSide.LEFT -> Offset(point.x - distance, point.y)
        AnchorSide.RIGHT -> Offset(point.x + distance, point.y)
        AnchorSide.AUTO -> point
    }

    fun computeCubicControls(
        start: Offset,
        end: Offset,
        fromSide: AnchorSide,
        toSide: AnchorSide,
    ): Pair<Offset, Offset> {
        val dist = hypot(end.x - start.x, end.y - start.y)
        val handleLen = (dist * 0.4f).coerceIn(36f, 120f)
        val c1 = offsetBySide(start, fromSide, handleLen)
        val c2 = offsetBySide(end, toSide, handleLen)
        return c1 to c2
    }

    private fun computeLabelPosition(
        waypoints: List<Offset>,
        style: EdgeStyle,
        start: Offset,
        end: Offset,
        fromSide: AnchorSide,
        toSide: AnchorSide,
    ): Offset {
        if (style == EdgeStyle.CURVED) {
            val (c1, c2) = computeCubicControls(start, end, fromSide, toSide)
            val t = 0.5f
            val inv = 1f - t
            val x = inv * inv * inv * start.x + 3 * inv * inv * t * c1.x + 3 * inv * t * t * c2.x + t * t * t * end.x
            val y = inv * inv * inv * start.y + 3 * inv * inv * t * c1.y + 3 * inv * t * t * c2.y + t * t * t * end.y
            return Offset(x, y)
        }
        if (waypoints.size < 2) return Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)

        // Find midpoint along polyline length
        var totalLength = 0f
        val lengths = FloatArray(waypoints.size - 1)
        for (i in 0 until waypoints.size - 1) {
            val seg = hypot(waypoints[i + 1].x - waypoints[i].x, waypoints[i + 1].y - waypoints[i].y)
            lengths[i] = seg
            totalLength += seg
        }
        if (totalLength < 1f) return Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)

        val target = totalLength * 0.48f
        var acc = 0f
        for (i in lengths.indices) {
            if (acc + lengths[i] >= target && lengths[i] > 0.01f) {
                val frac = (target - acc) / lengths[i]
                return Offset(
                    waypoints[i].x + (waypoints[i + 1].x - waypoints[i].x) * frac,
                    waypoints[i].y + (waypoints[i + 1].y - waypoints[i].y) * frac,
                )
            }
            acc += lengths[i]
        }
        return Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
    }

    fun hitTestNode(node: FlowNode, point: Offset, padding: Float = 8f): Boolean {
        val left = node.x - padding
        val top = node.y - padding
        val right = node.right + padding
        val bottom = node.bottom + padding
        if (point.x < left || point.x > right || point.y < top || point.y > bottom) return false

        return when (node.shape) {
            NodeShape.DIAMOND -> {
                val rx = node.width / 2f + padding
                val ry = node.height / 2f + padding
                val nx = abs(point.x - node.centerX) / rx
                val ny = abs(point.y - node.centerY) / ry
                nx + ny <= 1.08f
            }
            NodeShape.CONNECTOR, NodeShape.OR_GATE, NodeShape.SUMMING_JUNCTION -> {
                val r = min(node.width, node.height) / 2f + padding
                hypot(point.x - node.centerX, point.y - node.centerY) <= r
            }
            else -> true
        }
    }

    fun distanceToEdge(point: Offset, geometry: EdgeGeometry, style: EdgeStyle): Float {
        if (style == EdgeStyle.CURVED) {
            val (c1, c2) = computeCubicControls(
                geometry.start,
                geometry.end,
                geometry.startSide,
                geometry.endSide,
            )
            var minDist = Float.MAX_VALUE
            var prev = geometry.start
            val steps = 16
            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val inv = 1f - t
                val x = inv * inv * inv * geometry.start.x + 3 * inv * inv * t * c1.x +
                    3 * inv * t * t * c2.x + t * t * t * geometry.end.x
                val y = inv * inv * inv * geometry.start.y + 3 * inv * inv * t * c1.y +
                    3 * inv * t * t * c2.y + t * t * t * geometry.end.y
                val curr = Offset(x, y)
                minDist = min(minDist, distanceToSegment(point, prev, curr))
                prev = curr
            }
            return minDist
        }

        val pts = geometry.waypoints
        if (pts.size < 2) return Float.MAX_VALUE
        var minDist = Float.MAX_VALUE
        for (i in 0 until pts.size - 1) {
            minDist = min(minDist, distanceToSegment(point, pts[i], pts[i + 1]))
        }
        return minDist
    }

    private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 0.0001f) return hypot(p.x - a.x, p.y - a.y)
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = a.x + t * dx
        val projY = a.y + t * dy
        return hypot(p.x - projX, p.y - projY)
    }
}
