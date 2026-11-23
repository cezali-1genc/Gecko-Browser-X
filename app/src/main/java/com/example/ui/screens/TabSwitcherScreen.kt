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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.engine.BrowserTab
import com.example.engine.UrlUtils
import com.example.i18n.AppTranslations
import com.example.ui.state.BrowserViewModel
import com.example.ui.theme.PrivateBorder
import com.example.ui.theme.PrivateDeepViolet
import com.example.ui.theme.PrivateSurface
import com.example.ui.theme.PrivateViolet

private enum class TabFilter { ALL, PINNED, STANDARD, PRIVATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val strings = remember(settings.appLanguage) { AppTranslations.forLang(settings.appLanguage) }

    var filter by remember { mutableStateOf(TabFilter.ALL) }
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var groupTargetTab by remember { mutableStateOf<BrowserTab?>(null) }

    BackHandler(onBack = onNavigateBack)

    val groupNames = remember(tabs) {
        tabs.mapNotNull { it.groupName?.takeIf { g -> g.isNotBlank() } }.distinct().sorted()
    }

    val filteredTabs = remember(tabs, filter, selectedGroupFilter, searchQuery) {
        val base = when {
            selectedGroupFilter != null -> tabs.filter { it.groupName == selectedGroupFilter }
            filter == TabFilter.ALL -> tabs
            filter == TabFilter.PINNED -> tabs.filter { it.isPinned }
            filter == TabFilter.STANDARD -> tabs.filterNot { it.isPrivate }
            filter == TabFilter.PRIVATE -> tabs.filter { it.isPrivate }
            else -> tabs
        }
        if (searchQuery.isBlank()) base
        else base.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true) ||
                (it.groupName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }
    val hasPrivateTabs = tabs.any { it.isPrivate }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tabs_title, tabs.size)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (hasPrivateTabs) {
                        TextButton(
                            onClick = { viewModel.closeAllPrivateTabs() },
                            modifier = Modifier.testTag("close_all_private_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = null,
                                tint = PrivateViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.tabs_clear_private), color = PrivateViolet)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.createNewTab(isPrivate = false, groupName = selectedGroupFilter)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_tab_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.tabs_new))
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.createNewTab(isPrivate = true)
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrivateViolet),
                        border = BorderStroke(1.dp, PrivateBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_private_tab_button")
                    ) {
                        Icon(
                            Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.tabs_new_private))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.tabs_search_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("tabs_search_input")
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedGroupFilter == null && filter == TabFilter.ALL,
                        onClick = { selectedGroupFilter = null; filter = TabFilter.ALL },
                        label = { Text(stringResource(R.string.tabs_all, tabs.size)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedGroupFilter == null && filter == TabFilter.PINNED,
                        onClick = { selectedGroupFilter = null; filter = TabFilter.PINNED },
                        label = { Text(stringResource(R.string.tabs_pinned, tabs.count { it.isPinned })) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedGroupFilter == null && filter == TabFilter.STANDARD,
                        onClick = { selectedGroupFilter = null; filter = TabFilter.STANDARD },
                        label = { Text(stringResource(R.string.tabs_standard, tabs.count { !it.isPrivate })) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedGroupFilter == null && filter == TabFilter.PRIVATE,
                        onClick = { selectedGroupFilter = null; filter = TabFilter.PRIVATE },
                        label = { Text(stringResource(R.string.tabs_private, tabs.count { it.isPrivate })) }
                    )
                }
                items(groupNames) { g ->
                    FilterChip(
                        selected = selectedGroupFilter == g,
                        onClick = { selectedGroupFilter = if (selectedGroupFilter == g) null else g },
                        leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("$g (${tabs.count { it.groupName == g }})") }
                    )
                }
            }

            if (filteredTabs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.tabs_empty),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 165.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTabs, key = { it.id }) { tab ->
                        TabCard(
                            tab = tab,
                            isActive = tab.id == activeTabId,
                            onSelect = {
                                viewModel.switchTab(tab.id)
                                onNavigateBack()
                            },
                            onTogglePin = { viewModel.togglePinTab(tab.id) },
                            onAssignGroup = { groupTargetTab = tab },
                            onClose = { viewModel.closeTab(tab.id) }
                        )
                    }
                }
            }
        }
    }

    groupTargetTab?.let { target ->
        var groupText by remember(target) { mutableStateOf(target.groupName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { groupTargetTab = null },
            title = { Text(strings.tabGroupPrompt) },
            text = {
                OutlinedTextField(
                    value = groupText,
                    onValueChange = { groupText = it },
                    placeholder = { Text(strings.tabGroupHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setTabGroup(target.id, groupText)
                        groupTargetTab = null
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { groupTargetTab = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onAssignGroup: () -> Unit,
    onClose: () -> Unit
) {
    val borderColor = when {
        isActive && tab.isPrivate -> PrivateViolet
        isActive -> MaterialTheme.colorScheme.primary
        tab.isPrivate -> PrivateBorder.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }

    val cardBg = if (tab.isPrivate) PrivateSurface else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(166.dp)
            .clickable(onClick = onSelect)
            .testTag("tab_card_${tab.id}"),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
        tonalElevation = if (isActive) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (tab.isPrivate) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                    contentDescription = null,
                    tint = if (tab.isPrivate) PrivateViolet else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = tab.groupName ?: if (tab.isPrivate) stringResource(R.string.menu_private_tab) else "Web",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tab.isPrivate) PrivateViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (tab.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = stringResource(if (tab.isPinned) R.string.tabs_unpin else R.string.tabs_pin),
                        tint = if (tab.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = onAssignGroup,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = stringResource(R.string.tabs_group),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("close_tab_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_tab),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Column {
                Text(
                    text = tab.title.ifBlank { stringResource(R.string.tabs_new) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = UrlUtils.formatDisplayHost(tab.url),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isActive) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (tab.isPrivate) PrivateDeepViolet else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.tabs_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tab.isPrivate) PrivateViolet else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
