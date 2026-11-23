package com.example.data.preferences

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore AES-256-GCM encryption helper for sensitive
 * local browser configuration values and saved website credentials.
 */
class SecureVault(private val context: Context) {

    private val prefsName = "browserx_secure_vault"
    private val keyAlias = "browserx_aes_master_key"

    fun isAndroidKeyStoreActive(): Boolean {
        return runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            true
        }.getOrDefault(false)
    }

    fun putEncryptedString(key: String, value: String) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val encrypted = encryptString(value)
        if (encrypted != null) {
            prefs.edit().putString(key, encrypted).apply()
        }
    }

    fun getEncryptedString(key: String): String? {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null) ?: return null
        return decryptString(raw)
    }

    fun remove(key: String) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().remove(key).apply()
    }

    fun encryptString(plainText: String): String? {
        val secretKey = getOrCreateKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val ivEncoded = Base64.encodeToString(iv, Base64.NO_WRAP)
            val dataEncoded = Base64.encodeToString(cipherText, Base64.NO_WRAP)
            "$ivEncoded:$dataEncoded"
        }.getOrNull()
    }

    fun decryptString(payload: String): String? {
        val parts = payload.split(":")
        if (parts.size != 2) return null
        val secretKey = getOrCreateKey() ?: return null
        return runCatching {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey? {
        val keystoreKey = runCatching {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val existing = keyStore.getKey(keyAlias, null) as? SecretKey
            if (existing != null) return existing

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }.getOrNull()

        if (keystoreKey != null) return keystoreKey

        // Robolectric JVM lacks the AndroidKeyStore daemon; allow ephemeral AES-256 test key ONLY under Robolectric.
        if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
            return robolectricTestKey ?: synchronized(SecureVault::class.java) {
                robolectricTestKey ?: KeyGenerator.getInstance("AES").apply { init(256) }
                    .generateKey()
                    .also { robolectricTestKey = it }
            }
        }
        return null
    }

    companion object {
        @Volatile
        private var robolectricTestKey: SecretKey? = null
    }
}
