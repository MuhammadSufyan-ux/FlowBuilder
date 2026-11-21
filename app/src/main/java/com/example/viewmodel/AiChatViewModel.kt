package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowAiRepository
import com.example.domain.model.AiChatTurn
import com.example.domain.model.AiConfigurationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
)

data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val aiConfig: AiConfigurationState = AiConfigurationState(),
)

class AiChatViewModel(
    private val userId: String,
    private val aiRepository: FlowAiRepository,
) : ViewModel() {

    private val systemPromptText = """
        You are FlowBuilder AI Assistant, a friendly, clear, and accurate communication and problem-solving chatbot.
        Your role:
        - Answer user questions clearly and solve programming, algorithm, flowchart logic, math, study, and general problems step-by-step.
        - Help users understand flowchart shapes, nodes, connections, branching, loops, and pseudocode.
        - Reply strictly with helpful written answers and explanations.
        - Match the user's language naturally (English, Urdu, or Roman Urdu).
        - Keep answers well-structured, concise, and easy to read on a mobile screen.
    """.trimIndent()

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            aiRepository.observeConfiguration(userId).collect { config ->
                _uiState.update { it.copy(aiConfig = config) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), errorMessage = null, isLoading = false) }
    }

    fun sendMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isBlank() || _uiState.value.isLoading) return

        val previousMessages = _uiState.value.messages
        val userMsg = ChatMessage(isUser = true, text = prompt)
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderAssistant = ChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            isStreaming = true,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg + placeholderAssistant,
                isLoading = true,
                errorMessage = null,
            )
        }

        val turns = (previousMessages + userMsg)
            .filter { it.text.isNotBlank() }
            .map { msg ->
                AiChatTurn(
                    role = if (msg.isUser) "user" else "assistant",
                    content = msg.text,
                )
            }

        viewModelScope.launch {
            try {
                val result = aiRepository.sendChatMessage(
                    userId = userId,
                    systemInstruction = systemPromptText,
                    history = turns,
                    onStreamingText = { partial ->
                        updateAssistantMessage(
                            id = assistantMsgId,
                            text = partial,
                            streaming = true,
                        )
                    },
                )
                result.onSuccess { reply ->
                    updateAssistantMessage(assistantMsgId, reply.trim(), streaming = false)
                    _uiState.update { it.copy(isLoading = false) }
                }.onFailure { err ->
                    removeMessage(assistantMsgId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Could not reach AI Assistant. Check your connection.",
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                removeMessage(assistantMsgId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Could not reach AI Assistant. Check your connection.",
                    )
                }
            }
        }
    }

    private fun updateAssistantMessage(id: String, text: String, streaming: Boolean) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == id) msg.copy(text = text, isStreaming = streaming) else msg
                }
            )
        }
    }

    private fun removeMessage(id: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages.filterNot { it.id == id })
        }
    }

    class Factory(
        private val userId: String,
        private val aiRepository: FlowAiRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiChatViewModel(
                userId = userId,
                aiRepository = aiRepository,
            ) as T
        }
    }
}
