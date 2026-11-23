package com.example.ui.state

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.local.BrowserExtensionEntity
import com.example.data.local.DictionaryEntryEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.DownloadStatus
import com.example.data.local.HistoryEntity
import com.example.data.local.PermissionDecision
import com.example.data.local.ReadingListEntity
import com.example.data.local.SavedCredentialEntity
import com.example.data.local.SitePermissionEntity
import com.example.data.local.SpeedDialEntity
import com.example.data.preferences.BrowserSettingsState
import com.example.data.preferences.CookiePolicy
import com.example.data.preferences.DnsProvider
import com.example.data.preferences.HomeWallpaperPreset
import com.example.data.preferences.HomepageMode
import com.example.data.preferences.SearchEngine
import com.example.data.preferences.SettingsRepository
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.ToolbarStyle
import com.example.data.preferences.TrackingProtectionLevel
import com.example.data.repository.BrowserDataRepository
import com.example.data.repository.CredentialRepository
import com.example.data.repository.DecryptedCredential
import com.example.data.repository.DictionaryEntryItem
import com.example.data.repository.DownloadBridge
import com.example.data.repository.OmniboxSuggestion
import com.example.data.repository.ReaderArticle
import com.example.data.repository.SearchSuggestionRepository
import com.example.data.repository.ToolsRepository
import com.example.data.repository.WeatherSnapshot
import com.example.data.autofill.BrowserAutofillTracker
import com.example.data.vpn.BrowserDnsVpnService
import com.example.engine.BrowserEngine
import com.example.engine.BrowserTab
import com.example.engine.EngineSessionHandle
import com.example.engine.FindInPageState
import com.example.engine.PageLoadError
import com.example.engine.PendingAndroidPermissionRequest
import com.example.engine.PendingDownloadRequest
import com.example.engine.PendingFileUpload
import com.example.engine.PendingSitePermission
import com.example.engine.PendingWebPrompt
import com.example.engine.SecurityDetails
import com.example.engine.SecurityState
import com.example.engine.SessionEventCallbacks
import com.example.engine.UrlUtils
import com.example.i18n.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoSession
import java.io.InputStream
import java.net.URLEncoder
import java.util.UUID

data class PendingSaveCredential(
    val origin: String,
    val username: String,
    val password: String
)

class BrowserViewModel(
    private val browserEngine: BrowserEngine,
    private val dataRepository: BrowserDataRepository,
    private val settingsRepository: SettingsRepository,
    private val suggestionRepository: SearchSuggestionRepository,
    private val downloadBridge: DownloadBridge,
    private val credentialRepository: CredentialRepository,
    private val toolsRepository: ToolsRepository
) : ViewModel(), SessionEventCallbacks {

    private val sessionHandles = mutableMapOf<String, EngineSessionHandle>()
    private var suggestionJob: Job? = null
    private var lastRecordedHistoryUrl: String? = null
    private var lastRecordedHistoryTime: Long = 0L

    private val bundledAdBlockHosts: Set<String> by lazy {
        toolsRepository.loadAdBlockHosts()
    }

    val bundledDictionary: List<DictionaryEntryItem> by lazy {
        toolsRepository.loadBundledDictionary()
    }

    val settings: StateFlow<BrowserSettingsState> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = BrowserSettingsState()
        )

    val bookmarks: StateFlow<List<BookmarkEntity>> = dataRepository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = dataRepository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentVisitedSites: StateFlow<List<HistoryEntity>> = history
        .map { list ->
            list.distinctBy { UrlUtils.extractHost(it.url).ifBlank { it.url } }.take(8)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = dataRepository.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sitePermissions: StateFlow<List<SitePermissionEntity>> = dataRepository.sitePermissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val speedDials: StateFlow<List<SpeedDialEntity>> = dataRepository.speedDials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val readingList: StateFlow<List<ReadingListEntity>> = dataRepository.readingList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val extensions: StateFlow<List<BrowserExtensionEntity>> = dataRepository.extensions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dictionaryEntries: StateFlow<List<DictionaryEntryEntity>> = dataRepository.dictionaryEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedCredentials: StateFlow<List<SavedCredentialEntity>> = credentialRepository.allCredentials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isVpnActive: StateFlow<Boolean> = BrowserDnsVpnService.isRunning

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _activeTab = MutableStateFlow(BrowserTab(id = "bootstrap"))
    val activeTab: StateFlow<BrowserTab> = _activeTab.asStateFlow()

    val isActiveTabBookmarked: StateFlow<Boolean> = combine(activeTab, bookmarks) { tab, list ->
        !tab.isHomePage && list.any { it.url.equals(tab.url, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isActiveTabInReadingList: StateFlow<Boolean> = combine(activeTab, readingList) { tab, list ->
        !tab.isHomePage && list.any { it.url.equals(tab.url, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _suggestions = MutableStateFlow<List<OmniboxSuggestion>>(emptyList())
    val suggestions: StateFlow<List<OmniboxSuggestion>> = _suggestions.asStateFlow()

    private val _pendingPrompt = MutableStateFlow<PendingWebPrompt?>(null)
    val pendingPrompt: StateFlow<PendingWebPrompt?> = _pendingPrompt.asStateFlow()

    private val _pendingFileUpload = MutableStateFlow<PendingFileUpload?>(null)
    val pendingFileUpload: StateFlow<PendingFileUpload?> = _pendingFileUpload.asStateFlow()

    private val _pendingSitePermission = MutableStateFlow<PendingSitePermission?>(null)
    val pendingSitePermission: StateFlow<PendingSitePermission?> = _pendingSitePermission.asStateFlow()

    private val _pendingAndroidPermissions =
        MutableStateFlow<PendingAndroidPermissionRequest?>(null)
    val pendingAndroidPermissions: StateFlow<PendingAndroidPermissionRequest?> =
        _pendingAndroidPermissions.asStateFlow()

    private val _pendingDownload = MutableStateFlow<PendingDownloadRequest?>(null)
    val pendingDownload: StateFlow<PendingDownloadRequest?> = _pendingDownload.asStateFlow()

    private val _manualFullScreen = MutableStateFlow(false)
    val manualFullScreen: StateFlow<Boolean> = _manualFullScreen.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _findInPageState = MutableStateFlow(FindInPageState())
    val findInPageState: StateFlow<FindInPageState> = _findInPageState.asStateFlow()

    private val _weatherSnapshot = MutableStateFlow<WeatherSnapshot?>(null)
    val weatherSnapshot: StateFlow<WeatherSnapshot?> = _weatherSnapshot.asStateFlow()

    private val _readerArticle = MutableStateFlow<ReaderArticle?>(null)
    val readerArticle: StateFlow<ReaderArticle?> = _readerArticle.asStateFlow()

    private val _isReaderLoading = MutableStateFlow(false)
    val isReaderLoading: StateFlow<Boolean> = _isReaderLoading.asStateFlow()

    private val _pendingSaveCredential = MutableStateFlow<PendingSaveCredential?>(null)
    val pendingSaveCredential: StateFlow<PendingSaveCredential?> = _pendingSaveCredential.asStateFlow()

    private val _pendingAutofillCredentials = MutableStateFlow<List<DecryptedCredential>>(emptyList())
    val pendingAutofillCredentials: StateFlow<List<DecryptedCredential>> =
        _pendingAutofillCredentials.asStateFlow()

    init {
        viewModelScope.launch {
            dataRepository.seedDefaultsIfEmpty()
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { latest ->
                BrowserAutofillTracker.isAutofillEnabled = latest.passwordSavePromptEnabled
                browserEngine.applySettings(latest)
                if (latest.showWeatherOnHome && _weatherSnapshot.value == null) {
                    refreshWeather()
                }
            }
        }
        viewModelScope.launch {
            combine(dataRepository.extensions, settingsRepository.settingsFlow) { exts, st ->
                val customHosts = exts
                    .filter { it.isEnabled && it.customRuleDomains.isNotBlank() }
                    .flatMap { it.customRuleDomains.split(",", "\n", ";") }
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()
                val blocked = if (st.adFilterRulesEnabled) {
                    bundledAdBlockHosts + customHosts
                } else {
                    customHosts
                }
                exts to blocked
            }.collect { (exts, blocked) ->
                browserEngine.syncExtensions(exts, blocked)
            }
        }
        if (_tabs.value.isEmpty()) {
            createNewTab(isPrivate = false, initialUrl = null)
        }
    }

    fun getGeckoSession(tabId: String): GeckoSession? {
        return sessionHandles[tabId]?.geckoSessionOrNull()
    }

    // --- Tab Management (Multi-tab, Groups, Pinning) ---

    fun createNewTab(
        isPrivate: Boolean = false,
        initialUrl: String? = null,
        groupName: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val currentSettings = settings.value
        val resolvedStartUrl = when {
            !initialUrl.isNullOrBlank() -> initialUrl
            currentSettings.homepageMode == HomepageMode.CUSTOM_URL -> currentSettings.customHomepageUrl
            else -> null
        }

        val handle = browserEngine.createSession(
            tabId = id,
            isPrivate = isPrivate,
            desktopMode = false,
            settings = currentSettings,
            callbacks = this
        )
        sessionHandles[id] = handle

        val showHome = resolvedStartUrl == null || UrlUtils.isHomeUri(resolvedStartUrl)
        val initialTabUrl = if (showHome) UrlUtils.INTERNAL_HOME_URI else resolvedStartUrl
        val tab = BrowserTab(
            id = id,
            title = defaultTabTitle(isPrivate),
            url = initialTabUrl,
            isPrivate = isPrivate,
            isHomePage = showHome,
            groupName = groupName,
            securityState = if (showHome) SecurityState.HOME_INTERNAL else SecurityState.INSECURE_HTTP
        )

        _tabs.update { sortTabs(it + tab) }
        _activeTabId.value = id
        syncActiveTab()
        _manualFullScreen.value = false
        closeFindInPage()
        _pendingAutofillCredentials.value = emptyList()

        if (!showHome) {
            handle.loadUri(initialTabUrl)
        }
        return id
    }

    fun switchTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
            syncActiveTab()
            _suggestions.value = emptyList()
            _manualFullScreen.value = false
            closeFindInPage()
            val tab = activeTab.value
            if (!tab.isHomePage && !tab.isPrivate) {
                refreshAutofillForUrl(tab.url)
            } else {
                _pendingAutofillCredentials.value = emptyList()
            }
        }
    }

    fun togglePinTab(tabId: String) {
        _tabs.update { list ->
            val updated = list.map { tab ->
                if (tab.id == tabId) tab.copy(isPinned = !tab.isPinned) else tab
            }
            sortTabs(updated)
        }
        syncActiveTab()
    }

    fun setTabGroup(tabId: String, groupName: String?) {
        val cleaned = groupName?.trim()?.takeIf { it.isNotBlank() }
        updateTab(tabId) { it.copy(groupName = cleaned) }
    }

    fun closeTab(tabId: String) {
        val existingTabs = _tabs.value
        val closingTab = existingTabs.firstOrNull { it.id == tabId } ?: return
        val closingIndex = existingTabs.indexOfFirst { it.id == tabId }

        sessionHandles.remove(tabId)?.close()
        val remaining = existingTabs.filterNot { it.id == tabId }
        _tabs.value = remaining

        if (closingTab.isPrivate && remaining.none { it.isPrivate }) {
            browserEngine.clearPrivateSessionData()
        }

        if (remaining.isEmpty()) {
            createNewTab(isPrivate = false, initialUrl = null)
        } else if (_activeTabId.value == tabId) {
            val nextIndex = (closingIndex - 1).coerceIn(0, remaining.lastIndex)
            _activeTabId.value = remaining[nextIndex].id
            syncActiveTab()
        } else {
            syncActiveTab()
        }
        _manualFullScreen.value = false
        closeFindInPage()
    }

    fun closeAllPrivateTabs() {
        val current = _tabs.value
        val privateTabs = current.filter { it.isPrivate }
        if (privateTabs.isEmpty()) return

        privateTabs.forEach { tab ->
            sessionHandles.remove(tab.id)?.close()
        }
        browserEngine.clearPrivateSessionData()

        val standardTabs = current.filterNot { it.isPrivate }
        _tabs.value = standardTabs
        if (standardTabs.isEmpty()) {
            createNewTab(isPrivate = false, initialUrl = null)
        } else if (standardTabs.none { it.id == _activeTabId.value }) {
            _activeTabId.value = standardTabs.last().id
            syncActiveTab()
        } else {
            syncActiveTab()
        }
        showBanner(msg(tr = "Tüm gizli sekmeler ve oturum verileri temizlendi", en = "All private tabs & ephemeral data cleared"))
    }

    private fun sortTabs(list: List<BrowserTab>): List<BrowserTab> {
        val pinned = list.filter { it.isPinned }
        val unpinned = list.filterNot { it.isPinned }
        return pinned + unpinned
    }

    // --- Navigation & Omnibox ---

    fun submitOmniboxInput(rawInput: String) {
        _suggestions.value = emptyList()
        val resolved = UrlUtils.resolveInputToUri(rawInput, settings.value.searchEngine)
        navigateCurrentTab(resolved)
    }

    fun navigateCurrentTab(uri: String) {
        val tab = activeTab.value
        if (UrlUtils.isHomeUri(uri)) {
            goHome()
            return
        }
        closeFindInPage()
        _pendingAutofillCredentials.value = emptyList()
        updateTab(tab.id) {
            it.copy(
                isHomePage = false,
                url = uri,
                isLoading = true,
                progress = 10,
                loadError = null,
                blockedTrackersCount = 0
            )
        }
        sessionHandles[tab.id]?.loadUri(uri)
    }

    fun openUrlFromExternalIntent(uri: String) {
        if (!UrlUtils.isSafeExternalNavigationUri(uri)) return
        val current = activeTab.value
        if (current.isHomePage && !current.isPrivate) {
            navigateCurrentTab(uri)
        } else {
            createNewTab(isPrivate = false, initialUrl = uri)
        }
    }

    fun goHome() {
        val tab = activeTab.value
        val currentSettings = settings.value
        if (currentSettings.homepageMode == HomepageMode.CUSTOM_URL) {
            navigateCurrentTab(currentSettings.customHomepageUrl)
            return
        }
        sessionHandles[tab.id]?.stop()
        closeFindInPage()
        _pendingAutofillCredentials.value = emptyList()
        updateTab(tab.id) {
            it.copy(
                isHomePage = true,
                title = defaultTabTitle(it.isPrivate),
                url = UrlUtils.INTERNAL_HOME_URI,
                isLoading = false,
                progress = 0,
                isAtTop = true,
                securityState = SecurityState.HOME_INTERNAL,
                securityDetails = SecurityDetails(),
                loadError = null,
                isFullScreen = false
            )
        }
        _manualFullScreen.value = false
    }

    fun goBack() {
        val tab = activeTab.value
        if (tab.isFullScreen) {
            sessionHandles[tab.id]?.exitFullScreen()
            return
        }
        if (_manualFullScreen.value) {
            _manualFullScreen.value = false
            return
        }
        if (tab.canGoBack && !tab.isHomePage) {
            updateTab(tab.id) { it.copy(loadError = null) }
            sessionHandles[tab.id]?.goBack()
        } else if (!tab.isHomePage) {
            goHome()
        }
    }

    fun goForward() {
        val tab = activeTab.value
        if (tab.canGoForward) {
            updateTab(tab.id) { it.copy(loadError = null) }
            sessionHandles[tab.id]?.goForward()
        }
    }

    fun reloadOrStop() {
        val tab = activeTab.value
        if (tab.isHomePage) return
        if (tab.isLoading) {
            sessionHandles[tab.id]?.stop()
            updateTab(tab.id) { it.copy(isLoading = false, progress = 0) }
        } else {
            updateTab(tab.id) { it.copy(loadError = null, isLoading = true, progress = 15) }
            sessionHandles[tab.id]?.reload()
        }
    }

    fun toggleDesktopMode() {
        val tab = activeTab.value
        val next = !tab.desktopMode
        updateTab(tab.id) { it.copy(desktopMode = next) }
        sessionHandles[tab.id]?.setDesktopMode(next)
        showBanner(
            if (next) msg("Masaüstü görünümü istendi", "Desktop site requested")
            else msg("Mobil görünüm istendi", "Mobile site requested")
        )
    }

    fun toggleManualFullScreen() {
        val tab = activeTab.value
        if (tab.isFullScreen) {
            sessionHandles[tab.id]?.exitFullScreen()
        } else {
            _manualFullScreen.update { !it }
        }
    }

    fun exitAllFullScreen() {
        val tab = activeTab.value
        if (tab.isFullScreen) {
            sessionHandles[tab.id]?.exitFullScreen()
        }
        _manualFullScreen.value = false
    }

    fun updateOmniboxSuggestions(query: String) {
        suggestionJob?.cancel()
        if (query.trim().length < 2) {
            _suggestions.value = emptyList()
            return
        }
        val currentSettings = settings.value
        val isPrivate = activeTab.value.isPrivate
        suggestionJob = viewModelScope.launch {
            delay(200)
            val items = suggestionRepository.fetchSuggestions(
                query = query,
                searchEngine = currentSettings.searchEngine,
                remoteSuggestionsAllowed = currentSettings.searchSuggestionsEnabled,
                isPrivateTab = isPrivate
            )
            _suggestions.value = items
        }
    }

    fun clearSuggestions() {
        suggestionJob?.cancel()
        _suggestions.value = emptyList()
    }

    // --- Find in Page, Reader Mode, Translation, PDF ---

    fun openFindInPage() {
        if (activeTab.value.isHomePage) return
        _findInPageState.update { it.copy(active = true) }
    }

    fun findInPage(query: String, backwards: Boolean = false) {
        val tab = activeTab.value
        if (tab.isHomePage) return
        _findInPageState.update { it.copy(active = true, query = query) }
        if (query.isBlank()) {
            sessionHandles[tab.id]?.clearFindInPage()
            _findInPageState.update { it.copy(currentMatch = 0, totalMatches = 0) }
            return
        }
        sessionHandles[tab.id]?.findInPage(query, backwards) { found, current, total ->
            _findInPageState.update {
                it.copy(
                    currentMatch = if (found) current else 0,
                    totalMatches = if (found) total else 0
                )
            }
        }
    }

    fun closeFindInPage() {
        val tab = activeTab.value
        sessionHandles[tab.id]?.clearFindInPage()
        _findInPageState.value = FindInPageState()
    }

    fun openReaderMode() {
        val tab = activeTab.value
        if (tab.isHomePage) return
        _isReaderLoading.value = true
        viewModelScope.launch {
            val article = toolsRepository.extractReaderArticle(tab.url)
            _isReaderLoading.value = false
            if (article == null || article.paragraphs.isEmpty()) {
                showBanner(msg("Bu sayfa için okuma modu çıkarılamadı", "Reader mode unavailable for this page"))
            } else {
                _readerArticle.value = article
            }
        }
    }

    fun closeReaderMode() {
        _readerArticle.value = null
    }

    fun translateCurrentPage(targetLangTag: String) {
        val tab = activeTab.value
        if (tab.isHomePage || !UrlUtils.isSafeExternalNavigationUri(tab.url)) return
        val encodedUrl = runCatching { URLEncoder.encode(tab.url, "UTF-8") }.getOrDefault(tab.url)
        val lang = targetLangTag.take(2).lowercase()
        val translateUrl = "https://translate.google.com/translate?sl=auto&tl=$lang&u=$encodedUrl"
        navigateCurrentTab(translateUrl)
    }

    fun buildTextTranslationUrl(text: String, targetLangTag: String): String {
        val encoded = runCatching { URLEncoder.encode(text.trim(), "UTF-8") }.getOrDefault(text)
        val lang = targetLangTag.take(2).lowercase()
        return "https://translate.google.com/?sl=auto&tl=$lang&text=$encoded&op=translate"
    }

    fun exportCurrentPagePdf(context: Context) {
        val tab = activeTab.value
        if (tab.isHomePage) return
        val handle = sessionHandles[tab.id] ?: return
        handle.exportPdf { stream ->
            if (stream == null) {
                showBanner(msg("Bu sayfa PDF olarak dışa aktarılamadı", "Could not export page as PDF"))
                return@exportPdf
            }
            viewModelScope.launch {
                val savedUri = savePdfStreamToDownloads(context, tab.title, stream)
                if (savedUri != null) {
                    dataRepository.insertDownload(
                        DownloadEntity(
                            fileName = "${tab.title.ifBlank { "sayfa" }.take(32)}.pdf",
                            url = tab.url,
                            mimeType = "application/pdf",
                            status = DownloadStatus.COMPLETED,
                            localUri = savedUri.toString(),
                            downloadedBytes = 1L,
                            totalBytes = 1L
                        )
                    )
                    showBanner(msg("PDF İndirilenler'e kaydedildi", "PDF saved to Downloads"))
                } else {
                    showBanner(msg("PDF kaydedilemedi", "Failed to save PDF"))
                }
            }
        }
    }

    private suspend fun savePdfStreamToDownloads(
        context: Context,
        title: String,
        stream: InputStream
    ): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(36).ifBlank { "ModLU_Page" } + ".pdf"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val cv = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                    ?: return@runCatching null
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    stream.use { it.copyTo(out) }
                }
                uri
            } else {
                val file = java.io.File(context.cacheDir, safeName)
                file.outputStream().use { out -> stream.use { it.copyTo(out) } }
                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        }.getOrNull()
    }

    // --- Bookmarks & Reading List ---

    fun toggleCurrentBookmark() {
        val tab = activeTab.value
        if (tab.isHomePage) return
        viewModelScope.launch {
            val saved = dataRepository.toggleBookmark(tab.title, tab.url)
            showBanner(if (saved) msg("Yer imi kaydedildi", "Bookmark saved") else msg("Yer imi kaldırıldı", "Bookmark removed"))
        }
    }

    fun saveBookmark(title: String, url: String, folder: String = "Genel") {
        viewModelScope.launch {
            dataRepository.saveBookmark(title, url, folder)
            showBanner(msg("Yer imi kaydedildi", "Bookmark saved"))
        }
    }

    fun updateBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            dataRepository.updateBookmark(bookmark)
            showBanner(msg("Yer imi güncellendi", "Bookmark updated"))
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { dataRepository.deleteBookmark(id) }
    }

    fun saveCurrentPageToReadingList() {
        val tab = activeTab.value
        if (tab.isHomePage) return
        viewModelScope.launch {
            dataRepository.saveToReadingList(tab.title, tab.url)
            showBanner(msg("Okuma listesine eklendi", "Saved to Reading List"))
        }
    }

    fun toggleReadingListRead(id: Long, isRead: Boolean) {
        viewModelScope.launch { dataRepository.toggleReadingListRead(id, isRead) }
    }

    fun deleteReadingListItem(id: Long) {
        viewModelScope.launch { dataRepository.deleteReadingListItem(id) }
    }

    fun clearReadingList() {
        viewModelScope.launch { dataRepository.clearReadingList() }
    }

    // --- History ---

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { dataRepository.deleteHistoryItem(id) }
    }

    fun clearHistoryLastHours(hours: Int) {
        viewModelScope.launch {
            dataRepository.clearHistoryLastHours(hours)
            showBanner(msg("Son geçmiş temizlendi", "Cleared recent history"))
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            dataRepository.clearAllHistory()
            showBanner(msg("Tarama geçmişi temizlendi", "Browsing history cleared"))
        }
    }

    // --- Speed Dials & Weather ---

    fun addSpeedDial(title: String, rawUrl: String) {
        val resolved = UrlUtils.resolveInputToUri(rawUrl, settings.value.searchEngine)
        viewModelScope.launch { dataRepository.addSpeedDial(title, resolved) }
    }

    fun deleteSpeedDial(id: Long) {
        viewModelScope.launch { dataRepository.deleteSpeedDial(id) }
    }

    fun refreshWeather() {
        val s = settings.value
        viewModelScope.launch {
            val snap = toolsRepository.fetchWeather(s.weatherCity, s.weatherLatitude, s.weatherLongitude)
            if (snap != null) {
                _weatherSnapshot.value = snap
            }
        }
    }

    fun setWeatherConfig(city: String, lat: Double, lon: Double, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWeatherCity(city, lat, lon, enabled)
            if (enabled) {
                val snap = toolsRepository.fetchWeather(city, lat, lon)
                if (snap != null) _weatherSnapshot.value = snap
            }
        }
    }

    // --- Background Downloads ---

    fun confirmDownload(request: PendingDownloadRequest) {
        _pendingDownload.value = null
        viewModelScope.launch {
            val id = downloadBridge.enqueueDownload(
                url = request.url,
                suggestedFileName = request.fileName,
                mimeType = request.mimeType,
                contentLength = request.contentLength
            )
            if (id > 0L) {
                showBanner(msg("İndiriliyor: ${request.fileName}", "Downloading ${request.fileName}"))
            } else {
                showBanner(msg("İndirme başlatılamadı", "Unable to start download"))
            }
        }
    }

    fun dismissDownloadPrompt() {
        _pendingDownload.value = null
    }

    fun pauseDownload(id: Long) = downloadBridge.pauseDownload(id)

    fun resumeDownload(id: Long) = downloadBridge.resumeDownload(id)

    fun cancelDownload(item: DownloadEntity) = downloadBridge.cancelDownload(item)

    fun retryDownload(item: DownloadEntity) {
        downloadBridge.resumeDownload(item.id)
    }

    fun onSystemDownloadCompleted(systemDownloadId: Long) {
        viewModelScope.launch {
            downloadBridge.handleSystemDownloadCompleted(systemDownloadId)
        }
    }

    fun openDownload(item: DownloadEntity) {
        val opened = downloadBridge.openDownloadedFile(item)
        if (!opened) {
            showBanner(msg("Bu dosyayı açacak uygulama bulunamadı: ${item.fileName}", "No app available to open ${item.fileName}"))
        }
    }

    fun deleteDownload(item: DownloadEntity) {
        downloadBridge.cancelDownload(item)
        viewModelScope.launch { dataRepository.deleteDownload(item.id) }
    }

    fun clearDownloadHistory() {
        viewModelScope.launch { dataRepository.clearDownloads() }
    }

    // --- Password Manager (Disabled in Private Mode) ---

    fun saveCredential(origin: String, username: String, password: String) {
        viewModelScope.launch {
            val ok = credentialRepository.saveCredential(origin, username, password)
            if (ok) {
                showBanner(msg("Giriş bilgileri güvenli kasaya kaydedildi", "Credentials saved to secure vault"))
            }
        }
    }

    fun updateCredential(id: Long, origin: String, username: String, password: String) {
        viewModelScope.launch {
            val ok = credentialRepository.updateCredential(id, origin, username, password)
            if (ok) {
                showBanner(msg("Kayıtlı şifre güncellendi", "Saved credential updated"))
            }
        }
    }

    fun isHardwareKeystoreAvailable(): Boolean = credentialRepository.isHardwareKeystoreAvailable()

    fun fillPendingCredential(cred: DecryptedCredential): Boolean {
        val tab = activeTab.value
        if (tab.isPrivate) return false
        val filled = sessionHandles[tab.id]?.performAutofill(cred.username, cred.password) == true
        _pendingAutofillCredentials.value = emptyList()
        if (filled) {
            showBanner(msg("${cred.username} otomatik dolduruldu", "Autofilled ${cred.username}"))
        }
        return filled
    }

    fun confirmPendingSaveCredential() {
        val pending = _pendingSaveCredential.value ?: return
        _pendingSaveCredential.value = null
        saveCredential(pending.origin, pending.username, pending.password)
    }

    fun dismissPendingSaveCredential() {
        _pendingSaveCredential.value = null
    }

    fun decryptCredentialPassword(entity: SavedCredentialEntity): String? =
        credentialRepository.decryptPassword(entity)

    fun deleteCredential(id: Long) {
        viewModelScope.launch { credentialRepository.deleteCredential(id) }
    }

    fun clearAllCredentials() {
        viewModelScope.launch {
            credentialRepository.clearAllCredentials()
            showBanner(msg("Kayıtlı tüm şifreler silindi", "All saved passwords erased"))
        }
    }

    fun dismissAutofillBanner() {
        _pendingAutofillCredentials.value = emptyList()
    }

    private fun refreshAutofillForUrl(url: String) {
        val tab = activeTab.value
        if (tab.isPrivate || tab.isHomePage || !settings.value.passwordSavePromptEnabled) {
            _pendingAutofillCredentials.value = emptyList()
            return
        }
        viewModelScope.launch {
            _pendingAutofillCredentials.value =
                credentialRepository.getCredentialsForOrigin(url, isPrivateSession = false)
        }
    }

    // --- Extensions & Offline Dictionary ---

    fun setExtensionEnabled(extensionId: String, enabled: Boolean) {
        viewModelScope.launch { dataRepository.setExtensionEnabled(extensionId, enabled) }
    }

    fun addCustomExtension(name: String, description: String, domains: String) {
        viewModelScope.launch {
            dataRepository.addCustomFilterExtension(name, description, domains)
            showBanner(msg("Özel kural eklentisi eklendi", "Custom rule extension added"))
        }
    }

    fun deleteCustomExtension(extensionId: String) {
        viewModelScope.launch { dataRepository.deleteCustomExtension(extensionId) }
    }

    fun addDictionaryTerm(term: String, trDef: String, enDef: String) {
        viewModelScope.launch {
            dataRepository.addDictionaryTerm(term, trDef, enDef)
            showBanner(msg("Terim sözlüğe eklendi", "Term saved to dictionary"))
        }
    }

    fun deleteDictionaryTerm(id: Long) {
        viewModelScope.launch { dataRepository.deleteDictionaryTerm(id) }
    }

    // --- Settings, Language, DNS & VPN ---

    fun setAppLanguage(tag: String) {
        viewModelScope.launch {
            settingsRepository.setAppLanguage(tag)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { settingsRepository.setSearchEngine(engine) }
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setJavaScriptEnabled(enabled)
            showBanner(
                if (enabled) msg("JavaScript etkinleştirildi", "JavaScript enabled")
                else msg("JavaScript devre dışı (sayfayı yenileyin)", "JavaScript disabled (reload page)")
            )
        }
    }

    fun setTrackingProtection(level: TrackingProtectionLevel) {
        viewModelScope.launch {
            settingsRepository.setTrackingProtection(level)
            val enableSessionEtp = level != TrackingProtectionLevel.OFF
            _tabs.value.forEach { tab ->
                sessionHandles[tab.id]?.setTrackingProtection(tab.isPrivate || enableSessionEtp)
            }
        }
    }

    fun setCookiePolicy(policy: CookiePolicy) {
        viewModelScope.launch { settingsRepository.setCookiePolicy(policy) }
    }

    fun setSearchSuggestionsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSearchSuggestionsEnabled(enabled) }
    }

    fun setSaveHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSaveHistoryEnabled(enabled) }
    }

    fun setPrivacyToggles(
        popupBlocking: Boolean? = null,
        httpsOnly: Boolean? = null,
        adFilterRules: Boolean? = null,
        passwordPrompt: Boolean? = null
    ) {
        viewModelScope.launch {
            popupBlocking?.let { settingsRepository.setPopupBlockingEnabled(it) }
            httpsOnly?.let { settingsRepository.setHttpsOnlyEnabled(it) }
            adFilterRules?.let { settingsRepository.setAdFilterRulesEnabled(it) }
            passwordPrompt?.let { settingsRepository.setPasswordSavePromptEnabled(it) }
        }
    }

    fun updateHomepageConfig(
        mode: HomepageMode,
        customUrl: String,
        showSpeedDials: Boolean,
        showRecentSites: Boolean = settings.value.showRecentSitesOnHome,
        showWeather: Boolean = settings.value.showWeatherOnHome
    ) {
        viewModelScope.launch {
            settingsRepository.setHomepageConfiguration(
                mode = mode,
                customUrl = customUrl,
                showSpeedDials = showSpeedDials,
                showRecentSites = showRecentSites,
                showWeather = showWeather
            )
            showBanner(msg("Ana sayfa tercihleri güncellendi", "Homepage preferences updated"))
        }
    }

    fun setHomeWallpaper(preset: HomeWallpaperPreset, customUri: String?) {
        viewModelScope.launch {
            settingsRepository.setHomeWallpaper(preset, customUri)
        }
    }

    fun setToolbarStyle(style: ToolbarStyle) {
        viewModelScope.launch { settingsRepository.setToolbarStyle(style) }
    }

    fun setGestures(edgeGestures: Boolean, pullToRefresh: Boolean) {
        viewModelScope.launch { settingsRepository.setGestures(edgeGestures, pullToRefresh) }
    }

    fun setPageZoomPercent(percent: Int) {
        viewModelScope.launch { settingsRepository.setPageZoomPercent(percent) }
    }

    fun setDnsConfiguration(
        provider: DnsProvider,
        customPrimary: String,
        customSecondary: String,
        customDohUri: String
    ) {
        viewModelScope.launch {
            settingsRepository.setDnsConfiguration(provider, customPrimary, customSecondary, customDohUri)
            showBanner(msg("DNS profili güncellendi", "DNS profile updated"))
        }
    }

    fun setVpnShieldActive(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVpnShieldEnabled(enabled)
        }
        val s = settings.value
        val (primary, secondary) = if (s.dnsProvider == DnsProvider.CUSTOM) {
            s.customDnsPrimary to s.customDnsSecondary
        } else {
            (s.dnsProvider.primaryIp.ifBlank { "1.1.1.1" }) to (s.dnsProvider.secondaryIp.ifBlank { "1.0.0.1" })
        }
        if (enabled) {
            BrowserDnsVpnService.start(context, primary, secondary, s.dnsProvider.displayName)
            showBanner(msg("DNS VPN Kalkanı başlatıldı", "DNS VPN Shield started"))
        } else {
            BrowserDnsVpnService.stop(context)
            showBanner(msg("DNS VPN Kalkanı durduruldu", "DNS VPN Shield stopped"))
        }
    }

    fun clearEngineSiteData(clearCookies: Boolean, clearCache: Boolean, clearSiteData: Boolean) {
        browserEngine.clearBrowsingData(clearCookies, clearCache, clearSiteData)
        showBanner(msg("Tarayıcı site verileri temizlendi", "Browser site data cleared"))
    }

    // --- Site Permissions ---

    fun updateSitePermissionRule(
        origin: String,
        permissionType: String,
        decision: PermissionDecision
    ) {
        viewModelScope.launch {
            dataRepository.setPermissionDecision(origin, permissionType, decision)
        }
    }

    fun deleteSitePermissionRule(id: Long) {
        viewModelScope.launch { dataRepository.deleteSitePermission(id) }
    }

    fun clearAllSitePermissions() {
        viewModelScope.launch {
            dataRepository.clearAllSitePermissions()
            showBanner(msg("Site izinleri sıfırlandı", "Site permissions reset"))
        }
    }

    // --- Prompt / Permission / File Upload resolutions ---

    fun dismissWebPrompt() {
        val current = _pendingPrompt.value ?: return
        when (current) {
            is PendingWebPrompt.JsAlert -> current.result.complete(current.prompt.dismiss())
            is PendingWebPrompt.JsConfirm -> current.result.complete(current.prompt.dismiss())
            is PendingWebPrompt.JsText -> current.result.complete(current.prompt.dismiss())
            is PendingWebPrompt.Choice -> current.result.complete(current.prompt.dismiss())
        }
        _pendingPrompt.value = null
    }

    fun confirmJsAlert(prompt: PendingWebPrompt.JsAlert) {
        prompt.result.complete(prompt.prompt.dismiss())
        _pendingPrompt.value = null
    }

    fun resolveJsConfirm(prompt: PendingWebPrompt.JsConfirm, confirmed: Boolean) {
        val buttonType = if (confirmed) {
            GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE
        } else {
            GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE
        }
        prompt.result.complete(prompt.prompt.confirm(buttonType))
        _pendingPrompt.value = null
    }

    fun resolveJsText(prompt: PendingWebPrompt.JsText, text: String) {
        prompt.result.complete(prompt.prompt.confirm(text))
        _pendingPrompt.value = null
    }

    fun resolveChoicePrompt(prompt: PendingWebPrompt.Choice, selectedIds: List<String>) {
        prompt.result.complete(prompt.prompt.confirm(selectedIds.toTypedArray()))
        _pendingPrompt.value = null
    }

    fun resolveFileUpload(uris: Array<Uri>?) {
        val upload = _pendingFileUpload.value ?: return
        _pendingFileUpload.value = null
        upload.onFilesSelected(uris)
    }

    fun resolveAndroidPermissions(granted: Boolean) {
        val req = _pendingAndroidPermissions.value ?: return
        _pendingAndroidPermissions.value = null
        req.onResult(granted)
    }

    fun resolveSitePermission(allow: Boolean, remember: Boolean) {
        val req = _pendingSitePermission.value ?: return
        _pendingSitePermission.value = null
        req.onDecision(allow, remember)
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showInfoMessage(message: String) {
        showBanner(message)
    }

    private fun showBanner(message: String) {
        _statusBanner.value = message
    }

    private fun msg(tr: String, en: String): String {
        val lang = LocaleHelper.resolveLocale(settings.value.appLanguage).language
        return if (lang == "tr") tr else en
    }

    private fun defaultTabTitle(isPrivate: Boolean): String {
        val lang = LocaleHelper.resolveLocale(settings.value.appLanguage).language
        return if (isPrivate) {
            if (lang == "tr") "Gizli Oturum" else "Private Session"
        } else {
            if (lang == "tr") "Başlangıç Sayfası" else "Start Page"
        }
    }

    // --- SessionEventCallbacks ---

    override fun onLocationChanged(tabId: String, url: String) {
        if (UrlUtils.isHomeUri(url)) return
        val tab = _tabs.value.firstOrNull { it.id == tabId }
        val inferredSecurity = when {
            tab?.loadError?.isSecurityError == true -> SecurityState.CERT_ERROR
            url.startsWith("https://", ignoreCase = true) -> SecurityState.VERIFIED_HTTPS
            url.startsWith("http://", ignoreCase = true) -> SecurityState.INSECURE_HTTP
            else -> SecurityState.HOME_INTERNAL
        }
        updateTab(tabId) {
            it.copy(
                url = url,
                isHomePage = false,
                securityState = inferredSecurity,
                loadError = null
            )
        }
        if (tabId == _activeTabId.value && tab?.isPrivate == false) {
            refreshAutofillForUrl(url)
        } else if (tabId == _activeTabId.value) {
            _pendingAutofillCredentials.value = emptyList()
        }
    }

    override fun onTitleChanged(tabId: String, title: String) {
        updateTab(tabId) {
            if (it.isHomePage) it else it.copy(title = title)
        }
    }

    override fun onProgressChanged(tabId: String, progress: Int) {
        updateTab(tabId) {
            if (it.isHomePage) it else it.copy(progress = progress, isLoading = progress in 1..99)
        }
    }

    override fun onPageStarted(tabId: String, url: String) {
        if (UrlUtils.isHomeUri(url)) return
        updateTab(tabId) {
            it.copy(
                url = url,
                isHomePage = false,
                isLoading = true,
                progress = 10,
                isAtTop = true,
                loadError = null,
                blockedTrackersCount = 0
            )
        }
    }

    override fun onPageStopped(tabId: String, success: Boolean) {
        updateTab(tabId) {
            it.copy(isLoading = false, progress = if (success) 100 else 0)
        }
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        if (success && !tab.isPrivate && !tab.isHomePage && settings.value.saveHistoryEnabled) {
            val now = System.currentTimeMillis()
            if (tab.url != lastRecordedHistoryUrl || now - lastRecordedHistoryTime > 4_000L) {
                lastRecordedHistoryUrl = tab.url
                lastRecordedHistoryTime = now
                viewModelScope.launch {
                    dataRepository.recordHistory(tab.title, tab.url)
                }
            }
        }
    }

    override fun onCanGoBackChanged(tabId: String, canGoBack: Boolean) {
        updateTab(tabId) { it.copy(canGoBack = canGoBack) }
    }

    override fun onCanGoForwardChanged(tabId: String, canGoForward: Boolean) {
        updateTab(tabId) { it.copy(canGoForward = canGoForward) }
    }

    override fun onScrollTopChanged(tabId: String, isAtTop: Boolean) {
        updateTab(tabId) { it.copy(isAtTop = isAtTop) }
    }

    override fun onSecurityChanged(
        tabId: String,
        details: SecurityDetails,
        isSecure: Boolean
    ) {
        updateTab(tabId) { tab ->
            if (tab.isHomePage) tab
            else tab.copy(
                securityDetails = details,
                securityState = when {
                    tab.loadError?.isSecurityError == true -> SecurityState.CERT_ERROR
                    isSecure -> SecurityState.VERIFIED_HTTPS
                    else -> SecurityState.INSECURE_HTTP
                }
            )
        }
    }

    override fun onTrackerBlocked(tabId: String) {
        updateTab(tabId) { it.copy(blockedTrackersCount = it.blockedTrackersCount + 1) }
        viewModelScope.launch {
            settingsRepository.incrementTrackersBlocked(1L)
        }
    }

    override fun onFullScreenChanged(tabId: String, fullScreen: Boolean) {
        updateTab(tabId) { it.copy(isFullScreen = fullScreen) }
        if (!fullScreen) {
            _manualFullScreen.value = false
        }
    }

    override fun onLoadError(tabId: String, error: PageLoadError) {
        updateTab(tabId) {
            it.copy(
                isLoading = false,
                progress = 0,
                isHomePage = false,
                url = error.uri.ifBlank { it.url },
                securityState = if (error.isSecurityError) SecurityState.CERT_ERROR else it.securityState,
                loadError = error
            )
        }
    }

    override fun onNewTabRequested(
        sourceTabId: String,
        isPrivate: Boolean,
        uri: String
    ): EngineSessionHandle? {
        val newId = createNewTab(isPrivate = isPrivate, initialUrl = uri)
        return sessionHandles[newId]
    }

    override fun onCloseTabRequested(tabId: String) {
        closeTab(tabId)
    }

    override fun onExternalDownloadResponse(tabId: String, request: PendingDownloadRequest) {
        _pendingDownload.value = request
    }

    override fun onWebPrompt(prompt: PendingWebPrompt) {
        _pendingPrompt.value = prompt
    }

    override fun onFileUploadPrompt(upload: PendingFileUpload) {
        _pendingFileUpload.value = upload
    }

    override fun onAndroidPermissionsNeeded(request: PendingAndroidPermissionRequest) {
        _pendingAndroidPermissions.value = request
    }

    override fun onContentPermissionQuery(
        origin: String,
        permissionKey: String,
        permissionLabel: String,
        isMedia: Boolean,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val savedDecision = dataRepository.getPermissionDecision(origin, permissionKey)
            when (savedDecision) {
                PermissionDecision.ALLOW -> callback(true)
                PermissionDecision.BLOCK -> callback(false)
                PermissionDecision.ASK -> {
                    _pendingSitePermission.value = PendingSitePermission(
                        origin = origin,
                        permissionKey = permissionKey,
                        permissionLabel = permissionLabel,
                        isMediaRequest = isMedia,
                        onDecision = { allow, remember ->
                            if (remember) {
                                viewModelScope.launch {
                                    dataRepository.setPermissionDecision(
                                        origin = origin,
                                        permissionType = permissionKey,
                                        decision = if (allow) PermissionDecision.ALLOW else PermissionDecision.BLOCK
                                    )
                                }
                            }
                            callback(allow)
                        }
                    )
                }
            }
        }
    }

    override fun onFetchCredentialsForDomain(
        domain: String,
        callback: (List<Pair<String, String>>) -> Unit
    ) {
        val tab = activeTab.value
        if (tab.isPrivate || !settings.value.passwordSavePromptEnabled) {
            callback(emptyList())
            return
        }
        viewModelScope.launch {
            val creds = credentialRepository.getCredentialsForOrigin(domain, isPrivateSession = false)
            _pendingAutofillCredentials.value = creds
            callback(creds.map { it.username to it.password })
        }
    }

    override fun onAutofillFieldFocused(tabId: String, domainOrUrl: String) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        if (tab.isPrivate || tabId != _activeTabId.value || !settings.value.passwordSavePromptEnabled) {
            _pendingAutofillCredentials.value = emptyList()
            return
        }
        viewModelScope.launch {
            _pendingAutofillCredentials.value =
                credentialRepository.getCredentialsForOrigin(domainOrUrl, isPrivateSession = false)
        }
    }

    override fun onAutofillFocusCleared(tabId: String) {
        if (tabId == _activeTabId.value) {
            _pendingAutofillCredentials.value = emptyList()
        }
    }

    override fun onSaveCredentialRequested(origin: String, username: String, password: String) {
        val tab = activeTab.value
        if (tab.isPrivate || !settings.value.passwordSavePromptEnabled) return
        _pendingSaveCredential.value = PendingSaveCredential(origin, username, password)
    }

    override fun onPopupBlocked(tabId: String, uri: String) {
        showBanner(msg("Açılır pencere engellendi", "Pop-up blocked"))
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { tab -> if (tab.id == tabId) transform(tab) else tab }
        }
        syncActiveTab()
    }

    private fun syncActiveTab() {
        val currentId = _activeTabId.value
        val tabList = _tabs.value
        val resolved = tabList.firstOrNull { it.id == currentId }
            ?: tabList.firstOrNull()
            ?: BrowserTab(id = "bootstrap")
        _activeTab.value = resolved
        BrowserAutofillTracker.isPrivateSessionActive = resolved.isPrivate
    }

    override fun onCleared() {
        super.onCleared()
        sessionHandles.values.forEach { it.close() }
        sessionHandles.clear()
    }

    class Factory(
        private val browserEngine: BrowserEngine,
        private val dataRepository: BrowserDataRepository,
        private val settingsRepository: SettingsRepository,
        private val suggestionRepository: SearchSuggestionRepository,
        private val downloadBridge: DownloadBridge,
        private val credentialRepository: CredentialRepository,
        private val toolsRepository: ToolsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BrowserViewModel(
                browserEngine = browserEngine,
                dataRepository = dataRepository,
                settingsRepository = settingsRepository,
                suggestionRepository = suggestionRepository,
                downloadBridge = downloadBridge,
                credentialRepository = credentialRepository,
                toolsRepository = toolsRepository
            ) as T
        }
    }
}
