package com.guilherme.honeypot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "honeypot_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        private val KEY_ADMIN_PIN_HASH = stringPreferencesKey("admin_pin_hash")
        private val KEY_ADMIN_PIN_SALT = stringPreferencesKey("admin_pin_salt")
        private val KEY_TELEGRAM_TOKEN = stringPreferencesKey("telegram_token")
        private val KEY_TELEGRAM_CHAT_ID = stringPreferencesKey("telegram_chat_id")
        private val KEY_SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        private val KEY_DRY_RUN = booleanPreferencesKey("dry_run")
    }

    suspend fun isSetupComplete(): Boolean {
        return context.dataStore.data.map { it[KEY_SETUP_COMPLETE] ?: false }.first()
    }

    suspend fun savePin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        context.dataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hash
            prefs[KEY_PIN_SALT] = salt
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val storedHash = prefs[KEY_PIN_HASH] ?: return false
        val salt = prefs[KEY_PIN_SALT] ?: return false
        return hashPin(pin, salt) == storedHash
    }

    suspend fun saveAdminPin(pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        context.dataStore.edit { prefs ->
            prefs[KEY_ADMIN_PIN_HASH] = hash
            prefs[KEY_ADMIN_PIN_SALT] = salt
        }
    }

    suspend fun verifyAdminPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val storedHash = prefs[KEY_ADMIN_PIN_HASH] ?: return false
        val salt = prefs[KEY_ADMIN_PIN_SALT] ?: return false
        return hashPin(pin, salt) == storedHash
    }

    suspend fun saveTelegramConfig(token: String, chatId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TELEGRAM_TOKEN] = token
            prefs[KEY_TELEGRAM_CHAT_ID] = chatId
        }
    }

    suspend fun getTelegramToken(): String {
        return context.dataStore.data.map { it[KEY_TELEGRAM_TOKEN] ?: "" }.first()
    }

    suspend fun getTelegramChatId(): String {
        return context.dataStore.data.map { it[KEY_TELEGRAM_CHAT_ID] ?: "" }.first()
    }

    suspend fun setSetupComplete() {
        context.dataStore.edit { prefs ->
            prefs[KEY_SETUP_COMPLETE] = true
        }
    }

    suspend fun isDryRun(): Boolean {
        return context.dataStore.data.map { it[KEY_DRY_RUN] ?: false }.first()
    }

    suspend fun setDryRun(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRY_RUN] = enabled
        }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt$pin".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
