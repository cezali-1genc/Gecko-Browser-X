package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.engine.UrlUtils
import com.example.i18n.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.browserDataStore by preferencesDataStore(name = "browserx_settings")

enum class ThemeMode(val labelTr: String, val labelEn: String) {
    DARK("AMOLED Koyu Opera", "AMOLED Obsidian Dark"),
    LIGHT("Aydınlık Opera", "Daylight Light"),
    SYSTEM("Sistem Teması", "Follow System");

    fun display(lang: String): String = if (lang == "tr") labelTr else labelEn
}

enum class SearchEngine(
    val displayName: String,
    val searchUrlTemplate: String,
    val suggestUrlTemplate: String?
) {
    DUCKDUCKGO(
        displayName = "DuckDuckGo",
        searchUrlTemplate = "https://duckduckgo.com/?q={query}",
        suggestUrlTemplate = "https://duckduckgo.com/ac/?q={query}&type=list"
    ),
    GOOGLE(
        displayName = "Google",
        searchUrlTemplate = "https://www.google.com/search?q={query}",
        suggestUrlTemplate = "https://suggestqueries.google.com/complete/search?client=firefox&q={query}"
    ),
    BRAVE(
        displayName = "Brave Search",
        searchUrlTemplate = "https://search.brave.com/search?q={query}",
        suggestUrlTemplate = "https://search.brave.com/api/suggest?q={query}"
    ),
    STARTPAGE(
        displayName = "Startpage",
        searchUrlTemplate = "https://www.startpage.com/do/dsearch?query={query}",
        suggestUrlTemplate = "https://duckduckgo.com/ac/?q={query}&type=list"
    ),
    ECOSIA(
        displayName = "Ecosia",
        searchUrlTemplate = "https://www.ecosia.org/search?q={query}",
        suggestUrlTemplate = "https://ac.ecosia.org/autocomplete?q={query}&type=list"
    )
}

enum class TrackingProtectionLevel(
    val labelTr: String,
    val summaryTr: String,
    val labelEn: String,
    val summaryEn: String
) {
    STRICT(
        "Sıkı",
        "Bilinen izleyicileri, sosyal medya widget'larını, kripto madencileri ve parmak izi betiklerini engeller",
        "Strict",
        "Blocks known trackers, social widgets, cryptominers, and fingerprinters"
    ),
    STANDARD(
        "Standart",
        "İzleyici koruması ile web sitesi uyumluluğunu dengeler",
        "Standard",
        "Balances tracker protection and broad web compatibility"
    ),
    OFF(
        "Kapalı",
        "Üçüncü taraf izleyicilere izin verir (önerilmez)",
        "Disabled",
        "Allows third-party trackers (not recommended)"
    );

    fun label(lang: String): String = if (lang == "tr") labelTr else labelEn
    fun summary(lang: String): String = if (lang == "tr") summaryTr else summaryEn
}

enum class CookiePolicy(
    val labelTr: String,
    val summaryTr: String,
    val labelEn: String,
    val summaryEn: String
) {
    ISOLATE_THIRD_PARTY(
        "Tam Çerez Koruması",
        "Üçüncü taraf çerezleri site bazında izole eder (dFPI)",
        "Total Cookie Protection",
        "Isolates third-party cookies per site (dFPI)"
    ),
    BLOCK_TRACKERS(
        "İzleyici Çerezlerini Engelle",
        "Bilinen izleyici alan adlarının çerezlerini engeller",
        "Block Tracker Cookies",
        "Blocks cookies from known tracking domains"
    ),
    FIRST_PARTY_ONLY(
        "Yalnızca Birinci Taraf",
        "Tüm üçüncü taraf çerezlerini reddeder",
        "First-Party Only",
        "Rejects all third-party cookies"
    ),
    ACCEPT_ALL(
        "Tüm Çerezlere İzin Ver",
        "Birinci ve üçüncü taraf tüm çerezleri kabul eder",
        "Allow All Cookies",
        "Accepts first-party and third-party cookies"
    );

    fun label(lang: String): String = if (lang == "tr") labelTr else labelEn
    fun summary(lang: String): String = if (lang == "tr") summaryTr else summaryEn
}

enum class HomepageMode(val labelTr: String, val labelEn: String) {
    START_PAGE("Akıllı Opera Ana Ekran", "Smart Start Page"),
    CUSTOM_URL("Özel Ana Sayfa Adresi", "Custom Homepage URL");

    fun label(lang: String): String = if (lang == "tr") labelTr else labelEn
}

enum class DnsProvider(
    val displayName: String,
    val primaryIp: String,
    val secondaryIp: String,
    val dohUrl: String
) {
    SYSTEM("Sistem Varsayılan DNS", "", "", ""),
    CLOUDFLARE("Cloudflare 1.1.1.1", "1.1.1.1", "1.0.0.1", "https://cloudflare-dns.com/dns-query"),
    QUAD9("Quad9 Güvenlik (9.9.9.9)", "9.9.9.9", "149.112.112.112", "https://dns.quad9.net/dns-query"),
    ADGUARD("AdGuard Reklam & İzleyici DNS", "94.140.14.14", "94.140.15.15", "https://dns.adguard-dns.com/dns-query"),
    GOOGLE("Google Public DNS", "8.8.8.8", "8.8.4.4", "https://dns.google/dns-query"),
    CUSTOM("Özel DNS / DoH", "1.1.1.1", "1.0.0.1", "https://cloudflare-dns.com/dns-query")
}

enum class HomeWallpaperPreset(val id: String, val labelTr: String, val labelEn: String) {
    OPERA_CRIMSON("crimson", "Opera Kızıl Neon", "Opera Crimson Glow"),
    OBSIDIAN_CYAN("cyan", "Obsidyen Camgöbeği", "Obsidian Cyan"),
    MIDNIGHT_AMOLED("amoled", "Saf AMOLED Siyah", "Pure AMOLED"),
    CUSTOM_IMAGE("custom", "Özel Fotoğraf", "Custom Photo")
}

enum class ToolbarStyle(val labelTr: String, val labelEn: String) {
    OPERA_BOTTOM("Opera Alt Gezinme Çubuğu", "Opera Bottom Navigation"),
    COMPACT_TOP("Kompakt Üst Araç Çubuğu", "Compact Top Bar")
}

data class BrowserSettingsState(
    val appLanguage: String = LocaleHelper.DEFAULT_TAG,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val javaScriptEnabled: Boolean = true,
    val trackingProtection: TrackingProtectionLevel = TrackingProtectionLevel.STRICT,
    val cookiePolicy: CookiePolicy = CookiePolicy.ISOLATE_THIRD_PARTY,
    val searchSuggestionsEnabled: Boolean = true,
    val saveHistoryEnabled: Boolean = true,
    val popupBlockingEnabled: Boolean = true,
    val httpsOnlyEnabled: Boolean = false,
    val adFilterRulesEnabled: Boolean = true,
    val passwordSavePromptEnabled: Boolean = true,
    val homepageMode: HomepageMode = HomepageMode.START_PAGE,
    val customHomepageUrl: String = "https://duckduckgo.com",
    val showSpeedDialsOnHome: Boolean = true,
    val showRecentSitesOnHome: Boolean = true,
    val showWeatherOnHome: Boolean = false,
    val weatherCity: String = "Istanbul",
    val weatherLatitude: Double = 41.0082,
    val weatherLongitude: Double = 28.9784,
    val homeWallpaperPreset: HomeWallpaperPreset = HomeWallpaperPreset.OPERA_CRIMSON,
    val homeWallpaperUri: String? = null,
    val toolbarStyle: ToolbarStyle = ToolbarStyle.OPERA_BOTTOM,
    val edgeGesturesEnabled: Boolean = true,
    val pullToRefreshEnabled: Boolean = true,
    val pageZoomPercent: Int = 100,
    val dnsProvider: DnsProvider = DnsProvider.CLOUDFLARE,
    val customDnsPrimary: String = "1.1.1.1",
    val customDnsSecondary: String = "1.0.0.1",
    val customDohUri: String = "https://cloudflare-dns.com/dns-query",
    val vpnShieldEnabled: Boolean = false,
    val totalTrackersBlocked: Long = 0L
)

class SettingsRepository(private val context: Context) {
    private val secureVault = SecureVault(context)

    private object Keys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val JS_ENABLED = booleanPreferencesKey("js_enabled")
        val TRACKING_LEVEL = stringPreferencesKey("tracking_level")
        val COOKIE_POLICY = stringPreferencesKey("cookie_policy")
        val SEARCH_SUGGESTIONS = booleanPreferencesKey("search_suggestions")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val POPUP_BLOCKING = booleanPreferencesKey("popup_blocking")
        val HTTPS_ONLY = booleanPreferencesKey("https_only")
        val AD_FILTER_RULES = booleanPreferencesKey("ad_filter_rules")
        val PW_SAVE_PROMPT = booleanPreferencesKey("pw_save_prompt")
        val HOMEPAGE_MODE = stringPreferencesKey("homepage_mode")
        val CUSTOM_HOMEPAGE_URL = stringPreferencesKey("custom_homepage_url")
        val SHOW_SPEED_DIALS = booleanPreferencesKey("show_speed_dials")
        val SHOW_RECENT_SITES = booleanPreferencesKey("show_recent_sites")
        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val WEATHER_LAT = stringPreferencesKey("weather_lat")
        val WEATHER_LON = stringPreferencesKey("weather_lon")
        val WALLPAPER_PRESET = stringPreferencesKey("wallpaper_preset")
        val WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        val TOOLBAR_STYLE = stringPreferencesKey("toolbar_style")
        val EDGE_GESTURES = booleanPreferencesKey("edge_gestures")
        val PULL_TO_REFRESH = booleanPreferencesKey("pull_to_refresh")
        val PAGE_ZOOM_PERCENT = intPreferencesKey("page_zoom_percent")
        val DNS_PROVIDER = stringPreferencesKey("dns_provider")
        val CUSTOM_DNS_PRIMARY = stringPreferencesKey("custom_dns_primary")
        val CUSTOM_DNS_SECONDARY = stringPreferencesKey("custom_dns_secondary")
        val CUSTOM_DOH_URI = stringPreferencesKey("custom_doh_uri")
        val VPN_SHIELD_ENABLED = booleanPreferencesKey("vpn_shield_enabled")
        val TRACKERS_BLOCKED = longPreferencesKey("total_trackers_blocked")
    }

    val settingsFlow: Flow<BrowserSettingsState> = context.browserDataStore.data.map { prefs ->
        val storedHomepage = secureVault.getEncryptedString("custom_homepage_url")
            ?: prefs[Keys.CUSTOM_HOMEPAGE_URL]
            ?: "https://duckduckgo.com"

        BrowserSettingsState(
            appLanguage = prefs[Keys.APP_LANGUAGE] ?: LocaleHelper.DEFAULT_TAG,
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.DARK,
            searchEngine = prefs[Keys.SEARCH_ENGINE]?.let {
                runCatching { SearchEngine.valueOf(it) }.getOrNull()
            } ?: SearchEngine.DUCKDUCKGO,
            javaScriptEnabled = prefs[Keys.JS_ENABLED] ?: true,
            trackingProtection = prefs[Keys.TRACKING_LEVEL]?.let {
                runCatching { TrackingProtectionLevel.valueOf(it) }.getOrNull()
            } ?: TrackingProtectionLevel.STRICT,
            cookiePolicy = prefs[Keys.COOKIE_POLICY]?.let {
                runCatching { CookiePolicy.valueOf(it) }.getOrNull()
            } ?: CookiePolicy.ISOLATE_THIRD_PARTY,
            searchSuggestionsEnabled = prefs[Keys.SEARCH_SUGGESTIONS] ?: true,
            saveHistoryEnabled = prefs[Keys.SAVE_HISTORY] ?: true,
            popupBlockingEnabled = prefs[Keys.POPUP_BLOCKING] ?: true,
            httpsOnlyEnabled = prefs[Keys.HTTPS_ONLY] ?: false,
            adFilterRulesEnabled = prefs[Keys.AD_FILTER_RULES] ?: true,
            passwordSavePromptEnabled = prefs[Keys.PW_SAVE_PROMPT] ?: true,
            homepageMode = prefs[Keys.HOMEPAGE_MODE]?.let {
                runCatching { HomepageMode.valueOf(it) }.getOrNull()
            } ?: HomepageMode.START_PAGE,
            customHomepageUrl = storedHomepage,
            showSpeedDialsOnHome = prefs[Keys.SHOW_SPEED_DIALS] ?: true,
            showRecentSitesOnHome = prefs[Keys.SHOW_RECENT_SITES] ?: true,
            showWeatherOnHome = prefs[Keys.SHOW_WEATHER] ?: false,
            weatherCity = prefs[Keys.WEATHER_CITY] ?: "Istanbul",
            weatherLatitude = prefs[Keys.WEATHER_LAT]?.toDoubleOrNull() ?: 41.0082,
            weatherLongitude = prefs[Keys.WEATHER_LON]?.toDoubleOrNull() ?: 28.9784,
            homeWallpaperPreset = prefs[Keys.WALLPAPER_PRESET]?.let {
                runCatching { HomeWallpaperPreset.valueOf(it) }.getOrNull()
            } ?: HomeWallpaperPreset.OPERA_CRIMSON,
            homeWallpaperUri = prefs[Keys.WALLPAPER_URI],
            toolbarStyle = prefs[Keys.TOOLBAR_STYLE]?.let {
                runCatching { ToolbarStyle.valueOf(it) }.getOrNull()
            } ?: ToolbarStyle.OPERA_BOTTOM,
            edgeGesturesEnabled = prefs[Keys.EDGE_GESTURES] ?: true,
            pullToRefreshEnabled = prefs[Keys.PULL_TO_REFRESH] ?: true,
            pageZoomPercent = (prefs[Keys.PAGE_ZOOM_PERCENT] ?: 100).coerceIn(50, 200),
            dnsProvider = prefs[Keys.DNS_PROVIDER]?.let {
                runCatching { DnsProvider.valueOf(it) }.getOrNull()
            } ?: DnsProvider.CLOUDFLARE,
            customDnsPrimary = prefs[Keys.CUSTOM_DNS_PRIMARY] ?: "1.1.1.1",
            customDnsSecondary = prefs[Keys.CUSTOM_DNS_SECONDARY] ?: "1.0.0.1",
            customDohUri = prefs[Keys.CUSTOM_DOH_URI] ?: "https://cloudflare-dns.com/dns-query",
            vpnShieldEnabled = prefs[Keys.VPN_SHIELD_ENABLED] ?: false,
            totalTrackersBlocked = prefs[Keys.TRACKERS_BLOCKED] ?: 0L
        )
    }

    suspend fun setAppLanguage(tag: String) {
        context.browserDataStore.edit { it[Keys.APP_LANGUAGE] = tag }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.browserDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.browserDataStore.edit { it[Keys.SEARCH_ENGINE] = engine.name }
    }

    suspend fun setJavaScriptEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.JS_ENABLED] = enabled }
    }

    suspend fun setTrackingProtection(level: TrackingProtectionLevel) {
        context.browserDataStore.edit { it[Keys.TRACKING_LEVEL] = level.name }
    }

    suspend fun setCookiePolicy(policy: CookiePolicy) {
        context.browserDataStore.edit { it[Keys.COOKIE_POLICY] = policy.name }
    }

    suspend fun setSearchSuggestionsEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.SEARCH_SUGGESTIONS] = enabled }
    }

    suspend fun setSaveHistoryEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.SAVE_HISTORY] = enabled }
    }

    suspend fun setPopupBlockingEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.POPUP_BLOCKING] = enabled }
    }

    suspend fun setHttpsOnlyEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.HTTPS_ONLY] = enabled }
    }

    suspend fun setAdFilterRulesEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.AD_FILTER_RULES] = enabled }
    }

    suspend fun setPasswordSavePromptEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.PW_SAVE_PROMPT] = enabled }
    }

    suspend fun setHomepageConfiguration(
        mode: HomepageMode,
        customUrl: String,
        showSpeedDials: Boolean,
        showRecentSites: Boolean = true,
        showWeather: Boolean = false
    ) {
        val sanitizedUrl = if (customUrl.isBlank()) {
            "https://duckduckgo.com"
        } else {
            val resolved = UrlUtils.resolveInputToUri(customUrl, SearchEngine.DUCKDUCKGO)
            if (UrlUtils.isSafeExternalNavigationUri(resolved)) resolved else "https://duckduckgo.com"
        }
        secureVault.putEncryptedString("custom_homepage_url", sanitizedUrl)
        context.browserDataStore.edit {
            it[Keys.HOMEPAGE_MODE] = mode.name
            it[Keys.CUSTOM_HOMEPAGE_URL] = sanitizedUrl
            it[Keys.SHOW_SPEED_DIALS] = showSpeedDials
            it[Keys.SHOW_RECENT_SITES] = showRecentSites
            it[Keys.SHOW_WEATHER] = showWeather
        }
    }

    suspend fun setWeatherCity(city: String, lat: Double, lon: Double, enabled: Boolean) {
        context.browserDataStore.edit {
            it[Keys.WEATHER_CITY] = city
            it[Keys.WEATHER_LAT] = lat.toString()
            it[Keys.WEATHER_LON] = lon.toString()
            it[Keys.SHOW_WEATHER] = enabled
        }
    }

    suspend fun setHomeWallpaper(preset: HomeWallpaperPreset, customUri: String?) {
        context.browserDataStore.edit {
            it[Keys.WALLPAPER_PRESET] = preset.name
            if (customUri != null) it[Keys.WALLPAPER_URI] = customUri
            else if (preset != HomeWallpaperPreset.CUSTOM_IMAGE) it.remove(Keys.WALLPAPER_URI)
        }
    }

    suspend fun setToolbarStyle(style: ToolbarStyle) {
        context.browserDataStore.edit { it[Keys.TOOLBAR_STYLE] = style.name }
    }

    suspend fun setGestures(edgeGestures: Boolean, pullToRefresh: Boolean) {
        context.browserDataStore.edit {
            it[Keys.EDGE_GESTURES] = edgeGestures
            it[Keys.PULL_TO_REFRESH] = pullToRefresh
        }
    }

    suspend fun setPageZoomPercent(percent: Int) {
        context.browserDataStore.edit { it[Keys.PAGE_ZOOM_PERCENT] = percent.coerceIn(50, 200) }
    }

    suspend fun setDnsConfiguration(
        provider: DnsProvider,
        customPrimary: String,
        customSecondary: String,
        customDohUri: String
    ) {
        context.browserDataStore.edit {
            it[Keys.DNS_PROVIDER] = provider.name
            it[Keys.CUSTOM_DNS_PRIMARY] = customPrimary.trim()
            it[Keys.CUSTOM_DNS_SECONDARY] = customSecondary.trim()
            it[Keys.CUSTOM_DOH_URI] = customDohUri.trim()
        }
    }

    suspend fun setVpnShieldEnabled(enabled: Boolean) {
        context.browserDataStore.edit { it[Keys.VPN_SHIELD_ENABLED] = enabled }
    }

    suspend fun incrementTrackersBlocked(delta: Long = 1L) {
        context.browserDataStore.edit { prefs ->
            val current = prefs[Keys.TRACKERS_BLOCKED] ?: 0L
            prefs[Keys.TRACKERS_BLOCKED] = current + delta
        }
    }
}
