package com.example

import com.example.data.export.FlowExportManager
import com.example.data.local.FlowGraphSerializer
import com.example.domain.engine.FlowDslGenerator
import com.example.domain.engine.FlowLayoutEngine
import com.example.domain.engine.FlowValidator
import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowGraph
import com.example.domain.model.FlowNode
import com.example.domain.model.FlowProject
import com.example.domain.model.LayoutDirection
import com.example.domain.model.NodeShape
import com.example.domain.model.ShapeCategory
import com.example.domain.model.ValidationSeverity
import com.example.domain.templates.FlowTemplates
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowBuilderEngineTest {

    @Test
    fun templatesLibraryContainsAllEightRequiredTemplates() {
        assertEquals(8, FlowTemplates.allTemplates.size)
        val titles = FlowTemplates.allTemplates.map { it.title }
        assertTrue(titles.contains("Simple Algorithm"))
        assertTrue(titles.contains("Student Registration System"))
        assertTrue(titles.contains("Login Process"))
        assertTrue(titles.contains("ATM Workflow"))
        assertTrue(titles.contains("E-commerce Order Process"))
        assertTrue(titles.contains("Decision Making"))
        assertTrue(titles.contains("Programming Loops"))
        assertTrue(titles.contains("Software Development Lifecycle"))
    }

    @Test
    fun shapesCatalogContainsAllTwentyFourShapesWithNamesAndCategories() {
        val shapes = NodeShape.entries
        assertEquals(24, shapes.size)
        assertTrue(shapes.all { it.displayName.isNotBlank() })
        assertTrue(ShapeCategory.entries.all { category ->
            shapes.any { it.category == category }
        })
        assertTrue(shapes.contains(NodeShape.TEXT_BOX))
        assertTrue(shapes.contains(NodeShape.SUBROUTINE))
        assertTrue(shapes.contains(NodeShape.DATABASE))
        assertTrue(shapes.contains(NodeShape.OFF_PAGE_CONNECTOR))
    }

    @Test
    fun validatorDetectsDisconnectedNodeAndIncompleteDecision() {
        val n1 = FlowNode(id = "n1", shape = NodeShape.OVAL, text = "Start")
        val n2 = FlowNode(id = "n2", shape = NodeShape.DIAMOND, text = "Check?")
        val n3 = FlowNode(id = "n3", shape = NodeShape.RECTANGLE, text = "Only Branch")
        val orphan = FlowNode(id = "n4", shape = NodeShape.RECTANGLE, text = "Floating Node")
        val note = FlowNode(id = "n5", shape = NodeShape.TEXT_BOX, text = "Canvas Note")
        val annotation = FlowNode(id = "n6", shape = NodeShape.ANNOTATION, text = "Side Comment")
        val graph = FlowGraph(
            nodes = listOf(n1, n2, n3, orphan, note, annotation),
            edges = listOf(
                FlowEdge(fromNodeId = "n1", toNodeId = "n2"),
                FlowEdge(fromNodeId = "n2", toNodeId = "n3", label = "Yes"),
            ),
        )

        val issues = FlowValidator.validate(graph)
        assertTrue(issues.any { it.id == "disconnected_n4" && it.severity == ValidationSeverity.ERROR })
        assertTrue(issues.any { it.id == "decision_branch_n2" && it.severity == ValidationSeverity.ERROR })
        assertTrue(issues.any { it.id == "missing_end" })
        // Standalone TEXT_BOX and ANNOTATION nodes must not be flagged as disconnected
        assertFalse(issues.any { it.id == "disconnected_n5" })
        assertFalse(issues.any { it.id == "disconnected_n6" })
    }

    @Test
    fun autoLayoutHandlesCyclicLoopsWithoutCrashing() {
        val loopTemplate = FlowTemplates.allTemplates.first { it.title == "Programming Loops" }
        val topDown = FlowLayoutEngine.autoArrange(loopTemplate.graph, LayoutDirection.TOP_DOWN)
        val leftRight = FlowLayoutEngine.autoArrange(loopTemplate.graph, LayoutDirection.LEFT_RIGHT)
        val compact = FlowLayoutEngine.autoArrange(loopTemplate.graph, LayoutDirection.COMPACT_TREE)

        assertEquals(loopTemplate.graph.nodes.size, topDown.nodes.size)
        assertEquals(loopTemplate.graph.nodes.size, leftRight.nodes.size)
        assertEquals(loopTemplate.graph.nodes.size, compact.nodes.size)
        // Verify Start is above/left of End
        val startTd = topDown.nodes.first { it.text == "Start" }
        val endTd = topDown.nodes.first { it.text == "End" }
        assertTrue(startTd.y < endTd.y)
    }

    @Test
    fun jsonExportAndImportRoundTripsGraphFaithfully() {
        val serializer = FlowGraphSerializer()
        val template = FlowTemplates.allTemplates.first { it.title == "Login Process" }
        val project = FlowProject(
            title = "Auth Flow",
            description = "Offline test",
            category = "Software",
            graph = template.graph,
        )

        val json = serializer.encodeProjectExport(project)
        val decoded = serializer.decodeProjectExport(json).getOrThrow()

        assertEquals("Auth Flow", decoded.title)
        assertEquals(template.graph.nodes.size, decoded.graph.nodes.size)
        assertEquals(template.graph.edges.size, decoded.graph.edges.size)
    }

    @Test
    fun dslGeneratorOutputsValidFlowchartSyntaxAndPseudocode() {
        val template = FlowTemplates.allTemplates.first { it.title == "Simple Algorithm" }
        val dsl = FlowDslGenerator.generateMermaidDsl(template.graph)
        assertTrue(dsl.startsWith("flowchart TD"))
        assertTrue(dsl.contains("-->|Yes|"))
        assertTrue(dsl.contains("style A fill:"))
        assertFalse(dsl.contains("Empty canvas"))

        val pseudocode = FlowDslGenerator.generatePseudocode(template.graph, template.title)
        assertTrue(pseudocode.contains("ALGORITHM Simple_Algorithm"))
        assertTrue(pseudocode.contains("BEGIN"))
        assertTrue(pseudocode.contains("IF ("))
        assertTrue(pseudocode.trimEnd().endsWith("END"))
    }

    @Test
    fun htmlExportGeneratesInteractiveStandaloneSvgDocument() = runBlocking {
        val template = FlowTemplates.allTemplates.first { it.title == "Login Process" }
        val htmlBytes = FlowExportManager.renderToHtmlBytes(
            graph = template.graph,
            title = template.title,
            darkBackground = false,
        )
        val html = htmlBytes.toString(Charsets.UTF_8)
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<svg"))
        assertTrue(html.contains("Login Process"))
        assertTrue(html.contains("Start"))
    }

    @Test
    fun universalAiSanitizationProbeOrderModelRankingAndRedactionWork() {
        // 1. Key sanitization strips quotes, Bearer prefix, and accidental clipboard whitespace
        assertEquals(
            "gsk_live1234567890",
            com.example.data.remote.UniversalAiProviderService.sanitizeApiKey("  \"Bearer gsk_live123\n4567890\"  ")
        )

        // 2. Smart probe order prioritizes most likely provider while keeping universal fallback
        val groqOrder = com.example.data.remote.UniversalAiProviderService.buildProbeOrder("gsk_abc123")
        assertEquals(com.example.domain.model.AiProviderType.GROQ, groqOrder.first())
        assertTrue(groqOrder.contains(com.example.domain.model.AiProviderType.OPENROUTER))

        val openRouterOrder = com.example.data.remote.UniversalAiProviderService.buildProbeOrder("sk-or-v1-xyz789")
        assertEquals(com.example.domain.model.AiProviderType.OPENROUTER, openRouterOrder.first())
        assertTrue(openRouterOrder.contains(com.example.domain.model.AiProviderType.GROQ))

        val openAiOrder = com.example.data.remote.UniversalAiProviderService.buildProbeOrder("sk-proj-abc123")
        assertEquals(com.example.domain.model.AiProviderType.OPENAI, openAiOrder.first())

        // 3. Automatic Groq model selection filters audio/guard models and picks top instruction model
        val groqModels = com.example.data.remote.UniversalAiProviderService.rankChatModels(
            provider = com.example.domain.model.AiProviderType.GROQ,
            availableModelIds = listOf(
                "whisper-large-v3",
                "llama-guard-3-8b",
                "playai-tts",
                "llama-3.1-8b-instant",
                "llama-3.3-70b-versatile",
            ),
        )
        assertEquals("llama-3.3-70b-versatile", groqModels.first())
        assertFalse(groqModels.any { it.contains("whisper") || it.contains("guard") || it.contains("tts") })

        // 4. Automatic OpenRouter model selection prioritizes free-tier models when account is on free tier
        val freeOrModels = com.example.data.remote.UniversalAiProviderService.rankChatModels(
            provider = com.example.domain.model.AiProviderType.OPENROUTER,
            availableModelIds = listOf(
                "openai/gpt-4o-mini",
                "meta-llama/llama-3.3-70b-instruct:free",
                "text-embedding-3-small",
            ),
            preferFreeTier = true,
        )
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", freeOrModels.first())
        assertFalse(freeOrModels.any { it.contains("embedding") })

        // 5. Secret redaction ensures API keys never appear in error messages
        val secretKey = "gsk_SuperSecretKey987654"
        val redacted = com.example.data.remote.UniversalAiProviderService.redactSecret(
            message = "Auth failed for key $secretKey at endpoint",
            apiKey = secretKey,
        )
        assertFalse(redacted.contains(secretKey))
        assertFalse(redacted.contains("gsk_Supe"))
        assertTrue(redacted.contains("[REDACTED]"))
    }
}
