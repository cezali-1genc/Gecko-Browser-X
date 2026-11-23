package com.example.data.repository

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.download.BrowserDownloadService
import com.example.data.local.DownloadEntity
import com.example.data.local.DownloadStatus
import com.example.engine.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadBridge(
    private val context: Context,
    private val dataRepository: BrowserDataRepository
) {
    private val systemDownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    suspend fun enqueueDownload(
        url: String,
        suggestedFileName: String,
        mimeType: String,
        contentLength: Long
    ): Long = withContext(Dispatchers.IO) {
        if (!UrlUtils.isSafeExternalNavigationUri(url)) {
            return@withContext -1L
        }

        val safeName = UrlUtils.sanitizeDownloadFilename(url, null, mimeType)
            .let { if (suggestedFileName.isNotBlank()) suggestedFileName else it }

        val entity = DownloadEntity(
            systemDownloadId = -1L,
            fileName = safeName,
            url = url,
            mimeType = mimeType.ifBlank { "application/octet-stream" },
            totalBytes = contentLength,
            downloadedBytes = 0L,
            status = DownloadStatus.QUEUED,
            supportsResume = true
        )
        val rowId = dataRepository.insertDownload(entity)
        runCatching {
            BrowserDownloadService.enqueueAction(context, BrowserDownloadService.ACTION_START, rowId)
        }
        rowId
    }

    fun pauseDownload(downloadId: Long) {
        runCatching {
            BrowserDownloadService.enqueueAction(context, BrowserDownloadService.ACTION_PAUSE, downloadId)
        }
    }

    fun resumeDownload(downloadId: Long) {
        runCatching {
            BrowserDownloadService.enqueueAction(context, BrowserDownloadService.ACTION_RESUME, downloadId)
        }
    }

    fun cancelDownload(item: DownloadEntity) {
        if (item.systemDownloadId > 0L) {
            runCatching { systemDownloadManager?.remove(item.systemDownloadId) }
        }
        runCatching {
            BrowserDownloadService.enqueueAction(context, BrowserDownloadService.ACTION_CANCEL, item.id)
        }
    }

    suspend fun refreshActiveDownloads() = withContext(Dispatchers.IO) {
        val active = dataRepository.getActiveDownloads()
        for (item in active) {
            if (item.systemDownloadId <= 0L) continue
            handleSystemDownloadCompleted(item.systemDownloadId)
        }
    }

    suspend fun handleSystemDownloadCompleted(systemDownloadId: Long) = withContext(Dispatchers.IO) {
        val manager = systemDownloadManager ?: return@withContext
        val entity = dataRepository.findDownloadBySystemId(systemDownloadId) ?: return@withContext
        val uri = runCatching { manager.getUriForDownloadedFile(systemDownloadId) }.getOrNull()
        if (uri != null) {
            dataRepository.updateDownloadProgress(
                id = entity.id,
                status = DownloadStatus.COMPLETED,
                downloadedBytes = entity.totalBytes.coerceAtLeast(entity.downloadedBytes),
                totalBytes = entity.totalBytes,
                localUri = uri.toString()
            )
        }
    }

    fun openDownloadedFile(item: DownloadEntity): Boolean {
        val targetUri = item.localUri?.let { Uri.parse(it) }
            ?: if (item.systemDownloadId > 0L) {
                runCatching { systemDownloadManager?.getUriForDownloadedFile(item.systemDownloadId) }.getOrNull()
            } else {
                null
            } ?: return false

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(targetUri, item.mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
