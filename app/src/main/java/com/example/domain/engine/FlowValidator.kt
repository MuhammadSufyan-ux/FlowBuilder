package com.example.domain.engine

import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowGraph
import com.example.domain.model.NodeShape
import com.example.domain.model.ValidationIssue
import com.example.domain.model.ValidationSeverity

object FlowValidator {

    private fun isAnnotationLike(shape: NodeShape): Boolean =
        shape == NodeShape.TEXT_BOX || shape == NodeShape.ANNOTATION

    fun validate(graph: FlowGraph): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val activeNodes = graph.nodes.filterNot { isAnnotationLike(it.shape) }

        if (activeNodes.isEmpty()) {
            issues.add(
                ValidationIssue(
                    id = "empty_diagram",
                    severity = ValidationSeverity.INFO,
                    title = "Empty Canvas",
                    description = "Add a Terminator (Start) shape to begin designing your flowchart.",
                )
            )
            return issues
        }

        val nodeMap = activeNodes.associateBy { it.id }
        val outgoing = activeNodes.associate { it.id to mutableListOf<String>() }
        val incoming = activeNodes.associate { it.id to mutableListOf<String>() }
        val outgoingEdges = activeNodes.associate { it.id to mutableListOf<FlowEdge>() }

        graph.edges.forEach { edge ->
            if (edge.fromNodeId in nodeMap && edge.toNodeId in nodeMap) {
                outgoing[edge.fromNodeId]?.add(edge.toNodeId)
                incoming[edge.toNodeId]?.add(edge.fromNodeId)
                outgoingEdges[edge.fromNodeId]?.add(edge)
            }
        }

        val ovals = activeNodes.filter { it.shape == NodeShape.OVAL }
        val startNodes = ovals.filter {
            incoming[it.id].isNullOrEmpty() || it.text.contains("start", ignoreCase = true) ||
                it.text.contains("begin", ignoreCase = true)
        }
        val endNodes = ovals.filter {
            outgoing[it.id].isNullOrEmpty() || it.text.contains("end", ignoreCase = true) ||
                it.text.contains("stop", ignoreCase = true) || it.text.contains("finish", ignoreCase = true)
        }

        if (startNodes.isEmpty()) {
            issues.add(
                ValidationIssue(
                    id = "missing_start",
                    severity = ValidationSeverity.ERROR,
                    title = "Missing Start Node",
                    description = "Every complete flowchart should begin with a Terminator (Start) shape.",
                )
            )
        }

        if (endNodes.isEmpty() && activeNodes.size > 1) {
            issues.add(
                ValidationIssue(
                    id = "missing_end",
                    severity = ValidationSeverity.WARNING,
                    title = "Missing End Node",
                    description = "Add an End (Terminator) node to clearly conclude the algorithm.",
                )
            )
        }

        activeNodes.forEach { node ->
            val inCount = incoming[node.id]?.size ?: 0
            val outCount = outgoing[node.id]?.size ?: 0

            if (inCount == 0 && outCount == 0 && activeNodes.size > 1) {
                issues.add(
                    ValidationIssue(
                        id = "disconnected_${node.id}",
                        severity = ValidationSeverity.ERROR,
                        title = "Disconnected Node: \"${node.text.take(22)}\"",
                        description = "This ${node.shape.displayName} is not connected to any other node.",
                        nodeId = node.id,
                    )
                )
            } else {
                if (outCount == 0 && node.shape != NodeShape.OVAL && node.shape != NodeShape.OFF_PAGE_CONNECTOR) {
                    issues.add(
                        ValidationIssue(
                            id = "dead_end_${node.id}",
                            severity = ValidationSeverity.WARNING,
                            title = "Missing Outgoing Flow: \"${node.text.take(22)}\"",
                            description = "A ${node.shape.displayName} should connect to a next step or End node.",
                            nodeId = node.id,
                        )
                    )
                }

                if (node.shape == NodeShape.DIAMOND) {
                    val edges = outgoingEdges[node.id].orEmpty()
                    if (edges.size < 2) {
                        issues.add(
                            ValidationIssue(
                                id = "decision_branch_${node.id}",
                                severity = ValidationSeverity.ERROR,
                                title = "Incomplete Decision: \"${node.text.take(22)}\"",
                                description = "Decision diamonds need at least 2 outgoing branches (e.g., Yes and No).",
                                nodeId = node.id,
                            )
                        )
                    } else if (edges.any { it.label.isBlank() }) {
                        issues.add(
                            ValidationIssue(
                                id = "decision_label_${node.id}",
                                severity = ValidationSeverity.WARNING,
                                title = "Unlabeled Decision Branch",
                                description = "Label outgoing arrows from \"${node.text.take(20)}\" (such as Yes / No) for clarity.",
                                nodeId = node.id,
                            )
                        )
                    }
                }
            }
        }

        if (startNodes.isNotEmpty() && activeNodes.size > 1) {
            val visited = mutableSetOf<String>()
            val queue = ArrayDeque<String>()
            startNodes.forEach {
                visited.add(it.id)
                queue.add(it.id)
            }
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                outgoing[curr].orEmpty().forEach { next ->
                    if (visited.add(next)) queue.add(next)
                }
            }
            activeNodes.forEach { node ->
                if (node.id !in visited &&
                    !((incoming[node.id]?.size ?: 0) == 0 && (outgoing[node.id]?.size ?: 0) == 0)
                ) {
                    issues.add(
                        ValidationIssue(
                            id = "unreachable_${node.id}",
                            severity = ValidationSeverity.WARNING,
                            title = "Unreachable Node: \"${node.text.take(22)}\"",
                            description = "No path leads from Start to this node.",
                            nodeId = node.id,
                        )
                    )
                }
            }
        }

        return issues.sortedBy {
            when (it.severity) {
                ValidationSeverity.ERROR -> 0
                ValidationSeverity.WARNING -> 1
                ValidationSeverity.INFO -> 2
            }
        }
    }
}
