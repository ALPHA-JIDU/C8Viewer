package com.jihad.chatviewer.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesDataStore(private val context: Context) {

    private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
    private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    private val KEY_MY_NAME = stringPreferencesKey("my_name")
    private val KEY_IMPORTED = booleanPreferencesKey("imported")
    private val KEY_LAST_OPENED_THREAD_ID = longPreferencesKey("last_opened_thread_id")

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_APP_LOCK_ENABLED] ?: false }
    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }
    val isImported: Flow<Boolean> = context.dataStore.data.map { it[KEY_IMPORTED] ?: false }
    val lastOpenedThreadId: Flow<Long?> = context.dataStore.data.map { it[KEY_LAST_OPENED_THREAD_ID] }

    suspend fun setAppLock(enabled: Boolean, pin: String?, allowBiometric: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
            prefs[KEY_BIOMETRIC_ENABLED] = allowBiometric
            if (!pin.isNullOrBlank()) {
                prefs[KEY_PIN_HASH] = hashPin(pin)
            }
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        var isValid = false
        context.dataStore.edit { prefs ->
            val stored = prefs[KEY_PIN_HASH]
            isValid = stored != null && stored == hashPin(pin)
        }
        return isValid
    }

    suspend fun saveMyName(name: String) {
        context.dataStore.edit { it[KEY_MY_NAME] = name }
    }

    suspend fun setImported(value: Boolean) {
        context.dataStore.edit { it[KEY_IMPORTED] = value }
    }

    suspend fun saveLastOpenedThreadId(threadId: Long) {
        context.dataStore.edit { it[KEY_LAST_OPENED_THREAD_ID] = threadId }
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
