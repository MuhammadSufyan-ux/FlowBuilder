package com.example.data.local

import com.example.domain.model.FlowGraph
import com.example.domain.model.FlowProject
import com.example.domain.model.FlowProjectExportPayload
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class FlowGraphSerializer {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val graphAdapter = moshi.adapter(FlowGraph::class.java)
    private val prettyGraphAdapter = moshi.adapter(FlowGraph::class.java).indent("  ")
    private val exportAdapter = moshi.adapter(FlowProjectExportPayload::class.java).indent("  ")

    fun encodeGraph(graph: FlowGraph): String = graphAdapter.toJson(graph)

    fun encodeGraphPretty(graph: FlowGraph): String = prettyGraphAdapter.toJson(graph)

    fun decodeGraph(json: String): FlowGraph {
        return try {
            graphAdapter.fromJson(json) ?: FlowGraph()
        } catch (_: Exception) {
            FlowGraph()
        }
    }

    fun encodeProjectExport(project: FlowProject): String {
        val payload = FlowProjectExportPayload(
            title = project.title,
            description = project.description,
            category = project.category,
            graph = project.graph,
        )
        return exportAdapter.toJson(payload)
    }

    fun decodeProjectExport(json: String): Result<FlowProjectExportPayload> {
        return try {
            val payload = exportAdapter.fromJson(json)
            if (payload != null && payload.title.isNotBlank()) {
                Result.success(payload)
            } else {
                // Fallback: try parsing raw FlowGraph JSON
                val graph = graphAdapter.fromJson(json)
                if (graph != null) {
                    Result.success(
                        FlowProjectExportPayload(
                            title = "Imported Flowchart",
                            description = "Imported from JSON file",
                            graph = graph,
                        )
                    )
                } else {
                    Result.failure(IllegalArgumentException("Unsupported JSON format"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
