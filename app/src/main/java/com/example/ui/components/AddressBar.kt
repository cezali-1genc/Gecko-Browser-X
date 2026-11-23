package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.OmniboxSuggestion
import com.example.data.repository.SuggestionKind
import com.example.engine.BrowserTab
import com.example.engine.FindInPageState
import com.example.engine.SecurityState
import com.example.engine.UrlUtils
import com.example.i18n.AppTranslations
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.PrivateBorder
import com.example.ui.theme.PrivateSurface
import com.example.ui.theme.PrivateViolet
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber

@Composable
fun BrowserAddressBar(
    tab: BrowserTab,
    suggestions: List<OmniboxSuggestion>,
    tabCount: Int,
    isBookmarked: Boolean,
    isWideLayout: Boolean,
    appLanguage: String,
    findInPageState: FindInPageState,
    focusRequestTrigger: Int,
    onQueryChanged: (String) -> Unit,
    onSubmitInput: (String) -> Unit,
    onSuggestionClick: (OmniboxSuggestion) -> Unit,
    onClearSuggestions: () -> Unit,
    onSecurityClick: () -> Unit,
    onReloadOrStop: () -> Unit,
    onVoiceSearch: () -> Unit,
    onQrScan: () -> Unit,
    onFindQueryChange: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrev: () -> Unit,
    onCloseFind: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trStrings = remember(appLanguage) { AppTranslations.forLang(appLanguage) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var textValue by remember(tab.id, tab.url, tab.isHomePage) {
        mutableStateOf(if (tab.isHomePage) "" else tab.url)
    }

    LaunchedEffect(focusRequestTrigger) {
        if (focusRequestTrigger > 0) {
            focusRequester.requestFocus()
        }
    }

    val containerColor = if (tab.isPrivate) {
        PrivateSurface
    } else {
        MaterialTheme.colorScheme.surface
    }

    val pillBorderColor = when {
        isFocused && tab.isPrivate -> PrivateViolet
        isFocused -> MaterialTheme.colorScheme.primary
        tab.isPrivate -> PrivateBorder.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
    ) {
        if (findInPageState.active) {
            FindInPageBar(
                state = findInPageState,
                strings = trStrings,
                onQueryChange = onFindQueryChange,
                onPrev = onFindPrev,
                onNext = onFindNext,
                onClose = onCloseFind
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isWideLayout) {
                    IconButton(
                        onClick = onBack,
                        enabled = tab.canGoBack || !tab.isHomePage,
                        modifier = Modifier.testTag("top_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                    IconButton(
                        onClick = onForward,
                        enabled = tab.canGoForward,
                        modifier = Modifier.testTag("top_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.cd_forward)
                        )
                    }
                    IconButton(
                        onClick = onHome,
                        modifier = Modifier.testTag("top_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = stringResource(R.string.cd_home)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = if (tab.isPrivate) {
                        PrivateBorder.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(1.dp, pillBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Security Badge
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable(onClick = onSecurityClick)
                                .testTag("security_badge_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            val (secIcon, secTint) = when {
                                tab.isPrivate && tab.isHomePage ->
                                    Icons.Filled.VisibilityOff to PrivateViolet
                                tab.securityState == SecurityState.HOME_INTERNAL ->
                                    Icons.Filled.Shield to MaterialTheme.colorScheme.primary
                                tab.securityState == SecurityState.VERIFIED_HTTPS ->
                                    Icons.Filled.Lock to VerifiedEmerald
                                tab.securityState == SecurityState.INSECURE_HTTP ->
                                    Icons.Filled.WarningAmber to WarningAmber
                                else ->
                                    Icons.Filled.GppBad to DangerCoral
                            }
                            Icon(
                                imageVector = secIcon,
                                contentDescription = stringResource(R.string.cd_security),
                                tint = secTint,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Omnibox input / display
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (isFocused) {
                                BasicTextField(
                                    value = textValue,
                                    onValueChange = { updated ->
                                        textValue = updated
                                        onQueryChanged(updated)
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Go
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onGo = {
                                            focusManager.clearFocus()
                                            onSubmitInput(textValue)
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { state ->
                                            isFocused = state.isFocused
                                            if (!state.isFocused) {
                                                onClearSuggestions()
                                                textValue = if (tab.isHomePage) "" else tab.url
                                            }
                                        }
                                        .testTag("address_bar_input"),
                                    decorationBox = { innerTextField ->
                                        if (textValue.isEmpty()) {
                                            Text(
                                                text = if (tab.isPrivate) {
                                                    stringResource(R.string.address_bar_private_hint)
                                                } else {
                                                    stringResource(R.string.address_bar_hint)
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clickable {
                                            textValue = if (tab.isHomePage) "" else tab.url
                                            isFocused = true
                                        }
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { if (it.isFocused) isFocused = true }
                                        .testTag("address_bar_display"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (tab.isHomePage) {
                                        Text(
                                            text = if (tab.isPrivate) {
                                                stringResource(R.string.address_bar_private_hint)
                                            } else {
                                                stringResource(R.string.address_bar_hint)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            text = UrlUtils.formatDisplayHost(tab.url),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (tab.blockedTrackersCount > 0) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = String.format(trStrings.blockedCount, tab.blockedTrackersCount),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(
                                                        horizontal = 6.dp,
                                                        vertical = 2.dp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Trailing actions inside omnibox
                        if (isFocused && textValue.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    textValue = ""
                                    onClearSuggestions()
                                },
                                modifier = Modifier.testTag("clear_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_clear_url),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (isFocused) {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    onVoiceSearch()
                                },
                                modifier = Modifier.testTag("voice_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = stringResource(R.string.cd_voice_search),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    onQrScan()
                                },
                                modifier = Modifier.testTag("qr_scan_omnibox_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = stringResource(R.string.cd_qr_scan),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (!tab.isHomePage) {
                            IconButton(
                                onClick = onReloadOrStop,
                                modifier = Modifier.testTag("reload_stop_button")
                            ) {
                                Icon(
                                    imageVector = if (tab.isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
                                    contentDescription = if (tab.isLoading) {
                                        stringResource(R.string.cd_stop)
                                    } else {
                                        stringResource(R.string.cd_reload)
                                    },
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                if (isWideLayout) {
                    if (!tab.isHomePage) {
                        IconButton(
                            onClick = onBookmarkToggle,
                            modifier = Modifier.testTag("top_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = stringResource(R.string.cd_bookmark_toggle),
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TabCountButton(
                        tabCount = tabCount,
                        isPrivate = tab.isPrivate,
                        onClick = onOpenTabs,
                        modifier = Modifier.testTag("top_tabs_button")
                    )
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.testTag("top_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.cd_menu)
                        )
                    }
                }
            }
        }

        // Loading progress bar
        if (tab.isLoading) {
            LinearProgressIndicator(
                progress = { (tab.progress.coerceIn(5, 100)) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = if (tab.isPrivate) PrivateViolet else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        }

        // Suggestions Dropdown
        AnimatedVisibility(
            visible = isFocused && suggestions.isNotEmpty() && !findInPageState.active,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            OmniboxSuggestionsDropdown(
                suggestions = suggestions,
                onSelect = { item ->
                    focusManager.clearFocus()
                    isFocused = false
                    onSuggestionClick(item)
                },
                onFill = { query ->
                    textValue = query
                    onQueryChanged(query)
                }
            )
        }
    }
}

@Composable
fun OmniboxSuggestionsDropdown(
    suggestions: List<OmniboxSuggestion>,
    onSelect: (OmniboxSuggestion) -> Unit,
    onFill: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
        ) {
            items(suggestions) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (item.kind) {
                        SuggestionKind.BOOKMARK -> Icons.Filled.Star
                        SuggestionKind.HISTORY -> Icons.Filled.History
                        SuggestionKind.SEARCH_WEB -> Icons.Filled.Search
                    }
                    val tint = when (item.kind) {
                        SuggestionKind.BOOKMARK -> MaterialTheme.colorScheme.primary
                        SuggestionKind.HISTORY -> MaterialTheme.colorScheme.secondary
                        SuggestionKind.SEARCH_WEB -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.kind != SuggestionKind.SEARCH_WEB) {
                            Text(
                                text = item.urlOrQuery,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = { onFill(item.urlOrQuery) }) {
                        Icon(
                            imageVector = Icons.Filled.NorthWest,
                            contentDescription = stringResource(R.string.cd_clear_url),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FindInPageBar(
    state: FindInPageState,
    strings: AppTranslations.UiStrings,
    onQueryChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("find_in_page_input"),
                    decorationBox = { inner ->
                        if (state.query.isEmpty()) {
                            Text(
                                text = strings.findHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                )
                Text(
                    text = if (state.totalMatches > 0) "${state.currentMatch}/${state.totalMatches}" else strings.noMatches,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        IconButton(onClick = onClose, modifier = Modifier.testTag("close_find_button")) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
        }
    }
}

@Composable
fun TabCountButton(
    tabCount: Int,
    isPrivate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(26.dp),
            shape = RoundedCornerShape(7.dp),
            color = androidx.compose.ui.graphics.Color.Transparent,
            border = BorderStroke(
                width = 1.8.dp,
                color = if (isPrivate) PrivateViolet else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (tabCount > 99) "99+" else tabCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPrivate) PrivateViolet else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
