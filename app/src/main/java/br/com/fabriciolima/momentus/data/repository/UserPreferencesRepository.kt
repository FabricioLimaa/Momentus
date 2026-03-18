package br.com.fabriciolima.momentus.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class UserPreferences(
    val email: String,
    val rememberMe: Boolean,
    val lastAnimationDate: Long
)

@Singleton
class UserPreferencesRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val USER_EMAIL = stringPreferencesKey("user_email")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val LAST_ANIMATION_DATE = longPreferencesKey("last_animation_date")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val email = preferences[PreferencesKeys.USER_EMAIL] ?: ""
            val rememberMe = preferences[PreferencesKeys.REMEMBER_ME] ?: false
            val lastAnimationDate = preferences[PreferencesKeys.LAST_ANIMATION_DATE] ?: 0L
            UserPreferences(email, rememberMe, lastAnimationDate)
        }

    suspend fun updateRememberMe(rememberMe: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMEMBER_ME] = rememberMe
        }
    }

    suspend fun updateUserEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_EMAIL] = email
        }
    }

    suspend fun updateLastAnimationDate(date: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_ANIMATION_DATE] = date
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
