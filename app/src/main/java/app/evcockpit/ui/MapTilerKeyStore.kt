package app.evcockpit.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.evcockpit.navigation.RouteKeyCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private val Context.mapTilerCredentials by preferencesDataStore(name = "maptiler_credentials_v1")

/**
 * MapTiler anahtarı APK içine gömülmez; Android Keystore AES-GCM ile şifrelenir.
 * Anahtar yalnızca MapTiler'a yapılacak harita stili/karosu isteğinde kullanılır.
 */
class MapTilerKeyStore(context: Context) {
    private val app = context.applicationContext
    private val field = stringPreferencesKey("ciphertext_v1")

    val key: Flow<String?> = app.mapTilerCredentials.data.map { values ->
        values[field]?.let { encrypted ->
            runCatching { RouteKeyCipher.decrypt(encrypted) }.getOrNull()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun save(rawKey: String) {
        val trimmed = rawKey.trim()
        require(trimmed.matches(Regex("[a-zA-Z0-9_-]{6,256}"))) {
            "MapTiler anahtarının biçimi geçersiz."
        }
        val encrypted = RouteKeyCipher.encrypt(trimmed)
        app.mapTilerCredentials.edit { it[field] = encrypted }
    }

    suspend fun clear() {
        app.mapTilerCredentials.edit { it.remove(field) }
    }
}
