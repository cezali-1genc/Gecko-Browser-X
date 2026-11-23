package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.DownloadEntity
import com.example.data.local.DownloadStatus
import com.example.engine.UrlUtils
import com.example.i18n.LocaleHelper
import com.example.ui.state.BrowserViewModel
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: BrowserViewModel,
    onOpenPdfUri: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val locale = remember(settings.appLanguage) { LocaleHelper.resolveLocale(settings.appLanguage) }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.downloads_title, downloads.size)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (downloads.isNotEmpty()) {
                        TextButton(
                            onClick = viewModel::clearDownloadHistory,
                            modifier = Modifier.testTag("clear_downloads_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (downloads.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.downloads_empty_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.downloads_empty_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadItemCard(
                        item = item,
                        locale = locale,
                        onOpen = {
                            val isPdf = item.mimeType.contains("pdf", ignoreCase = true) ||
                                item.fileName.endsWith(".pdf", ignoreCase = true)
                            if (isPdf && !item.localUri.isNullOrBlank()) {
                                onOpenPdfUri(item.localUri)
                            } else {
                                viewModel.openDownload(item)
                            }
                        },
                        onPause = { viewModel.pauseDownload(item.id) },
                        onResume = { viewModel.resumeDownload(item.id) },
                        onCancel = { viewModel.cancelDownload(item) },
                        onRetry = { viewModel.retryDownload(item) },
                        onDelete = { viewModel.deleteDownload(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadEntity,
    locale: java.util.Locale,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, tint, statusLabel) = when (item.status) {
        DownloadStatus.COMPLETED -> Triple(Icons.Filled.CloudDone, VerifiedEmerald, stringResource(R.string.dl_completed))
        DownloadStatus.IN_PROGRESS -> Triple(Icons.Filled.CloudDownload, MaterialTheme.colorScheme.primary, stringResource(R.string.dl_downloading))
        DownloadStatus.PAUSED -> Triple(Icons.Filled.Pause, WarningAmber, stringResource(R.string.dl_paused))
        DownloadStatus.QUEUED -> Triple(Icons.Filled.CloudDownload, MaterialTheme.colorScheme.secondary, stringResource(R.string.dl_queued))
        DownloadStatus.FAILED -> Triple(Icons.Filled.ErrorOutline, DangerCoral, stringResource(R.string.dl_failed))
        DownloadStatus.CANCELLED -> Triple(Icons.Filled.ErrorOutline, MaterialTheme.colorScheme.onSurfaceVariant, stringResource(R.string.dl_cancelled))
    }

    val extBadge = item.fileName.substringAfterLast('.', "").uppercase().take(5).ifBlank { "FILE" }
    val percent = if (item.totalBytes > 0L) {
        (item.downloadedBytes.toFloat() / item.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.status == DownloadStatus.COMPLETED, onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = extBadge,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    val sizeInfo = if (item.totalBytes > 0L) {
                        "${UrlUtils.formatBytes(item.downloadedBytes)} / ${UrlUtils.formatBytes(item.totalBytes)}"
                    } else {
                        UrlUtils.formatBytes(item.downloadedBytes)
                    }
                    Text(
                        text = "$statusLabel • $sizeInfo • ${UrlUtils.formatDisplayHost(item.url)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (item.status == DownloadStatus.IN_PROGRESS || item.status == DownloadStatus.PAUSED) {
                Spacer(Modifier.height(8.dp))
                if (item.totalBytes > 0L) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { percent },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = LocaleHelper.formatPercent(percent, locale),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else if (item.status == DownloadStatus.IN_PROGRESS) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            if (!item.failureReason.isNullOrBlank() && item.status == DownloadStatus.FAILED) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.failureReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = DangerCoral
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (item.status) {
                    DownloadStatus.IN_PROGRESS, DownloadStatus.QUEUED -> {
                        OutlinedButton(onClick = onPause, modifier = Modifier.testTag("pause_dl_${item.id}")) {
                            Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.dl_pause))
                        }
                        TextButton(onClick = onCancel, modifier = Modifier.testTag("cancel_dl_${item.id}")) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.dl_cancel))
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        OutlinedButton(onClick = onResume, modifier = Modifier.testTag("resume_dl_${item.id}")) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.dl_resume))
                        }
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.dl_cancel))
                        }
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        OutlinedButton(onClick = onRetry) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.dl_retry))
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        OutlinedButton(onClick = onOpen) {
                            Text(stringResource(R.string.dl_open))
                        }
                    }
                }
            }
        }
    }
}
