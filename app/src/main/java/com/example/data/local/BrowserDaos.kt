package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    fun observeIsBookmarked(url: String): Flow<Boolean>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY createdAt DESC LIMIT 8")
    suspend fun search(query: String): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity)

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC LIMIT 8")
    suspend fun search(query: String): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE visitedAt >= :sinceTimestamp")
    suspend fun deleteSince(sinceTimestamp: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Query("SELECT * FROM downloads WHERE systemDownloadId = :systemId LIMIT 1")
    suspend fun findBySystemId(systemId: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'IN_PROGRESS')")
    suspend fun getActiveDownloads(): List<DownloadEntity>

    @Query("UPDATE downloads SET status = :status, downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, localUri = :localUri, failureReason = :failureReason, supportsResume = :supportsResume WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        status: DownloadStatus,
        downloadedBytes: Long,
        totalBytes: Long,
        localUri: String?,
        failureReason: String? = null,
        supportsResume: Boolean = true
    )

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}

@Dao
interface SitePermissionDao {
    @Query("SELECT * FROM site_permissions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SitePermissionEntity>>

    @Query("SELECT * FROM site_permissions WHERE origin = :origin AND permissionType = :permissionType LIMIT 1")
    suspend fun findRule(origin: String, permissionType: String): SitePermissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: SitePermissionEntity)

    @Query("DELETE FROM site_permissions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM site_permissions")
    suspend fun clearAll()
}

@Dao
interface SpeedDialDao {
    @Query("SELECT * FROM speed_dials ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<SpeedDialEntity>>

    @Query("SELECT COUNT(*) FROM speed_dials")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dials: List<SpeedDialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dial: SpeedDialEntity)

    @Update
    suspend fun update(dial: SpeedDialEntity)

    @Query("DELETE FROM speed_dials WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CredentialDao {
    @Query("SELECT * FROM saved_credentials ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SavedCredentialEntity>>

    @Query("SELECT * FROM saved_credentials WHERE origin = :origin ORDER BY updatedAt DESC")
    suspend fun findByOrigin(origin: String): List<SavedCredentialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(credential: SavedCredentialEntity): Long

    @Update
    suspend fun update(credential: SavedCredentialEntity)

    @Query("DELETE FROM saved_credentials WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_credentials")
    suspend fun clearAll()
}

@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<ReadingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReadingListEntity)

    @Query("UPDATE reading_list SET isRead = :isRead WHERE id = :id")
    suspend fun setRead(id: Long, isRead: Boolean)

    @Query("DELETE FROM reading_list WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reading_list")
    suspend fun clearAll()
}

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM browser_extensions ORDER BY installedAt ASC")
    fun observeAll(): Flow<List<BrowserExtensionEntity>>

    @Query("SELECT COUNT(*) FROM browser_extensions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BrowserExtensionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BrowserExtensionEntity)

    @Query("UPDATE browser_extensions SET isEnabled = :enabled WHERE extensionId = :extensionId")
    suspend fun setEnabled(extensionId: String, enabled: Boolean)

    @Query("DELETE FROM browser_extensions WHERE extensionId = :extensionId AND isBuiltIn = 0")
    suspend fun deleteCustom(extensionId: String)
}

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary_entries ORDER BY term ASC")
    fun observeAll(): Flow<List<DictionaryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DictionaryEntryEntity)

    @Query("DELETE FROM dictionary_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
