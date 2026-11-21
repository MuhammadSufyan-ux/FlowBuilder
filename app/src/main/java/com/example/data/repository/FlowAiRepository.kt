package com.example.data.repository

import android.util.Log
import com.example.data.local.SecureApiKeyStore
import com.example.data.remote.InvalidApiKeyException
import com.example.data.remote.UniversalAiProviderService
import com.example.domain.engine.FlowDslGenerator
import com.example.domain.engine.FlowLayoutEngine
import com.example.domain.model.AiChatTurn
import com.example.domain.model.AiConfigurationState
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AiProviderType
import com.example.domain.model.AppFontFamily
import com.example.domain.model.FlowEdge
import com.example.domain.model.FlowGraph
import com.example.domain.model.FlowNode
import com.example.domain.model.GeneratedStepsPayload
import com.example.domain.model.GridStyle
import com.example.domain.model.LayoutDirection
import com.example.domain.model.NodeShape
import com.example.domain.model.ValidationIssue
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "FlowAiRepository"

/**
 * Single Clean Architecture repository powering all FlowBuilder AI capabilities:
 * - Secure per-user API configuration & auto-discovery (Groq, OpenRouter, OpenAI)
 * - Automatic model selection & failover
 * - Flowchart generation, AI Assistant chat, workflow improvements, error explanations,
 *   step generation, and node/connection walkthroughs.
 */
class FlowAiRepository(
    private val keyStore: SecureApiKeyStore,
    private val providerService: UniversalAiProviderService = UniversalAiProviderService(),
) {

    // Lazy built-in Firebase AI Logic models used only when the user has not configured a personal API key
    private val builtInChatModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val builtInFlowModel by lazy {
        val allowedShapeNames = NodeShape.entries
            .filter { it != NodeShape.TEXT_BOX }
            .map { it.name }

        val flowSchema = Schema.obj(
            properties = mapOf(
                "nodes" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "id" to Schema.string(description = "Short unique id like n1, n2"),
                            "shape" to Schema.enumeration(
                                values = allowedShapeNames,
                                description = "Flowchart node shape",
                            ),
                            "text" to Schema.string(description = "Concise node text (2 to 6 words)"),
                            "fillColorHex" to Schema.string(description = "Soft hex fill color like #BEF6AC"),
                        ),
                        optionalProperties = listOf("fillColorHex"),
                    ),
                ),
                "edges" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "from" to Schema.string(description = "Source node id"),
                            "to" to Schema.string(description = "Target node id"),
                            "label" to Schema.string(description = "Optional edge label like Yes or No"),
                        ),
                        optionalProperties = listOf("label"),
                    ),
                ),
            ),
        )

        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.3f
                responseMimeType = "application/json"
                responseSchema = flowSchema
            },
        )
    }

    fun observeConfiguration(userId: String): Flow<AiConfigurationState> = keyStore.observeConfig(userId)

    suspend fun getConfiguration(userId: String): AiConfigurationState = keyStore.getConfig(userId)

    suspend fun migrateLegacyKeyIfNeeded(userId: String, legacyKey: String) {
        keyStore.migrateLegacyKeyIfNeeded(userId, legacyKey)
    }

    /**
     * Saves the user's API key encrypted at rest, automatically identifies Groq/OpenRouter/OpenAI,
     * retrieves live available models, picks the optimal model, and verifies connectivity.
     */
    suspend fun saveAndConfigureApiKey(
        userId: String,
        rawApiKey: String,
    ): AiConfigurationState {
        val cleanKey = UniversalAiProviderService.sanitizeApiKey(rawApiKey)
        if (cleanKey.isBlank()) {
            keyStore.removeConfig(userId)
            return AiConfigurationState(status = AiConnectionStatus.NOT_CONFIGURED)
        }

        val discovery = providerService.discoverAndValidateKey(cleanKey)
        keyStore.saveConfig(
            userId = userId,
            rawApiKey = cleanKey,
            provider = discovery.provider,
            selectedModel = discovery.selectedModel,
            fallbackModels = discovery.fallbackModels,
            status = discovery.status,
            statusDetail = discovery.statusDetail,
        )
        return keyStore.getConfig(userId)
    }

    /**
     * Tests either a newly pasted key (if non-blank) or the currently stored encrypted key.
     */
    suspend fun testConnection(
        userId: String,
        draftApiKey: String? = null,
    ): AiConfigurationState {
        val cleanedDraft = draftApiKey?.let { UniversalAiProviderService.sanitizeApiKey(it) }.orEmpty()
        val effectiveKey = if (cleanedDraft.isNotBlank()) {
            cleanedDraft
        } else {
            keyStore.getDecryptedApiKey(userId)
        }

        if (effectiveKey.isBlank()) {
            keyStore.removeConfig(userId)
            return AiConfigurationState(
                status = AiConnectionStatus.NOT_CONFIGURED,
                statusDetail = "Paste your AI API key to connect.",
            )
        }

        val discovery = providerService.discoverAndValidateKey(effectiveKey)
        keyStore.saveConfig(
            userId = userId,
            rawApiKey = effectiveKey,
            provider = discovery.provider,
            selectedModel = discovery.selectedModel,
            fallbackModels = discovery.fallbackModels,
            status = discovery.status,
            statusDetail = discovery.statusDetail,
        )
        return keyStore.getConfig(userId)
    }

    suspend fun removeApiKey(userId: String): AiConfigurationState {
        keyStore.removeConfig(userId)
        return AiConfigurationState(status = AiConnectionStatus.NOT_CONFIGURED)
    }

    // -------------------------------------------------------------------------
    // Unified AI Features (All use the configured provider + auto-selected model)
    // -------------------------------------------------------------------------

    /**
     * 1. Generates a full flowchart from a natural-language prompt.
     */
    suspend fun generateFlowchart(
        userId: String,
        projectTitle: String,
        prompt: String,
        gridStyle: GridStyle,
        snapToGrid: Boolean,
        defaultFont: AppFontFamily,
    ): Result<FlowGraph> {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Please describe the workflow you want to generate."))
        }

        val allowedShapes = NodeShape.entries
            .filter { it != NodeShape.TEXT_BOX }
            .joinToString(", ") { it.name }

        val systemPrompt = """
            You are an expert software architect and flowchart designer.
            Respond ONLY with a valid JSON object matching this exact structure:
            {
              "nodes": [
                { "id": "n1", "shape": "OVAL", "text": "Start", "fillColorHex": "#BEF6AC" }
              ],
              "edges": [
                { "from": "n1", "to": "n2", "label": "" }
              ]
            }
            Rules:
            - Allowed shapes: $allowedShapes.
            - Always begin with an OVAL ("Start") and finish with an OVAL ("End").
            - Keep labels concise (2 to 6 words).
            - Use DIAMOND for decisions and label outgoing branches "Yes" / "No" or short conditions.
            - Produce 5 to 14 well-connected nodes.
            - Soft hex fills: #BEF6AC (start), #FA93B2 (end), #A3E9CC (decision), #F1F0D4 (process), #D4EFF0 (data/io/database), #E5D9FF (subroutine), #FFE4BA (preparation/delay).
        """.trimIndent()

        val userPrompt = buildString {
            if (projectTitle.isNotBlank()) appendLine("Project title: $projectTitle")
            append("Create a clear flowchart for: $cleanPrompt")
        }

        val resolved = resolvePersonalProvider(userId)
        return if (resolved != null) {
            val completion = executePersonalCompletion(
                userId = userId,
                resolved = resolved,
                systemInstruction = systemPrompt,
                turns = listOf(AiChatTurn("user", userPrompt)),
                requireJsonObject = true,
                temperature = 0.3,
            )
            completion.fold(
                onSuccess = { parseFlowGraphJson(it, gridStyle, snapToGrid, defaultFont) },
                onFailure = { Result.failure(it) },
            )
        } else {
            generateFlowchartWithBuiltInAi(userPrompt, gridStyle, snapToGrid, defaultFont)
        }
    }

    /**
     * 2. Multi-turn AI Assistant chat (uses configured Groq/OpenRouter/OpenAI key when set,
     *    or built-in streaming Gemini AI when no personal key is configured).
     */
    suspend fun sendChatMessage(
        userId: String,
        systemInstruction: String,
        history: List<AiChatTurn>,
        onStreamingText: (String) -> Unit,
    ): Result<String> {
        val resolved = resolvePersonalProvider(userId)
        if (resolved != null) {
            return executePersonalCompletion(
                userId = userId,
                resolved = resolved,
                systemInstruction = systemInstruction,
                turns = history.takeLast(14),
                requireJsonObject = false,
                temperature = 0.5,
            ).onSuccess { full ->
                onStreamingText(full.trim())
            }
        }

        // Built-in Firebase AI Logic fallback when no personal key is configured
        return try {
            val latestUser = history.lastOrNull { it.role == "user" }?.content.orEmpty()
            val prior = history.dropLast(1).takeLast(14).map { turn ->
                content(if (turn.role == "user") "user" else "model") { text(turn.content) }
            }
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash",
                systemInstruction = content { text(systemInstruction) },
            )
            val chat = model.startChat(history = prior)
            val accumulated = StringBuilder()
            withTimeout(60_000L) {
                chat.sendMessageStream(latestUser).collect { chunk ->
                    val delta = chunk.text.orEmpty()
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        onStreamingText(accumulated.toString())
                    }
                }
            }
            val finalText = accumulated.toString().trim()
            if (finalText.isBlank()) {
                Result.failure(IllegalStateException("AI returned an empty answer. Please try again."))
            } else {
                Result.success(finalText)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Response timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Built-in AI chat failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "AI request failed."))
        }
    }

    /**
     * 3. Suggests actionable improvements for the user's current flowchart.
     */
    suspend fun suggestWorkflowImprovements(
        userId: String,
        title: String,
        graph: FlowGraph,
    ): Result<String> {
        val contextSummary = summarizeGraphForPrompt(title, graph)
        val systemPrompt = """
            You are FlowBuilder's workflow optimization expert.
            Review the provided flowchart and give 3 to 5 concise, high-impact suggestions to improve clarity, edge-case handling, shape semantics, and logical flow.
            Use clean bullet points. Keep it brief and directly tied to the diagram's actual nodes.
        """.trimIndent()
        return runTextWorkflowPrompt(userId, systemPrompt, contextSummary)
    }

    /**
     * 4. Explains detected validation issues in plain language and shows exact steps to fix them.
     */
    suspend fun explainWorkflowErrors(
        userId: String,
        title: String,
        graph: FlowGraph,
        issues: List<ValidationIssue>,
    ): Result<String> {
        val contextSummary = buildString {
            appendLine(summarizeGraphForPrompt(title, graph))
            appendLine()
            appendLine("Detected Validation Issues:")
            if (issues.isEmpty()) {
                appendLine("- No structural validation errors detected.")
            } else {
                issues.forEach { issue ->
                    appendLine("- [${issue.severity.name}] ${issue.title}: ${issue.description}")
                }
            }
        }
        val systemPrompt = """
            You are FlowBuilder's diagram debugger.
            Explain each validation issue clearly in simple terms and give exact step-by-step instructions to fix each one in the flowchart editor.
            If there are no errors, confirm the diagram is structurally valid and mention any subtle logic checks to keep in mind.
        """.trimIndent()
        return runTextWorkflowPrompt(userId, systemPrompt, contextSummary)
    }

    /**
     * 5. Helps users understand the selected node & its connections, or walks through the full diagram.
     */
    suspend fun explainNodesAndConnections(
        userId: String,
        title: String,
        graph: FlowGraph,
        focusedNodeId: String?,
    ): Result<String> {
        val focusedNode = focusedNodeId?.let { id -> graph.nodes.firstOrNull { it.id == id } }
        val contextSummary = buildString {
            appendLine(summarizeGraphForPrompt(title, graph))
            if (focusedNode != null) {
                appendLine()
                appendLine("Focused Node: [${focusedNode.shape.displayName}] \"${focusedNode.text}\"")
                val incoming = graph.edges.filter { it.toNodeId == focusedNode.id }
                    .mapNotNull { e -> graph.nodes.firstOrNull { it.id == e.fromNodeId }?.text?.let { "$it (${e.label.ifBlank { "flow" }})" } }
                val outgoing = graph.edges.filter { it.fromNodeId == focusedNode.id }
                    .mapNotNull { e -> graph.nodes.firstOrNull { it.id == e.toNodeId }?.text?.let { "$it (${e.label.ifBlank { "flow" }})" } }
                appendLine("Incoming from: ${if (incoming.isEmpty()) "None" else incoming.joinToString(", ")}")
                appendLine("Outgoing to: ${if (outgoing.isEmpty()) "None" else outgoing.joinToString(", ")}")
            }
        }
        val systemPrompt = if (focusedNode != null) {
            """
                You are FlowBuilder's diagram guide.
                Explain clearly what the focused node represents (including its flowchart symbol meaning), how data/control enters it from incoming connections, and what happens along its outgoing connections.
                Keep it concise, friendly, and easy to follow.
            """.trimIndent()
        } else {
            """
                You are FlowBuilder's diagram guide.
                Walk through this flowchart from start to finish, explaining the purpose of its key shapes, decisions, and connections in clear, simple language.
            """.trimIndent()
        }
        return runTextWorkflowPrompt(userId, systemPrompt, contextSummary)
    }

    /**
     * 6. Generates new workflow steps to append to the existing canvas based on user instruction.
     */
    suspend fun generateWorkflowSteps(
        userId: String,
        title: String,
        graph: FlowGraph,
        instruction: String,
        defaultFont: AppFontFamily,
    ): Result<GeneratedStepsPayload> {
        val cleanInstruction = instruction.trim()
        if (cleanInstruction.isBlank()) {
            return Result.failure(IllegalArgumentException("Describe the workflow steps you want to add."))
        }

        val allowedShapes = NodeShape.entries
            .filter { it != NodeShape.TEXT_BOX }
            .joinToString(", ") { it.name }

        val existingNodesDesc = graph.nodes.mapIndexed { idx, n ->
            "existing_${idx + 1} (${n.shape.name}): \"${n.text}\""
        }.joinToString("\n")
        val existingIdMap = graph.nodes.mapIndexed { idx, n ->
            "existing_${idx + 1}" to n.id
        }.toMap()

        val systemPrompt = """
            You are FlowBuilder's workflow step generator.
            Generate 2 to 6 NEW nodes to extend the user's existing flowchart according to their request.
            Respond ONLY with valid JSON matching this schema:
            {
              "summary": "Short 1-sentence description of what was added",
              "attachFromExistingId": "existing_1 or empty string",
              "nodes": [
                { "id": "new_1", "shape": "RECTANGLE", "text": "Step label", "fillColorHex": "#F1F0D4" }
              ],
              "edges": [
                { "from": "new_1", "to": "new_2", "label": "" }
              ]
            }
            Rules:
            - Allowed shapes: $allowedShapes.
            - Use short concise labels (2 to 6 words).
            - Optionally set attachFromExistingId to one of the existing node keys to connect the first new node automatically.
            - Edges may connect new nodes to each other or to existing node keys.
        """.trimIndent()

        val userPrompt = buildString {
            appendLine("Project: $title")
            appendLine("Existing nodes:")
            appendLine(existingNodesDesc.ifBlank { "(Canvas is currently empty)" })
            appendLine()
            append("User request for new steps: $cleanInstruction")
        }

        val rawJsonResult = runJsonWorkflowPrompt(userId, systemPrompt, userPrompt)
        return rawJsonResult.fold(
            onSuccess = { raw ->
                parseAdditionalStepsJson(
                    rawContent = raw,
                    existingGraph = graph,
                    existingIdMap = existingIdMap,
                    defaultFont = defaultFont,
                )
            },
            onFailure = { Result.failure(it) },
        )
    }

    // -------------------------------------------------------------------------
    // Internal Provider Dispatch & Parsing
    // -------------------------------------------------------------------------

    private data class ResolvedPersonalProvider(
        val provider: AiProviderType,
        val apiKey: String,
        val selectedModel: String,
        val fallbackModels: List<String>,
    )

    private suspend fun resolvePersonalProvider(userId: String): ResolvedPersonalProvider? {
        val rawKey = keyStore.getDecryptedApiKey(userId)
        if (rawKey.isBlank()) return null

        val config = keyStore.getConfig(userId)
        val provider = config.provider
        if (provider != null && config.selectedModel.isNotBlank()) {
            return ResolvedPersonalProvider(
                provider = provider,
                apiKey = rawKey,
                selectedModel = config.selectedModel,
                fallbackModels = config.fallbackModels,
            )
        }

        // Key is stored (e.g., migrated or untested) — auto-discover provider & model on the fly!
        val discovery = providerService.discoverAndValidateKey(rawKey)
        keyStore.saveConfig(
            userId = userId,
            rawApiKey = rawKey,
            provider = discovery.provider,
            selectedModel = discovery.selectedModel,
            fallbackModels = discovery.fallbackModels,
            status = discovery.status,
            statusDetail = discovery.statusDetail,
        )
        return if (discovery.provider != null && discovery.selectedModel.isNotBlank()) {
            ResolvedPersonalProvider(
                provider = discovery.provider,
                apiKey = rawKey,
                selectedModel = discovery.selectedModel,
                fallbackModels = discovery.fallbackModels,
            )
        } else {
            // Throw explicit error downstream rather than silently ignoring user's broken key
            ResolvedPersonalProvider(
                provider = discovery.provider ?: AiProviderType.GROQ,
                apiKey = rawKey,
                selectedModel = "",
                fallbackModels = emptyList(),
            )
        }
    }

    private suspend fun executePersonalCompletion(
        userId: String,
        resolved: ResolvedPersonalProvider,
        systemInstruction: String,
        turns: List<AiChatTurn>,
        requireJsonObject: Boolean,
        temperature: Double,
    ): Result<String> {
        val result = try {
            withTimeout(60_000L) {
                providerService.executeChatCompletionWithFailover(
                    provider = resolved.provider,
                    rawApiKey = resolved.apiKey,
                    selectedModel = resolved.selectedModel,
                    fallbackModels = resolved.fallbackModels,
                    systemInstruction = systemInstruction,
                    turns = turns,
                    requireJsonObject = requireJsonObject,
                    temperature = temperature,
                )
            }
        } catch (e: TimeoutCancellationException) {
            return Result.failure(IllegalStateException("AI request timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        }

        return result.fold(
            onSuccess = { completion ->
                // Persist rotated model automatically if failover switched to a healthier model
                if (completion.modelUsed != resolved.selectedModel) {
                    keyStore.updateActiveModel(userId, completion.modelUsed)
                }
                keyStore.updateConnectionStatus(
                    userId = userId,
                    status = AiConnectionStatus.CONNECTED,
                    statusDetail = "${completion.provider.displayName} connected and ready.",
                )
                Result.success(completion.content)
            },
            onFailure = { err ->
                val status = if (err is InvalidApiKeyException) {
                    AiConnectionStatus.INVALID_API_KEY
                } else {
                    AiConnectionStatus.CONNECTION_FAILED
                }
                keyStore.updateConnectionStatus(
                    userId = userId,
                    status = status,
                    statusDetail = err.message,
                )
                Result.failure(err)
            },
        )
    }

    private suspend fun runTextWorkflowPrompt(
        userId: String,
        systemPrompt: String,
        userPrompt: String,
    ): Result<String> {
        val resolved = resolvePersonalProvider(userId)
        if (resolved != null) {
            return executePersonalCompletion(
                userId = userId,
                resolved = resolved,
                systemInstruction = systemPrompt,
                turns = listOf(AiChatTurn("user", userPrompt)),
                requireJsonObject = false,
                temperature = 0.4,
            )
        }
        return try {
            val response = withTimeout(60_000L) {
                builtInChatModel.generateContent("$systemPrompt\n\n$userPrompt")
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("AI returned an empty response."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("AI request timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Built-in workflow text prompt failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "AI request failed."))
        }
    }

    private suspend fun runJsonWorkflowPrompt(
        userId: String,
        systemPrompt: String,
        userPrompt: String,
    ): Result<String> {
        val resolved = resolvePersonalProvider(userId)
        if (resolved != null) {
            return executePersonalCompletion(
                userId = userId,
                resolved = resolved,
                systemInstruction = systemPrompt,
                turns = listOf(AiChatTurn("user", userPrompt)),
                requireJsonObject = true,
                temperature = 0.3,
            )
        }
        return try {
            val response = withTimeout(60_000L) {
                builtInChatModel.generateContent("$systemPrompt\n\n$userPrompt")
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("AI returned an empty response."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("AI step generation timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Built-in workflow step generation failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Could not generate workflow steps."))
        }
    }

    private suspend fun generateFlowchartWithBuiltInAi(
        userPrompt: String,
        gridStyle: GridStyle,
        snapToGrid: Boolean,
        defaultFont: AppFontFamily,
    ): Result<FlowGraph> {
        return try {
            val fullPrompt = buildString {
                appendLine("You are an expert software architect who creates clean flowcharts.")
                appendLine("Rules:")
                appendLine("- Begin with an OVAL ('Start') and finish with an OVAL ('End').")
                appendLine("- Keep node labels concise (2 to 6 words).")
                appendLine("- Use DIAMOND for decisions and label outgoing edges 'Yes' / 'No'.")
                appendLine("- Produce 5 to 14 connected nodes.")
                appendLine("- Soft hex fills: #BEF6AC (start), #FA93B2 (end), #A3E9CC (decision), #F1F0D4 (process), #D4EFF0 (data/io/database), #E5D9FF (subroutine), #FFE4BA (preparation/delay).")
                append(userPrompt)
            }
            val response = withTimeout(60_000L) {
                builtInFlowModel.generateContent(fullPrompt)
            }
            val text = response.text
                ?: return Result.failure(IllegalStateException("AI returned an empty flowchart."))
            parseFlowGraphJson(text, gridStyle, snapToGrid, defaultFont)
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("AI generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Built-in AI flowchart generation failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Built-in AI failed to generate flowchart."))
        }
    }

    private fun summarizeGraphForPrompt(title: String, graph: FlowGraph): String {
        val dsl = FlowDslGenerator.generateMermaidDsl(graph)
        val pseudo = FlowDslGenerator.generatePseudocode(graph, title)
        return buildString {
            appendLine("Flowchart Title: $title")
            appendLine("Nodes count: ${graph.nodes.size}, Edges count: ${graph.edges.size}")
            appendLine("Flowchart DSL:")
            appendLine(dsl)
            appendLine()
            appendLine("Pseudocode:")
            appendLine(pseudo)
        }
    }

    private fun extractJsonObject(rawContent: String): JSONObject? {
        val trimmed = rawContent.trim()
        val slice = if (trimmed.startsWith("{")) {
            trimmed
        } else {
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
        }
        return runCatching { JSONObject(slice) }.getOrNull()
    }

    private fun parseFlowGraphJson(
        rawContent: String,
        gridStyle: GridStyle,
        snapToGrid: Boolean,
        defaultFont: AppFontFamily,
    ): Result<FlowGraph> {
        val parsed = extractJsonObject(rawContent)
            ?: return Result.failure(IllegalStateException("AI returned an unexpected format. Please try again."))

        val nodesJson = parsed.optJSONArray("nodes") ?: JSONArray()
        val edgesJson = parsed.optJSONArray("edges") ?: JSONArray()
        if (nodesJson.length() == 0) {
            return Result.failure(IllegalStateException("AI returned an empty flowchart. Try a clearer prompt."))
        }

        val idRemap = mutableMapOf<String, String>()
        val nodes = mutableListOf<FlowNode>()
        for (i in 0 until nodesJson.length()) {
            val obj = nodesJson.optJSONObject(i) ?: continue
            val rawId = obj.optString("id").ifBlank { "n${i + 1}" }
            val newId = UUID.randomUUID().toString()
            idRemap[rawId] = newId

            val shapeName = obj.optString("shape", "RECTANGLE").uppercase().trim()
            val shape = runCatching { NodeShape.valueOf(shapeName) }.getOrDefault(NodeShape.RECTANGLE)
            val text = obj.optString("text").trim().ifBlank { shape.defaultText }
            val rawColor = obj.optString("fillColorHex").trim()
            val fillColor = if (rawColor.matches(Regex("^#[0-9a-fA-F]{6}$"))) rawColor else shape.defaultFillHex

            nodes.add(
                FlowNode(
                    id = newId,
                    shape = shape,
                    text = text,
                    width = shape.defaultWidth,
                    height = shape.defaultHeight,
                    fillColorHex = fillColor,
                    fontFamily = defaultFont,
                )
            )
        }

        val edges = mutableListOf<FlowEdge>()
        for (i in 0 until edgesJson.length()) {
            val obj = edgesJson.optJSONObject(i) ?: continue
            val fromKey = obj.optString("from").ifBlank { obj.optString("fromNodeId") }
            val toKey = obj.optString("to").ifBlank { obj.optString("toNodeId") }
            val fromId = idRemap[fromKey] ?: continue
            val toId = idRemap[toKey] ?: continue
            if (fromId == toId) continue
            edges.add(
                FlowEdge(
                    fromNodeId = fromId,
                    toNodeId = toId,
                    label = obj.optString("label", "").trim(),
                )
            )
        }

        val arranged = FlowLayoutEngine.autoArrange(
            FlowGraph(nodes = nodes, edges = edges, gridStyle = gridStyle, snapToGrid = snapToGrid),
            LayoutDirection.TOP_DOWN,
        )
        return Result.success(arranged)
    }

    private fun parseAdditionalStepsJson(
        rawContent: String,
        existingGraph: FlowGraph,
        existingIdMap: Map<String, String>,
        defaultFont: AppFontFamily,
    ): Result<GeneratedStepsPayload> {
        val parsed = extractJsonObject(rawContent)
            ?: return Result.failure(IllegalStateException("AI returned an unexpected step format. Please try again."))

        val nodesJson = parsed.optJSONArray("nodes") ?: JSONArray()
        val edgesJson = parsed.optJSONArray("edges") ?: JSONArray()
        if (nodesJson.length() == 0) {
            return Result.failure(IllegalStateException("AI did not generate any new steps. Try a more specific prompt."))
        }

        val baseX = existingGraph.nodes.maxOfOrNull { it.centerX }?.minus(80f) ?: 160f
        val baseY = (existingGraph.nodes.maxOfOrNull { it.bottom } ?: 80f) + 96f

        val idRemap = existingIdMap.toMutableMap()
        val addedNodes = mutableListOf<FlowNode>()
        for (i in 0 until nodesJson.length()) {
            val obj = nodesJson.optJSONObject(i) ?: continue
            val rawId = obj.optString("id").ifBlank { "new_${i + 1}" }
            val newId = UUID.randomUUID().toString()
            idRemap[rawId] = newId

            val shapeName = obj.optString("shape", "RECTANGLE").uppercase().trim()
            val shape = runCatching { NodeShape.valueOf(shapeName) }.getOrDefault(NodeShape.RECTANGLE)
            val text = obj.optString("text").trim().ifBlank { shape.defaultText }
            val rawColor = obj.optString("fillColorHex").trim()
            val fillColor = if (rawColor.matches(Regex("^#[0-9a-fA-F]{6}$"))) rawColor else shape.defaultFillHex

            addedNodes.add(
                FlowNode(
                    id = newId,
                    shape = shape,
                    text = text,
                    x = baseX,
                    y = baseY + i * 124f,
                    width = shape.defaultWidth,
                    height = shape.defaultHeight,
                    fillColorHex = fillColor,
                    fontFamily = defaultFont,
                )
            )
        }

        val addedEdges = mutableListOf<FlowEdge>()
        val attachKey = parsed.optString("attachFromExistingId").trim()
        val attachFromId = existingIdMap[attachKey]
        if (attachFromId != null && addedNodes.isNotEmpty()) {
            addedEdges.add(FlowEdge(fromNodeId = attachFromId, toNodeId = addedNodes.first().id))
        }

        for (i in 0 until edgesJson.length()) {
            val obj = edgesJson.optJSONObject(i) ?: continue
            val fromKey = obj.optString("from").ifBlank { obj.optString("fromNodeId") }
            val toKey = obj.optString("to").ifBlank { obj.optString("toNodeId") }
            val fromId = idRemap[fromKey] ?: continue
            val toId = idRemap[toKey] ?: continue
            if (fromId == toId) continue
            if (addedEdges.none { it.fromNodeId == fromId && it.toNodeId == toId }) {
                addedEdges.add(
                    FlowEdge(
                        fromNodeId = fromId,
                        toNodeId = toId,
                        label = obj.optString("label", "").trim(),
                    )
                )
            }
        }

        val summary = parsed.optString("summary").trim().ifBlank {
            "Added ${addedNodes.size} new workflow step(s)."
        }
        return Result.success(
            GeneratedStepsPayload(
                summary = summary,
                addedNodes = addedNodes,
                addedEdges = addedEdges,
            )
        )
    }
}
