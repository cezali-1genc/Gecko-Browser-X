package com.example.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.BrowserXApplication
import com.example.MainActivity
import com.example.R
import com.example.data.local.DownloadEntity
import com.example.data.local.DownloadStatus
import com.example.engine.UrlUtils
import com.example.i18n.LocaleHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class BrowserDownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "browserx_downloads_channel"
        const val SUMMARY_NOTIF_ID = 7001

        const val ACTION_START = "com.example.download.START"
        const val ACTION_PAUSE = "com.example.download.PAUSE"
        const val ACTION_RESUME = "com.example.download.RESUME"
        const val ACTION_CANCEL = "com.example.download.CANCEL"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        fun enqueueAction(context: Context, action: String, downloadId: Long) {
            val intent = Intent(context, BrowserDownloadService::class.java).apply {
                this.action = action
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && action in setOf(ACTION_START, ACTION_RESUME)) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val pausedFlags = ConcurrentHashMap<Long, Boolean>()
    private val cancelledFlags = ConcurrentHashMap<Long, Boolean>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Android 15+ FGS timeout compliance: pause active jobs gracefully and stopSelf().
        activeJobs.keys.forEach { pausedFlags[it] = true }
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1L) ?: -1L
        when (intent?.action) {
            ACTION_START, ACTION_RESUME -> {
                if (downloadId > 0L) {
                    startForeground(SUMMARY_NOTIF_ID, buildForegroundNotification(downloadId, "...", 0, false))
                    launchDownload(downloadId)
                }
            }
            ACTION_PAUSE -> {
                if (downloadId > 0L) {
                    pausedFlags[downloadId] = true
                    activeJobs[downloadId]?.cancel()
                }
            }
            ACTION_CANCEL -> {
                if (downloadId > 0L) {
                    cancelledFlags[downloadId] = true
                    activeJobs[downloadId]?.cancel()
                    serviceScope.launch {
                        markCancelled(downloadId)
                        stopIfIdle()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun launchDownload(downloadId: Long) {
        if (activeJobs[downloadId]?.isActive == true) return
        pausedFlags[downloadId] = false
        cancelledFlags[downloadId] = false

        val job = serviceScope.launch {
            val container = (application as BrowserXApplication).container
            val repo = container.dataRepository
            val entity = repo.findDownloadById(downloadId)
            if (entity == null || !UrlUtils.isSafeExternalNavigationUri(entity.url)) {
                stopIfIdle()
                return@launch
            }

            val partDir = File(filesDir, "downloads_part").apply { mkdirs() }
            val partFile = File(partDir, "dl_${entity.id}.part")
            var downloaded = if (partFile.exists()) partFile.length() else 0L
            var totalBytes = entity.totalBytes

            try {
                repo.updateDownloadProgress(
                    id = entity.id,
                    status = DownloadStatus.IN_PROGRESS,
                    downloadedBytes = downloaded,
                    totalBytes = totalBytes,
                    localUri = entity.localUri,
                    failureReason = null,
                    supportsResume = true
                )

                val reqBuilder = Request.Builder().url(entity.url)
                if (downloaded > 0L) {
                    reqBuilder.header("Range", "bytes=$downloaded-")
                }

                httpClient.newCall(reqBuilder.build()).execute().use { response ->
                    val code = response.code
                    if (!response.isSuccessful && code != 206) {
                        repo.updateDownloadProgress(
                            id = entity.id,
                            status = DownloadStatus.FAILED,
                            downloadedBytes = downloaded,
                            totalBytes = totalBytes,
                            localUri = null,
                            failureReason = "HTTP $code",
                            supportsResume = false
                        )
                        postStatusNotification(entity.id, entity.fileName, isError = true)
                        return@use
                    }

                    val body = response.body ?: throw IllegalStateException("Empty response")
                    val appendMode = (code == 206 && downloaded > 0L)
                    if (!appendMode) {
                        downloaded = 0L
                        if (partFile.exists()) partFile.delete()
                    }

                    val contentLen = body.contentLength()
                    totalBytes = when {
                        appendMode && contentLen > 0L -> downloaded + contentLen
                        contentLen > 0L -> contentLen
                        else -> entity.totalBytes
                    }
                    val supportsResume = code == 206 ||
                        response.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true

                    var lastUpdateAt = 0L
                    body.byteStream().use { input ->
                        FileOutputStream(partFile, appendMode).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                if (pausedFlags[downloadId] == true || cancelledFlags[downloadId] == true) {
                                    throw CancellationException("User paused or cancelled")
                                }
                                output.write(buffer, 0, read)
                                downloaded += read
                                val now = System.currentTimeMillis()
                                if (now - lastUpdateAt >= 350L) {
                                    lastUpdateAt = now
                                    repo.updateDownloadProgress(
                                        id = entity.id,
                                        status = DownloadStatus.IN_PROGRESS,
                                        downloadedBytes = downloaded,
                                        totalBytes = totalBytes,
                                        localUri = null,
                                        supportsResume = supportsResume
                                    )
                                    val pct = if (totalBytes > 0L) ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100) else 0
                                    updateNotification(entity.id, buildForegroundNotification(entity.id, entity.fileName, pct, false))
                                }
                            }
                            output.flush()
                        }
                    }

                    val finalUri = exportCompletedFile(partFile, entity)
                    partFile.delete()

                    repo.updateDownloadProgress(
                        id = entity.id,
                        status = DownloadStatus.COMPLETED,
                        downloadedBytes = downloaded,
                        totalBytes = if (totalBytes > 0L) totalBytes else downloaded,
                        localUri = finalUri?.toString(),
                        failureReason = null,
                        supportsResume = supportsResume
                    )
                    postStatusNotification(entity.id, entity.fileName, isError = false)
                }
            } catch (_: CancellationException) {
                if (cancelledFlags[downloadId] == true) {
                    partFile.delete()
                    markCancelled(downloadId)
                } else if (pausedFlags[downloadId] == true) {
                    repo.updateDownloadProgress(
                        id = entity.id,
                        status = DownloadStatus.PAUSED,
                        downloadedBytes = partFile.length(),
                        totalBytes = totalBytes,
                        localUri = null,
                        failureReason = null,
                        supportsResume = true
                    )
                    updateNotification(entity.id, buildForegroundNotification(entity.id, entity.fileName, 0, true))
                }
            } catch (t: Throwable) {
                repo.updateDownloadProgress(
                    id = entity.id,
                    status = DownloadStatus.FAILED,
                    downloadedBytes = partFile.length(),
                    totalBytes = totalBytes,
                    localUri = null,
                    failureReason = t.localizedMessage ?: t::class.java.simpleName,
                    supportsResume = partFile.length() > 0L
                )
                postStatusNotification(entity.id, entity.fileName, isError = true)
            } finally {
                activeJobs.remove(downloadId)
                stopIfIdle()
            }
        }
        activeJobs[downloadId] = job
    }

    private suspend fun markCancelled(downloadId: Long) {
        val repo = (application as BrowserXApplication).container.dataRepository
        val existing = repo.findDownloadById(downloadId) ?: return
        File(File(filesDir, "downloads_part"), "dl_${existing.id}.part").delete()
        repo.updateDownloadProgress(
            id = existing.id,
            status = DownloadStatus.CANCELLED,
            downloadedBytes = existing.downloadedBytes,
            totalBytes = existing.totalBytes,
            localUri = null
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(notifIdFor(downloadId))
    }

    private fun exportCompletedFile(partFile: File, entity: DownloadEntity): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, entity.fileName)
                put(MediaStore.Downloads.MIME_TYPE, entity.mimeType.ifBlank { "application/octet-stream" })
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = contentResolver
            val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (itemUri != null) {
                resolver.openOutputStream(itemUri)?.use { out ->
                    partFile.inputStream().use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            itemUri
        } else {
            val outDir = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir, "").apply {
                mkdirs()
            }
            val dest = File(outDir, entity.fileName)
            partFile.inputStream().use { input ->
                FileOutputStream(dest).use { out -> input.copyTo(out) }
            }
            runCatching {
                FileProvider.getUriForFile(this, "${packageName}.fileprovider", dest)
            }.getOrElse { Uri.fromFile(dest) }
        }
    }

    private suspend fun localizedStringsContext(): Context {
        val container = (application as BrowserXApplication).container
        val lang = runCatching { container.settingsRepository.settingsFlow.first().appLanguage }
            .getOrDefault(LocaleHelper.DEFAULT_TAG)
        return LocaleHelper.localizedContext(this, lang)
    }

    private fun buildForegroundNotification(
        downloadId: Long,
        fileName: String,
        percent: Int,
        isPaused: Boolean
    ): Notification {
        val container = (application as BrowserXApplication).container
        val lang = kotlinx.coroutines.runBlocking {
            runCatching { container.settingsRepository.settingsFlow.first().appLanguage }
                .getOrDefault(LocaleHelper.DEFAULT_TAG)
        }
        val loc = LocaleHelper.localizedContext(this, lang)

        val openIntent = PendingIntent.getActivity(
            this,
            downloadId.toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleAction = if (isPaused) {
            val resumePi = PendingIntent.getService(
                this,
                (downloadId * 10 + 1).toInt(),
                Intent(this, BrowserDownloadService::class.java).apply {
                    action = ACTION_RESUME
                    putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            NotificationCompat.Action.Builder(0, loc.getString(R.string.dl_resume), resumePi).build()
        } else {
            val pausePi = PendingIntent.getService(
                this,
                (downloadId * 10 + 2).toInt(),
                Intent(this, BrowserDownloadService::class.java).apply {
                    action = ACTION_PAUSE
                    putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            NotificationCompat.Action.Builder(0, loc.getString(R.string.dl_pause), pausePi).build()
        }

        val cancelPi = PendingIntent.getService(
            this,
            (downloadId * 10 + 3).toInt(),
            Intent(this, BrowserDownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cancelAction = NotificationCompat.Action.Builder(0, loc.getString(R.string.dl_cancel), cancelPi).build()

        val title = if (isPaused) {
            loc.getString(R.string.dl_notif_paused, fileName)
        } else {
            loc.getString(R.string.dl_notif_downloading, percent, fileName)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(loc.getString(R.string.app_name))
            .setContentText(title)
            .setProgress(100, percent, percent == 0 && !isPaused)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(toggleAction)
            .addAction(cancelAction)
            .build()
    }

    private suspend fun postStatusNotification(downloadId: Long, fileName: String, isError: Boolean) {
        val loc = localizedStringsContext()
        val msg = if (isError) {
            loc.getString(R.string.dl_notif_failed, fileName)
        } else {
            loc.getString(R.string.dl_notif_complete, fileName)
        }
        val openIntent = PendingIntent.getActivity(
            this,
            downloadId.toInt() + 1000,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (isError) android.R.drawable.stat_notify_error else android.R.drawable.stat_sys_download_done)
            .setContentTitle(loc.getString(R.string.app_name))
            .setContentText(msg)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        getSystemService(NotificationManager::class.java).notify(notifIdFor(downloadId), notif)
    }

    private fun updateNotification(downloadId: Long, notification: Notification) {
        getSystemService(NotificationManager::class.java).notify(notifIdFor(downloadId), notification)
    }

    private fun stopIfIdle() {
        if (activeJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun notifIdFor(downloadId: Long): Int = 8000 + (downloadId % 1000).toInt()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.dl_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
