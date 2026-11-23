package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.SavedCredentialEntity
import com.example.i18n.AppTranslations
import com.example.ui.state.BrowserViewModel
import com.example.ui.theme.PrivateViolet
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val credentials by viewModel.savedCredentials.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val strings = remember(settings.appLanguage) { AppTranslations.forLang(settings.appLanguage) }
    val keystoreActive = remember { viewModel.isHardwareKeystoreAvailable() }

    val isSystemAutofillActive = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                context.getSystemService(AutofillManager::class.java)?.hasEnabledAutofillServices() == true
            }.getOrDefault(false)
        } else {
            false
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCredential by remember { mutableStateOf<SavedCredentialEntity?>(null) }

    val filtered = remember(credentials, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) credentials
        else credentials.filter {
            it.host.contains(q, ignoreCase = true) ||
                it.origin.contains(q, ignoreCase = true) ||
                it.username.contains(q, ignoreCase = true)
        }
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pw_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (credentials.isNotEmpty()) {
                        TextButton(
                            onClick = viewModel::clearAllCredentials,
                            modifier = Modifier.testTag("pw_erase_all")
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.eraseAllPasswords)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_password_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.pw_add))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Keystore & Autofill Summary Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, VerifiedEmerald.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.EnhancedEncryption,
                            contentDescription = null,
                            tint = VerifiedEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (keystoreActive) "Android Keystore • AES-256-GCM"
                            else "AES-256-GCM Vault",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.passwordPromptTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = strings.passwordPromptSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.passwordSavePromptEnabled,
                            onCheckedChange = { viewModel.setPrivacyToggles(passwordPrompt = it) },
                            modifier = Modifier.testTag("pw_autofill_switch")
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isSystemAutofillActive) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pw_enable_system_autofill")
                        ) {
                            Text(stringResource(R.string.pw_system_autofill_cta))
                        }
                    }
                }
            }

            if (activeTab.isPrivate) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = PrivateViolet.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, PrivateViolet.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = stringResource(R.string.pw_never_private),
                        style = MaterialTheme.typography.bodySmall,
                        color = PrivateViolet,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (credentials.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.pw_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("pw_search_field")
                )
            }

            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.pw_empty_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.pw_empty_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { item ->
                        CredentialCard(
                            entity = item,
                            strings = strings,
                            decryptPassword = { viewModel.decryptCredentialPassword(item) },
                            onEdit = { editingCredential = item },
                            onCopyPassword = { plain ->
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("password", plain))
                                viewModel.showInfoMessage(context.getString(R.string.pw_copied))
                            },
                            onDelete = { viewModel.deleteCredential(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CredentialEditorDialog(
            title = stringResource(R.string.pw_add),
            initialSite = "https://",
            initialUsername = "",
            initialPassword = "",
            onConfirm = { site, username, password ->
                viewModel.saveCredential(site, username, password)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingCredential?.let { target ->
        val decrypted = remember(target.id) { viewModel.decryptCredentialPassword(target).orEmpty() }
        CredentialEditorDialog(
            title = stringResource(R.string.edit),
            initialSite = target.origin,
            initialUsername = target.username,
            initialPassword = decrypted,
            onConfirm = { site, username, password ->
                viewModel.updateCredential(target.id, site, username, password)
                editingCredential = null
            },
            onDismiss = { editingCredential = null }
        )
    }
}

@Composable
private fun CredentialEditorDialog(
    title: String,
    initialSite: String,
    initialUsername: String,
    initialPassword: String,
    onConfirm: (site: String, username: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var site by remember { mutableStateOf(initialSite) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = site,
                    onValueChange = { site = it },
                    label = { Text(stringResource(R.string.pw_site)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.pw_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.pw_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(site, username, password) },
                enabled = site.isNotBlank() && username.isNotBlank() && password.isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun CredentialCard(
    entity: SavedCredentialEntity,
    strings: AppTranslations.UiStrings,
    decryptPassword: () -> String?,
    onEdit: () -> Unit,
    onCopyPassword: (String) -> Unit,
    onDelete: () -> Unit
) {
    var revealed by remember(entity.id) { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entity.host, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(entity.username, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = revealed ?: "••••••••••••",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    revealed = if (revealed == null) decryptPassword() else null
                }
            ) {
                Icon(
                    imageVector = if (revealed == null) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = strings.showPassword,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = {
                    val plain = revealed ?: decryptPassword()
                    if (plain != null) onCopyPassword(plain)
                }
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = strings.copyPassword, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp))
            }
        }
    }
}
