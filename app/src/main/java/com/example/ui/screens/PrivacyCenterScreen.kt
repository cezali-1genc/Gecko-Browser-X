package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.preferences.CookiePolicy
import com.example.data.preferences.TrackingProtectionLevel
import com.example.i18n.AppTranslations
import com.example.i18n.LocaleHelper
import com.example.ui.state.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCenterScreen(
    viewModel: BrowserViewModel,
    onOpenSitePermissions: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sitePermissions by viewModel.sitePermissions.collectAsStateWithLifecycle()
    val strings = remember(settings.appLanguage) { AppTranslations.forLang(settings.appLanguage) }
    val resolvedLang = LocaleHelper.resolveLocale(settings.appLanguage).language

    var showTrackingDialog by remember { mutableStateOf(false) }
    var showCookieDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_center_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsActionCard(
                    icon = Icons.Filled.Shield,
                    title = strings.etpTitle,
                    subtitle = "${settings.trackingProtection.label(resolvedLang)} — ${settings.trackingProtection.summary(resolvedLang)}",
                    onClick = { showTrackingDialog = true },
                    testTag = "privacy_etp"
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Filled.Cookie,
                    title = strings.cookiePolicyTitle,
                    subtitle = "${settings.cookiePolicy.label(resolvedLang)} — ${settings.cookiePolicy.summary(resolvedLang)}",
                    onClick = { showCookieDialog = true },
                    testTag = "privacy_cookies"
                )
            }
            item {
                SettingsSwitchCard(
                    icon = Icons.Filled.Block,
                    title = strings.popupBlockingTitle,
                    subtitle = strings.popupBlockingSub,
                    checked = settings.popupBlockingEnabled,
                    onCheckedChange = { viewModel.setPrivacyToggles(popupBlocking = it) },
                    testTag = "privacy_popups"
                )
            }
            item {
                SettingsSwitchCard(
                    icon = Icons.Filled.Https,
                    title = strings.httpsOnlyTitle,
                    subtitle = strings.httpsOnlySub,
                    checked = settings.httpsOnlyEnabled,
                    onCheckedChange = { viewModel.setPrivacyToggles(httpsOnly = it) },
                    testTag = "privacy_https_only"
                )
            }
            item {
                SettingsSwitchCard(
                    icon = Icons.Filled.FilterAlt,
                    title = strings.adFilterTitle,
                    subtitle = strings.adFilterSub,
                    checked = settings.adFilterRulesEnabled,
                    onCheckedChange = { viewModel.setPrivacyToggles(adFilterRules = it) },
                    testTag = "privacy_ad_filter"
                )
            }
            item {
                SettingsSwitchCard(
                    icon = Icons.Filled.Key,
                    title = strings.passwordPromptTitle,
                    subtitle = strings.passwordPromptSub,
                    checked = settings.passwordSavePromptEnabled,
                    onCheckedChange = { viewModel.setPrivacyToggles(passwordPrompt = it) },
                    testTag = "privacy_pw_prompt"
                )
            }
            item {
                SettingsSwitchCard(
                    icon = Icons.Filled.Code,
                    title = strings.jsEngineTitle,
                    subtitle = strings.jsEngineSub,
                    checked = settings.javaScriptEnabled,
                    onCheckedChange = viewModel::setJavaScriptEnabled,
                    testTag = "privacy_js"
                )
            }
            item {
                SettingsSwitchCard(
                    icon = Icons.Filled.History,
                    title = strings.saveHistoryTitle,
                    subtitle = strings.saveHistorySub,
                    checked = settings.saveHistoryEnabled,
                    onCheckedChange = viewModel::setSaveHistoryEnabled,
                    testTag = "privacy_history"
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Filled.Security,
                    title = strings.sitePermissionsTitle,
                    subtitle = "${sitePermissions.size}",
                    onClick = onOpenSitePermissions,
                    testTag = "privacy_site_permissions"
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Filled.DeleteForever,
                    title = strings.clearDataTitle,
                    subtitle = strings.clearDataSub,
                    onClick = { showClearDataDialog = true },
                    testTag = "privacy_clear_data"
                )
            }
        }
    }

    if (showTrackingDialog) {
        SingleChoiceDialog(
            title = strings.etpTitle,
            options = TrackingProtectionLevel.entries.map {
                it to "${it.label(resolvedLang)}: ${it.summary(resolvedLang)}"
            },
            selected = settings.trackingProtection,
            onSelect = {
                viewModel.setTrackingProtection(it)
                showTrackingDialog = false
            },
            onDismiss = { showTrackingDialog = false }
        )
    }

    if (showCookieDialog) {
        SingleChoiceDialog(
            title = strings.cookiePolicyTitle,
            options = CookiePolicy.entries.map {
                it to "${it.label(resolvedLang)}: ${it.summary(resolvedLang)}"
            },
            selected = settings.cookiePolicy,
            onSelect = {
                viewModel.setCookiePolicy(it)
                showCookieDialog = false
            },
            onDismiss = { showCookieDialog = false }
        )
    }

    if (showClearDataDialog) {
        var cookies by remember { mutableStateOf(true) }
        var cache by remember { mutableStateOf(true) }
        var siteData by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text(strings.clearDataTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { cookies = !cookies },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = cookies, onCheckedChange = { cookies = it })
                        Spacer(Modifier.width(8.dp))
                        Text(strings.clearCookies)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { cache = !cache },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = cache, onCheckedChange = { cache = it })
                        Spacer(Modifier.width(8.dp))
                        Text(strings.clearCache)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { siteData = !siteData },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = siteData, onCheckedChange = { siteData = it })
                        Spacer(Modifier.width(8.dp))
                        Text(strings.clearSiteStorage)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearEngineSiteData(cookies, cache, siteData)
                        showClearDataDialog = false
                    },
                    enabled = cookies || cache || siteData
                ) {
                    Text(strings.clearNow)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
