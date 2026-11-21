package com.example.domain.model

enum class AiProviderType(
    val displayName: String,
    val baseUrl: String,
) {
    GROQ(
        displayName = "Groq",
        baseUrl = "https://api.groq.com/openai/v1",
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
    ),
    OPENAI(
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
    ),
}

enum class AiConnectionStatus(val label: String) {
    CONNECTED("Connected"),
    INVALID_API_KEY("Invalid API Key"),
    CONNECTION_FAILED("Connection Failed"),
    NOT_CONFIGURED("Not Configured"),
}

data class AiConfigurationState(
    val hasApiKey: Boolean = false,
    val provider: AiProviderType? = null,
    val selectedModel: String = "",
    val fallbackModels: List<String> = emptyList(),
    val status: AiConnectionStatus = AiConnectionStatus.NOT_CONFIGURED,
    val statusDetail: String? = null,
    val lastTestedAt: Long = 0L,
)

data class AiChatTurn(
    val role: String, // "user" or "assistant"
    val content: String,
)

data class GeneratedStepsPayload(
    val summary: String,
    val addedNodes: List<FlowNode>,
    val addedEdges: List<FlowEdge>,
)
