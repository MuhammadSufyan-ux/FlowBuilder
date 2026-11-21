package com.example.domain.engine

import com.example.domain.model.AnchorSide
import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowGraph
import com.example.domain.model.FlowNode
import com.example.domain.model.LayoutDirection
import com.example.domain.model.NodeShape
import kotlin.math.max

object FlowLayoutEngine {

    /**
     * Automatically arranges nodes into clean hierarchical layers, handling
     * branching decisions, merges, disconnected components, and back-edges (loops).
     */
    fun autoArrange(graph: FlowGraph, direction: LayoutDirection): FlowGraph {
        if (graph.nodes.isEmpty()) return graph

        val nodeMap = graph.nodes.associateBy { it.id }
        val outgoing = mutableMapOf<String, MutableList<String>>()
        val incoming = mutableMapOf<String, MutableList<String>>()
        graph.nodes.forEach {
            outgoing[it.id] = mutableListOf()
            incoming[it.id] = mutableListOf()
        }

        graph.edges.forEach { edge ->
            if (edge.fromNodeId in nodeMap && edge.toNodeId in nodeMap && edge.fromNodeId != edge.toNodeId) {
                outgoing[edge.fromNodeId]?.add(edge.toNodeId)
                incoming[edge.toNodeId]?.add(edge.fromNodeId)
            }
        }

        // Prioritize Start oval nodes, then zero in-degree nodes
        val roots = graph.nodes.filter { node ->
            val inDeg = incoming[node.id]?.size ?: 0
            inDeg == 0 || (node.shape == NodeShape.OVAL && node.text.contains("start", ignoreCase = true))
        }.sortedWith(
            compareByDescending<FlowNode> { it.shape == NodeShape.OVAL && it.text.contains("start", ignoreCase = true) }
                .thenBy { incoming[it.id]?.size ?: 0 }
                .thenBy { it.y }
                .thenBy { it.x }
        )

        val layerOf = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()

        fun assignFromSeed(seedId: String, startLayer: Int) {
            if (seedId in layerOf) return
            layerOf[seedId] = startLayer
            queue.add(seedId)
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                val currLayer = layerOf[curr] ?: 0
                val children = outgoing[curr].orEmpty()
                for (child in children) {
                    if (child !in layerOf) {
                        layerOf[child] = currLayer + 1
                        queue.add(child)
                    }
                }
            }
        }

        // Seed connected components starting from roots
        for (root in roots) {
            if (root.id !in layerOf) {
                assignFromSeed(root.id, 0)
            }
        }

        // Catch any pure cycle components that had no zero-in-degree root
        for (node in graph.nodes) {
            if (node.id !in layerOf) {
                assignFromSeed(node.id, 0)
            }
        }

        // Push explicit "End" / "Stop" ovals to the final layer if they have no outgoing edges
        val maxExistingLayer = layerOf.values.maxOrNull() ?: 0
        graph.nodes.forEach { node ->
            val isTerminalEnd = node.shape == NodeShape.OVAL &&
                outgoing[node.id].isNullOrEmpty() &&
                incoming[node.id]?.isNotEmpty() == true
            if (isTerminalEnd && (layerOf[node.id] ?: 0) < maxExistingLayer) {
                val maxParentLayer = incoming[node.id].orEmpty().maxOfOrNull { layerOf[it] ?: 0 } ?: 0
                layerOf[node.id] = max(maxParentLayer + 1, layerOf[node.id] ?: 0)
            }
        }

        // Group into ordered layers
        val maxLayer = layerOf.values.maxOrNull() ?: 0
        val layers = MutableList(maxLayer + 1) { mutableListOf<FlowNode>() }
        graph.nodes.forEach { node ->
            val layer = (layerOf[node.id] ?: 0).coerceIn(0, maxLayer)
            layers[layer].add(node)
        }

        // Simple barycenter ordering to reduce edge crossings
        val orderIndex = mutableMapOf<String, Float>()
        layers[0].sortBy { it.x }
        layers[0].forEachIndexed { idx, node -> orderIndex[node.id] = idx.toFloat() }

        for (l in 1..maxLayer) {
            layers[l].sortBy { node ->
                val parents = incoming[node.id].orEmpty().filter { (layerOf[it] ?: -1) < l }
                if (parents.isNotEmpty()) {
                    parents.map { orderIndex[it] ?: 0f }.average().toFloat()
                } else {
                    node.x / 1000f + 100f
                }
            }
            layers[l].forEachIndexed { idx, node -> orderIndex[node.id] = idx.toFloat() }
        }

        val hGap = when (direction) {
            LayoutDirection.TOP_DOWN -> 64f
            LayoutDirection.LEFT_RIGHT -> 88f
            LayoutDirection.COMPACT_TREE -> 36f
        }
        val vGap = when (direction) {
            LayoutDirection.TOP_DOWN -> 76f
            LayoutDirection.LEFT_RIGHT -> 52f
            LayoutDirection.COMPACT_TREE -> 48f
        }

        val updatedNodes = mutableMapOf<String, FlowNode>()
        val startX = 120f
        val startY = 100f

        if (direction == LayoutDirection.LEFT_RIGHT) {
            val layerWidths = layers.map { layer -> layer.maxOfOrNull { it.width } ?: 140f }
            val maxColumnHeight = layers.maxOfOrNull { layer ->
                if (layer.isEmpty()) 0f
                else layer.sumOf { it.height.toDouble() }.toFloat() + (layer.size - 1) * vGap
            } ?: 0f

            var cursorX = startX
            layers.forEachIndexed { lIdx, layer ->
                if (layer.isEmpty()) return@forEachIndexed
                val colWidth = layerWidths[lIdx]
                val totalHeight = layer.sumOf { it.height.toDouble() }.toFloat() + (layer.size - 1) * vGap
                var cursorY = startY + (maxColumnHeight - totalHeight) / 2f

                layer.forEach { node ->
                    val nx = FlowGeometry.snap(cursorX + (colWidth - node.width) / 2f, graph.snapToGrid)
                    val ny = FlowGeometry.snap(cursorY, graph.snapToGrid)
                    updatedNodes[node.id] = node.copy(x = nx, y = ny)
                    cursorY += node.height + vGap
                }
                cursorX += colWidth + hGap
            }
        } else {
            // TOP_DOWN or COMPACT_TREE
            val layerHeights = layers.map { layer -> layer.maxOfOrNull { it.height } ?: 72f }
            val maxRowWidth = layers.maxOfOrNull { layer ->
                if (layer.isEmpty()) 0f
                else layer.sumOf { it.width.toDouble() }.toFloat() + (layer.size - 1) * hGap
            } ?: 0f

            var cursorY = startY
            layers.forEachIndexed { lIdx, layer ->
                if (layer.isEmpty()) return@forEachIndexed
                val rowHeight = layerHeights[lIdx]
                val totalWidth = layer.sumOf { it.width.toDouble() }.toFloat() + (layer.size - 1) * hGap
                var cursorX = startX + (maxRowWidth - totalWidth) / 2f

                layer.forEach { node ->
                    val nx = FlowGeometry.snap(cursorX, graph.snapToGrid)
                    val ny = FlowGeometry.snap(cursorY + (rowHeight - node.height) / 2f, graph.snapToGrid)
                    updatedNodes[node.id] = node.copy(x = nx, y = ny)
                    cursorX += node.width + hGap
                }
                cursorY += rowHeight + vGap
            }
        }

        // Reset edge sides to AUTO so arrows re-attach cleanly in the new orientation
        val cleanedEdges: List<FlowEdge> = graph.edges.map { edge ->
            edge.copy(fromSide = AnchorSide.AUTO, toSide = AnchorSide.AUTO)
        }

        return graph.copy(
            nodes = graph.nodes.map { updatedNodes[it.id] ?: it },
            edges = cleanedEdges,
        )
    }
}
