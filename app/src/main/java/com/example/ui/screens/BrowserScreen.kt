package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.preferences.HomeWallpaperPreset
import com.example.data.preferences.HomepageMode
import com.example.data.preferences.ToolbarStyle
import com.example.data.repository.SuggestionKind
import com.example.i18n.AppTranslations
import com.example.ui.components.AutofillBanner
import com.example.ui.components.BottomControlBar
import com.example.ui.components.BrowserAddressBar
import com.example.ui.components.DownloadConfirmDialog
import com.example.ui.components.GeckoViewport
import com.example.ui.components.GeckoViewportController
import com.example.ui.components.PageErrorOverlay
import com.example.ui.components.ReaderModeSheet
import com.example.ui.components.SaveCredentialDialog
import com.example.ui.components.SecurityDetailsSheet
import com.example.ui.components.SitePermissionDialog
import com.example.ui.components.StartPage
import com.example.ui.components.TabCountButton
import com.example.ui.components.TranslateSheet
import com.example.ui.components.WebPromptDialog
import com.example.ui.components.ZoomControlDialog
import com.example.ui.state.BrowserViewModel
import com.example.ui.theme.OperaCrimson
import com.example.ui.theme.PrivateViolet
import com.example.ui.theme.VerifiedEmerald
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    isDefaultBrowser: Boolean,
    onRequestDefaultBrowser: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onOpenBookmarksAndHistory: (initialTab: Int) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSitePermissions: () -> Unit,
    onOpenPrivacyCenter: () -> Unit,
    onOpenDnsVpn: () -> Unit,
    onOpenPasswordManager: () -> Unit,
    onOpenExtensions: () -> Unit,
    onOpenQrTools: () -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenPdfViewer: () -> Unit,
    onOpenLegalRegistry: () -> Unit,
    onFullScreenStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val recentSites by viewModel.recentVisitedSites.collectAsStateWithLifecycle()
    val speedDials by viewModel.speedDials.collectAsStateWithLifecycle()
    val weatherSnapshot by viewModel.weatherSnapshot.collectAsStateWithLifecycle()
    val isVpnActive by viewModel.isVpnActive.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isActiveTabBookmarked.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val findState by viewModel.findInPageState.collectAsStateWithLifecycle()
    val readerArticle by viewModel.readerArticle.collectAsStateWithLifecycle()
    val isReaderLoading by viewModel.isReaderLoading.collectAsStateWithLifecycle()
    val pendingPrompt by viewModel.pendingPrompt.collectAsStateWithLifecycle()
    val pendingSitePermission by viewModel.pendingSitePermission.collectAsStateWithLifecycle()
    val pendingDownload by viewModel.pendingDownload.collectAsStateWithLifecycle()
    val pendingSaveCredential by viewModel.pendingSaveCredential.collectAsStateWithLifecycle()
    val pendingAutofillCreds by viewModel.pendingAutofillCredentials.collectAsStateWithLifecycle()
    val manualFullScreen by viewModel.manualFullScreen.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val strings = remember(settings.appLanguage) { AppTranslations.forLang(settings.appLanguage) }
    val viewportController = remember { GeckoViewportController() }

    val isImmersiveFullScreen = activeTab.isFullScreen || manualFullScreen
    val isStartPageActive = activeTab.isHomePage && settings.homepageMode == HomepageMode.START_PAGE

    var showMenuSheet by remember { mutableStateOf(false) }
    var showSecuritySheet by remember { mutableStateOf(false) }
    var showZoomDialog by remember { mutableStateOf(false) }
    var showTranslateSheet by remember { mutableStateOf(false) }
    var focusAddressTrigger by remember { mutableIntStateOf(0) }
    var focusStartSearchTrigger by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.submitOmniboxInput(spoken)
            }
        }
    }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.setHomeWallpaper(HomeWallpaperPreset.CUSTOM_IMAGE, uri.toString())
        }
    }

    val launchVoiceSearch: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.cd_voice_search))
        }
        val launched = runCatching {
            voiceSearchLauncher.launch(intent)
            true
        }.getOrDefault(false)
        if (!launched) {
            viewModel.showInfoMessage(context.getString(R.string.err_speech_unavailable))
        }
    }

    val captureScreenshot: () -> Unit = {
        viewportController.captureScreenshot { bitmap ->
            if (bitmap == null) {
                viewModel.showInfoMessage(strings.screenshotFailed)
            } else {
                scope.launch {
                    val saved = saveScreenshotToGallery(context, bitmap)
                    viewModel.showInfoMessage(if (saved) strings.screenshotSaved else strings.screenshotFailed)
                }
            }
        }
    }

    LaunchedEffect(isImmersiveFullScreen) {
        onFullScreenStateChanged(isImmersiveFullScreen)
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    BackHandler(
        enabled = findState.active || isImmersiveFullScreen || activeTab.canGoBack || !activeTab.isHomePage
    ) {
        if (findState.active) {
            viewModel.closeFindInPage()
        } else {
            viewModel.goBack()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Tablet Navigation Rail
            if (isWideLayout && !isImmersiveFullScreen) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.height(12.dp))
                    NavigationRailItem(
                        selected = false,
                        onClick = viewModel::goBack,
                        enabled = activeTab.canGoBack || !activeTab.isHomePage,
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back)) }
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = viewModel::goForward,
                        enabled = activeTab.canGoForward,
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.cd_forward)) }
                    )
                    NavigationRailItem(
                        selected = activeTab.isHomePage,
                        onClick = {
                            if (activeTab.isHomePage) focusStartSearchTrigger++
                            else viewModel.goHome()
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.cd_home)) }
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = onOpenTabSwitcher,
                        icon = {
                            TabCountButton(
                                tabCount = tabs.size,
                                isPrivate = activeTab.isPrivate,
                                onClick = onOpenTabSwitcher
                            )
                        }
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = { showMenuSheet = true },
                        icon = { Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu)) }
                    )
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    // CRITICAL: Only ONE Search Bar on screen when Start Page is active!
                    AnimatedVisibility(
                        visible = !isImmersiveFullScreen && !isStartPageActive,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        BrowserAddressBar(
                            tab = activeTab,
                            suggestions = suggestions,
                            tabCount = tabs.size,
                            isBookmarked = isBookmarked,
                            isWideLayout = isWideLayout,
                            appLanguage = settings.appLanguage,
                            findInPageState = findState,
                            focusRequestTrigger = focusAddressTrigger,
                            onQueryChanged = viewModel::updateOmniboxSuggestions,
                            onSubmitInput = viewModel::submitOmniboxInput,
                            onSuggestionClick = { suggestion ->
                                when (suggestion.kind) {
                                    SuggestionKind.SEARCH_WEB ->
                                        viewModel.submitOmniboxInput(suggestion.urlOrQuery)
                                    SuggestionKind.BOOKMARK, SuggestionKind.HISTORY ->
                                        viewModel.navigateCurrentTab(suggestion.urlOrQuery)
                                }
                            },
                            onClearSuggestions = viewModel::clearSuggestions,
                            onSecurityClick = { showSecuritySheet = true },
                            onReloadOrStop = viewModel::reloadOrStop,
                            onVoiceSearch = launchVoiceSearch,
                            onQrScan = onOpenQrTools,
                            onFindQueryChange = { q -> viewModel.findInPage(q, backwards = false) },
                            onFindNext = { viewModel.findInPage(findState.query, backwards = false) },
                            onFindPrev = { viewModel.findInPage(findState.query, backwards = true) },
                            onCloseFind = viewModel::closeFindInPage,
                            onBack = viewModel::goBack,
                            onForward = viewModel::goForward,
                            onHome = viewModel::goHome,
                            onBookmarkToggle = viewModel::toggleCurrentBookmark,
                            onOpenTabs = onOpenTabSwitcher,
                            onOpenMenu = { showMenuSheet = true },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                },
                bottomBar = {
                    val showBottom = !isImmersiveFullScreen && !isWideLayout &&
                        (settings.toolbarStyle == ToolbarStyle.OPERA_BOTTOM || isStartPageActive)
                    AnimatedVisibility(
                        visible = showBottom,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        BottomControlBar(
                            tab = activeTab,
                            tabCount = tabs.size,
                            isVpnActive = isVpnActive,
                            onBack = viewModel::goBack,
                            onForward = viewModel::goForward,
                            onCenterAction = {
                                if (isStartPageActive) focusStartSearchTrigger++
                                else viewModel.goHome()
                            },
                            onOpenTabs = onOpenTabSwitcher,
                            onOpenMenu = { showMenuSheet = true }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when {
                        activeTab.isHomePage -> {
                            StartPage(
                                isPrivate = activeTab.isPrivate,
                                appLanguage = settings.appLanguage,
                                searchEngine = settings.searchEngine,
                                totalTrackersBlocked = settings.totalTrackersBlocked,
                                isVpnActive = isVpnActive,
                                isDefaultBrowser = isDefaultBrowser,
                                showSpeedDials = settings.showSpeedDialsOnHome,
                                showRecentSites = settings.showRecentSitesOnHome,
                                showWeather = settings.showWeatherOnHome,
                                weatherSnapshot = weatherSnapshot,
                                wallpaperPreset = settings.homeWallpaperPreset,
                                wallpaperUri = settings.homeWallpaperUri,
                                speedDials = speedDials,
                                recentSites = recentSites,
                                recentBookmarks = bookmarks,
                                suggestions = suggestions,
                                focusSearchTrigger = focusStartSearchTrigger,
                                onQueryChanged = viewModel::updateOmniboxSuggestions,
                                onSubmitInput = viewModel::submitOmniboxInput,
                                onSuggestionClick = { suggestion ->
                                    when (suggestion.kind) {
                                        SuggestionKind.SEARCH_WEB ->
                                            viewModel.submitOmniboxInput(suggestion.urlOrQuery)
                                        SuggestionKind.BOOKMARK, SuggestionKind.HISTORY ->
                                            viewModel.navigateCurrentTab(suggestion.urlOrQuery)
                                    }
                                },
                                onClearSuggestions = viewModel::clearSuggestions,
                                onSelectSearchEngine = viewModel::setSearchEngine,
                                onVoiceSearch = launchVoiceSearch,
                                onQrScan = onOpenQrTools,
                                onRequestDefaultBrowser = onRequestDefaultBrowser,
                                onOpenWallpaperCustomizer = {
                                    wallpaperPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onRefreshWeather = viewModel::refreshWeather,
                                onOpenUrl = viewModel::navigateCurrentTab,
                                onAddSpeedDial = viewModel::addSpeedDial,
                                onDeleteSpeedDial = viewModel::deleteSpeedDial,
                                onOpenBookmarks = { onOpenBookmarksAndHistory(0) },
                                onOpenVpnSettings = onOpenDnsVpn,
                                onOpenLegalRegistry = onOpenLegalRegistry
                            )
                        }

                        activeTab.loadError != null -> {
                            PageErrorOverlay(
                                error = activeTab.loadError!!,
                                appLanguage = settings.appLanguage,
                                onGoBack = viewModel::goBack,
                                onReload = viewModel::reloadOrStop
                            )
                        }

                        else -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (pendingAutofillCreds.isNotEmpty() && !activeTab.isPrivate) {
                                    AutofillBanner(
                                        credentials = pendingAutofillCreds,
                                        onFillCredential = { cred ->
                                            val injected = viewModel.fillPendingCredential(cred)
                                            if (!injected) {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                cm?.setPrimaryClip(ClipData.newPlainText("password", cred.password))
                                                viewModel.showInfoMessage("${cred.username} • ${context.getString(R.string.pw_copied)}")
                                            }
                                        },
                                        onDismiss = viewModel::dismissAutofillBanner
                                    )
                                }
                                GeckoViewport(
                                    tabId = activeTab.id,
                                    geckoSession = viewModel.getGeckoSession(activeTab.id),
                                    controller = viewportController,
                                    isAtTop = activeTab.isAtTop,
                                    canGoBack = activeTab.canGoBack,
                                    canGoForward = activeTab.canGoForward,
                                    edgeGesturesEnabled = settings.edgeGesturesEnabled,
                                    pullToRefreshEnabled = settings.pullToRefreshEnabled,
                                    onSwipeBack = viewModel::goBack,
                                    onSwipeForward = viewModel::goForward,
                                    onPullToRefresh = viewModel::reloadOrStop,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Floating exit fullscreen button when user entered manual fullscreen
                    if (manualFullScreen && !activeTab.isFullScreen) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .clickable { viewModel.exitAllFullScreen() }
                                .testTag("exit_fullscreen_pill"),
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            tonalElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FullscreenExit,
                                    contentDescription = stringResource(R.string.cd_exit_fullscreen),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.menu_exit_fullscreen), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMenuSheet) {
        val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            sheetState = menuSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.brand_full_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OperaCrimson
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isVpnActive) VerifiedEmerald.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isVpnActive) "VPN OK" else stringResource(R.string.brand_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isVpnActive) VerifiedEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Top 4 Opera Quick Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Filled.Add,
                        label = stringResource(R.string.menu_new_tab),
                        onClick = {
                            showMenuSheet = false
                            viewModel.createNewTab(isPrivate = false)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("menu_new_tab")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.VisibilityOff,
                        label = stringResource(R.string.menu_private_tab),
                        accentTint = PrivateViolet,
                        onClick = {
                            showMenuSheet = false
                            viewModel.createNewTab(isPrivate = true)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("menu_new_private_tab")
                    )
                    QuickActionTile(
                        icon = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        label = if (isBookmarked) stringResource(R.string.menu_saved) else stringResource(R.string.menu_bookmark),
                        enabled = !activeTab.isHomePage,
                        onClick = {
                            showMenuSheet = false
                            viewModel.toggleCurrentBookmark()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("menu_bookmark_toggle")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Refresh,
                        label = stringResource(R.string.menu_reload),
                        enabled = !activeTab.isHomePage,
                        onClick = {
                            showMenuSheet = false
                            viewModel.reloadOrStop()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Spacer(Modifier.height(10.dp))

                // Opera Grid Tools Rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Filled.Bookmark,
                        label = stringResource(R.string.menu_bookmarks),
                        onClick = { showMenuSheet = false; onOpenBookmarksAndHistory(0) },
                        modifier = Modifier.weight(1f).testTag("menu_bookmarks")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.History,
                        label = stringResource(R.string.menu_history),
                        onClick = { showMenuSheet = false; onOpenBookmarksAndHistory(1) },
                        modifier = Modifier.weight(1f).testTag("menu_history")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.BookmarkAdd,
                        label = stringResource(R.string.menu_reading_list),
                        onClick = {
                            showMenuSheet = false
                            if (!activeTab.isHomePage) viewModel.saveCurrentPageToReadingList()
                            else onOpenBookmarksAndHistory(2)
                        },
                        modifier = Modifier.weight(1f).testTag("menu_reading_list")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.CloudDownload,
                        label = stringResource(R.string.menu_downloads),
                        onClick = { showMenuSheet = false; onOpenDownloads() },
                        modifier = Modifier.weight(1f).testTag("menu_downloads")
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Filled.VpnLock,
                        label = stringResource(R.string.menu_dns_vpn),
                        accentTint = if (isVpnActive) VerifiedEmerald else OperaCrimson,
                        onClick = { showMenuSheet = false; onOpenDnsVpn() },
                        modifier = Modifier.weight(1f).testTag("menu_dns_vpn")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Shield,
                        label = stringResource(R.string.menu_privacy_center),
                        onClick = { showMenuSheet = false; onOpenPrivacyCenter() },
                        modifier = Modifier.weight(1f).testTag("menu_privacy_center")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Key,
                        label = stringResource(R.string.menu_passwords),
                        onClick = { showMenuSheet = false; onOpenPasswordManager() },
                        modifier = Modifier.weight(1f).testTag("menu_passwords")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Extension,
                        label = stringResource(R.string.menu_extensions),
                        onClick = { showMenuSheet = false; onOpenExtensions() },
                        modifier = Modifier.weight(1f).testTag("menu_extensions")
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Filled.FindInPage,
                        label = stringResource(R.string.menu_find_in_page),
                        enabled = !activeTab.isHomePage,
                        onClick = { showMenuSheet = false; viewModel.openFindInPage() },
                        modifier = Modifier.weight(1f).testTag("menu_find")
                    )
                    QuickActionTile(
                        icon = Icons.AutoMirrored.Filled.ChromeReaderMode,
                        label = stringResource(R.string.menu_reader_mode),
                        enabled = !activeTab.isHomePage,
                        onClick = { showMenuSheet = false; viewModel.openReaderMode() },
                        modifier = Modifier.weight(1f).testTag("menu_reader")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.Translate,
                        label = stringResource(R.string.menu_translate_page),
                        onClick = { showMenuSheet = false; showTranslateSheet = true },
                        modifier = Modifier.weight(1f).testTag("menu_translate")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.QrCodeScanner,
                        label = stringResource(R.string.menu_qr_tools),
                        onClick = { showMenuSheet = false; onOpenQrTools() },
                        modifier = Modifier.weight(1f).testTag("menu_qr")
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Filled.CameraAlt,
                        label = stringResource(R.string.menu_screenshot),
                        enabled = !activeTab.isHomePage,
                        onClick = { showMenuSheet = false; captureScreenshot() },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Filled.PictureAsPdf,
                        label = if (activeTab.isHomePage) strings.pdfViewerTitle else stringResource(R.string.menu_export_pdf),
                        onClick = {
                            showMenuSheet = false
                            if (!activeTab.isHomePage) viewModel.exportCurrentPagePdf(context)
                            else onOpenPdfViewer()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        label = stringResource(R.string.menu_dictionary),
                        onClick = { showMenuSheet = false; onOpenDictionary() },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Filled.ZoomIn,
                        label = stringResource(R.string.menu_zoom),
                        onClick = { showMenuSheet = false; showZoomDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                if (!activeTab.isHomePage) {
                    MenuListRow(
                        icon = Icons.Filled.Share,
                        label = stringResource(R.string.menu_share_link),
                        onClick = {
                            showMenuSheet = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
                                putExtra(Intent.EXTRA_TEXT, activeTab.url)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.menu_share_link)))
                        },
                        testTag = "menu_share"
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleDesktopMode() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.DesktopWindows, contentDescription = null)
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.menu_desktop_site), modifier = Modifier.weight(1f))
                    Switch(
                        checked = activeTab.desktopMode,
                        onCheckedChange = { viewModel.toggleDesktopMode() }
                    )
                }

                MenuListRow(
                    icon = Icons.Filled.Fullscreen,
                    label = if (isImmersiveFullScreen) stringResource(R.string.menu_exit_fullscreen)
                    else stringResource(R.string.menu_fullscreen),
                    onClick = {
                        showMenuSheet = false
                        viewModel.toggleManualFullScreen()
                    },
                    testTag = "menu_fullscreen"
                )

                if (!isDefaultBrowser) {
                    MenuListRow(
                        icon = Icons.Filled.Public,
                        label = stringResource(R.string.menu_default_browser),
                        onClick = {
                            showMenuSheet = false
                            onRequestDefaultBrowser()
                        },
                        testTag = "menu_default_browser"
                    )
                }

                MenuListRow(
                    icon = Icons.Filled.Settings,
                    label = stringResource(R.string.menu_settings),
                    onClick = {
                        showMenuSheet = false
                        onOpenSettings()
                    },
                    testTag = "menu_settings"
                )

                MenuListRow(
                    icon = Icons.Filled.Info,
                    label = stringResource(R.string.menu_about),
                    onClick = {
                        showMenuSheet = false
                        onOpenLegalRegistry()
                    },
                    testTag = "menu_about"
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showSecuritySheet) {
        SecurityDetailsSheet(
            tab = activeTab,
            appLanguage = settings.appLanguage,
            onDismiss = { showSecuritySheet = false },
            onOpenSitePermissions = onOpenSitePermissions
        )
    }

    if (showZoomDialog) {
        ZoomControlDialog(
            currentZoom = settings.pageZoomPercent,
            appLanguage = settings.appLanguage,
            onApplyZoom = viewModel::setPageZoomPercent,
            onDismiss = { showZoomDialog = false }
        )
    }

    if (showTranslateSheet) {
        TranslateSheet(
            isOnWebPage = !activeTab.isHomePage,
            appLanguage = settings.appLanguage,
            onTranslatePage = viewModel::translateCurrentPage,
            onTranslateText = { text, target ->
                viewModel.navigateCurrentTab(viewModel.buildTextTranslationUrl(text, target))
            },
            onDismiss = { showTranslateSheet = false }
        )
    }

    if (isReaderLoading || readerArticle != null) {
        ReaderModeSheet(
            article = readerArticle,
            isLoading = isReaderLoading,
            appLanguage = settings.appLanguage,
            onDismiss = viewModel::closeReaderMode
        )
    }

    pendingSaveCredential?.let { saveReq ->
        SaveCredentialDialog(
            pending = saveReq,
            onConfirm = viewModel::confirmPendingSaveCredential,
            onDismiss = viewModel::dismissPendingSaveCredential
        )
    }

    pendingPrompt?.let { prompt ->
        WebPromptDialog(
            prompt = prompt,
            onDismiss = viewModel::dismissWebPrompt,
            onConfirmAlert = viewModel::confirmJsAlert,
            onResolveConfirm = viewModel::resolveJsConfirm,
            onResolveText = viewModel::resolveJsText,
            onResolveChoice = viewModel::resolveChoicePrompt
        )
    }

    pendingSitePermission?.let { req ->
        SitePermissionDialog(
            request = req,
            appLanguage = settings.appLanguage,
            onDecision = viewModel::resolveSitePermission
        )
    }

    pendingDownload?.let { dl ->
        DownloadConfirmDialog(
            request = dl,
            onConfirm = { viewModel.confirmDownload(dl) },
            onDismiss = viewModel::dismissDownloadPrompt
        )
    }
}

private suspend fun saveScreenshotToGallery(context: Context, bitmap: Bitmap): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val fileName = "ModLU_Screenshot_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ModLUBrowserX")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                    ?: return@runCatching false
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            } else {
                val file = java.io.File(context.cacheDir, fileName)
                file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                true
            }
        }.getOrDefault(false)
    }

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) accentTint else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MenuListRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.width(14.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
