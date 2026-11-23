package com.example.i18n

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.compose.ui.unit.LayoutDirection
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

data class SupportedLanguage(
    val tag: String,
    val nativeName: String
)

object LocaleHelper {
    const val SYSTEM_TAG = "system"
    const val DEFAULT_TAG = "tr"

    val supportedLanguages = listOf(
        SupportedLanguage("tr", "Türkçe (Varsayılan)"),
        SupportedLanguage("en", "English"),
        SupportedLanguage("de", "Deutsch"),
        SupportedLanguage("fr", "Français"),
        SupportedLanguage("ar", "العربية"),
        SupportedLanguage("ru", "Русский"),
        SupportedLanguage("es", "Español"),
        SupportedLanguage("pt", "Português"),
        SupportedLanguage("zh", "中文"),
        SupportedLanguage("ja", "日本語")
    )

    private val supportedTags = supportedLanguages.map { it.tag }.toSet()

    fun resolveLocale(languageTag: String): Locale {
        val tag = if (languageTag == SYSTEM_TAG) {
            val sys = Locale.getDefault().language.lowercase(Locale.ROOT)
            if (sys in supportedTags) sys else DEFAULT_TAG
        } else {
            if (languageTag in supportedTags) languageTag else DEFAULT_TAG
        }
        return Locale.forLanguageTag(tag)
    }

    fun localizedContext(baseContext: Context, languageTag: String): Context {
        val locale = resolveLocale(languageTag)
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return baseContext.createConfigurationContext(config)
    }

    fun layoutDirectionFor(locale: Locale): LayoutDirection {
        val dir = android.text.TextUtils.getLayoutDirectionFromLocale(locale)
        return if (dir == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    fun languageDisplayName(languageTag: String): String {
        if (languageTag == SYSTEM_TAG) return "Sistem Dili (${resolveLocale(SYSTEM_TAG).displayLanguage})"
        return supportedLanguages.firstOrNull { it.tag == languageTag }?.nativeName ?: "Türkçe"
    }

    fun formatDateTime(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
            .format(Date(timestamp))
    }

    fun formatNumber(value: Long, locale: Locale = Locale.getDefault()): String {
        return NumberFormat.getNumberInstance(locale).format(value)
    }

    fun formatPercent(fraction: Float, locale: Locale = Locale.getDefault()): String {
        return NumberFormat.getPercentInstance(locale).format(fraction.coerceIn(0f, 1f))
    }
}
