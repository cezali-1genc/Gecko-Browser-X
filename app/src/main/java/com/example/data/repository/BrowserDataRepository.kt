package com.example.data.repository

import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import com.example.data.local.BrowserExtensionEntity
import com.example.data.local.DictionaryDao
import com.example.data.local.DictionaryEntryEntity
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.local.DownloadStatus
import com.example.data.local.ExtensionDao
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.data.local.PermissionDecision
import com.example.data.local.ReadingListDao
import com.example.data.local.ReadingListEntity
import com.example.data.local.SitePermissionDao
import com.example.data.local.SitePermissionEntity
import com.example.data.local.SpeedDialDao
import com.example.data.local.SpeedDialEntity
import com.example.engine.UrlUtils
import kotlinx.coroutines.flow.Flow

class BrowserDataRepository(
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao,
    private val sitePermissionDao: SitePermissionDao,
    private val speedDialDao: SpeedDialDao,
    private val readingListDao: ReadingListDao,
    private val extensionDao: ExtensionDao,
    private val dictionaryDao: DictionaryDao
) {
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()
    val history: Flow<List<HistoryEntity>> = historyDao.observeAll()
    val downloads: Flow<List<DownloadEntity>> = downloadDao.observeAll()
    val sitePermissions: Flow<List<SitePermissionEntity>> = sitePermissionDao.observeAll()
    val speedDials: Flow<List<SpeedDialEntity>> = speedDialDao.observeAll()
    val readingList: Flow<List<ReadingListEntity>> = readingListDao.observeAll()
    val extensions: Flow<List<BrowserExtensionEntity>> = extensionDao.observeAll()
    val dictionaryEntries: Flow<List<DictionaryEntryEntity>> = dictionaryDao.observeAll()

    fun observeIsBookmarked(url: String): Flow<Boolean> = bookmarkDao.observeIsBookmarked(url)

    suspend fun seedDefaultsIfEmpty() {
        if (speedDialDao.count() == 0) {
            speedDialDao.insertAll(
                listOf(
                    SpeedDialEntity(
                        title = "DuckDuckGo",
                        url = "https://duckduckgo.com",
                        accentHex = 0xFFDE5833,
                        sortOrder = 0
                    ),
                    SpeedDialEntity(
                        title = "Wikipedia",
                        url = "https://www.wikipedia.org",
                        accentHex = 0xFF3366CC,
                        sortOrder = 1
                    ),
                    SpeedDialEntity(
                        title = "Mozilla",
                        url = "https://www.mozilla.org",
                        accentHex = 0xFF00B8D4,
                        sortOrder = 2
                    ),
                    SpeedDialEntity(
                        title = "GitHub",
                        url = "https://github.com",
                        accentHex = 0xFF6366F1,
                        sortOrder = 3
                    )
                )
            )
        }
        if (extensionDao.count() == 0) {
            extensionDao.insertAll(
                listOf(
                    BrowserExtensionEntity(
                        extensionId = "modlu-shield@modlu1apk.browserx",
                        name = "ModLU Kozmetik & İzleyici Kalkanı",
                        description = "GeckoView WebExtension üzerinden rahatsız edici reklam alanlarını ve takip betiklerini azaltır.",
                        version = "1.0.0",
                        assetPath = "resource://android/assets/extensions/modlu_shield/",
                        isBuiltIn = true,
                        isEnabled = true
                    ),
                    BrowserExtensionEntity(
                        extensionId = "modlu-dark@modlu1apk.browserx",
                        name = "ModLU Gece Okuma Stili",
                        description = "Parlak web sayfalarına koyu renk şeması iyileştirmesi uygular.",
                        version = "1.0.0",
                        assetPath = "resource://android/assets/extensions/modlu_dark_reader/",
                        isBuiltIn = true,
                        isEnabled = false
                    )
                )
            )
        }
    }

    suspend fun toggleBookmark(title: String, url: String): Boolean {
        if (UrlUtils.isHomeUri(url)) return false
        val existing = bookmarkDao.findByUrl(url)
        return if (existing != null) {
            bookmarkDao.deleteById(existing.id)
            false
        } else {
            bookmarkDao.upsert(
                BookmarkEntity(
                    title = title.ifBlank { UrlUtils.formatDisplayHost(url) },
                    url = url
                )
            )
            true
        }
    }

    suspend fun saveBookmark(title: String, url: String, folder: String = "Genel") {
        if (!UrlUtils.isSafeExternalNavigationUri(url)) return
        bookmarkDao.upsert(
            BookmarkEntity(
                title = title.ifBlank { UrlUtils.formatDisplayHost(url) },
                url = url,
                folder = folder
            )
        )
    }

    suspend fun updateBookmark(bookmark: BookmarkEntity) = bookmarkDao.update(bookmark)
    suspend fun deleteBookmark(id: Long) = bookmarkDao.deleteById(id)

    suspend fun recordHistory(title: String, url: String) {
        if (UrlUtils.isHomeUri(url) || !UrlUtils.isSafeExternalNavigationUri(url)) return
        historyDao.insert(
            HistoryEntity(
                title = title.ifBlank { UrlUtils.formatDisplayHost(url) },
                url = url,
                visitedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) = historyDao.deleteById(id)

    suspend fun clearHistoryLastHours(hours: Int) {
        val cutoff = System.currentTimeMillis() - hours * 3_600_000L
        historyDao.deleteSince(cutoff)
    }

    suspend fun clearAllHistory() = historyDao.clearAll()

    suspend fun addSpeedDial(title: String, url: String) {
        if (!UrlUtils.isSafeExternalNavigationUri(url)) return
        val palette = listOf(0xFFFF1B2D, 0xFF00E5FF, 0xFF7C4DFF, 0xFF10B981, 0xFFF59E0B)
        val color = palette[kotlin.math.abs(url.hashCode()) % palette.size]
        speedDialDao.insert(
            SpeedDialEntity(
                title = title.ifBlank { UrlUtils.formatDisplayHost(url) },
                url = url,
                accentHex = color
            )
        )
    }

    suspend fun updateSpeedDial(dial: SpeedDialEntity) = speedDialDao.update(dial)
    suspend fun deleteSpeedDial(id: Long) = speedDialDao.deleteById(id)

    suspend fun searchBookmarks(query: String): List<BookmarkEntity> =
        if (query.isBlank()) emptyList() else bookmarkDao.search(query)

    suspend fun searchHistory(query: String): List<HistoryEntity> =
        if (query.isBlank()) emptyList() else historyDao.search(query)

    suspend fun getPermissionDecision(origin: String, permissionType: String): PermissionDecision {
        return sitePermissionDao.findRule(origin, permissionType)?.decision ?: PermissionDecision.ASK
    }

    suspend fun setPermissionDecision(
        origin: String,
        permissionType: String,
        decision: PermissionDecision
    ) {
        if (origin.isBlank()) return
        sitePermissionDao.upsert(
            SitePermissionEntity(
                origin = origin,
                permissionType = permissionType,
                decision = decision,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSitePermission(id: Long) = sitePermissionDao.deleteById(id)
    suspend fun clearAllSitePermissions() = sitePermissionDao.clearAll()

    suspend fun insertDownload(entity: DownloadEntity): Long = downloadDao.insert(entity)
    suspend fun findDownloadById(id: Long): DownloadEntity? = downloadDao.findById(id)
    suspend fun getActiveDownloads(): List<DownloadEntity> = downloadDao.getActiveDownloads()
    suspend fun findDownloadBySystemId(systemId: Long): DownloadEntity? =
        downloadDao.findBySystemId(systemId)

    suspend fun updateDownloadProgress(
        id: Long,
        status: DownloadStatus,
        downloadedBytes: Long,
        totalBytes: Long,
        localUri: String?,
        failureReason: String? = null,
        supportsResume: Boolean = true
    ) {
        downloadDao.updateProgress(
            id = id,
            status = status,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            localUri = localUri,
            failureReason = failureReason,
            supportsResume = supportsResume
        )
    }

    suspend fun deleteDownload(id: Long) = downloadDao.deleteById(id)
    suspend fun clearDownloads() = downloadDao.clearAll()

    suspend fun saveToReadingList(title: String, url: String, excerpt: String = "") {
        if (!UrlUtils.isSafeExternalNavigationUri(url)) return
        readingListDao.upsert(
            ReadingListEntity(
                title = title.ifBlank { UrlUtils.formatDisplayHost(url) },
                url = url,
                excerpt = excerpt
            )
        )
    }

    suspend fun toggleReadingListRead(id: Long, isRead: Boolean) = readingListDao.setRead(id, isRead)
    suspend fun deleteReadingListItem(id: Long) = readingListDao.deleteById(id)
    suspend fun clearReadingList() = readingListDao.clearAll()

    suspend fun setExtensionEnabled(extensionId: String, enabled: Boolean) =
        extensionDao.setEnabled(extensionId, enabled)

    suspend fun addCustomFilterExtension(name: String, description: String, domains: String) {
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "custom" }
        val extId = "custom-$slug-${System.currentTimeMillis()}@modlu1apk.browserx"
        extensionDao.upsert(
            BrowserExtensionEntity(
                extensionId = extId,
                name = name.trim(),
                description = description.trim(),
                version = "1.0.0",
                assetPath = "",
                isBuiltIn = false,
                isEnabled = true,
                customRuleDomains = domains.trim()
            )
        )
    }

    suspend fun deleteCustomExtension(extensionId: String) = extensionDao.deleteCustom(extensionId)

    suspend fun addDictionaryTerm(term: String, trDef: String, enDef: String) {
        if (term.isBlank()) return
        dictionaryDao.upsert(
            DictionaryEntryEntity(
                term = term.trim(),
                trDef = trDef.trim(),
                enDef = enDef.trim(),
                isUserAdded = true
            )
        )
    }

    suspend fun deleteDictionaryTerm(id: Long) = dictionaryDao.deleteById(id)
}
