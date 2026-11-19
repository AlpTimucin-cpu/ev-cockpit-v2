package app.evcockpit.navigation

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.routeCredentials by preferencesDataStore(name = "route_credentials_v1")

/**
 * Only encrypted ciphertext+IV is stored in Preferences DataStore.
 * The non-exportable AES key lives in AndroidKeyStore; never logs secrets.
 * No API key is included in source, Gradle properties, BuildConfig or APK assets.
 */
class EncryptedRouteKeyStore(context: Context) {
    private val application = context.applicationContext
    private val credential = stringPreferencesKey("ors_ciphertext_v1")

    val isConfigured: Flow<Boolean> =
        application.routeCredentials.data.map { !it[credential].isNullOrEmpty() }

    suspend fun save(userKey: String) = withContext(Dispatchers.IO) {
        val value = userKey.trim()
        require(value.isNotEmpty() && value.length <= 4096) { "Anahtar uzunluğu geçersiz." }
        val encrypted = RouteKeyCipher.encrypt(value)
        application.routeCredentials.edit { it[credential] = encrypted }
    }

    suspend fun read(): String? = withContext(Dispatchers.IO) {
        val encrypted = application.routeCredentials.data.first()[credential]
            ?: return@withContext null
        RouteKeyCipher.decrypt(encrypted)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        application.routeCredentials.edit { it.remove(credential) }
    }
}

internal object RouteKeyCipher {
    private const val ALIAS = "ev_cockpit_ors_aes_gcm_v1"
    private const val PREFIX = "v1:"
    private const val IV_BYTES = 12

    @Synchronized
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(value: String): String {
        require(value.startsWith(PREFIX)) { "Şifreli veri sürümü tanınmıyor." }
        val payload = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
        require(payload.size > IV_BYTES + 16) { "Şifreli anahtar verisi geçersiz." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, payload.copyOfRange(0, IV_BYTES)),
        )
        return cipher.doFinal(payload.copyOfRange(IV_BYTES, payload.size))
            .toString(Charsets.UTF_8)
    }
}
