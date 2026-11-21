package com.example.data.remote

import android.util.Log
import com.example.domain.model.AiChatTurn
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AiProviderType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TAG = "UniversalAiProvider"

data class ProviderDiscoveryResult(
    val status: AiConnectionStatus,
    val provider: AiProviderType? = null,
    val selectedModel: String = "",
    val fallbackModels: List<String> = emptyList(),
    val statusDetail: String = "",
)

data class AiCompletionResult(
    val content: String,
    val modelUsed: String,
    val provider: AiProviderType,
)

class UniversalAiProviderService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(55, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private val EXCLUDED_MODEL_KEYWORDS = listOf(
            "whisper", "tts", "audio", "speech", "embed", "embedding", "moderation",
            "guard", "safeguard", "dall-e", "image", "realtime", "transcribe",
            "rerank", "babbage", "davinci", "canary", "playai", "distil-whisper",
            "sora", "omni-moderation", "computer-use", "deep-research",
        )

        private val GROQ_PREFERRED_ORDER = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "meta-llama/llama-4-scout-17b-16e-instruct",
            "meta-llama/llama-4-maverick-17b-128e-instruct",
            "qwen/qwen3-32b",
            "qwen-qwq-32b",
            "deepseek-r1-distill-llama-70b",
            "gemma2-9b-it",
            "llama3-70b-8192",
            "llama3-8b-8192",
            "mixtral-8x7b-32768",
        )

        private val OPENROUTER_FREE_PREFERRED_ORDER = listOf(
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemini-2.0-flash-exp:free",
            "mistralai/mistral-small-3.1-24b-instruct:free",
            "qwen/qwen-2.5-72b-instruct:free",
            "qwen/qwen3-14b:free",
            "deepseek/deepseek-chat-v3-0324:free",
            "google/gemma-3-27b-it:free",
            "meta-llama/llama-3.2-3b-instruct:free",
        )

        private val OPENROUTER_PAID_PREFERRED_ORDER = listOf(
            "openai/gpt-4o-mini",
            "google/gemini-2.0-flash-001",
            "anthropic/claude-3.5-haiku",
            "meta-llama/llama-3.3-70b-instruct",
            "deepseek/deepseek-chat",
            "qwen/qwen-2.5-72b-instruct",
            "mistralai/mistral-small-3.1-24b-instruct",
        )

        private val OPENAI_PREFERRED_ORDER = listOf(
            "gpt-4o-mini",
            "gpt-4.1-mini",
            "gpt-4.1-nano",
            "gpt-4o",
            "gpt-4.1",
            "gpt-3.5-turbo",
        )

        /**
         * Cleans accidental clipboard whitespace, newlines, surrounding quotes, or "Bearer " prefix.
         */
        fun sanitizeApiKey(raw: String): String {
            var cleaned = raw.trim()
                .removeSurrounding("\"")
                .removeSurrounding("'")
                .trim()
            if (cleaned.startsWith("Bearer ", ignoreCase = true)) {
                cleaned = cleaned.substring(7).trim()
            }
            return cleaned.replace(Regex("\\s+"), "")
        }

        /**
         * Determines the fastest probe order across supported providers without trusting prefix alone.
         */
        fun buildProbeOrder(cleanKey: String): List<AiProviderType> = when {
            cleanKey.startsWith("gsk_") -> listOf(
                AiProviderType.GROQ,
                AiProviderType.OPENROUTER,
                AiProviderType.OPENAI,
            )
            cleanKey.startsWith("sk-or-") -> listOf(
                AiProviderType.OPENROUTER,
                AiProviderType.GROQ,
                AiProviderType.OPENAI,
            )
            cleanKey.startsWith("sk-") -> listOf(
                AiProviderType.OPENAI,
                AiProviderType.OPENROUTER,
                AiProviderType.GROQ,
            )
            else -> listOf(
                AiProviderType.GROQ,
                AiProviderType.OPENROUTER,
                AiProviderType.OPENAI,
            )
        }

        /**
         * Filters and ranks models returned live from a provider's `/models` endpoint,
         * preferring fast, capable instruction-following chat models.
         */
        fun rankChatModels(
            provider: AiProviderType,
            availableModelIds: List<String>,
            preferFreeTier: Boolean = false,
        ): List<String> {
            val chatCandidates = availableModelIds
                .map { it.trim() }
                .filter { id ->
                    if (id.isBlank()) return@filter false
                    val lower = id.lowercase()
                    EXCLUDED_MODEL_KEYWORDS.none { banned -> lower.contains(banned) }
                }
                .distinct()

            if (chatCandidates.isEmpty()) return emptyList()

            val preferredList = when (provider) {
                AiProviderType.GROQ -> GROQ_PREFERRED_ORDER
                AiProviderType.OPENROUTER -> if (preferFreeTier) {
                    OPENROUTER_FREE_PREFERRED_ORDER + OPENROUTER_PAID_PREFERRED_ORDER
                } else {
                    OPENROUTER_PAID_PREFERRED_ORDER + OPENROUTER_FREE_PREFERRED_ORDER
                }
                AiProviderType.OPENAI -> OPENAI_PREFERRED_ORDER
            }

            val exactMatches = preferredList.filter { pref ->
                chatCandidates.any { it.equals(pref, ignoreCase = true) }
            }.map { pref ->
                chatCandidates.first { it.equals(pref, ignoreCase = true) }
            }

            val remaining = chatCandidates
                .filterNot { candidate -> exactMatches.any { it.equals(candidate, ignoreCase = true) } }
                .sortedWith(
                    compareByDescending<String> { id ->
                        val lower = id.lowercase()
                        var score = 0
                        if (provider == AiProviderType.OPENROUTER && preferFreeTier && lower.endsWith(":free")) score += 50
                        if (lower.contains("instruct") || lower.contains("versatile") || lower.contains("instant")) score += 20
                        if (lower.contains("llama-3.3") || lower.contains("llama-4") || lower.contains("gpt-4o-mini") || lower.contains("gpt-4.1-mini")) score += 18
                        if (lower.contains("flash") || lower.contains("mini") || lower.contains("fast")) score += 12
                        if (lower.contains("qwen") || lower.contains("llama") || lower.contains("mistral") || lower.contains("deepseek") || lower.contains("gemini") || lower.contains("gpt-4")) score += 8
                        if (lower.contains("vision") || lower.contains("preview")) score -= 4
                        score
                    }
                )

            return (exactMatches + remaining).distinct().take(12)
        }

        /**
         * Ensures no portion of the API key can ever leak into error strings or logs.
         */
        fun redactSecret(message: String, apiKey: String): String {
            if (apiKey.length < 6) return message
            var safe = message.replace(apiKey, "[REDACTED]")
            val prefix = apiKey.take(8)
            val suffix = apiKey.takeLast(6)
            safe = safe.replace(prefix, "[REDACTED]").replace(suffix, "[REDACTED]")
            return safe
        }
    }

    /**
     * Safely identifies whether the entered key belongs to Groq, OpenRouter, or OpenAI,
     * retrieves the provider's live available models, selects a suitable model, and verifies connectivity.
     */
    suspend fun discoverAndValidateKey(rawApiKey: String): ProviderDiscoveryResult = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(rawApiKey)
        if (cleanKey.isBlank()) {
            return@withContext ProviderDiscoveryResult(
                status = AiConnectionStatus.NOT_CONFIGURED,
                statusDetail = "Enter your Groq or OpenRouter API key to connect.",
            )
        }

        val probeOrder = buildProbeOrder(cleanKey)
        var hadNetworkFailure = false
        var sawAuthRejection = false

        for (provider in probeOrder) {
            when (val probe = probeProviderModels(provider, cleanKey)) {
                is ModelProbeOutcome.Authenticated -> {
                    val rankedModels = rankChatModels(
                        provider = provider,
                        availableModelIds = probe.modelIds,
                        preferFreeTier = probe.preferFreeTier,
                    )
                    if (rankedModels.isEmpty()) {
                        return@withContext ProviderDiscoveryResult(
                            status = AiConnectionStatus.CONNECTION_FAILED,
                            provider = provider,
                            statusDetail = "Connected to ${provider.displayName}, but no compatible chat models were returned.",
                        )
                    }

                    // Verify actual chat completion works and auto-select the first responsive model
                    val verifyResult = verifyWorkingModel(
                        provider = provider,
                        apiKey = cleanKey,
                        candidates = rankedModels,
                    )
                    return@withContext verifyResult.fold(
                        onSuccess = { verifiedModel ->
                            val orderedFallbacks = rankedModels.filterNot { it == verifiedModel }.take(8)
                            ProviderDiscoveryResult(
                                status = AiConnectionStatus.CONNECTED,
                                provider = provider,
                                selectedModel = verifiedModel,
                                fallbackModels = orderedFallbacks,
                                statusDetail = "${provider.displayName} connected and ready.",
                            )
                        },
                        onFailure = { err ->
                            val detail = redactSecret(err.message ?: "Connection check failed.", cleanKey)
                            val status = if (err is InvalidApiKeyException) {
                                AiConnectionStatus.INVALID_API_KEY
                            } else {
                                AiConnectionStatus.CONNECTION_FAILED
                            }
                            ProviderDiscoveryResult(
                                status = status,
                                provider = provider,
                                selectedModel = rankedModels.first(),
                                fallbackModels = rankedModels.drop(1).take(8),
                                statusDetail = detail,
                            )
                        }
                    )
                }

                is ModelProbeOutcome.InvalidKey -> {
                    sawAuthRejection = true
                }

                is ModelProbeOutcome.NetworkError -> {
                    hadNetworkFailure = true
                }
            }
        }

        if (hadNetworkFailure && !sawAuthRejection) {
            ProviderDiscoveryResult(
                status = AiConnectionStatus.CONNECTION_FAILED,
                statusDetail = "Unable to reach AI providers. Check your internet connection and try again.",
            )
        } else {
            ProviderDiscoveryResult(
                status = AiConnectionStatus.INVALID_API_KEY,
                statusDetail = "API key was not accepted by Groq, OpenRouter, or OpenAI. Verify your key and try again.",
            )
        }
    }

    /**
     * Fetches fresh available chat models for a known provider when cached models become unavailable.
     */
    suspend fun refreshAvailableModels(
        provider: AiProviderType,
        rawApiKey: String,
    ): List<String> = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(rawApiKey)
        if (cleanKey.isBlank()) return@withContext emptyList()
        when (val probe = probeProviderModels(provider, cleanKey)) {
            is ModelProbeOutcome.Authenticated -> rankChatModels(
                provider = provider,
                availableModelIds = probe.modelIds,
                preferFreeTier = probe.preferFreeTier,
            )
            else -> emptyList()
        }
    }

    /**
     * Executes a chat or structured-JSON completion using the configured provider and auto-selected model.
     * Automatically fails over to the next available model if the primary model is unavailable or overloaded.
     */
    suspend fun executeChatCompletionWithFailover(
        provider: AiProviderType,
        rawApiKey: String,
        selectedModel: String,
        fallbackModels: List<String>,
        systemInstruction: String,
        turns: List<AiChatTurn>,
        requireJsonObject: Boolean = false,
        temperature: Double = 0.4,
    ): Result<AiCompletionResult> = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(rawApiKey)
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(InvalidApiKeyException("AI API key is not configured."))
        }

        val initialCandidates = buildList {
            if (selectedModel.isNotBlank()) add(selectedModel)
            fallbackModels.forEach { if (it.isNotBlank() && it !in this) add(it) }
        }.ifEmpty {
            refreshAvailableModels(provider, cleanKey)
        }

        if (initialCandidates.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("No available models found for ${provider.displayName}.")
            )
        }

        val tried = mutableSetOf<String>()
        var lastError: Throwable? = null

        suspend fun tryCandidateList(candidates: List<String>): AiCompletionResult? {
            for (modelId in candidates) {
                if (!tried.add(modelId)) continue
                val outcome = singleCompletionRequest(
                    provider = provider,
                    apiKey = cleanKey,
                    modelId = modelId,
                    systemInstruction = systemInstruction,
                    turns = turns,
                    requireJsonObject = requireJsonObject,
                    temperature = temperature,
                )
                when (outcome) {
                    is SingleCompletionOutcome.Success -> {
                        return AiCompletionResult(
                            content = outcome.content,
                            modelUsed = modelId,
                            provider = provider,
                        )
                    }
                    is SingleCompletionOutcome.InvalidKey -> {
                        lastError = InvalidApiKeyException(outcome.message)
                        return null
                    }
                    is SingleCompletionOutcome.ModelUnavailableRetryNext -> {
                        lastError = IllegalStateException(outcome.message)
                        // Continue automatically to next candidate model
                    }
                    is SingleCompletionOutcome.NetworkFailure -> {
                        lastError = IllegalStateException(outcome.message)
                        return null
                    }
                }
            }
            return null
        }

        val firstPass = tryCandidateList(initialCandidates)
        if (firstPass != null) return@withContext Result.success(firstPass)

        // If every cached model failed due to availability (not auth/network), refresh live `/models` once
        if (lastError !is InvalidApiKeyException) {
            val freshModels = refreshAvailableModels(provider, cleanKey).filterNot { it in tried }
            if (freshModels.isNotEmpty()) {
                val secondPass = tryCandidateList(freshModels.take(4))
                if (secondPass != null) return@withContext Result.success(secondPass)
            }
        }

        val safeMsg = redactSecret(
            lastError?.message ?: "${provider.displayName} request failed. Please test your connection in Settings.",
            cleanKey,
        )
        val failure = if (lastError is InvalidApiKeyException) {
            InvalidApiKeyException(safeMsg)
        } else {
            IllegalStateException(safeMsg)
        }
        Result.failure(failure)
    }

    private sealed interface ModelProbeOutcome {
        data class Authenticated(val modelIds: List<String>, val preferFreeTier: Boolean) : ModelProbeOutcome
        data object InvalidKey : ModelProbeOutcome
        data class NetworkError(val message: String) : ModelProbeOutcome
    }

    private fun probeProviderModels(
        provider: AiProviderType,
        apiKey: String,
    ): ModelProbeOutcome {
        return try {
            var preferFreeTier = false

            // OpenRouter's /models endpoint is public, so verify the API key via /auth/key first!
            if (provider == AiProviderType.OPENROUTER) {
                val authReq = buildBaseRequest("${provider.baseUrl}/auth/key", provider, apiKey).get().build()
                client.newCall(authReq).execute().use { authResp ->
                    val authBody = authResp.body?.string().orEmpty()
                    if (authResp.code == 401 || authResp.code == 403) {
                        return ModelProbeOutcome.InvalidKey
                    }
                    if (!authResp.isSuccessful) {
                        return ModelProbeOutcome.NetworkError("OpenRouter verification failed (HTTP ${authResp.code}).")
                    }
                    val dataObj = runCatching { JSONObject(authBody).optJSONObject("data") }.getOrNull()
                    if (dataObj != null) {
                        val isFree = dataObj.optBoolean("is_free_tier", false)
                        val limitRemaining = dataObj.optDouble("limit_remaining", Double.NaN)
                        preferFreeTier = isFree || (!limitRemaining.isNaN() && limitRemaining <= 0.001)
                    }
                }
            }

            val modelsReq = buildBaseRequest("${provider.baseUrl}/models", provider, apiKey).get().build()
            client.newCall(modelsReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401 || resp.code == 403) {
                    return ModelProbeOutcome.InvalidKey
                }
                if (!resp.isSuccessful) {
                    return ModelProbeOutcome.NetworkError("${provider.displayName} models fetch failed (HTTP ${resp.code}).")
                }

                val modelIds = parseModelIds(body)
                ModelProbeOutcome.Authenticated(modelIds = modelIds, preferFreeTier = preferFreeTier)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Provider probe error for ${provider.displayName}: ${e.javaClass.simpleName}")
            ModelProbeOutcome.NetworkError(mapNetworkError(e, provider.displayName))
        }
    }

    private fun verifyWorkingModel(
        provider: AiProviderType,
        apiKey: String,
        candidates: List<String>,
    ): Result<String> {
        var lastErr: Throwable? = null
        for (modelId in candidates.take(6)) {
            val outcome = singleCompletionRequest(
                provider = provider,
                apiKey = apiKey,
                modelId = modelId,
                systemInstruction = "Respond concisely.",
                turns = listOf(AiChatTurn(role = "user", content = "Reply OK")),
                requireJsonObject = false,
                temperature = 0.0,
                maxTokens = 8,
            )
            when (outcome) {
                is SingleCompletionOutcome.Success -> return Result.success(modelId)
                is SingleCompletionOutcome.InvalidKey -> return Result.failure(InvalidApiKeyException(outcome.message))
                is SingleCompletionOutcome.ModelUnavailableRetryNext -> {
                    lastErr = IllegalStateException(outcome.message)
                }
                is SingleCompletionOutcome.NetworkFailure -> {
                    return Result.failure(IllegalStateException(outcome.message))
                }
            }
        }
        return Result.failure(
            lastErr ?: IllegalStateException("Could not establish a working model on ${provider.displayName}.")
        )
    }

    private sealed interface SingleCompletionOutcome {
        data class Success(val content: String) : SingleCompletionOutcome
        data class InvalidKey(val message: String) : SingleCompletionOutcome
        data class ModelUnavailableRetryNext(val message: String) : SingleCompletionOutcome
        data class NetworkFailure(val message: String) : SingleCompletionOutcome
    }

    private fun singleCompletionRequest(
        provider: AiProviderType,
        apiKey: String,
        modelId: String,
        systemInstruction: String,
        turns: List<AiChatTurn>,
        requireJsonObject: Boolean,
        temperature: Double,
        maxTokens: Int? = null,
    ): SingleCompletionOutcome {
        fun executePayload(withJsonMode: Boolean): Pair<Int, String> {
            val messages = JSONArray().apply {
                if (systemInstruction.isNotBlank()) {
                    put(JSONObject().put("role", "system").put("content", systemInstruction))
                }
                turns.forEach { turn ->
                    put(JSONObject().put("role", turn.role).put("content", turn.content))
                }
            }
            val payload = JSONObject().apply {
                put("model", modelId)
                put("temperature", temperature)
                if (maxTokens != null) put("max_tokens", maxTokens)
                if (withJsonMode) {
                    put("response_format", JSONObject().put("type", "json_object"))
                }
                put("messages", messages)
            }
            val req = buildBaseRequest("${provider.baseUrl}/chat/completions", provider, apiKey)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                return resp.code to (resp.body?.string().orEmpty())
            }
        }

        return try {
            var (code, rawBody) = executePayload(withJsonMode = requireJsonObject)

            // Some models reject response_format: json_object with HTTP 400; retry cleanly without it once
            if (code == 400 && requireJsonObject) {
                val retry = executePayload(withJsonMode = false)
                code = retry.first
                rawBody = retry.second
            }

            if (code in 200..299) {
                val content = runCatching {
                    val root = JSONObject(rawBody)
                    val choices = root.getJSONArray("choices")
                    choices.getJSONObject(0).getJSONObject("message").getString("content")
                }.getOrNull()
                if (!content.isNullOrBlank()) {
                    SingleCompletionOutcome.Success(content)
                } else {
                    SingleCompletionOutcome.ModelUnavailableRetryNext("Empty response from provider.")
                }
            } else {
                val errorObj = runCatching { JSONObject(rawBody).optJSONObject("error") }.getOrNull()
                val rawDetail = errorObj?.optString("message")?.takeIf { it.isNotBlank() }
                val safeDetail = rawDetail?.let { redactSecret(it, apiKey) }
                when (code) {
                    401, 403 -> SingleCompletionOutcome.InvalidKey(
                        "Invalid ${provider.displayName} API key."
                    )
                    400, 402, 404, 429, 502, 503 -> SingleCompletionOutcome.ModelUnavailableRetryNext(
                        safeDetail ?: "${provider.displayName} model unavailable (HTTP $code)."
                    )
                    else -> SingleCompletionOutcome.NetworkFailure(
                        safeDetail ?: "${provider.displayName} request failed (HTTP $code)."
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            SingleCompletionOutcome.NetworkFailure(mapNetworkError(e, provider.displayName))
        } catch (e: Exception) {
            SingleCompletionOutcome.NetworkFailure(
                redactSecret(e.message ?: "Unexpected connection error.", apiKey)
            )
        }
    }

    private fun buildBaseRequest(
        url: String,
        provider: AiProviderType,
        apiKey: String,
    ): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
        if (provider == AiProviderType.OPENROUTER) {
            builder.addHeader("HTTP-Referer", "https://flowbuilder.app")
            builder.addHeader("X-Title", "FlowBuilder")
        }
        return builder
    }

    private fun parseModelIds(rawJson: String): List<String> {
        return runCatching {
            val root = JSONObject(rawJson)
            val data = root.optJSONArray("data") ?: return emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    val id = obj.optString("id").trim()
                    if (id.isNotBlank()) add(id)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun mapNetworkError(e: Exception, providerName: String): String = when (e) {
        is UnknownHostException -> "No internet connection. Unable to reach $providerName."
        is SocketTimeoutException -> "$providerName request timed out. Please try again."
        else -> "Unable to connect to $providerName. Check your internet connection."
    }
}

class InvalidApiKeyException(message: String) : IllegalStateException(message)
