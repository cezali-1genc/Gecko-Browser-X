package com.example.data.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.BrowserXApplication
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Tracks whether the browser's currently active session is a Private Browsing tab so
 * both in-browser autofill and the Android AutofillService strictly exclude Private Mode.
 */
object BrowserAutofillTracker {
    @Volatile
    var isPrivateSessionActive: Boolean = false

    @Volatile
    var isAutofillEnabled: Boolean = true
}

private data class ParsedAutofillFields(
    var webDomain: String? = null,
    var usernameId: AutofillId? = null,
    var usernameValue: String? = null,
    var passwordId: AutofillId? = null,
    var passwordValue: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
class BrowserAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        // Strictly exclude private browsing sessions and disabled autofill state
        if (BrowserAutofillTracker.isPrivateSessionActive || !BrowserAutofillTracker.isAutofillEnabled) {
            callback.onSuccess(null)
            return
        }

        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val fields = parseStructure(structure)
        val domain = fields.webDomain?.takeIf { it.isNotBlank() }
        val userId = fields.usernameId
        val passId = fields.passwordId

        if (domain == null || (userId == null && passId == null)) {
            callback.onSuccess(null)
            return
        }

        val credentialRepo = (application as? BrowserXApplication)?.container?.credentialRepository
        if (credentialRepo == null) {
            callback.onSuccess(null)
            return
        }

        val job = serviceScope.launch {
            val origin = if (domain.startsWith("http")) domain else "https://$domain"
            val matches = credentialRepo.getCredentialsForOrigin(
                originOrUrl = origin,
                isPrivateSession = BrowserAutofillTracker.isPrivateSessionActive
            )

            if (cancellationSignal.isCanceled) return@launch

            val responseBuilder = FillResponse.Builder()
            var hasSaveIds = false
            val saveIds = mutableListOf<AutofillId>()
            var saveMask = 0
            if (userId != null) {
                saveIds.add(userId)
                saveMask = saveMask or SaveInfo.SAVE_DATA_TYPE_USERNAME
                hasSaveIds = true
            }
            if (passId != null) {
                saveIds.add(passId)
                saveMask = saveMask or SaveInfo.SAVE_DATA_TYPE_PASSWORD
                hasSaveIds = true
            }

            if (hasSaveIds) {
                responseBuilder.setSaveInfo(
                    SaveInfo.Builder(saveMask, saveIds.toTypedArray()).build()
                )
            }

            if (matches.isEmpty() && !hasSaveIds) {
                callback.onSuccess(null)
                return@launch
            }

            matches.take(20).forEach { cred ->
                val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
                    setTextViewText(android.R.id.text1, cred.username)
                    setTextViewText(android.R.id.text2, "${cred.host} • ${getString(R.string.app_name)}")
                }
                val datasetBuilder = Dataset.Builder()
                var addedField = false
                if (userId != null) {
                    datasetBuilder.setValue(userId, AutofillValue.forText(cred.username), presentation)
                    addedField = true
                }
                if (passId != null) {
                    datasetBuilder.setValue(passId, AutofillValue.forText(cred.password), presentation)
                    addedField = true
                }
                if (addedField) {
                    responseBuilder.addDataset(datasetBuilder.build())
                }
            }

            callback.onSuccess(responseBuilder.build())
        }

        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        if (BrowserAutofillTracker.isPrivateSessionActive || !BrowserAutofillTracker.isAutofillEnabled) {
            callback.onSuccess()
            return
        }

        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess()
            return
        }

        val fields = parseStructure(structure)
        val domain = fields.webDomain?.takeIf { it.isNotBlank() }
        val user = fields.usernameValue?.trim().orEmpty()
        val pass = fields.passwordValue.orEmpty()

        val credentialRepo = (application as? BrowserXApplication)?.container?.credentialRepository
        if (credentialRepo == null || domain == null || user.isBlank() || pass.isEmpty()) {
            callback.onSuccess()
            return
        }

        serviceScope.launch {
            val origin = if (domain.startsWith("http")) domain else "https://$domain"
            credentialRepo.saveCredential(origin, user, pass)
            callback.onSuccess()
        }
    }

    private fun parseStructure(structure: AssistStructure): ParsedAutofillFields {
        val result = ParsedAutofillFields()
        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode ?: continue
            traverseNode(root, result)
        }
        return result
    }

    private fun traverseNode(node: AssistStructure.ViewNode, acc: ParsedAutofillFields) {
        if (acc.webDomain.isNullOrBlank()) {
            node.webDomain?.takeIf { it.isNotBlank() }?.let { acc.webDomain = it }
        }

        val autofillId = node.autofillId
        val hints = node.autofillHints?.map { it.lowercase() }.orEmpty()
        val hintText = node.hint?.lowercase().orEmpty()
        val idEntry = node.idEntry?.lowercase().orEmpty()
        val inputType = node.inputType
        val isPasswordInput = (inputType and InputType.TYPE_MASK_VARIATION) in setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        ) || (inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD) == InputType.TYPE_NUMBER_VARIATION_PASSWORD

        if (autofillId != null) {
            val isPassword = hints.any { it.contains("password") } ||
                isPasswordInput ||
                hintText.contains("password") ||
                hintText.contains("şifre") ||
                hintText.contains("parola") ||
                idEntry.contains("password")

            val isUsername = !isPassword && (
                hints.any {
                    it.contains("username") || it.contains("email") || it.contains("phone")
                } ||
                    (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
                    (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                    hintText.contains("email") ||
                    hintText.contains("user") ||
                    hintText.contains("kullanıcı") ||
                    idEntry.contains("user") ||
                    idEntry.contains("email")
                )

            val valueText = node.autofillValue?.takeIf { it.isText }?.textValue?.toString()
                ?: node.text?.toString()

            if (isPassword && acc.passwordId == null) {
                acc.passwordId = autofillId
                acc.passwordValue = valueText
            } else if (isUsername && acc.usernameId == null) {
                acc.usernameId = autofillId
                acc.usernameValue = valueText
            }
        }

        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { traverseNode(it, acc) }
        }
    }
}
