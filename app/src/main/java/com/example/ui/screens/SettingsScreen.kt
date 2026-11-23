package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.PermissionDecision
import com.example.data.preferences.HomeWallpaperPreset
import com.example.data.preferences.HomepageMode
import com.example.data.preferences.SearchEngine
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.ToolbarStyle
import com.example.i18n.AppTranslations
import com.example.i18n.LocaleHelper
import com.example.ui.components.ZoomControlDialog
import com.example.ui.state.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    isDefaultBrowser: Boolean,
    onRequestDefaultBrowser: () -> Unit,
    openPermissionsDirectly: Boolean = false,
    onOpenPrivacyCenter: () -> Unit,
    onOpenDnsVpn: () -> Unit,
    onOpenPasswordManager: () -> Unit,
    onOpenExtensions: () -> Unit,
    onOpenDictionary: () -> Unit,
    onOpenQrTools: () -> Unit,
    onOpenLegalRegistry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sitePermissions by viewModel.sitePermissions.collectAsStateWithLifecycle()
    val savedCredentials by viewModel.savedCredentials.collectAsStateWithLifecycle()
    val isVpnActive by viewModel.isVpnActive.collectAsStateWithLifecycle()
    val strings = remember(settings.appLanguage) { AppTranslations.forLang(settings.appLanguage) }
    val resolvedLang = LocaleHelper.resolveLocale(settings.appLanguage).language

    val isSystemAutofillActive = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                context.getSystemService(AutofillManager::class.java)?.hasEnabledAutofillServices() == true
            }.getOrDefault(false)
        } else {
            false
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showToolbarDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showWeatherDialog by remember { mutableStateOf(false) }
    var showZoomDialog by remember { mutableStateOf(false) }
    var showHomepageDialog by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember(openPermissionsDirectly) { mutableStateOf(openPermissionsDirectly) }

    val wallpaperPicker = rememberLauncherForActivityResult(
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

    BackHandler {
        if (showPermissionsSheet) showPermissionsSheet = false
        else onNavigateBack()
    }

    if (showPermissionsSheet) {
        SitePermissionsManagerScreen(
            rules = sitePermissions,
            strings = strings,
            onUpdateDecision = { origin, key, decision ->
                viewModel.updateSitePermissionRule(origin, key, decision)
            },
            onDeleteRule = viewModel::deleteSitePermissionRule,
            onClearAll = viewModel::clearAllSitePermissions,
            onBack = {
                if (openPermissionsDirectly) onNavigateBack()
                else showPermissionsSheet = false
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
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
                .testTag("settings_list")
        ) {
            item {
                SettingsActionCard(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = LocaleHelper.languageDisplayName(settings.appLanguage),
                    onClick = { showLanguageDialog = true },
                    testTag = "setting_language"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Public,
                    title = stringResource(R.string.menu_default_browser),
                    subtitle = if (isDefaultBrowser) stringResource(R.string.start_default_active)
                    else stringResource(R.string.start_default_browser_banner),
                    onClick = onRequestDefaultBrowser,
                    testTag = "setting_default_browser"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Palette,
                    title = "Opera ${stringResource(R.string.menu_settings)}",
                    subtitle = settings.themeMode.display(resolvedLang),
                    onClick = { showThemeDialog = true },
                    testTag = "setting_theme"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.ViewCarousel,
                    title = strings.toolbarStyleTitle,
                    subtitle = if (resolvedLang == "tr") settings.toolbarStyle.labelTr else settings.toolbarStyle.labelEn,
                    onClick = { showToolbarDialog = true },
                    testTag = "setting_toolbar_style"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Wallpaper,
                    title = strings.customWallpaper,
                    subtitle = if (resolvedLang == "tr") settings.homeWallpaperPreset.labelTr else settings.homeWallpaperPreset.labelEn,
                    onClick = { showWallpaperDialog = true },
                    testTag = "setting_wallpaper"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Home,
                    title = strings.homepageConfigTitle,
                    subtitle = if (settings.homepageMode == HomepageMode.START_PAGE) {
                        strings.startPage
                    } else {
                        settings.customHomepageUrl
                    },
                    onClick = { showHomepageDialog = true },
                    testTag = "setting_homepage"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.WbSunny,
                    title = strings.weatherSetup,
                    subtitle = if (settings.showWeatherOnHome) "${settings.weatherCity} ✓" else stringResource(R.string.cancel),
                    onClick = { showWeatherDialog = true },
                    testTag = "setting_weather"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Search,
                    title = strings.searchEnginePick,
                    subtitle = settings.searchEngine.displayName,
                    onClick = { showSearchEngineDialog = true },
                    testTag = "setting_search_engine"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.ZoomIn,
                    title = String.format(strings.zoomTitle, settings.pageZoomPercent),
                    subtitle = "${settings.pageZoomPercent}%",
                    onClick = { showZoomDialog = true },
                    testTag = "setting_zoom"
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Filled.Swipe,
                    title = strings.edgeGesturesTitle,
                    subtitle = strings.edgeGesturesSub,
                    checked = settings.edgeGesturesEnabled,
                    onCheckedChange = { viewModel.setGestures(it, settings.pullToRefreshEnabled) },
                    testTag = "setting_edge_gestures"
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Filled.Swipe,
                    title = strings.pullRefreshTitle,
                    subtitle = strings.pullRefreshSub,
                    checked = settings.pullToRefreshEnabled,
                    onCheckedChange = { viewModel.setGestures(settings.edgeGesturesEnabled, it) },
                    testTag = "setting_pull_refresh"
                )
            }

            item { HorizontalDivider() }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Shield,
                    title = stringResource(R.string.menu_privacy_center),
                    subtitle = "${settings.trackingProtection.label(resolvedLang)} • ${settings.cookiePolicy.label(resolvedLang)}",
                    onClick = onOpenPrivacyCenter,
                    testTag = "setting_privacy_center"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.VpnLock,
                    title = stringResource(R.string.menu_dns_vpn),
                    subtitle = "${settings.dnsProvider.displayName} • ${if (isVpnActive) stringResource(R.string.dns_vpn_active) else stringResource(R.string.dns_vpn_inactive)}",
                    onClick = onOpenDnsVpn,
                    testTag = "setting_dns_vpn"
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Filled.Key,
                    title = strings.passwordPromptTitle,
                    subtitle = strings.passwordPromptSub,
                    checked = settings.passwordSavePromptEnabled,
                    onCheckedChange = { viewModel.setPrivacyToggles(passwordPrompt = it) },
                    testTag = "setting_password_autofill_toggle"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Key,
                    title = stringResource(R.string.menu_passwords),
                    subtitle = "${savedCredentials.size} • Android Keystore AES-256-GCM",
                    onClick = onOpenPasswordManager,
                    testTag = "setting_passwords"
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                item {
                    SettingsActionCard(
                        icon = Icons.Filled.Security,
                        title = stringResource(R.string.pw_system_autofill_title),
                        subtitle = if (isSystemAutofillActive) {
                            stringResource(R.string.pw_system_autofill_active)
                        } else {
                            stringResource(R.string.pw_system_autofill_cta)
                        },
                        onClick = {
                            runCatching {
                                val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        },
                        testTag = "setting_system_autofill"
                    )
                }
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Extension,
                    title = stringResource(R.string.menu_extensions),
                    subtitle = strings.extensionsSub,
                    onClick = onOpenExtensions,
                    testTag = "setting_extensions"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Security,
                    title = strings.sitePermissionsTitle,
                    subtitle = "${sitePermissions.size}",
                    onClick = { showPermissionsSheet = true },
                    testTag = "setting_site_permissions"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = stringResource(R.string.menu_dictionary),
                    subtitle = stringResource(R.string.brand_short),
                    onClick = onOpenDictionary,
                    testTag = "setting_dictionary"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.QrCodeScanner,
                    title = stringResource(R.string.menu_qr_tools),
                    subtitle = "${strings.qrScanTab} • ${strings.qrCreateTab}",
                    onClick = onOpenQrTools,
                    testTag = "setting_qr_tools"
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.menu_about),
                    subtitle = "${stringResource(R.string.brand_team)} • ${stringResource(R.string.brand_developer)}",
                    onClick = onOpenLegalRegistry,
                    testTag = "setting_about"
                )
            }
        }
    }

    if (showLanguageDialog) {
        val options = listOf(LocaleHelper.SYSTEM_TAG to stringResource(R.string.lang_follow_system)) +
            LocaleHelper.supportedLanguages.map { it.tag to it.nativeName }
        SingleChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = options,
            selected = settings.appLanguage,
            onSelect = {
                viewModel.setAppLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = stringResource(R.string.brand_full_title),
            options = ThemeMode.entries.map { it to it.display(resolvedLang) },
            selected = settings.themeMode,
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showToolbarDialog) {
        SingleChoiceDialog(
            title = strings.toolbarStyleTitle,
            options = ToolbarStyle.entries.map { it to (if (resolvedLang == "tr") it.labelTr else it.labelEn) },
            selected = settings.toolbarStyle,
            onSelect = {
                viewModel.setToolbarStyle(it)
                showToolbarDialog = false
            },
            onDismiss = { showToolbarDialog = false }
        )
    }

    if (showSearchEngineDialog) {
        SingleChoiceDialog(
            title = strings.searchEnginePick,
            options = SearchEngine.entries.map { it to it.displayName },
            selected = settings.searchEngine,
            onSelect = {
                viewModel.setSearchEngine(it)
                showSearchEngineDialog = false
            },
            onDismiss = { showSearchEngineDialog = false }
        )
    }

    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text(strings.customWallpaper) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeWallpaperPreset.entries.filter { it != HomeWallpaperPreset.CUSTOM_IMAGE }.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setHomeWallpaper(preset, null)
                                    showWallpaperDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.homeWallpaperPreset == preset,
                                onClick = {
                                    viewModel.setHomeWallpaper(preset, null)
                                    showWallpaperDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (resolvedLang == "tr") preset.labelTr else preset.labelEn)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            showWallpaperDialog = false
                            wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.wallpaperGallery)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWallpaperDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showWeatherDialog) {
        val cities = listOf(
            Triple("Istanbul", 41.0082, 28.9784),
            Triple("Ankara", 39.9334, 32.8597),
            Triple("Izmir", 38.4237, 27.1428),
            Triple("London", 51.5072, -0.1276),
            Triple("Berlin", 52.5200, 13.4050),
            Triple("Tokyo", 35.6762, 139.6503)
        )
        var weatherOn by remember(settings.showWeatherOnHome) { mutableStateOf(settings.showWeatherOnHome) }
        AlertDialog(
            onDismissRequest = { showWeatherDialog = false },
            title = { Text(strings.weatherSetup) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { weatherOn = !weatherOn },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = weatherOn, onCheckedChange = { weatherOn = it })
                        Spacer(Modifier.width(6.dp))
                        Text(strings.showWeather)
                    }
                    HorizontalDivider()
                    cities.forEach { (city, lat, lon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setWeatherConfig(city, lat, lon, weatherOn)
                                    showWeatherDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.weatherCity == city,
                                onClick = {
                                    viewModel.setWeatherConfig(city, lat, lon, weatherOn)
                                    showWeatherDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(city)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setWeatherConfig(
                            settings.weatherCity,
                            settings.weatherLatitude,
                            settings.weatherLongitude,
                            weatherOn
                        )
                        showWeatherDialog = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showWeatherDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showHomepageDialog) {
        HomepageConfigDialog(
            currentMode = settings.homepageMode,
            currentUrl = settings.customHomepageUrl,
            showSpeedDials = settings.showSpeedDialsOnHome,
            showRecentSites = settings.showRecentSitesOnHome,
            resolvedLang = resolvedLang,
            strings = strings,
            onSave = { mode, url, dials, recent ->
                viewModel.updateHomepageConfig(mode, url, dials, recent, settings.showWeatherOnHome)
                showHomepageDialog = false
            },
            onDismiss = { showHomepageDialog = false }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SitePermissionsManagerScreen(
    rules: List<com.example.data.local.SitePermissionEntity>,
    strings: AppTranslations.UiStrings,
    onUpdateDecision: (origin: String, permissionType: String, decision: PermissionDecision) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.sitePermissionsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (rules.isNotEmpty()) {
                        TextButton(onClick = onClearAll) { Text(strings.resetAll) }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (rules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(strings.noSavedPermissions, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(rules, key = { it.id }) { rule ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.origin, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${rule.permissionType} • ${rule.decision.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val next = if (rule.decision == PermissionDecision.ALLOW) {
                                        PermissionDecision.BLOCK
                                    } else {
                                        PermissionDecision.ALLOW
                                    }
                                    onUpdateDecision(rule.origin, rule.permissionType, next)
                                }
                            ) {
                                Text(if (rule.decision == PermissionDecision.ALLOW) strings.allowed else strings.blocked)
                            }
                            IconButton(onClick = { onDeleteRule(rule.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun <T> SingleChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
private fun HomepageConfigDialog(
    currentMode: HomepageMode,
    currentUrl: String,
    showSpeedDials: Boolean,
    showRecentSites: Boolean,
    resolvedLang: String,
    strings: AppTranslations.UiStrings,
    onSave: (HomepageMode, String, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(currentMode) }
    var url by remember { mutableStateOf(currentUrl) }
    var dials by remember { mutableStateOf(showSpeedDials) }
    var recent by remember { mutableStateOf(showRecentSites) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.homepageConfigTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HomepageMode.entries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mode = entry },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == entry, onClick = { mode = entry })
                        Spacer(Modifier.width(8.dp))
                        Text(entry.label(resolvedLang))
                    }
                }

                if (mode == HomepageMode.CUSTOM_URL) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(strings.shortcutUrl) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dials = !dials },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = dials, onCheckedChange = { dials = it })
                    Spacer(Modifier.width(8.dp))
                    Text(strings.showShortcuts)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { recent = !recent },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = recent, onCheckedChange = { recent = it })
                    Spacer(Modifier.width(8.dp))
                    Text(strings.showRecent)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(mode, url, dials, recent) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
