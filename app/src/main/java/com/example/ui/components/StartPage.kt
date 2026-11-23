package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.SpeedDialEntity
import com.example.data.preferences.HomeWallpaperPreset
import com.example.data.preferences.SearchEngine
import com.example.data.repository.OmniboxSuggestion
import com.example.data.repository.WeatherSnapshot
import com.example.engine.UrlUtils
import com.example.i18n.AppTranslations
import com.example.i18n.LocaleHelper
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.OperaCrimson
import com.example.ui.theme.OperaCrimsonDark
import com.example.ui.theme.PrivateBorder
import com.example.ui.theme.PrivateSurface
import com.example.ui.theme.PrivateViolet
import com.example.ui.theme.VerifiedEmerald
import kotlin.math.roundToInt

@Composable
fun StartPage(
    isPrivate: Boolean,
    appLanguage: String,
    searchEngine: SearchEngine,
    totalTrackersBlocked: Long,
    isVpnActive: Boolean,
    isDefaultBrowser: Boolean,
    showSpeedDials: Boolean,
    showRecentSites: Boolean,
    showWeather: Boolean,
    weatherSnapshot: WeatherSnapshot?,
    wallpaperPreset: HomeWallpaperPreset,
    wallpaperUri: String?,
    speedDials: List<SpeedDialEntity>,
    recentSites: List<HistoryEntity>,
    recentBookmarks: List<BookmarkEntity>,
    suggestions: List<OmniboxSuggestion>,
    focusSearchTrigger: Int,
    onQueryChanged: (String) -> Unit,
    onSubmitInput: (String) -> Unit,
    onSuggestionClick: (OmniboxSuggestion) -> Unit,
    onClearSuggestions: () -> Unit,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onVoiceSearch: () -> Unit,
    onQrScan: () -> Unit,
    onRequestDefaultBrowser: () -> Unit,
    onOpenWallpaperCustomizer: () -> Unit,
    onRefreshWeather: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onAddSpeedDial: (title: String, url: String) -> Unit,
    onDeleteSpeedDial: (Long) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onOpenLegalRegistry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = remember(appLanguage) { AppTranslations.forLang(appLanguage) }
    val locale = remember(appLanguage) { LocaleHelper.resolveLocale(appLanguage) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var searchQuery by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var showAddDialDialog by remember { mutableStateOf(false) }
    var showEnginePickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(focusSearchTrigger) {
        if (focusSearchTrigger > 0) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Wallpaper Background Layer
        StartPageWallpaperBackdrop(
            isPrivate = isPrivate,
            preset = wallpaperPreset,
            customUri = wallpaperUri
        )

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Opera-style Top Brand + Status Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Opera Ring emblem
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(
                                3.dp,
                                Brush.linearGradient(
                                    colors = if (isPrivate) {
                                        listOf(PrivateViolet, PrivateBorder)
                                    } else {
                                        listOf(OperaCrimson, OperaCrimsonDark)
                                    }
                                )
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPrivate) Icons.Filled.VisibilityOff else Icons.Filled.Shield,
                                    contentDescription = stringResource(R.string.brand_full_title),
                                    tint = if (isPrivate) PrivateViolet else OperaCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.brand_full_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isPrivate) stringResource(R.string.private_mode_label)
                                else stringResource(R.string.engine_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPrivate) PrivateViolet else Color(0xFFCBD5E1)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            modifier = Modifier.clickable(onClick = onOpenVpnSettings),
                            shape = RoundedCornerShape(999.dp),
                            color = if (isVpnActive) {
                                VerifiedEmerald.copy(alpha = 0.2f)
                            } else {
                                Color.White.copy(alpha = 0.1f)
                            }
                        ) {
                            Text(
                                text = if (isVpnActive) {
                                    "VPN • ${strings.vpnActiveBanner}"
                                } else {
                                    stringResource(
                                        R.string.start_trackers_blocked,
                                        totalTrackersBlocked.toInt()
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isVpnActive) VerifiedEmerald else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        if (!isPrivate) {
                            Surface(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onOpenWallpaperCustomizer),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Wallpaper,
                                        contentDescription = stringResource(R.string.start_customize_bg),
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SINGLE UNIFIED OPERA SEARCH BAR ON SCREEN
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("start_page_search_button"),
                        shape = RoundedCornerShape(29.dp),
                        color = if (isPrivate) {
                            PrivateSurface.copy(alpha = 0.94f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        },
                        tonalElevation = 8.dp,
                        border = BorderStroke(
                            1.5.dp,
                            when {
                                searchFocused && isPrivate -> PrivateViolet
                                searchFocused -> OperaCrimson
                                isPrivate -> PrivateBorder.copy(alpha = 0.7f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search Engine Picker Pill inside single Search Bar
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showEnginePickerDialog = true }
                                    .testTag("start_engine_picker_button"),
                                shape = RoundedCornerShape(20.dp),
                                color = if (isPrivate) {
                                    PrivateViolet.copy(alpha = 0.18f)
                                } else {
                                    OperaCrimson.copy(alpha = 0.14f)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = if (isPrivate) PrivateViolet else OperaCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = searchEngine.displayName.split(" ").first(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { updated ->
                                        searchQuery = updated
                                        onQueryChanged(updated)
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(
                                        if (isPrivate) PrivateViolet else OperaCrimson
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Search
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (searchQuery.isNotBlank()) {
                                                focusManager.clearFocus()
                                                onSubmitInput(searchQuery)
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { state ->
                                            searchFocused = state.isFocused
                                            if (!state.isFocused) {
                                                onClearSuggestions()
                                            }
                                        }
                                        .testTag("start_page_search_input"),
                                    decorationBox = { inner ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = if (isPrivate) {
                                                    stringResource(R.string.address_bar_private_hint)
                                                } else {
                                                    stringResource(
                                                        R.string.start_search_with,
                                                        searchEngine.displayName
                                                    )
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        inner()
                                    }
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        onClearSuggestions()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_clear_url),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = onVoiceSearch,
                                    modifier = Modifier.testTag("start_voice_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = stringResource(R.string.cd_voice_search),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onQrScan,
                                    modifier = Modifier.testTag("start_qr_scan_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCodeScanner,
                                        contentDescription = stringResource(R.string.cd_qr_scan),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Live Omnibox Suggestions below single Start Page search bar
                    AnimatedVisibility(
                        visible = searchFocused && suggestions.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OmniboxSuggestionsDropdown(
                            suggestions = suggestions,
                            onSelect = { item ->
                                focusManager.clearFocus()
                                searchFocused = false
                                onSuggestionClick(item)
                            },
                            onFill = { query ->
                                searchQuery = query
                                onQueryChanged(query)
                            },
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            // Default Browser Choice Card (Varsayılan Tarayıcı Seçeneği)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("default_browser_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDefaultBrowser) {
                        VerifiedEmerald.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isDefaultBrowser) VerifiedEmerald.copy(alpha = 0.45f)
                        else OperaCrimson.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDefaultBrowser) Icons.Filled.VerifiedUser else Icons.Filled.Public,
                            contentDescription = null,
                            tint = if (isDefaultBrowser) VerifiedEmerald else OperaCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isDefaultBrowser) {
                                stringResource(R.string.start_default_active)
                            } else {
                                stringResource(R.string.start_default_browser_banner)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (!isDefaultBrowser) {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onRequestDefaultBrowser,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("make_default_browser_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.start_make_default),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Optional Weather Card
            if (showWeather && !isPrivate) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRefreshWeather),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WbSunny,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = weatherSnapshot?.city ?: stringResource(R.string.start_weather_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (weatherSnapshot != null) {
                                        "${weatherSnapshot.temperatureC.roundToInt()}°C • ${weatherSnapshot.windKmh.roundToInt()} km/h"
                                    } else {
                                        stringResource(R.string.start_weather_loading)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Customizable Speed Dials
            if (showSpeedDials) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.start_shortcuts),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        TextButton(
                            onClick = { showAddDialDialog = true },
                            modifier = Modifier.testTag("add_speed_dial_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.start_add_pin))
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    val rows = speedDials.chunked(4)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { dial ->
                                    SpeedDialCard(
                                        dial = dial,
                                        onClick = { onOpenUrl(dial.url) },
                                        onDelete = { onDeleteSpeedDial(dial.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(4 - rowItems.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Recently Visited Sites
            if (showRecentSites && recentSites.isNotEmpty() && !isPrivate) {
                item {
                    Text(
                        text = stringResource(R.string.start_recent_sites),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentSites, key = { it.id }) { site ->
                            Surface(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenUrl(site.url) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = UrlUtils.formatDisplayHost(site.url),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    Text(
                                        text = site.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Saved Bookmarks Strip
            if (recentBookmarks.isNotEmpty() && !isPrivate) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.start_saved_bookmarks),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        TextButton(onClick = onOpenBookmarks) {
                            Text(stringResource(R.string.start_view_all))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recentBookmarks.take(8), key = { it.id }) { bm ->
                            Surface(
                                modifier = Modifier
                                    .width(156.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenUrl(bm.url) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = UrlUtils.formatDisplayHost(bm.url),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = bm.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Brand Signature & Tescil/KVKK Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${stringResource(R.string.brand_developer)} • ${stringResource(R.string.brand_copyright)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onOpenLegalRegistry)
                            .testTag("start_legal_footer_button"),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Gavel,
                                contentDescription = null,
                                tint = OperaCrimson,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.menu_legal_registry),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialDialog) {
        AddSpeedDialDialog(
            strings = strings,
            onDismiss = { showAddDialDialog = false },
            onAdd = { title, url ->
                onAddSpeedDial(title, url)
                showAddDialDialog = false
            }
        )
    }

    if (showEnginePickerDialog) {
        AlertDialog(
            onDismissRequest = { showEnginePickerDialog = false },
            title = { Text(strings.searchEnginePick) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SearchEngine.entries.forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectSearchEngine(engine)
                                    showEnginePickerDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = engine == searchEngine,
                                onClick = {
                                    onSelectSearchEngine(engine)
                                    showEnginePickerDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(engine.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEnginePickerDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun StartPageWallpaperBackdrop(
    isPrivate: Boolean,
    preset: HomeWallpaperPreset,
    customUri: String?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isPrivate -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1A1033), PrivateSurface, AmoledBlack)
                            )
                        )
                )
            }
            preset == HomeWallpaperPreset.CUSTOM_IMAGE && !customUri.isNullOrBlank() -> {
                AsyncImage(
                    model = customUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.56f))
                )
            }
            preset == HomeWallpaperPreset.OBSIDIAN_CYAN -> {
                Image(
                    painter = painterResource(id = R.drawable.img_home_backdrop),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x99060D16), Color(0xEC06080E))
                            )
                        )
                )
            }
            preset == HomeWallpaperPreset.MIDNIGHT_AMOLED -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AmoledBlack)
                )
            }
            else -> {
                // OPERA_CRIMSON
                Image(
                    painter = painterResource(id = R.drawable.img_home_backdrop),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x882E060C),
                                    Color(0xD80A0C12),
                                    Color(0xF506070A)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun SpeedDialCard(
    dial: SpeedDialEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(dial.accentHex).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dial.title.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(dial.accentHex)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = dial.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AddSpeedDialDialog(
    strings: AppTranslations.UiStrings,
    onDismiss: () -> Unit,
    onAdd: (title: String, url: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newShortcutTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.shortcutLabel) },
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
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title.ifBlank { UrlUtils.formatDisplayHost(url) }, url) },
                enabled = url.trim().length > 4
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
