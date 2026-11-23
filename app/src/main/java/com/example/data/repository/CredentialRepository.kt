package com.example.data.repository

import android.content.Context
import com.example.data.local.CredentialDao
import com.example.data.local.SavedCredentialEntity
import com.example.data.preferences.SecureVault
import com.example.engine.UrlUtils
import kotlinx.coroutines.flow.Flow

data class DecryptedCredential(
    val id: Long,
    val origin: String,
    val host: String,
    val username: String,
    val password: String,
    val updatedAt: Long
)

class CredentialRepository(
    context: Context,
    private val credentialDao: CredentialDao
) {
    private val secureVault = SecureVault(context)

    val allCredentials: Flow<List<SavedCredentialEntity>> = credentialDao.observeAll()

    suspend fun saveCredential(
        originOrUrl: String,
        username: String,
        plainPassword: String
    ): Boolean {
        val normalizedOrigin = UrlUtils.extractOrigin(originOrUrl).ifBlank { originOrUrl.trim() }
        if (normalizedOrigin.isBlank() || username.isBlank() || plainPassword.isEmpty()) return false
        val encrypted = secureVault.encryptString(plainPassword) ?: return false
        val host = UrlUtils.extractHost(normalizedOrigin).ifBlank { normalizedOrigin }
        val now = System.currentTimeMillis()
        credentialDao.upsert(
            SavedCredentialEntity(
                origin = normalizedOrigin,
                host = host,
                username = username.trim(),
                encryptedPassword = encrypted,
                createdAt = now,
                updatedAt = now
            )
        )
        return true
    }

    suspend fun getCredentialsForOrigin(
        originOrUrl: String,
        isPrivateSession: Boolean
    ): List<DecryptedCredential> {
        if (isPrivateSession) return emptyList()
        val origin = UrlUtils.extractOrigin(originOrUrl).ifBlank { return emptyList() }
        return credentialDao.findByOrigin(origin).mapNotNull { entity ->
            val decrypted = decryptPassword(entity) ?: return@mapNotNull null
            DecryptedCredential(
                id = entity.id,
                origin = entity.origin,
                host = entity.host,
                username = entity.username,
                password = decrypted,
                updatedAt = entity.updatedAt
            )
        }
    }

    suspend fun updateCredential(
        id: Long,
        originOrUrl: String,
        username: String,
        plainPassword: String
    ): Boolean {
        val normalizedOrigin = UrlUtils.extractOrigin(originOrUrl).ifBlank { originOrUrl.trim() }
        if (normalizedOrigin.isBlank() || username.isBlank() || plainPassword.isEmpty()) return false
        val encrypted = secureVault.encryptString(plainPassword) ?: return false
        val host = UrlUtils.extractHost(normalizedOrigin).ifBlank { normalizedOrigin }
        val now = System.currentTimeMillis()
        credentialDao.update(
            SavedCredentialEntity(
                id = id,
                origin = normalizedOrigin,
                host = host,
                username = username.trim(),
                encryptedPassword = encrypted,
                createdAt = now,
                updatedAt = now
            )
        )
        return true
    }

    fun isHardwareKeystoreAvailable(): Boolean = secureVault.isAndroidKeyStoreActive()

    fun decryptPassword(entity: SavedCredentialEntity): String? {
        return secureVault.decryptString(entity.encryptedPassword)
    }

    suspend fun deleteCredential(id: Long) = credentialDao.deleteById(id)

    suspend fun clearAllCredentials() = credentialDao.clearAll()
}
