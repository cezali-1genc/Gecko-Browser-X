package com.example.engine

import android.net.Uri
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import java.io.InputStream
import java.util.UUID

enum class SecurityState {
    HOME_INTERNAL,
    VERIFIED_HTTPS,
    INSECURE_HTTP,
    CERT_ERROR
}

data class SecurityDetails(
    val host: String = "",
    val origin: String = "",
    val isSecure: Boolean = false,
    val issuer: String? = null,
    val subject: String? = null,
    val validUntil: String? = null
)

data class PageLoadError(
    val uri: String,
    val category: Int,
    val code: Int,
    val isSecurityError: Boolean,
    val title: String,
    val description: String
)

data class FindInPageState(
    val active: Boolean = false,
    val query: String = "",
    val currentMatch: Int = 0,
    val totalMatches: Int = 0
)

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Start Page",
    val url: String = UrlUtils.INTERNAL_HOME_URI,
    val isPrivate: Boolean = false,
    val isHomePage: Boolean = true,
    val isPinned: Boolean = false,
    val groupName: String? = null,
    val isAtTop: Boolean = true,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val securityState: SecurityState = SecurityState.HOME_INTERNAL,
    val securityDetails: SecurityDetails = SecurityDetails(),
    val blockedTrackersCount: Int = 0,
    val isFullScreen: Boolean = false,
    val desktopMode: Boolean = false,
    val loadError: PageLoadError? = null
)

sealed interface PendingWebPrompt {
    data class JsAlert(
        val title: String,
        val message: String,
        val result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
        val prompt: GeckoSession.PromptDelegate.AlertPrompt
    ) : PendingWebPrompt

    data class JsConfirm(
        val title: String,
        val message: String,
        val result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
        val prompt: GeckoSession.PromptDelegate.ButtonPrompt
    ) : PendingWebPrompt

    data class JsText(
        val title: String,
        val message: String,
        val defaultValue: String,
        val result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
        val prompt: GeckoSession.PromptDelegate.TextPrompt
    ) : PendingWebPrompt

    data class Choice(
        val title: String,
        val message: String,
        val multiple: Boolean,
        val options: List<ChoiceOption>,
        val result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
        val prompt: GeckoSession.PromptDelegate.ChoicePrompt
    ) : PendingWebPrompt
}

data class ChoiceOption(
    val id: String,
    val label: String,
    val selected: Boolean,
    val disabled: Boolean
)

data class PendingFileUpload(
    val allowMultiple: Boolean,
    val mimeTypes: Array<String>,
    val onFilesSelected: (Array<Uri>?) -> Unit
)

data class PendingSitePermission(
    val origin: String,
    val permissionKey: String,
    val permissionLabel: String,
    val isMediaRequest: Boolean,
    val onDecision: (allow: Boolean, remember: Boolean) -> Unit
)

data class PendingAndroidPermissionRequest(
    val permissions: Array<String>,
    val onResult: (granted: Boolean) -> Unit
)

data class PendingDownloadRequest(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val contentLength: Long,
    val isInsecureHttp: Boolean
)

data class PendingPdfExport(
    val title: String,
    val streamProvider: (() -> GeckoResult<InputStream>)? = null
)
