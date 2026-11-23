package com.example

import android.app.Activity
import android.app.DownloadManager
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.preferences.ThemeMode
import com.example.i18n.LocaleHelper
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BookmarksHistoryScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DnsVpnScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.ExtensionsScreen
import com.example.ui.screens.OfflineDictionaryScreen
import com.example.ui.screens.PasswordManagerScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.PrivacyCenterScreen
import com.example.ui.screens.QrToolsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TabSwitcherScreen
import com.example.ui.state.BrowserViewModel
import com.example.ui.theme.BrowserXTheme

object BrowserRoutes {
    const val BROWSER = "browser"
    const val TABS = "tabs"
    const val LIBRARY = "library/{tabIndex}"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings/{openPermissions}"
    const val PRIVACY_CENTER = "privacy_center"
    const val DNS_VPN = "dns_vpn"
    const val PASSWORDS = "passwords"
    const val EXTENSIONS = "extensions"
    const val QR_TOOLS = "qr_tools"
    const val DICTIONARY = "dictionary"
    const val PDF_VIEWER = "pdf_viewer?uri={uri}"
    const val ABOUT = "about"

    fun library(tabIndex: Int = 0) = "library/$tabIndex"
    fun settings(openPermissions: Boolean = false) = "settings/$openPermissions"
    fun pdfViewer(uri: String? = null): String {
        val encoded = if (uri.isNullOrBlank()) "" else Uri.encode(uri)
        return "pdf_viewer?uri=$encoded"
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels {
        val container = (application as BrowserXApplication).container
        BrowserViewModel.Factory(
            browserEngine = container.browserEngine,
            dataRepository = container.dataRepository,
            settingsRepository = container.settingsRepository,
            suggestionRepository = container.searchSuggestionRepository,
            downloadBridge = container.downloadBridge,
            credentialRepository = container.credentialRepository,
            toolsRepository = container.toolsRepository
        )
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    viewModel.onSystemDownloadCompleted(downloadId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )

        handleIncomingIntent(intent)

        setContent {
            val baseContext = LocalContext.current
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val resolvedLocale = remember(settings.appLanguage) {
                LocaleHelper.resolveLocale(settings.appLanguage)
            }
            val localizedContext = remember(settings.appLanguage) {
                LocaleHelper.localizedContext(baseContext, settings.appLanguage)
            }
            val layoutDirection = remember(resolvedLocale) {
                LocaleHelper.layoutDirectionFor(resolvedLocale)
            }

            val isDark = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(isDark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides layoutDirection
            ) {
                BrowserXTheme(themeMode = settings.themeMode) {
                    BrowserXRoot(
                        viewModel = viewModel,
                        onFullScreenChanged = { fullScreen ->
                            applyImmersiveMode(fullScreen)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(downloadReceiver) }
        super.onDestroy()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val dataUrl = intent.dataString
            if (!dataUrl.isNullOrBlank()) {
                viewModel.openUrlFromExternalIntent(dataUrl)
            }
        }
    }

    private fun applyImmersiveMode(fullScreen: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (fullScreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun checkIsDefaultBrowser(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        roleManager?.isRoleHeld(RoleManager.ROLE_BROWSER) == true
    } else {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        resolved?.activityInfo?.packageName == context.packageName
    }
}

@Composable
fun BrowserXRoot(
    viewModel: BrowserViewModel,
    onFullScreenChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var isDefaultBrowser by remember { mutableStateOf(checkIsDefaultBrowser(context)) }

    val pendingAndroidPermissions by viewModel.pendingAndroidPermissions.collectAsStateWithLifecycle()
    val pendingFileUpload by viewModel.pendingFileUpload.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.resolveAndroidPermissions(allGranted)
    }

    val singleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.resolveFileUpload(uri?.let { arrayOf(it) })
    }

    val multiFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.resolveFileUpload(if (uris.isEmpty()) null else uris.toTypedArray())
    }

    val defaultBrowserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultBrowser = checkIsDefaultBrowser(context)
    }

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.setVpnShieldActive(context, true)
        }
    }

    val requestDefaultBrowser: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
            ) {
                runCatching {
                    defaultBrowserLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
                }
            }
        } else {
            runCatching {
                defaultBrowserLauncher.launch(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            }
        }
    }

    val requestStartVpn: () -> Unit = {
        val prepareIntent = runCatching { VpnService.prepare(context) }.getOrNull()
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            viewModel.setVpnShieldActive(context, true)
        }
    }

    LaunchedEffect(pendingAndroidPermissions) {
        val req = pendingAndroidPermissions ?: return@LaunchedEffect
        permissionLauncher.launch(req.permissions)
    }

    LaunchedEffect(pendingFileUpload) {
        val upload = pendingFileUpload ?: return@LaunchedEffect
        if (upload.allowMultiple) {
            multiFileLauncher.launch(upload.mimeTypes)
        } else {
            singleFileLauncher.launch(upload.mimeTypes)
        }
    }

    NavHost(
        navController = navController,
        startDestination = BrowserRoutes.BROWSER
    ) {
        composable(BrowserRoutes.BROWSER) {
            BrowserScreen(
                viewModel = viewModel,
                isDefaultBrowser = isDefaultBrowser,
                onRequestDefaultBrowser = requestDefaultBrowser,
                onOpenTabSwitcher = { navController.navigate(BrowserRoutes.TABS) },
                onOpenBookmarksAndHistory = { tabIdx ->
                    navController.navigate(BrowserRoutes.library(tabIdx))
                },
                onOpenDownloads = { navController.navigate(BrowserRoutes.DOWNLOADS) },
                onOpenSettings = { navController.navigate(BrowserRoutes.settings(false)) },
                onOpenSitePermissions = { navController.navigate(BrowserRoutes.settings(true)) },
                onOpenPrivacyCenter = { navController.navigate(BrowserRoutes.PRIVACY_CENTER) },
                onOpenDnsVpn = { navController.navigate(BrowserRoutes.DNS_VPN) },
                onOpenPasswordManager = { navController.navigate(BrowserRoutes.PASSWORDS) },
                onOpenExtensions = { navController.navigate(BrowserRoutes.EXTENSIONS) },
                onOpenQrTools = { navController.navigate(BrowserRoutes.QR_TOOLS) },
                onOpenDictionary = { navController.navigate(BrowserRoutes.DICTIONARY) },
                onOpenPdfViewer = { navController.navigate(BrowserRoutes.pdfViewer(null)) },
                onOpenLegalRegistry = { navController.navigate(BrowserRoutes.ABOUT) },
                onFullScreenStateChanged = onFullScreenChanged
            )
        }

        composable(BrowserRoutes.TABS) {
            TabSwitcherScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = BrowserRoutes.LIBRARY,
            arguments = listOf(navArgument("tabIndex") { type = NavType.IntType })
        ) { backStackEntry ->
            val tabIndex = backStackEntry.arguments?.getInt("tabIndex") ?: 0
            BookmarksHistoryScreen(
                viewModel = viewModel,
                initialTab = tabIndex,
                onOpenUrl = { url -> viewModel.navigateCurrentTab(url) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BrowserRoutes.DOWNLOADS) {
            DownloadsScreen(
                viewModel = viewModel,
                onOpenPdfUri = { uri -> navController.navigate(BrowserRoutes.pdfViewer(uri)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = BrowserRoutes.SETTINGS,
            arguments = listOf(navArgument("openPermissions") { type = NavType.BoolType })
        ) { backStackEntry ->
            val openPermissions = backStackEntry.arguments?.getBoolean("openPermissions") ?: false
            SettingsScreen(
                viewModel = viewModel,
                isDefaultBrowser = isDefaultBrowser,
                onRequestDefaultBrowser = requestDefaultBrowser,
                openPermissionsDirectly = openPermissions,
                onOpenPrivacyCenter = { navController.navigate(BrowserRoutes.PRIVACY_CENTER) },
                onOpenDnsVpn = { navController.navigate(BrowserRoutes.DNS_VPN) },
                onOpenPasswordManager = { navController.navigate(BrowserRoutes.PASSWORDS) },
                onOpenExtensions = { navController.navigate(BrowserRoutes.EXTENSIONS) },
                onOpenDictionary = { navController.navigate(BrowserRoutes.DICTIONARY) },
                onOpenQrTools = { navController.navigate(BrowserRoutes.QR_TOOLS) },
                onOpenLegalRegistry = { navController.navigate(BrowserRoutes.ABOUT) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BrowserRoutes.PRIVACY_CENTER) {
            PrivacyCenterScreen(
                viewModel = viewModel,
                onOpenSitePermissions = { navController.navigate(BrowserRoutes.settings(true)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BrowserRoutes.DNS_VPN) {
            DnsVpnScreen(
                viewModel = viewModel,
                onRequestVpnStart = requestStartVpn,
                onStopVpn = { viewModel.setVpnShieldActive(context, false) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BrowserRoutes.PASSWORDS) {
            PasswordManagerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BrowserRoutes.EXTENSIONS) {
            ExtensionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BrowserRoutes.QR_TOOLS) {
            QrToolsScreen(
                viewModel = viewModel,
                onOpenScannedUrl = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BrowserRoutes.DICTIONARY) {
            OfflineDictionaryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = BrowserRoutes.PDF_VIEWER,
            arguments = listOf(navArgument("uri") { type = NavType.StringType; nullable = true; defaultValue = "" })
        ) { backStackEntry ->
            val rawUri = backStackEntry.arguments?.getString("uri")
            PdfViewerScreen(
                initialPdfUri = rawUri?.takeIf { it.isNotBlank() }?.let { Uri.decode(it) },
                appLanguage = settings.appLanguage,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BrowserRoutes.ABOUT) {
            AboutScreen(
                onOpenOfficialWebsite = { url -> viewModel.navigateCurrentTab(url) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
