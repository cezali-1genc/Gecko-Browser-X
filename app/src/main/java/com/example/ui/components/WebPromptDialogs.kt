package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.BrowserTab
import com.example.engine.PageLoadError
import com.example.engine.PendingDownloadRequest
import com.example.engine.PendingSitePermission
import com.example.engine.PendingWebPrompt
import com.example.engine.SecurityState
import com.example.engine.UrlUtils
import com.example.i18n.AppTranslations
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WarningAmber

@Composable
fun PageErrorOverlay(
    error: PageLoadError,
    appLanguage: String,
    onGoBack: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = remember(appLanguage) { AppTranslations.forLang(appLanguage) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                if (error.isSecurityError) DangerCoral.copy(alpha = 0.65f)
                else MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (error.isSecurityError) DangerCoral.copy(alpha = 0.16f)
                            else WarningAmber.copy(alpha = 0.16f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (error.isSecurityError) Icons.Filled.GppBad
                        else Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = if (error.isSecurityError) DangerCoral else WarningAmber,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = UrlUtils.extractHost(error.uri),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onGoBack,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("error_back_to_safety_button")
                    ) {
                        Text(strings.backToSafety)
                    }
                    if (!error.isSecurityError) {
                        OutlinedButton(
                            onClick = onReload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(strings.retry)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDetailsSheet(
    tab: BrowserTab,
    appLanguage: String,
    onDismiss: () -> Unit,
    onOpenSitePermissions: () -> Unit
) {
    val strings = remember(appLanguage) { AppTranslations.forLang(appLanguage) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, tint, heading) = when (tab.securityState) {
                    SecurityState.VERIFIED_HTTPS ->
                        Triple(Icons.Filled.Lock, VerifiedEmerald, stringResource(R.string.security_verified_https))
                    SecurityState.INSECURE_HTTP ->
                        Triple(Icons.Filled.WarningAmber, WarningAmber, stringResource(R.string.security_http_warning))
                    SecurityState.CERT_ERROR ->
                        Triple(Icons.Filled.GppBad, DangerCoral, stringResource(R.string.security_cert_error_title))
                    SecurityState.HOME_INTERNAL ->
                        Triple(Icons.Filled.Shield, MaterialTheme.colorScheme.primary, stringResource(R.string.brand_full_title))
                }
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(heading, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = UrlUtils.extractHost(tab.url),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            if (tab.securityDetails.issuer != null) {
                SecurityRow(strings.certIssuer, tab.securityDetails.issuer)
            }
            if (tab.securityDetails.validUntil != null) {
                SecurityRow(strings.validUntil, tab.securityDetails.validUntil)
            }
            SecurityRow(
                strings.etpTitle,
                String.format(strings.trackerStats, tab.blockedTrackersCount)
            )
            SecurityRow(
                strings.sessionMode,
                if (tab.isPrivate) stringResource(R.string.private_mode_label)
                else stringResource(R.string.engine_badge)
            )

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenSitePermissions()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.managePermissions)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SecurityRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun WebPromptDialog(
    prompt: PendingWebPrompt,
    onDismiss: () -> Unit,
    onConfirmAlert: (PendingWebPrompt.JsAlert) -> Unit,
    onResolveConfirm: (PendingWebPrompt.JsConfirm, Boolean) -> Unit,
    onResolveText: (PendingWebPrompt.JsText, String) -> Unit,
    onResolveChoice: (PendingWebPrompt.Choice, List<String>) -> Unit
) {
    when (prompt) {
        is PendingWebPrompt.JsAlert -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(prompt.title) },
                text = { Text(prompt.message) },
                confirmButton = {
                    Button(onClick = { onConfirmAlert(prompt) }) { Text(stringResource(R.string.close)) }
                }
            )
        }

        is PendingWebPrompt.JsConfirm -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(prompt.title) },
                text = { Text(prompt.message) },
                confirmButton = {
                    Button(onClick = { onResolveConfirm(prompt, true) }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = {
                    TextButton(onClick = { onResolveConfirm(prompt, false) }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        is PendingWebPrompt.JsText -> {
            var value by remember(prompt) { mutableStateOf(prompt.defaultValue) }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(prompt.title) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (prompt.message.isNotBlank()) Text(prompt.message)
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { onResolveText(prompt, value) }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        is PendingWebPrompt.Choice -> {
            val selectedIds = remember(prompt) {
                mutableStateListOf<String>().apply {
                    addAll(prompt.options.filter { it.selected }.map { it.id })
                }
            }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(prompt.title) },
                text = {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(prompt.options) { option ->
                            val isSelected = selectedIds.contains(option.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !option.disabled) {
                                        if (prompt.multiple) {
                                            if (isSelected) selectedIds.remove(option.id)
                                            else selectedIds.add(option.id)
                                        } else {
                                            onResolveChoice(prompt, listOf(option.id))
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (prompt.multiple) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        enabled = !option.disabled
                                    )
                                } else {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        enabled = !option.disabled
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = option.label,
                                    color = if (option.disabled) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    if (prompt.multiple) {
                        Button(onClick = { onResolveChoice(prompt, selectedIds.toList()) }) {
                            Text(stringResource(R.string.save))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@Composable
fun SitePermissionDialog(
    request: PendingSitePermission,
    appLanguage: String,
    onDecision: (allow: Boolean, remember: Boolean) -> Unit
) {
    val strings = remember(appLanguage) { AppTranslations.forLang(appLanguage) }
    var rememberChoice by remember(request) { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { onDecision(false, false) },
        icon = { Icon(Icons.Filled.Security, contentDescription = null) },
        title = { Text(strings.sitePermissionsTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${request.origin} → ${request.permissionLabel}")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { rememberChoice = !rememberChoice },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDecision(true, rememberChoice) }) {
                Text(strings.allowed)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDecision(false, rememberChoice) }) {
                Text(strings.blocked)
            }
        }
    )
}

@Composable
fun DownloadConfirmDialog(
    request: PendingDownloadRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = null,
                tint = if (request.isInsecureHttp) WarningAmber else MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(stringResource(R.string.dl_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = request.fileName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${UrlUtils.extractHost(request.url)} • ${UrlUtils.formatBytes(request.contentLength)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (request.isInsecureHttp) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WarningAmber.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = stringResource(R.string.dl_confirm_insecure_warn),
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningAmber,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_download_button")
            ) {
                Text(stringResource(R.string.dl_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
