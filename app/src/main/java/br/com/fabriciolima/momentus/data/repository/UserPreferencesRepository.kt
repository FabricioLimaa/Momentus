package br.com.fabriciolima.momentus.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeMode { LIGHT, DARK, SYSTEM }

data class UserPreferences(
    val email: String,
    val rememberMe: Boolean,
    val lastAnimationDate: Long,
    val themeMode: AppThemeMode,
    val primaryColorHex: String?,
    val cornerRadiusDp: Int,
    val fontSizeMultiplier: Float,
    val animationsEnabled: Boolean,
    val soundEnabled: Boolean,
    val hapticEnabled: Boolean,
    val lastSyncTimestamp: Long
)

@Singleton
class UserPreferencesRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val USER_EMAIL = stringPreferencesKey("user_email")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val LAST_ANIMATION_DATE = longPreferencesKey("last_animation_date")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PRIMARY_COLOR_HEX = stringPreferencesKey("primary_color_hex")
        val CORNER_RADIUS_DP = intPreferencesKey("corner_radius_dp")
        val FONT_SIZE_MULTIPLIER = floatPreferencesKey("font_size_multiplier")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val themeMode = try {
                AppThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            }

            UserPreferences(
                email = preferences[PreferencesKeys.USER_EMAIL] ?: "",
                rememberMe = preferences[PreferencesKeys.REMEMBER_ME] ?: false,
                lastAnimationDate = preferences[PreferencesKeys.LAST_ANIMATION_DATE] ?: 0L,
                themeMode = themeMode,
                primaryColorHex = preferences[PreferencesKeys.PRIMARY_COLOR_HEX],
                cornerRadiusDp = preferences[PreferencesKeys.CORNER_RADIUS_DP] ?: 12,
                fontSizeMultiplier = preferences[PreferencesKeys.FONT_SIZE_MULTIPLIER] ?: 1.0f,
                animationsEnabled = preferences[PreferencesKeys.ANIMATIONS_ENABLED] ?: true,
                soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true,
                hapticEnabled = preferences[PreferencesKeys.HAPTIC_ENABLED] ?: true,
                lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L
            )
        }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.THEME_MODE] = mode.name }
    }

    suspend fun updatePrimaryColor(hex: String) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.PRIMARY_COLOR_HEX] = hex }
    }

    suspend fun updateCornerRadius(dp: Int) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.CORNER_RADIUS_DP] = dp }
    }

    suspend fun updateFontSize(multiplier: Float) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.FONT_SIZE_MULTIPLIER] = multiplier }
    }

    suspend fun updateAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.ANIMATIONS_ENABLED] = enabled }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.SOUND_ENABLED] = enabled }
    }

    suspend fun updateHapticEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp }
    }

    suspend fun updateRememberMe(rememberMe: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.REMEMBER_ME] = rememberMe }
    }

    suspend fun updateUserEmail(email: String) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.USER_EMAIL] = email }
    }

    suspend fun updateLastAnimationDate(date: Long) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.LAST_ANIMATION_DATE] = date }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }
}
