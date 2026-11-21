package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppFontFamily
import com.example.domain.model.GridStyle
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flowbuilder_prefs")

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appFontFamily: AppFontFamily = AppFontFamily.PLUS_JAKARTA_SANS,
    val defaultGridStyle: GridStyle = GridStyle.PLUS,
    val defaultSnapToGrid: Boolean = true,
    val showAlignmentGuides: Boolean = true,
    val showLiveValidationBadges: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val openAiApiKey: String = "",
    val hasCompletedOnboarding: Boolean = false,
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_FONT_FAMILY = stringPreferencesKey("app_font_family")
        val DEFAULT_GRID_STYLE = stringPreferencesKey("default_grid_style")
        val DEFAULT_SNAP_TO_GRID = booleanPreferencesKey("default_snap_to_grid")
        val SHOW_ALIGNMENT_GUIDES = booleanPreferencesKey("show_alignment_guides")
        val SHOW_VALIDATION_BADGES = booleanPreferencesKey("show_validation_badges")
        val AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val HAS_SEEDED_STARTER = booleanPreferencesKey("has_seeded_starter")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            appFontFamily = prefs[Keys.APP_FONT_FAMILY]?.let { runCatching { AppFontFamily.valueOf(it) }.getOrNull() }
                ?: AppFontFamily.PLUS_JAKARTA_SANS,
            defaultGridStyle = prefs[Keys.DEFAULT_GRID_STYLE]?.let { runCatching { GridStyle.valueOf(it) }.getOrNull() }
                ?: GridStyle.PLUS,
            defaultSnapToGrid = prefs[Keys.DEFAULT_SNAP_TO_GRID] ?: true,
            showAlignmentGuides = prefs[Keys.SHOW_ALIGNMENT_GUIDES] ?: true,
            showLiveValidationBadges = prefs[Keys.SHOW_VALIDATION_BADGES] ?: true,
            autoSaveEnabled = prefs[Keys.AUTO_SAVE_ENABLED] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            openAiApiKey = prefs[Keys.OPENAI_API_KEY] ?: "",
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAppFontFamily(fontFamily: AppFontFamily) {
        context.dataStore.edit { it[Keys.APP_FONT_FAMILY] = fontFamily.name }
    }

    suspend fun setDefaultGridStyle(style: GridStyle) {
        context.dataStore.edit { it[Keys.DEFAULT_GRID_STYLE] = style.name }
    }

    suspend fun setDefaultSnapToGrid(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_SNAP_TO_GRID] = enabled }
    }

    suspend fun setShowAlignmentGuides(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_ALIGNMENT_GUIDES] = enabled }
    }

    suspend fun setShowLiveValidationBadges(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_VALIDATION_BADGES] = enabled }
    }

    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SAVE_ENABLED] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setOpenAiApiKey(apiKey: String) {
        context.dataStore.edit { it[Keys.OPENAI_API_KEY] = apiKey.trim() }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = completed }
    }

    suspend fun hasSeededStarter(): Boolean {
        return context.dataStore.data.map { it[Keys.HAS_SEEDED_STARTER] ?: false }.first()
    }

    suspend fun markStarterSeeded() {
        context.dataStore.edit { it[Keys.HAS_SEEDED_STARTER] = true }
    }
}
