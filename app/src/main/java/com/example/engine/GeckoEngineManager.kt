package com.example.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.SparseArray
import com.example.R
import com.example.data.local.BrowserExtensionEntity
import com.example.data.preferences.BrowserSettingsState
import com.example.data.preferences.CookiePolicy
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.TrackingProtectionLevel
import com.example.i18n.LocaleHelper
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.Autofill
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse
import java.io.InputStream
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

interface EngineSessionHandle {
    fun loadUri(uri: String)
    fun goBack()
    fun goForward()
    fun reload()
    fun stop()
    fun exitFullScreen()
    fun setDesktopMode(desktop: Boolean)
    fun setTrackingProtection(enabled: Boolean)
    fun findInPage(query: String, backwards: Boolean, onResult: (found: Boolean, current: Int, total: Int) -> Unit)
    fun clearFindInPage()
    fun exportPdf(onReady: (InputStream?) -> Unit)
    fun performAutofill(username: String, password: String): Boolean
    fun close()
    fun geckoSessionOrNull(): GeckoSession?
}

interface SessionEventCallbacks {
    fun onLocationChanged(tabId: String, url: String)
    fun onTitleChanged(tabId: String, title: String)
    fun onProgressChanged(tabId: String, progress: Int)
    fun onPageStarted(tabId: String, url: String)
    fun onPageStopped(tabId: String, success: Boolean)
    fun onCanGoBackChanged(tabId: String, canGoBack: Boolean)
    fun onCanGoForwardChanged(tabId: String, canGoForward: Boolean)
    fun onScrollTopChanged(tabId: String, isAtTop: Boolean)
    fun onSecurityChanged(tabId: String, details: SecurityDetails, isSecure: Boolean)
    fun onTrackerBlocked(tabId: String)
    fun onFullScreenChanged(tabId: String, fullScreen: Boolean)
    fun onLoadError(tabId: String, error: PageLoadError)
    fun onNewTabRequested(sourceTabId: String, isPrivate: Boolean, uri: String): EngineSessionHandle?
    fun onCloseTabRequested(tabId: String)
    fun onExternalDownloadResponse(tabId: String, request: PendingDownloadRequest)
    fun onWebPrompt(prompt: PendingWebPrompt)
    fun onFileUploadPrompt(upload: PendingFileUpload)
    fun onAndroidPermissionsNeeded(request: PendingAndroidPermissionRequest)
    fun onContentPermissionQuery(
        origin: String,
        permissionKey: String,
        permissionLabel: String,
        isMedia: Boolean,
        callback: (Boolean) -> Unit
    )
    fun onFetchCredentialsForDomain(domain: String, callback: (List<Pair<String, String>>) -> Unit)
    fun onAutofillFieldFocused(tabId: String, domainOrUrl: String)
    fun onAutofillFocusCleared(tabId: String)
    fun onSaveCredentialRequested(origin: String, username: String, password: String)
    fun onPopupBlocked(tabId: String, uri: String)
}

interface BrowserEngine {
    fun applySettings(settings: BrowserSettingsState)
    fun syncExtensions(extensions: List<BrowserExtensionEntity>, customBlockedHosts: Set<String>)
    fun createSession(
        tabId: String,
        isPrivate: Boolean,
        desktopMode: Boolean,
        settings: BrowserSettingsState,
        callbacks: SessionEventCallbacks
    ): EngineSessionHandle

    fun clearBrowsingData(clearCookies: Boolean, clearCache: Boolean, clearSiteData: Boolean)
    fun clearPrivateSessionData()
}

class GeckoEngineManager(private val context: Context) : BrowserEngine {

    private var runtime: GeckoRuntime? = null
    @Volatile
    private var latestSettings: BrowserSettingsState = BrowserSettingsState()
    @Volatile
    private var blockedDomains: Set<String> = emptySet()
    private val installedBuiltInExtensions = ConcurrentHashMap<String, WebExtension>()
    private val sessionAutofillStates = ConcurrentHashMap<String, SessionAutofillState>()
    @Volatile
    private var activeCallbacks: SessionEventCallbacks? = null

    private data class SessionAutofillState(
        @Volatile var currentUrl: String = "",
        @Volatile var domain: String = "",
        @Volatile var usernameNodeId: Int? = null,
        @Volatile var passwordNodeId: Int? = null,
        @Volatile var focusedNodeId: Int? = null,
        @Volatile var focusedHint: Int = Autofill.Hint.NONE
    )

    private fun ensureRuntime(settings: BrowserSettingsState): GeckoRuntime {
        latestSettings = settings
        runtime?.let { return it }

        val cbSettings = buildContentBlockingSettings(settings)
        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(settings.javaScriptEnabled)
            .contentBlocking(cbSettings)
            .preferredColorScheme(mapColorScheme(settings.themeMode))
            .fontSizeFactor(settings.pageZoomPercent / 100f)
            .aboutConfigEnabled(false)
            .consoleOutput(false)
            .build()

        return GeckoRuntime.create(context.applicationContext, runtimeSettings).also { created ->
            runtime = created
            created.autocompleteStorageDelegate = object : Autocomplete.StorageDelegate {
                override fun onLoginFetch(domain: String): GeckoResult<Array<Autocomplete.LoginEntry>> {
                    val result = GeckoResult<Array<Autocomplete.LoginEntry>>()
                    val cb = activeCallbacks
                    if (cb == null) {
                        result.complete(emptyArray())
                        return result
                    }
                    cb.onFetchCredentialsForDomain(domain) { pairs ->
                        val entries = pairs.map { (user, pass) ->
                            Autocomplete.LoginEntry.Builder()
                                .origin(domain)
                                .username(user)
                                .password(pass)
                                .build()
                        }.toTypedArray()
                        result.complete(entries)
                    }
                    return result
                }

                override fun onLoginSave(login: Autocomplete.LoginEntry) {
                    val origin = login.origin
                    val user = login.username
                    val pass = login.password
                    if (origin.isNotBlank() && user.isNotBlank() && pass.isNotEmpty()) {
                        activeCallbacks?.onSaveCredentialRequested(origin, user, pass)
                    }
                }
            }
        }
    }

    override fun applySettings(settings: BrowserSettingsState) {
        latestSettings = settings
        val activeRuntime = runtime ?: return
        activeRuntime.settings.javaScriptEnabled = settings.javaScriptEnabled
        activeRuntime.settings.preferredColorScheme = mapColorScheme(settings.themeMode)
        activeRuntime.settings.fontSizeFactor = settings.pageZoomPercent / 100f

        val cb = activeRuntime.settings.contentBlocking
        cb.setEnhancedTrackingProtectionLevel(mapEtpLevel(settings.trackingProtection))
        cb.setAntiTracking(mapAntiTrackingCategories(settings.trackingProtection))
        cb.setCookieBehavior(mapCookieBehavior(settings.cookiePolicy))
        cb.setCookieBehaviorPrivateMode(mapCookieBehavior(settings.cookiePolicy))
    }

    override fun syncExtensions(
        extensions: List<BrowserExtensionEntity>,
        customBlockedHosts: Set<String>
    ) {
        blockedDomains = customBlockedHosts
        val activeRuntime = runtime ?: return
        val controller = activeRuntime.webExtensionController
        for (ext in extensions) {
            if (!ext.isBuiltIn || ext.assetPath.isBlank()) continue
            val existing = installedBuiltInExtensions[ext.extensionId]
            if (existing == null) {
                if (ext.isEnabled) {
                    runCatching {
                        controller.installBuiltIn(ext.assetPath).accept { webExt ->
                            if (webExt != null) {
                                installedBuiltInExtensions[ext.extensionId] = webExt
                                controller.enable(webExt, WebExtensionController.EnableSource.USER)
                            }
                        }
                    }
                }
            } else {
                runCatching {
                    if (ext.isEnabled) {
                        controller.enable(existing, WebExtensionController.EnableSource.USER)
                    } else {
                        controller.disable(existing, WebExtensionController.EnableSource.USER)
                    }
                }
            }
        }
    }

    override fun createSession(
        tabId: String,
        isPrivate: Boolean,
        desktopMode: Boolean,
        settings: BrowserSettingsState,
        callbacks: SessionEventCallbacks
    ): EngineSessionHandle {
        activeCallbacks = callbacks
        val activeRuntime = ensureRuntime(settings)
        val sessionSettings = GeckoSessionSettings.Builder()
            .usePrivateMode(isPrivate)
            .useTrackingProtection(
                isPrivate || settings.trackingProtection != TrackingProtectionLevel.OFF
            )
            .userAgentMode(
                if (desktopMode) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            )
            .build()

        val session = GeckoSession(sessionSettings)
        attachDelegates(session, tabId, isPrivate, callbacks)
        session.open(activeRuntime)

        return object : EngineSessionHandle {
            override fun loadUri(uri: String) {
                val target = if (latestSettings.httpsOnlyEnabled &&
                    uri.startsWith("http://", ignoreCase = true) &&
                    !isLocalHostUri(uri)
                ) {
                    uri.replaceFirst("http://", "https://", ignoreCase = true)
                } else {
                    uri
                }
                session.loadUri(target)
            }

            override fun goBack() = session.goBack()
            override fun goForward() = session.goForward()
            override fun reload() = session.reload()
            override fun stop() = session.stop()
            override fun exitFullScreen() = session.exitFullScreen()
            override fun setDesktopMode(desktop: Boolean) {
                session.settings.userAgentMode = if (desktop) {
                    GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                } else {
                    GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                }
                session.reload()
            }

            override fun setTrackingProtection(enabled: Boolean) {
                session.settings.useTrackingProtection = enabled
            }

            override fun findInPage(
                query: String,
                backwards: Boolean,
                onResult: (found: Boolean, current: Int, total: Int) -> Unit
            ) {
                val flags = if (backwards) {
                    GeckoSession.FINDER_FIND_BACKWARDS or GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL
                } else {
                    GeckoSession.FINDER_DISPLAY_HIGHLIGHT_ALL
                }
                session.finder.find(query, flags).accept(
                    { res ->
                        if (res != null) {
                            onResult(res.found, res.current, res.total)
                        } else {
                            onResult(false, 0, 0)
                        }
                    },
                    { onResult(false, 0, 0) }
                )
            }

            override fun clearFindInPage() {
                runCatching { session.finder.clear() }
            }

            override fun exportPdf(onReady: (InputStream?) -> Unit) {
                runCatching {
                    session.saveAsPdf().accept(
                        { stream -> onReady(stream) },
                        { onReady(null) }
                    )
                }.onFailure { onReady(null) }
            }

            override fun performAutofill(username: String, password: String): Boolean {
                if (isPrivate) return false
                val state = sessionAutofillStates[tabId] ?: return false
                val values = SparseArray<CharSequence>()
                val userId = state.usernameNodeId
                val passId = state.passwordNodeId
                val focusedId = state.focusedNodeId

                if (userId != null && username.isNotBlank()) {
                    values.put(userId, username)
                }
                if (passId != null && password.isNotEmpty()) {
                    values.put(passId, password)
                }
                if (values.size() == 0 && focusedId != null) {
                    val fillVal = if (state.focusedHint == Autofill.Hint.PASSWORD) password else username
                    if (fillVal.isNotEmpty()) values.put(focusedId, fillVal)
                }
                if (values.size() == 0) return false
                return runCatching {
                    session.autofillSession.autofill(values)
                    true
                }.getOrDefault(false)
            }

            override fun close() {
                sessionAutofillStates.remove(tabId)
                runCatching { session.close() }
            }

            override fun geckoSessionOrNull(): GeckoSession = session
        }
    }

    override fun clearBrowsingData(
        clearCookies: Boolean,
        clearCache: Boolean,
        clearSiteData: Boolean
    ) {
        val activeRuntime = runtime ?: return
        var flags = 0L
        if (clearCookies) flags = flags or StorageController.ClearFlags.COOKIES
        if (clearCache) flags = flags or StorageController.ClearFlags.ALL_CACHES
        if (clearSiteData) flags = flags or StorageController.ClearFlags.SITE_DATA
        if (flags != 0L) {
            runCatching {
                activeRuntime.storageController.clearData(flags)
            }.onFailure {
                Log.w("GeckoEngineManager", "Failed to clear browsing data", it)
            }
        }
    }

    override fun clearPrivateSessionData() {
        val activeRuntime = runtime ?: return
        runCatching {
            activeRuntime.storageController.clearData(StorageController.ClearFlags.AUTH_SESSIONS)
        }
    }

    private fun attachDelegates(
        session: GeckoSession,
        tabId: String,
        isPrivate: Boolean,
        callbacks: SessionEventCallbacks
    ) {
        session.scrollDelegate = object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                callbacks.onScrollTopChanged(tabId, scrollY <= 4)
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                if (!url.isNullOrBlank()) {
                    sessionAutofillStates.getOrPut(tabId) { SessionAutofillState() }.currentUrl = url
                    callbacks.onLocationChanged(tabId, url)
                }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                callbacks.onCanGoBackChanged(tabId, canGoBack)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                callbacks.onCanGoForwardChanged(tabId, canGoForward)
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny> {
                val uri = request.uri
                val parsed = runCatching { Uri.parse(uri) }.getOrNull()
                val scheme = parsed?.scheme?.lowercase()
                val host = parsed?.host?.lowercase().orEmpty()

                if (scheme !in setOf("http", "https", "about", "blob")) {
                    return GeckoResult.deny()
                }

                // Ad & Tracker Filter Rules
                if (latestSettings.adFilterRulesEnabled && host.isNotBlank()) {
                    val isBlockedHost = blockedDomains.any { blocked ->
                        host == blocked || host.endsWith(".$blocked")
                    }
                    if (isBlockedHost) {
                        callbacks.onTrackerBlocked(tabId)
                        return GeckoResult.deny()
                    }
                }

                // Popup blocking when opening a new window without explicit user gesture
                if (latestSettings.popupBlockingEnabled &&
                    request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW &&
                    !request.hasUserGesture
                ) {
                    callbacks.onPopupBlocked(tabId, uri)
                    return GeckoResult.deny()
                }

                // HTTPS-Only automatic upgrade on external web hosts
                if (latestSettings.httpsOnlyEnabled &&
                    scheme == "http" &&
                    !isLocalHostUri(uri)
                ) {
                    val upgraded = uri.replaceFirst("http://", "https://", ignoreCase = true)
                    session.loadUri(upgraded)
                    return GeckoResult.deny()
                }

                return GeckoResult.allow()
            }

            override fun onNewSession(
                session: GeckoSession,
                uri: String
            ): GeckoResult<GeckoSession> {
                val handle = callbacks.onNewTabRequested(tabId, isPrivate, uri)
                val newGeckoSession = handle?.geckoSessionOrNull()
                return if (newGeckoSession != null) {
                    GeckoResult.fromValue(newGeckoSession)
                } else {
                    GeckoResult.fromValue(null)
                }
            }

            override fun onLoadError(
                session: GeckoSession,
                uri: String?,
                error: WebRequestError
            ): GeckoResult<String>? {
                val isSecurity = error.category == WebRequestError.ERROR_CATEGORY_SECURITY
                val (title, description) = mapWebRequestError(error)
                callbacks.onLoadError(
                    tabId,
                    PageLoadError(
                        uri = uri.orEmpty(),
                        category = error.category,
                        code = error.code,
                        isSecurityError = isSecurity,
                        title = title,
                        description = description
                    )
                )
                return null
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                callbacks.onPageStarted(tabId, url)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                callbacks.onPageStopped(tabId, success)
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                callbacks.onProgressChanged(tabId, progress)
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {
                val cert = securityInfo.certificate
                val validUntilStr = cert?.notAfter?.let {
                    DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleHelper.resolveLocale(latestSettings.appLanguage))
                        .format(Date(it.time))
                }
                val details = SecurityDetails(
                    host = securityInfo.host.orEmpty(),
                    origin = securityInfo.origin.orEmpty(),
                    isSecure = securityInfo.isSecure,
                    issuer = cert?.issuerX500Principal?.name,
                    subject = cert?.subjectX500Principal?.name,
                    validUntil = validUntilStr
                )
                callbacks.onSecurityChanged(tabId, details, securityInfo.isSecure)
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                if (!title.isNullOrBlank()) {
                    callbacks.onTitleChanged(tabId, title)
                }
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                callbacks.onFullScreenChanged(tabId, fullScreen)
            }

            override fun onCloseRequest(session: GeckoSession) {
                callbacks.onCloseTabRequested(tabId)
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                val url = response.uri
                val headers = response.headers
                val contentType = headers["Content-Type"] ?: "application/octet-stream"
                val contentDisposition = headers["Content-Disposition"]
                val contentLength = headers["Content-Length"]?.toLongOrNull() ?: -1L
                runCatching { response.body?.close() }

                val safeName = UrlUtils.sanitizeDownloadFilename(
                    url = url,
                    contentDisposition = contentDisposition,
                    mimeType = contentType
                )
                callbacks.onExternalDownloadResponse(
                    tabId,
                    PendingDownloadRequest(
                        url = url,
                        fileName = safeName,
                        mimeType = contentType,
                        contentLength = contentLength,
                        isInsecureHttp = url.startsWith("http://", ignoreCase = true)
                    )
                )
            }
        }

        session.contentBlockingDelegate = object : ContentBlocking.Delegate {
            override fun onContentBlocked(
                session: GeckoSession,
                event: ContentBlocking.BlockEvent
            ) {
                callbacks.onTrackerBlocked(tabId)
            }
        }

        session.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onAndroidPermissionsRequest(
                session: GeckoSession,
                permissions: Array<out String>?,
                callback: GeckoSession.PermissionDelegate.Callback
            ) {
                if (permissions.isNullOrEmpty()) {
                    callback.grant()
                    return
                }
                callbacks.onAndroidPermissionsNeeded(
                    PendingAndroidPermissionRequest(
                        permissions = permissions.map { it }.toTypedArray(),
                        onResult = { granted ->
                            if (granted) callback.grant() else callback.reject()
                        }
                    )
                )
            }

            override fun onContentPermissionRequest(
                session: GeckoSession,
                perm: GeckoSession.PermissionDelegate.ContentPermission
            ): GeckoResult<Int> {
                val result = GeckoResult<Int>()
                val origin = UrlUtils.extractOrigin(perm.uri).ifBlank { perm.uri }
                val (key, label) = when (perm.permission) {
                    GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION ->
                        "GEOLOCATION" to "Konum / Location"
                    GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION ->
                        "NOTIFICATIONS" to "Bildirimler / Notifications"
                    GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE ->
                        "STORAGE" to "Kalıcı Depolama / Storage"
                    GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE,
                    GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE ->
                        "AUTOPLAY" to "Otomatik Oynatma / Autoplay"
                    else -> "OTHER_${perm.permission}" to "Site Özelliği / Feature"
                }

                if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE) {
                    result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                    return result
                }

                callbacks.onContentPermissionQuery(
                    origin = origin,
                    permissionKey = key,
                    permissionLabel = label,
                    isMedia = false
                ) { allowed ->
                    result.complete(
                        if (allowed) GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                        else GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                    )
                }
                return result
            }

            override fun onMediaPermissionRequest(
                session: GeckoSession,
                uri: String,
                video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                callback: GeckoSession.PermissionDelegate.MediaCallback
            ) {
                val origin = UrlUtils.extractOrigin(uri).ifBlank { uri }
                val wantsVideo = !video.isNullOrEmpty()
                val wantsAudio = !audio.isNullOrEmpty()
                val key = when {
                    wantsVideo && wantsAudio -> "CAMERA_MICROPHONE"
                    wantsVideo -> "CAMERA"
                    else -> "MICROPHONE"
                }
                val label = when {
                    wantsVideo && wantsAudio -> "Kamera ve Mikrofon"
                    wantsVideo -> "Kamera"
                    else -> "Mikrofon"
                }

                callbacks.onContentPermissionQuery(
                    origin = origin,
                    permissionKey = key,
                    permissionLabel = label,
                    isMedia = true
                ) { allowed ->
                    if (allowed) {
                        callback.grant(video?.firstOrNull(), audio?.firstOrNull())
                    } else {
                        callback.reject()
                    }
                }
            }
        }

        session.promptDelegate = object : GeckoSession.PromptDelegate {
            override fun onAlertPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.AlertPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                callbacks.onWebPrompt(
                    PendingWebPrompt.JsAlert(
                        title = prompt.title ?: "Sayfa Uyarısı",
                        message = prompt.message.orEmpty(),
                        result = result,
                        prompt = prompt
                    )
                )
                return result
            }

            override fun onButtonPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.ButtonPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                callbacks.onWebPrompt(
                    PendingWebPrompt.JsConfirm(
                        title = prompt.title ?: "İşlemi Onayla",
                        message = prompt.message.orEmpty(),
                        result = result,
                        prompt = prompt
                    )
                )
                return result
            }

            override fun onTextPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.TextPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                callbacks.onWebPrompt(
                    PendingWebPrompt.JsText(
                        title = prompt.title ?: "Sayfa İstemi",
                        message = prompt.message.orEmpty(),
                        defaultValue = prompt.defaultValue.orEmpty(),
                        result = result,
                        prompt = prompt
                    )
                )
                return result
            }

            override fun onChoicePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.ChoicePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                val options = prompt.choices.map { c ->
                    ChoiceOption(
                        id = c.id,
                        label = c.label,
                        selected = c.selected,
                        disabled = c.disabled
                    )
                }
                callbacks.onWebPrompt(
                    PendingWebPrompt.Choice(
                        title = prompt.title ?: "Seçenek Belirle",
                        message = prompt.message.orEmpty(),
                        multiple = prompt.type == GeckoSession.PromptDelegate.ChoicePrompt.Type.MULTIPLE,
                        options = options,
                        result = result,
                        prompt = prompt
                    )
                )
                return result
            }

            override fun onFilePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.FilePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                val mimes = prompt.mimeTypes?.takeIf { it.isNotEmpty() } ?: arrayOf("*/*")
                val allowMultiple = prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE
                callbacks.onFileUploadPrompt(
                    PendingFileUpload(
                        allowMultiple = allowMultiple,
                        mimeTypes = mimes,
                        onFilesSelected = { uris ->
                            if (uris.isNullOrEmpty()) {
                                result.complete(prompt.dismiss())
                            } else {
                                result.complete(prompt.confirm(context, uris))
                            }
                        }
                    )
                )
                return result
            }
        }

        session.autofillDelegate = object : Autofill.Delegate {
            override fun onAutofill(
                session: GeckoSession,
                notification: Int,
                node: Autofill.Node?
            ) {
                // Strictly exclude private browsing sessions from autofill and saving
                if (isPrivate) {
                    sessionAutofillStates.remove(tabId)
                    return
                }

                when (notification) {
                    Autofill.Notify.SESSION_CANCELED -> {
                        sessionAutofillStates.remove(tabId)
                        callbacks.onAutofillFocusCleared(tabId)
                        return
                    }
                    Autofill.Notify.SESSION_COMMITTED -> {
                        val state = sessionAutofillStates[tabId]
                        if (node != null && latestSettings.passwordSavePromptEnabled) {
                            val extracted = extractCommittedCredentials(node)
                            val origin = extracted.first.ifBlank { state?.currentUrl.orEmpty() }
                            val user = extracted.second
                            val pass = extracted.third
                            if (origin.isNotBlank() && user.isNotBlank() && pass.isNotEmpty()) {
                                callbacks.onSaveCredentialRequested(origin, user, pass)
                            }
                        }
                        callbacks.onAutofillFocusCleared(tabId)
                        return
                    }
                }

                if (node == null) return
                val state = sessionAutofillStates.getOrPut(tabId) { SessionAutofillState() }
                scanAutofillTree(node, state)

                if (notification == Autofill.Notify.NODE_FOCUSED ||
                    notification == Autofill.Notify.SESSION_STARTED ||
                    notification == Autofill.Notify.NODE_UPDATED
                ) {
                    val hasCredentialFocus = node.focused && (
                        node.hint == Autofill.Hint.PASSWORD ||
                            node.hint == Autofill.Hint.USERNAME ||
                            node.hint == Autofill.Hint.EMAIL_ADDRESS ||
                            state.passwordNodeId != null ||
                            state.usernameNodeId != null
                        )
                    if (hasCredentialFocus || (notification == Autofill.Notify.SESSION_STARTED && (state.passwordNodeId != null || state.usernameNodeId != null))) {
                        val targetOrigin = state.domain.takeIf { it.isNotBlank() }?.let {
                            if (it.startsWith("http")) it else "https://$it"
                        } ?: state.currentUrl
                        if (targetOrigin.isNotBlank() && latestSettings.passwordSavePromptEnabled) {
                            callbacks.onAutofillFieldFocused(tabId, targetOrigin)
                        }
                    }
                } else if (notification == Autofill.Notify.NODE_BLURRED) {
                    state.focusedNodeId = null
                }
            }
        }
    }

    private fun scanAutofillTree(node: Autofill.Node, state: SessionAutofillState) {
        if (node.domain.isNotBlank()) {
            state.domain = node.domain
        }
        when (node.hint) {
            Autofill.Hint.PASSWORD -> state.passwordNodeId = node.id
            Autofill.Hint.USERNAME, Autofill.Hint.EMAIL_ADDRESS -> state.usernameNodeId = node.id
        }
        if (node.focused) {
            state.focusedNodeId = node.id
            state.focusedHint = node.hint
            if (node.hint == Autofill.Hint.NONE &&
                node.inputType == Autofill.InputType.TEXT &&
                state.usernameNodeId == null
            ) {
                state.usernameNodeId = node.id
            }
        }
        for (child in node.children) {
            scanAutofillTree(child, state)
        }
    }

    private fun extractCommittedCredentials(root: Autofill.Node): Triple<String, String, String> {
        var domain = ""
        var user = ""
        var pass = ""
        fun walk(n: Autofill.Node) {
            if (domain.isBlank() && n.domain.isNotBlank()) domain = n.domain
            val v = n.value.orEmpty()
            when (n.hint) {
                Autofill.Hint.PASSWORD -> if (pass.isEmpty() && v.isNotEmpty()) pass = v
                Autofill.Hint.USERNAME, Autofill.Hint.EMAIL_ADDRESS -> if (user.isBlank() && v.isNotBlank()) user = v
            }
            for (c in n.children) walk(c)
        }
        walk(root)
        val origin = if (domain.isNotBlank() && !domain.startsWith("http")) "https://$domain" else domain
        return Triple(origin, user, pass)
    }

    private fun isLocalHostUri(uri: String): Boolean {
        val lower = uri.lowercase()
        return lower.startsWith("http://localhost") ||
            lower.startsWith("http://127.") ||
            lower.startsWith("http://192.168.") ||
            lower.startsWith("http://10.")
    }

    private fun buildContentBlockingSettings(settings: BrowserSettingsState): ContentBlocking.Settings {
        return ContentBlocking.Settings.Builder()
            .enhancedTrackingProtectionLevel(mapEtpLevel(settings.trackingProtection))
            .antiTracking(mapAntiTrackingCategories(settings.trackingProtection))
            .cookieBehavior(mapCookieBehavior(settings.cookiePolicy))
            .cookieBehaviorPrivateMode(mapCookieBehavior(settings.cookiePolicy))
            .build()
    }

    private fun mapEtpLevel(level: TrackingProtectionLevel): Int {
        return when (level) {
            TrackingProtectionLevel.STRICT -> ContentBlocking.EtpLevel.STRICT
            TrackingProtectionLevel.STANDARD -> ContentBlocking.EtpLevel.DEFAULT
            TrackingProtectionLevel.OFF -> ContentBlocking.EtpLevel.NONE
        }
    }

    private fun mapAntiTrackingCategories(level: TrackingProtectionLevel): Int {
        return when (level) {
            TrackingProtectionLevel.STRICT ->
                ContentBlocking.AntiTracking.STRICT
            TrackingProtectionLevel.STANDARD ->
                ContentBlocking.AntiTracking.DEFAULT or ContentBlocking.AntiTracking.STP
            TrackingProtectionLevel.OFF ->
                ContentBlocking.AntiTracking.NONE
        }
    }

    private fun mapCookieBehavior(policy: CookiePolicy): Int {
        return when (policy) {
            CookiePolicy.ISOLATE_THIRD_PARTY ->
                ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS
            CookiePolicy.BLOCK_TRACKERS ->
                ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS
            CookiePolicy.FIRST_PARTY_ONLY ->
                ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY
            CookiePolicy.ACCEPT_ALL ->
                ContentBlocking.CookieBehavior.ACCEPT_ALL
        }
    }

    private fun mapColorScheme(mode: ThemeMode): Int {
        return when (mode) {
            ThemeMode.DARK -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
            ThemeMode.LIGHT -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
            ThemeMode.SYSTEM -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
        }
    }

    private fun mapWebRequestError(error: WebRequestError): Pair<String, String> {
        val loc = LocaleHelper.localizedContext(context, latestSettings.appLanguage)
        return when (error.category) {
            WebRequestError.ERROR_CATEGORY_SECURITY -> {
                loc.getString(R.string.security_cert_error_title) to
                    loc.getString(R.string.security_cert_error_message)
            }
            WebRequestError.ERROR_CATEGORY_NETWORK -> {
                loc.getString(R.string.err_network_title) to
                    loc.getString(R.string.err_network_desc, error.code)
            }
            WebRequestError.ERROR_CATEGORY_URI -> {
                loc.getString(R.string.err_uri_title) to
                    loc.getString(R.string.err_uri_desc, error.code)
            }
            else -> {
                loc.getString(R.string.err_generic_title) to
                    loc.getString(R.string.err_generic_desc, error.category, error.code)
            }
        }
    }
}
