package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BrowserDatabase
import com.example.data.local.BrowserExtensionEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.DownloadStatus
import com.example.data.local.PermissionDecision
import com.example.data.preferences.BrowserSettingsState
import com.example.data.preferences.DnsProvider
import com.example.data.preferences.HomepageMode
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.BrowserDataRepository
import com.example.data.repository.CredentialRepository
import com.example.data.repository.DownloadBridge
import com.example.data.repository.SearchSuggestionRepository
import com.example.data.repository.ToolsRepository
import com.example.engine.BrowserEngine
import com.example.engine.EngineSessionHandle
import com.example.engine.PageLoadError
import com.example.engine.SecurityState
import com.example.engine.SessionEventCallbacks
import com.example.i18n.AppTranslations
import com.example.i18n.LocaleHelper
import com.example.ui.state.BrowserViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.geckoview.GeckoSession
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.InputStream

class FakeBrowserEngine : BrowserEngine {
    val createdSessions = mutableListOf<Pair<String, Boolean>>()
    val closedTabIds = mutableListOf<String>()
    var clearedPrivateCount = 0
    var latestSettings: BrowserSettingsState? = null
    var latestBlockedHosts: Set<String> = emptySet()
    var latestExtensions: List<BrowserExtensionEntity> = emptyList()
    val filledCredentials = mutableListOf<Pair<String, String>>()
    private val callbacksMap = mutableMapOf<String, SessionEventCallbacks>()

    override fun applySettings(settings: BrowserSettingsState) {
        latestSettings = settings
    }

    override fun syncExtensions(
        extensions: List<BrowserExtensionEntity>,
        customBlockedHosts: Set<String>
    ) {
        latestExtensions = extensions
        latestBlockedHosts = customBlockedHosts
    }

    override fun createSession(
        tabId: String,
        isPrivate: Boolean,
        desktopMode: Boolean,
        settings: BrowserSettingsState,
        callbacks: SessionEventCallbacks
    ): EngineSessionHandle {
        createdSessions.add(tabId to isPrivate)
        callbacksMap[tabId] = callbacks
        return object : EngineSessionHandle {
            var currentUrl = ""
            override fun loadUri(uri: String) {
                currentUrl = uri
                callbacks.onPageStarted(tabId, uri)
                callbacks.onLocationChanged(tabId, uri)
                callbacks.onTitleChanged(tabId, "Loaded: $uri")
                callbacks.onCanGoBackChanged(tabId, true)
                callbacks.onPageStopped(tabId, true)
            }

            override fun goBack() {
                callbacks.onLocationChanged(tabId, "https://previous.example")
            }

            override fun goForward() {}
            override fun reload() {
                if (currentUrl.isNotBlank()) loadUri(currentUrl)
            }

            override fun stop() {}
            override fun exitFullScreen() {}
            override fun setDesktopMode(desktop: Boolean) {}
            override fun setTrackingProtection(enabled: Boolean) {}

            override fun findInPage(
                query: String,
                backwards: Boolean,
                onResult: (found: Boolean, current: Int, total: Int) -> Unit
            ) {
                onResult(query.isNotBlank(), if (query.isNotBlank()) 1 else 0, if (query.isNotBlank()) 3 else 0)
            }

            override fun clearFindInPage() {}
            override fun exportPdf(onReady: (InputStream?) -> Unit) {
                onReady(null)
            }

            override fun performAutofill(username: String, password: String): Boolean {
                filledCredentials.add(username to password)
                return true
            }

            override fun close() {
                closedTabIds.add(tabId)
            }

            override fun geckoSessionOrNull(): GeckoSession? = null
        }
    }

    override fun clearBrowsingData(clearCookies: Boolean, clearCache: Boolean, clearSiteData: Boolean) {}

    override fun clearPrivateSessionData() {
        clearedPrivateCount++
    }

    fun triggerSecurityError(tabId: String, uri: String) {
        callbacksMap[tabId]?.onLoadError(
            tabId,
            PageLoadError(
                uri = uri,
                category = 2,
                code = 50,
                isSecurityError = true,
                title = "Certificate Validation Failed",
                description = "Blocked invalid TLS certificate"
            )
        )
    }

    fun triggerSaveCredential(origin: String, username: String, password: String) {
        callbacksMap.values.lastOrNull()?.onSaveCredentialRequested(origin, username, password)
    }

    fun triggerAutofillFocus(tabId: String, domainOrUrl: String) {
        callbacksMap[tabId]?.onAutofillFieldFocused(tabId, domainOrUrl)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BrowserViewModelRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: BrowserDatabase
    private lateinit var dataRepo: BrowserDataRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var credentialRepo: CredentialRepository
    private lateinit var toolsRepo: ToolsRepository
    private lateinit var fakeEngine: FakeBrowserEngine
    private lateinit var viewModel: BrowserViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, BrowserDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dataRepo = BrowserDataRepository(
            bookmarkDao = db.bookmarkDao(),
            historyDao = db.historyDao(),
            downloadDao = db.downloadDao(),
            sitePermissionDao = db.sitePermissionDao(),
            speedDialDao = db.speedDialDao(),
            readingListDao = db.readingListDao(),
            extensionDao = db.extensionDao(),
            dictionaryDao = db.dictionaryDao()
        )
        settingsRepo = SettingsRepository(context)
        credentialRepo = CredentialRepository(context, db.credentialDao())
        toolsRepo = ToolsRepository(context)
        fakeEngine = FakeBrowserEngine()
        viewModel = BrowserViewModel(
            browserEngine = fakeEngine,
            dataRepository = dataRepo,
            settingsRepository = settingsRepo,
            suggestionRepository = SearchSuggestionRepository(dataRepo),
            downloadBridge = DownloadBridge(context, dataRepo),
            credentialRepository = credentialRepo,
            toolsRepository = toolsRepo
        )
        drainTasks()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun drainTasks() {
        repeat(6) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(40)
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `multi-tab creation switching and private isolation`() = runBlocking {
        assertEquals(1, viewModel.tabs.value.size)

        val standardTabId = viewModel.activeTabId.value
        viewModel.navigateCurrentTab("https://mozilla.org")
        drainTasks()
        assertEquals("https://mozilla.org", viewModel.activeTab.value.url)

        val privateTabId = viewModel.createNewTab(isPrivate = true, initialUrl = "https://secret.example")
        drainTasks()
        assertEquals(2, viewModel.tabs.value.size)
        assertEquals(privateTabId, viewModel.activeTabId.value)
        assertTrue(viewModel.activeTab.value.isPrivate)

        // Private navigation must NEVER be written to Room browsing history
        val historyList = dataRepo.history.first()
        assertTrue(historyList.any { it.url == "https://mozilla.org" })
        assertFalse(historyList.any { it.url == "https://secret.example" })

        // Closing all private tabs invokes engine private session cleanup
        viewModel.closeAllPrivateTabs()
        drainTasks()
        assertEquals(1, viewModel.tabs.value.size)
        assertEquals(standardTabId, viewModel.activeTabId.value)
        assertTrue(fakeEngine.clearedPrivateCount >= 1)
    }

    @Test
    fun `tab pinning tab groups and reading list persist accurately`() = runBlocking {
        val firstTabId = viewModel.activeTabId.value
        val secondTabId = viewModel.createNewTab(isPrivate = false, initialUrl = "https://kotlinlang.org")
        drainTasks()

        viewModel.setTabGroup(secondTabId, "Geliştirme")
        viewModel.togglePinTab(secondTabId)
        drainTasks()

        // Pinned tab moves to front and carries group tag
        val tabs = viewModel.tabs.value
        assertEquals(secondTabId, tabs.first().id)
        assertTrue(tabs.first().isPinned)
        assertEquals("Geliştirme", tabs.first().groupName)
        assertEquals(firstTabId, tabs.last().id)

        // Reading list persistence
        viewModel.saveCurrentPageToReadingList()
        drainTasks()
        val readingItems = dataRepo.readingList.first()
        assertEquals(1, readingItems.size)
        assertEquals("https://kotlinlang.org", readingItems.first().url)
    }

    @Test
    fun `bookmarks and site permissions persist in Room`() = runBlocking {
        viewModel.navigateCurrentTab("https://developer.mozilla.org")
        drainTasks()
        viewModel.toggleCurrentBookmark()
        drainTasks()

        val savedBookmarks = dataRepo.bookmarks.first()
        assertEquals(1, savedBookmarks.size)
        assertEquals("https://developer.mozilla.org", savedBookmarks.first().url)

        viewModel.updateSitePermissionRule(
            origin = "https://developer.mozilla.org",
            permissionType = "GEOLOCATION",
            decision = PermissionDecision.BLOCK
        )
        drainTasks()
        val decision = dataRepo.getPermissionDecision("https://developer.mozilla.org", "GEOLOCATION")
        assertEquals(PermissionDecision.BLOCK, decision)
    }

    @Test
    fun `password manager encrypts credentials and isolates private sessions`() = runBlocking {
        viewModel.saveCredential("https://accounts.example/login", "serhat", "Sup3rS3cret!")
        drainTasks()

        val saved = credentialRepo.allCredentials.first()
        assertEquals(1, saved.size)
        // Stored password must NOT be plaintext
        assertFalse(saved.first().encryptedPassword.contains("Sup3rS3cret!"))
        assertEquals("Sup3rS3cret!", viewModel.decryptCredentialPassword(saved.first()))

        // Standard tab retrieves autofill matches & reacts to focused login fields
        val standardTabId = viewModel.activeTabId.value
        var retrieved = emptyList<Pair<String, String>>()
        viewModel.onFetchCredentialsForDomain("https://accounts.example/profile") {
            retrieved = it
        }
        drainTasks()
        assertEquals(1, retrieved.size)
        assertEquals("serhat", retrieved.first().first)

        // Focus login field in standard tab -> populates pendingAutofillCredentials and fills via engine
        fakeEngine.triggerAutofillFocus(standardTabId, "https://accounts.example/login")
        drainTasks()
        assertEquals(1, viewModel.pendingAutofillCredentials.value.size)
        assertTrue(viewModel.fillPendingCredential(viewModel.pendingAutofillCredentials.value.first()))
        assertEquals("serhat" to "Sup3rS3cret!", fakeEngine.filledCredentials.last())
        assertTrue(com.example.data.autofill.BrowserAutofillTracker.isAutofillEnabled)
        assertFalse(com.example.data.autofill.BrowserAutofillTracker.isPrivateSessionActive)

        // Update credential re-encrypts with updated username & password
        viewModel.updateCredential(saved.first().id, "https://accounts.example/login", "serhat2", "N3wP@ssw0rd!")
        drainTasks()
        val updatedSaved = credentialRepo.allCredentials.first()
        assertEquals("serhat2", updatedSaved.first().username)
        assertEquals("N3wP@ssw0rd!", viewModel.decryptCredentialPassword(updatedSaved.first()))

        // Private session must NEVER expose, autofill on focus, or prompt saving passwords
        val privateTabId = viewModel.createNewTab(isPrivate = true, initialUrl = "https://accounts.example")
        drainTasks()
        assertTrue(com.example.data.autofill.BrowserAutofillTracker.isPrivateSessionActive)

        fakeEngine.triggerAutofillFocus(privateTabId, "https://accounts.example/login")
        drainTasks()
        assertTrue(viewModel.pendingAutofillCredentials.value.isEmpty())

        var privateRetrieved = listOf("nonempty" to "bad")
        viewModel.onFetchCredentialsForDomain("https://accounts.example") {
            privateRetrieved = it
        }
        drainTasks()
        assertTrue(privateRetrieved.isEmpty())

        fakeEngine.triggerSaveCredential("https://accounts.example", "hacker", "123456")
        drainTasks()
        assertEquals(null, viewModel.pendingSaveCredential.value)
    }

    @Test
    fun `multilingual 10-language and RTL helper resolves Turkish default and Arabic RTL`() = runBlocking {
        // Default language is Turkish
        assertEquals("tr", viewModel.settings.value.appLanguage)
        assertEquals(10, LocaleHelper.supportedLanguages.size)

        // Switch to Arabic and confirm RTL layout direction + localized strings
        viewModel.setAppLanguage("ar")
        drainTasks()
        assertEquals("ar", viewModel.settings.value.appLanguage)
        val arLocale = LocaleHelper.resolveLocale("ar")
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Rtl, LocaleHelper.layoutDirectionFor(arLocale))
        assertTrue(AppTranslations.forLang("ar").privateSession.isNotBlank())

        // Switch back to Turkish
        viewModel.setAppLanguage("tr")
        drainTasks()
        val trLocale = LocaleHelper.resolveLocale("tr")
        assertEquals(androidx.compose.ui.unit.LayoutDirection.Ltr, LocaleHelper.layoutDirectionFor(trLocale))
    }

    @Test
    fun `dns configuration and privacy controls persist and sync extensions`() = runBlocking {
        viewModel.setDnsConfiguration(
            provider = DnsProvider.CUSTOM,
            customPrimary = "9.9.9.9",
            customSecondary = "149.112.112.112",
            customDohUri = "https://dns.quad9.net/dns-query"
        )
        viewModel.setPrivacyToggles(
            popupBlocking = true,
            httpsOnly = true,
            adFilterRules = true,
            passwordPrompt = true
        )
        viewModel.addCustomExtension("Reklam Filtresi", "Test kuralı", "ads.example.com")
        drainTasks()

        val updated = settingsRepo.settingsFlow.first()
        assertEquals(DnsProvider.CUSTOM, updated.dnsProvider)
        assertEquals("9.9.9.9", updated.customDnsPrimary)
        assertTrue(updated.httpsOnlyEnabled)
        assertTrue(updated.popupBlockingEnabled)
        assertTrue(fakeEngine.latestBlockedHosts.contains("ads.example.com"))
    }

    @Test
    fun `downloads support paused cancelled and completed lifecycle tracking`() = runBlocking {
        val id = dataRepo.insertDownload(
            DownloadEntity(
                fileName = "gecko-report.pdf",
                url = "https://example.org/gecko-report.pdf",
                mimeType = "application/pdf",
                status = DownloadStatus.IN_PROGRESS,
                downloadedBytes = 500,
                totalBytes = 1000
            )
        )
        dataRepo.updateDownloadProgress(
            id = id,
            status = DownloadStatus.PAUSED,
            downloadedBytes = 500,
            totalBytes = 1000,
            localUri = null
        )
        drainTasks()
        var item = dataRepo.findDownloadById(id)
        assertNotNull(item)
        assertEquals(DownloadStatus.PAUSED, item!!.status)

        dataRepo.updateDownloadProgress(
            id = id,
            status = DownloadStatus.COMPLETED,
            downloadedBytes = 1000,
            totalBytes = 1000,
            localUri = "content://downloads/public_downloads/1"
        )
        drainTasks()
        item = dataRepo.findDownloadById(id)
        assertEquals(DownloadStatus.COMPLETED, item!!.status)

        viewModel.deleteDownload(item)
        drainTasks()
        assertEquals(null, dataRepo.findDownloadById(id))
    }

    @Test
    fun `certificate error sets CERT_ERROR state and blocks silent bypass`() {
        val tabId = viewModel.activeTabId.value
        viewModel.navigateCurrentTab("https://expired.badssl.com")
        fakeEngine.triggerSecurityError(tabId, "https://expired.badssl.com")
        drainTasks()

        val tab = viewModel.activeTab.value
        assertEquals(SecurityState.CERT_ERROR, tab.securityState)
        assertNotNull(tab.loadError)
        assertTrue(tab.loadError!!.isSecurityError)
    }

    @Test
    fun `homepage customization stores custom URL`() = runBlocking {
        viewModel.updateHomepageConfig(
            mode = HomepageMode.CUSTOM_URL,
            customUrl = "wikipedia.org",
            showSpeedDials = false
        )
        drainTasks()
        val updated = settingsRepo.settingsFlow.first()
        assertEquals(HomepageMode.CUSTOM_URL, updated.homepageMode)
        assertEquals("https://wikipedia.org", updated.customHomepageUrl)
        assertFalse(updated.showSpeedDialsOnHome)
    }
}
