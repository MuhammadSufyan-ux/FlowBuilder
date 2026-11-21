package com.example.domain.engine

import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowGraph
import com.example.domain.model.FlowNode
import com.example.domain.model.NodeShape

object FlowDslGenerator {

    fun generateMermaidDsl(graph: FlowGraph): String {
        if (graph.nodes.isEmpty()) {
            return "flowchart TD\n    %% Empty canvas — add shapes to generate flow code"
        }

        val sb = StringBuilder()
        sb.appendLine("flowchart TD")

        val aliasMap = mutableMapOf<String, String>()
        graph.nodes.forEachIndexed { index, node ->
            aliasMap[node.id] = indexToAlias(index)
        }

        val declared = mutableSetOf<String>()
        fun formatNode(node: FlowNode): String {
            val alias = aliasMap[node.id] ?: "N"
            val safeText = node.text.ifBlank { node.shape.displayName }
                .replace("\"", "'")
                .replace("\n", " ")
            return if (declared.add(node.id)) {
                when (node.shape) {
                    NodeShape.OVAL -> "$alias([\"$safeText\"])"
                    NodeShape.RECTANGLE, NodeShape.ALTERNATE_PROCESS, NodeShape.INTERNAL_STORAGE,
                    NodeShape.DOCUMENT, NodeShape.MULTIPLE_DOCUMENTS, NodeShape.DELAY,
                    NodeShape.LOOP_LIMIT, NodeShape.MANUAL_OPERATION, NodeShape.MANUAL_INPUT,
                    NodeShape.DISPLAY, NodeShape.STORED_DATA, NodeShape.TEXT_BOX, NodeShape.ANNOTATION ->
                        "$alias[\"$safeText\"]"
                    NodeShape.SUBROUTINE -> "$alias[[\"$safeText\"]]"
                    NodeShape.PREPARATION -> "$alias{{\"" + safeText + "\"}}"
                    NodeShape.DATABASE -> "$alias[(\"$safeText\")]"
                    NodeShape.DIAMOND -> "$alias{\"$safeText\"}"
                    NodeShape.PARALLELOGRAM -> "$alias[/\"$safeText\"/]"
                    NodeShape.CONNECTOR, NodeShape.OR_GATE, NodeShape.SUMMING_JUNCTION -> "$alias((\"$safeText\"))"
                    NodeShape.OFF_PAGE_CONNECTOR, NodeShape.MERGE -> "$alias>\"$safeText\"]"
                }
            } else {
                alias
            }
        }

        val nodeById = graph.nodes.associateBy { it.id }

        if (graph.edges.isNotEmpty()) {
            graph.edges.forEach { edge ->
                val from = nodeById[edge.fromNodeId] ?: return@forEach
                val to = nodeById[edge.toNodeId] ?: return@forEach
                val arrow = if (edge.isDashed) "-.->" else "-->"
                val fromStr = formatNode(from)
                val toStr = formatNode(to)
                if (edge.label.isNotBlank()) {
                    sb.appendLine("    $fromStr $arrow|${edge.label}| $toStr")
                } else {
                    sb.appendLine("    $fromStr $arrow $toStr")
                }
            }
        }

        graph.nodes.forEach { node ->
            if (node.id !in declared) {
                sb.appendLine("    ${formatNode(node)}")
            }
        }

        graph.nodes.forEach { node ->
            val alias = aliasMap[node.id] ?: return@forEach
            if (node.shape != NodeShape.TEXT_BOX) {
                sb.appendLine("    style $alias fill:${node.fillColorHex},stroke:${node.borderColorHex}")
            }
        }

        return sb.toString().trimEnd()
    }

    /**
     * Converts any FlowGraph into structured, readable algorithm Pseudocode.
     */
    fun generatePseudocode(graph: FlowGraph, title: String = "Algorithm"): String {
        val logicalNodes = graph.nodes.filter {
            it.shape != NodeShape.TEXT_BOX && it.shape != NodeShape.ANNOTATION
        }
        if (logicalNodes.isEmpty()) {
            return "ALGORITHM ${title.replace(" ", "_")}\nBEGIN\n    // Add shapes to generate pseudocode\nEND"
        }

        val nodeMap = logicalNodes.associateBy { it.id }
        val outgoing = mutableMapOf<String, MutableList<FlowEdge>>()
        val incomingCount = mutableMapOf<String, Int>()
        logicalNodes.forEach {
            outgoing[it.id] = mutableListOf()
            incomingCount[it.id] = 0
        }
        graph.edges.forEach { edge ->
            if (edge.fromNodeId in nodeMap && edge.toNodeId in nodeMap) {
                outgoing[edge.fromNodeId]?.add(edge)
                incomingCount[edge.toNodeId] = (incomingCount[edge.toNodeId] ?: 0) + 1
            }
        }

        val startNode = logicalNodes.firstOrNull {
            it.shape == NodeShape.OVAL &&
                (it.text.contains("start", true) || it.text.contains("begin", true) || (incomingCount[it.id] ?: 0) == 0)
        } ?: logicalNodes.minByOrNull { (incomingCount[it.id] ?: 0) * 1000 + it.y.toInt() }
            ?: logicalNodes.first()

        val sb = StringBuilder()
        val algoName = title.trim().ifBlank { "Flowchart" }.replace(Regex("[^a-zA-Z0-9]+"), "_")
        sb.appendLine("ALGORITHM $algoName")
        sb.appendLine("BEGIN")

        val visited = mutableSetOf<String>()
        val activeStack = mutableSetOf<String>()

        fun indent(depth: Int) = "    ".repeat(depth)

        fun emitNode(nodeId: String, depth: Int) {
            val node = nodeMap[nodeId] ?: return
            if (nodeId in activeStack) {
                sb.appendLine("${indent(depth)}REPEAT FROM \"${node.text.replace("\n", " ")}\"")
                return
            }
            if (!visited.add(nodeId)) {
                return
            }
            activeStack.add(nodeId)

            val label = node.text.replace("\n", "; ").trim().ifBlank { node.shape.displayName }
            val outs = outgoing[nodeId].orEmpty()

            when (node.shape) {
                NodeShape.OVAL -> {
                    if (outs.isEmpty()) {
                        sb.appendLine("${indent(depth)}STOP // $label")
                    } else {
                        sb.appendLine("${indent(depth)}// $label")
                        outs.forEach { emitNode(it.toNodeId, depth) }
                    }
                }
                NodeShape.DIAMOND -> {
                    val loopEdge = outs.firstOrNull {
                        it.label.contains("loop", true) || it.label.contains("retry", true) || it.toNodeId in activeStack
                    }
                    if (loopEdge != null && outs.size == 2) {
                        val exitEdge = outs.first { it.id != loopEdge.id }
                        sb.appendLine("${indent(depth)}WHILE ($label) DO")
                        emitNode(loopEdge.toNodeId, depth + 1)
                        sb.appendLine("${indent(depth)}END WHILE")
                        emitNode(exitEdge.toNodeId, depth)
                    } else if (outs.isNotEmpty()) {
                        val yesBranch = outs.firstOrNull {
                            it.label.equals("yes", true) || it.label.equals("true", true) || it.label.equals("pass", true)
                        } ?: outs.first()
                        val otherBranches = outs.filter { it.id != yesBranch.id }

                        sb.appendLine("${indent(depth)}IF ($label) THEN")
                        emitNode(yesBranch.toNodeId, depth + 1)
                        otherBranches.forEach { branch ->
                            val branchTag = branch.label.ifBlank { "ELSE" }
                            if (branchTag.equals("no", true) || branchTag.equals("false", true) || branchTag.equals("else", true)) {
                                sb.appendLine("${indent(depth)}ELSE")
                            } else {
                                sb.appendLine("${indent(depth)}ELSE // $branchTag")
                            }
                            emitNode(branch.toNodeId, depth + 1)
                        }
                        sb.appendLine("${indent(depth)}END IF")
                    } else {
                        sb.appendLine("${indent(depth)}IF ($label) THEN")
                        sb.appendLine("${indent(depth + 1)}// Define branch")
                        sb.appendLine("${indent(depth)}END IF")
                    }
                }
                NodeShape.PARALLELOGRAM, NodeShape.MANUAL_INPUT -> {
                    val verb = if (label.contains("print", true) || label.contains("output", true) || label.contains("show", true)) {
                        "OUTPUT"
                    } else {
                        "INPUT"
                    }
                    sb.appendLine("${indent(depth)}$verb $label")
                    outs.forEach { emitNode(it.toNodeId, depth) }
                }
                NodeShape.DISPLAY -> {
                    sb.appendLine("${indent(depth)}DISPLAY $label")
                    outs.forEach { emitNode(it.toNodeId, depth) }
                }
                NodeShape.SUBROUTINE -> {
                    sb.appendLine("${indent(depth)}CALL $label()")
                    outs.forEach { emitNode(it.toNodeId, depth) }
                }
                NodeShape.PREPARATION -> {
                    sb.appendLine("${indent(depth)}INIT $label")
                    outs.forEach { emitNode(it.toNodeId, depth) }
                }
                NodeShape.DELAY -> {
                    sb.appendLine("${indent(depth)}WAIT ($label)")
                    outs.forEach { emitNode(it.toNodeId, depth) }
                }
                NodeShape.DATABASE, NodeShape.STORED_DATA, NodeShape.INTERNAL_STORAGE -> {
                    sb.appendLine("${indent(depth)}STORE / QUERY $label")
                    outs.forEach { emitNode(it.toNodeId, depth) }
                }
                NodeShape.LOOP_LIMIT -> {
                    sb.appendLine("${indent(depth)}LOOP ($label)")
                    outs.forEach { emitNode(it.toNodeId, depth) }
                }
                else -> {
                    sb.appendLine("${indent(depth)}DO $label")
                    outs.forEach { emitNode(it.toNodeId, depth) }
                }
            }

            activeStack.remove(nodeId)
        }

        emitNode(startNode.id, 1)

        // Include any unvisited disconnected nodes as comments/steps
        val remaining = logicalNodes.filter { it.id !in visited }
        if (remaining.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("    // Additional / Detached Steps")
            remaining.forEach { node ->
                sb.appendLine("    STEP ${node.text.replace("\n", " ")}")
            }
        }

        sb.append("END")
        return sb.toString()
    }

    private fun indexToAlias(index: Int): String {
        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return if (index < 26) {
            letters[index].toString()
        } else {
            "${letters[(index / 26) - 1]}${letters[index % 26]}"
        }
    }
}
