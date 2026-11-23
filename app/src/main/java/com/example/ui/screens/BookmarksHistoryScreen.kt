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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.ReadingListEntity
import com.example.engine.UrlUtils
import com.example.i18n.AppTranslations
import com.example.i18n.LocaleHelper
import com.example.ui.state.BrowserViewModel
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksHistoryScreen(
    viewModel: BrowserViewModel,
    initialTab: Int,
    onOpenUrl: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val readingList by viewModel.readingList.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val strings = remember(settings.appLanguage) { AppTranslations.forLang(settings.appLanguage) }
    val locale = remember(settings.appLanguage) { LocaleHelper.resolveLocale(settings.appLanguage) }

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var searchQuery by remember { mutableStateOf("") }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onNavigateBack)

    val filteredBookmarks = remember(bookmarks, searchQuery) {
        if (searchQuery.isBlank()) bookmarks
        else bookmarks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredHistory = remember(history, searchQuery) {
        if (searchQuery.isBlank()) history
        else history.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredReadingList = remember(readingList, searchQuery) {
        if (searchQuery.isBlank()) readingList
        else readingList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (selectedTab == 1 && history.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearHistoryDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.clear))
                        }
                    } else if (selectedTab == 2 && readingList.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearReadingList) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddBookmarkDialog = true },
                    modifier = Modifier.testTag("add_bookmark_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_bookmarks, bookmarks.size), maxLines = 1) },
                    modifier = Modifier.testTag("library_bookmarks_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_history, history.size), maxLines = 1) },
                    modifier = Modifier.testTag("library_history_tab")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.tab_reading_list, readingList.size), maxLines = 1) },
                    modifier = Modifier.testTag("library_reading_list_tab")
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = {
                    Text(
                        when (selectedTab) {
                            0 -> stringResource(R.string.search_bookmarks)
                            1 -> stringResource(R.string.search_history)
                            else -> stringResource(R.string.search_reading_list)
                        }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            when (selectedTab) {
                0 -> {
                    if (filteredBookmarks.isEmpty()) {
                        EmptyLibraryState(
                            icon = Icons.Filled.Star,
                            title = strings.emptyBookmarks,
                            subtitle = stringResource(R.string.start_saved_bookmarks)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredBookmarks, key = { it.id }) { bm ->
                                BookmarkRow(
                                    bookmark = bm,
                                    onOpen = {
                                        onOpenUrl(bm.url)
                                        onNavigateBack()
                                    },
                                    onEdit = { editingBookmark = bm },
                                    onDelete = { viewModel.deleteBookmark(bm.id) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (filteredHistory.isEmpty()) {
                        EmptyLibraryState(
                            icon = Icons.Filled.History,
                            title = strings.emptyHistory,
                            subtitle = stringResource(R.string.private_mode_subtitle)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredHistory, key = { it.id }) { item ->
                                HistoryRow(
                                    entry = item,
                                    timeText = LocaleHelper.formatDateTime(item.visitedAt, locale),
                                    onOpen = {
                                        onOpenUrl(item.url)
                                        onNavigateBack()
                                    },
                                    onDelete = { viewModel.deleteHistoryItem(item.id) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    if (filteredReadingList.isEmpty()) {
                        EmptyLibraryState(
                            icon = Icons.Filled.BookmarkAdded,
                            title = strings.emptyReadingList,
                            subtitle = strings.noReadingExcerpt
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredReadingList, key = { it.id }) { item ->
                                ReadingListRow(
                                    item = item,
                                    strings = strings,
                                    savedText = LocaleHelper.formatDateTime(item.savedAt, locale),
                                    onOpen = {
                                        viewModel.toggleReadingListRead(item.id, true)
                                        onOpenUrl(item.url)
                                        onNavigateBack()
                                    },
                                    onToggleRead = { viewModel.toggleReadingListRead(item.id, !item.isRead) },
                                    onDelete = { viewModel.deleteReadingListItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddBookmarkDialog) {
        BookmarkEditDialog(
            titleHeading = stringResource(R.string.menu_bookmark),
            initialTitle = if (activeTab.isHomePage) "" else activeTab.title,
            initialUrl = if (activeTab.isHomePage) "https://" else activeTab.url,
            initialFolder = "Genel",
            strings = strings,
            onDismiss = { showAddBookmarkDialog = false },
            onSave = { title, url, folder ->
                viewModel.saveBookmark(title, url, folder)
                showAddBookmarkDialog = false
            }
        )
    }

    editingBookmark?.let { bm ->
        BookmarkEditDialog(
            titleHeading = stringResource(R.string.edit),
            initialTitle = bm.title,
            initialUrl = bm.url,
            initialFolder = bm.folder,
            strings = strings,
            onDismiss = { editingBookmark = null },
            onSave = { title, url, folder ->
                viewModel.updateBookmark(bm.copy(title = title, url = url, folder = folder))
                editingBookmark = null
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(strings.clearAllHistory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.clearHistoryPrompt)
                    OutlinedButton(
                        onClick = {
                            viewModel.clearHistoryLastHours(1)
                            showClearHistoryDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.last1Hour)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.clearHistoryLastHours(24)
                            showClearHistoryDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.last24Hours)
                    }
                    Button(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearHistoryDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.clearAllHistory)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun BookmarkRow(
    bookmark: BookmarkEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${UrlUtils.formatDisplayHost(bookmark.url)} • ${bookmark.folder}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntity,
    timeText: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${UrlUtils.formatDisplayHost(entry.url)} • $timeText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ReadingListRow(
    item: ReadingListEntity,
    strings: AppTranslations.UiStrings,
    savedText: String,
    onOpen: () -> Unit,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleRead) {
                Icon(
                    imageVector = if (item.isRead) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (item.isRead) strings.markUnread else strings.markRead,
                    tint = if (item.isRead) VerifiedEmerald else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (item.isRead) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${UrlUtils.formatDisplayHost(item.url)} • $savedText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BookmarkEditDialog(
    titleHeading: String,
    initialTitle: String,
    initialUrl: String,
    initialFolder: String,
    strings: AppTranslations.UiStrings,
    onDismiss: () -> Unit,
    onSave: (title: String, url: String, folder: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var url by remember { mutableStateOf(initialUrl) }
    var folder by remember { mutableStateOf(initialFolder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleHeading) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.titleLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(strings.shortcutUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = folder,
                    onValueChange = { folder = it },
                    label = { Text(strings.folderLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resolved = UrlUtils.resolveInputToUri(url, com.example.data.preferences.SearchEngine.DUCKDUCKGO)
                    onSave(
                        title.ifBlank { UrlUtils.formatDisplayHost(resolved) },
                        resolved,
                        folder.ifBlank { "Genel" }
                    )
                },
                enabled = url.trim().length > 3
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun EmptyLibraryState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
