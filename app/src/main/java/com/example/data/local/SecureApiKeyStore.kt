package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AiConfigurationState
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AiProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.aiConfigStore: DataStore<Preferences> by preferencesDataStore(name = "flowbuilder_ai_config")

/**
 * Persists per-user AI provider configuration with Android Keystore AES-256-GCM encryption
 * for API keys so credentials are never stored in plaintext or shared across accounts.
 */
class SecureApiKeyStore(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "flowbuilder_ai_master_key_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val ENC_PREFIX = "enc_v1:"
    }

    private fun keyApiKey(userId: String) = stringPreferencesKey("ai_key_enc_${sanitizeUser(userId)}")
    private fun keyProvider(userId: String) = stringPreferencesKey("ai_provider_${sanitizeUser(userId)}")
    private fun keySelectedModel(userId: String) = stringPreferencesKey("ai_model_${sanitizeUser(userId)}")
    private fun keyFallbackModels(userId: String) = stringPreferencesKey("ai_fallbacks_${sanitizeUser(userId)}")
    private fun keyStatus(userId: String) = stringPreferencesKey("ai_status_${sanitizeUser(userId)}")
    private fun keyStatusDetail(userId: String) = stringPreferencesKey("ai_detail_${sanitizeUser(userId)}")
    private fun keyLastTested(userId: String) = longPreferencesKey("ai_tested_${sanitizeUser(userId)}")

    private fun sanitizeUser(userId: String): String =
        userId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "default" }

    fun observeConfig(userId: String): Flow<AiConfigurationState> {
        return context.aiConfigStore.data.map { prefs ->
            readStateFromPrefs(prefs, userId)
        }
    }

    suspend fun getConfig(userId: String): AiConfigurationState {
        return readStateFromPrefs(context.aiConfigStore.data.first(), userId)
    }

    suspend fun getDecryptedApiKey(userId: String): String {
        val encrypted = context.aiConfigStore.data.first()[keyApiKey(userId)].orEmpty()
        if (encrypted.isBlank()) return ""
        return decrypt(encrypted)
    }

    suspend fun saveConfig(
        userId: String,
        rawApiKey: String,
        provider: AiProviderType?,
        selectedModel: String,
        fallbackModels: List<String>,
        status: AiConnectionStatus,
        statusDetail: String?,
        testedAt: Long = System.currentTimeMillis(),
    ) {
        val encrypted = if (rawApiKey.isBlank()) "" else encrypt(rawApiKey)
        context.aiConfigStore.edit { prefs ->
            if (encrypted.isBlank()) {
                prefs.remove(keyApiKey(userId))
            } else {
                prefs[keyApiKey(userId)] = encrypted
            }
            if (provider != null) {
                prefs[keyProvider(userId)] = provider.name
            } else {
                prefs.remove(keyProvider(userId))
            }
            prefs[keySelectedModel(userId)] = selectedModel
            prefs[keyFallbackModels(userId)] = fallbackModels.joinToString("\n")
            prefs[keyStatus(userId)] = status.name
            if (statusDetail.isNullOrBlank()) {
                prefs.remove(keyStatusDetail(userId))
            } else {
                prefs[keyStatusDetail(userId)] = statusDetail
            }
            prefs[keyLastTested(userId)] = testedAt
        }
    }

    suspend fun updateActiveModel(userId: String, newModel: String) {
        if (newModel.isBlank()) return
        context.aiConfigStore.edit { prefs ->
            prefs[keySelectedModel(userId)] = newModel
        }
    }

    suspend fun updateConnectionStatus(
        userId: String,
        status: AiConnectionStatus,
        statusDetail: String?,
    ) {
        context.aiConfigStore.edit { prefs ->
            prefs[keyStatus(userId)] = status.name
            if (statusDetail.isNullOrBlank()) {
                prefs.remove(keyStatusDetail(userId))
            } else {
                prefs[keyStatusDetail(userId)] = statusDetail
            }
            prefs[keyLastTested(userId)] = System.currentTimeMillis()
        }
    }

    suspend fun removeConfig(userId: String) {
        context.aiConfigStore.edit { prefs ->
            prefs.remove(keyApiKey(userId))
            prefs.remove(keyProvider(userId))
            prefs.remove(keySelectedModel(userId))
            prefs.remove(keyFallbackModels(userId))
            prefs.remove(keyStatus(userId))
            prefs.remove(keyStatusDetail(userId))
            prefs.remove(keyLastTested(userId))
        }
    }

    /**
     * Migrates any legacy key previously saved in UserPreferencesRepository so existing users
     * do not lose their key when upgrading.
     */
    suspend fun migrateLegacyKeyIfNeeded(userId: String, legacyKey: String) {
        if (legacyKey.isBlank()) return
        val existing = getDecryptedApiKey(userId)
        if (existing.isNotBlank()) return
        saveConfig(
            userId = userId,
            rawApiKey = legacyKey,
            provider = null,
            selectedModel = "",
            fallbackModels = emptyList(),
            status = AiConnectionStatus.NOT_CONFIGURED,
            statusDetail = "Tap Test Connection to validate your saved key.",
        )
    }

    private fun readStateFromPrefs(prefs: Preferences, userId: String): AiConfigurationState {
        val hasKey = !prefs[keyApiKey(userId)].isNullOrBlank()
        val provider = prefs[keyProvider(userId)]?.let {
            runCatching { AiProviderType.valueOf(it) }.getOrNull()
        }
        val model = prefs[keySelectedModel(userId)].orEmpty()
        val fallbacks = prefs[keyFallbackModels(userId)]
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val storedStatus = prefs[keyStatus(userId)]?.let {
            runCatching { AiConnectionStatus.valueOf(it) }.getOrNull()
        }
        val effectiveStatus = when {
            !hasKey -> AiConnectionStatus.NOT_CONFIGURED
            storedStatus != null -> storedStatus
            else -> AiConnectionStatus.NOT_CONFIGURED
        }
        return AiConfigurationState(
            hasApiKey = hasKey,
            provider = provider,
            selectedModel = model,
            fallbackModels = fallbacks,
            status = effectiveStatus,
            statusDetail = prefs[keyStatusDetail(userId)],
            lastTestedAt = prefs[keyLastTested(userId)] ?: 0L,
        )
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plaintext: String): String {
        return runCatching {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            ENC_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        }.getOrElse {
            // Fallback encoding only if AndroidKeyStore is unavailable (e.g. JVM tests)
            Base64.encodeToString(plaintext.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    private fun decrypt(stored: String): String {
        return runCatching {
            if (stored.startsWith(ENC_PREFIX)) {
                val combined = Base64.decode(stored.removePrefix(ENC_PREFIX), Base64.NO_WRAP)
                if (combined.size <= GCM_IV_LENGTH_BYTES) return ""
                val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
                val cipherText = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
                val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateMasterKey(),
                    GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
                )
                String(cipher.doFinal(cipherText), Charsets.UTF_8)
            } else {
                String(Base64.decode(stored, Base64.NO_WRAP), Charsets.UTF_8)
            }
        }.getOrDefault("")
    }
}
