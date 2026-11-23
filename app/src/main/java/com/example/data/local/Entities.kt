package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val folder: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "history",
    indices = [Index(value = ["visitedAt"]), Index(value = ["url"])]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    QUEUED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemDownloadId: Long = -1L,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val localUri: String? = null,
    val failureReason: String? = null,
    val supportsResume: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class PermissionDecision {
    ASK,
    ALLOW,
    BLOCK
}

@Entity(
    tableName = "site_permissions",
    indices = [Index(value = ["origin", "permissionType"], unique = true)]
)
data class SitePermissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val origin: String,
    val permissionType: String,
    val decision: PermissionDecision,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "speed_dials")
data class SpeedDialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val accentHex: Long,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "saved_credentials",
    indices = [Index(value = ["origin", "username"], unique = true), Index(value = ["origin"])]
)
data class SavedCredentialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val origin: String,
    val host: String,
    val username: String,
    val encryptedPassword: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reading_list",
    indices = [Index(value = ["url"], unique = true)]
)
data class ReadingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val excerpt: String = "",
    val isRead: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "browser_extensions",
    indices = [Index(value = ["extensionId"], unique = true)]
)
data class BrowserExtensionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val extensionId: String,
    val name: String,
    val description: String,
    val version: String,
    val assetPath: String,
    val isBuiltIn: Boolean = true,
    val isEnabled: Boolean = true,
    val customRuleDomains: String = "",
    val installedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dictionary_entries",
    indices = [Index(value = ["term"], unique = true)]
)
data class DictionaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    val trDef: String,
    val enDef: String,
    val isUserAdded: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
