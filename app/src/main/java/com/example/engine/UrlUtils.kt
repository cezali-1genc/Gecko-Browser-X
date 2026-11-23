package com.example.engine

import com.example.data.preferences.SearchEngine
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object UrlUtils {
    const val INTERNAL_HOME_URI = "about:browserx"

    private val BLOCKED_SCHEMES = setOf("javascript", "vbscript", "data", "file", "content")
    private val DOMAIN_REGEX = Regex(
        "^(localhost|([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}|(\\d{1,3}\\.){3}\\d{1,3})(:\\d{1,5})?(/.*)?$"
    )

    fun isHomeUri(uri: String?): Boolean {
        return uri.isNullOrBlank() ||
            uri.equals(INTERNAL_HOME_URI, ignoreCase = true) ||
            uri.equals("about:blank", ignoreCase = true)
    }

    /**
     * Resolves omnibox user input into a safe URL or search engine query URL.
     * Rejects unsafe scripts (`javascript:`, `data:`) from direct address bar execution.
     */
    fun resolveInputToUri(rawInput: String, searchEngine: SearchEngine): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return INTERNAL_HOME_URI
        if (trimmed.equals(INTERNAL_HOME_URI, ignoreCase = true)) return INTERNAL_HOME_URI

        val lower = trimmed.lowercase(Locale.ROOT)
        val colonIdx = lower.indexOf(':')
        if (colonIdx > 0) {
            val scheme = lower.substring(0, colonIdx)
            if (scheme in BLOCKED_SCHEMES) {
                return buildSearchUrl(trimmed, searchEngine)
            }
            if (scheme == "http" || scheme == "https" || scheme == "about") {
                return trimmed
            }
        }

        if (!trimmed.contains(' ') && DOMAIN_REGEX.matches(trimmed)) {
            val isLocalhostOrIp = lower.startsWith("localhost") ||
                lower.startsWith("127.") ||
                lower.startsWith("192.168.") ||
                lower.startsWith("10.")
            val prefix = if (isLocalhostOrIp) "http://" else "https://"
            return prefix + trimmed
        }

        return buildSearchUrl(trimmed, searchEngine)
    }

    fun buildSearchUrl(query: String, searchEngine: SearchEngine): String {
        val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
        return searchEngine.searchUrlTemplate.replace("{query}", encoded)
    }

    fun isSafeExternalNavigationUri(uri: String): Boolean {
        val parsed = runCatching { URI(uri.trim()) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase(Locale.ROOT) ?: return false
        val host = parsed.host
        return (scheme == "http" || scheme == "https") && !host.isNullOrBlank()
    }

    fun extractHost(uri: String?): String {
        if (isHomeUri(uri)) return "Start Page"
        val parsed = runCatching { URI(uri?.trim().orEmpty()) }.getOrNull() ?: return uri.orEmpty()
        return parsed.host?.takeIf { it.isNotBlank() } ?: uri.orEmpty()
    }

    fun extractOrigin(uri: String?): String {
        if (uri.isNullOrBlank()) return ""
        val parsed = runCatching { URI(uri.trim()) }.getOrNull() ?: return ""
        val scheme = parsed.scheme ?: return ""
        val host = parsed.host ?: return ""
        val port = parsed.port
        return if (port != -1) "$scheme://$host:$port" else "$scheme://$host"
    }

    fun formatDisplayHost(uri: String?): String {
        if (isHomeUri(uri)) return "Browser X Start"
        val host = extractHost(uri)
        return host.removePrefix("www.")
    }

    fun sanitizeDownloadFilename(
        url: String,
        contentDisposition: String?,
        mimeType: String?
    ): String {
        val fromDisposition = contentDisposition?.let { disp ->
            Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)
                .find(disp)?.groupValues?.getOrNull(1)
                ?.let { runCatching { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }.getOrNull() ?: it }
        }

        val fromUrl = runCatching {
            val path = URI(url).path ?: ""
            path.substringAfterLast('/').takeIf { it.isNotBlank() }
        }.getOrNull()

        val candidate = fromDisposition?.takeIf { it.isNotBlank() }
            ?: fromUrl
            ?: "download_${System.currentTimeMillis()}"

        val cleaned = candidate
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace("..", "_")
            .trim()

        val hasExtension = cleaned.contains('.')
        return if (!hasExtension && !mimeType.isNullOrBlank()) {
            val ext = when {
                mimeType.contains("pdf", ignoreCase = true) -> ".pdf"
                mimeType.contains("png", ignoreCase = true) -> ".png"
                mimeType.contains("jpeg", ignoreCase = true) -> ".jpg"
                mimeType.contains("zip", ignoreCase = true) -> ".zip"
                mimeType.contains("android.package-archive", ignoreCase = true) -> ".apk"
                else -> ""
            }
            (cleaned.ifBlank { "download_${System.currentTimeMillis()}" }) + ext
        } else {
            cleaned.ifBlank { "download_${System.currentTimeMillis()}" }
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "Unknown size"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
