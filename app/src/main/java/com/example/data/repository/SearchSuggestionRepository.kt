package com.example.data.repository

import com.example.data.preferences.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

enum class SuggestionKind {
    SEARCH_WEB,
    BOOKMARK,
    HISTORY
}

data class OmniboxSuggestion(
    val title: String,
    val urlOrQuery: String,
    val kind: SuggestionKind
)

class SearchSuggestionRepository(
    private val dataRepository: BrowserDataRepository
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSuggestions(
        query: String,
        searchEngine: SearchEngine,
        remoteSuggestionsAllowed: Boolean,
        isPrivateTab: Boolean
    ): List<OmniboxSuggestion> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val results = mutableListOf<OmniboxSuggestion>()

        // Local bookmarks first
        val bookmarks = dataRepository.searchBookmarks(trimmed)
        bookmarks.take(3).forEach { bm ->
            results.add(
                OmniboxSuggestion(
                    title = bm.title,
                    urlOrQuery = bm.url,
                    kind = SuggestionKind.BOOKMARK
                )
            )
        }

        // Non-private local history matches (skip in private tab to keep suggestions clean)
        if (!isPrivateTab) {
            val history = dataRepository.searchHistory(trimmed)
            history
                .distinctBy { it.url }
                .filter { h -> results.none { it.urlOrQuery == h.url } }
                .take(3)
                .forEach { entry ->
                    results.add(
                        OmniboxSuggestion(
                            title = entry.title,
                            urlOrQuery = entry.url,
                            kind = SuggestionKind.HISTORY
                        )
                    )
                }
        }

        // Never transmit private session keystrokes to remote autocomplete servers
        if (remoteSuggestionsAllowed && !isPrivateTab && !trimmed.startsWith("http")) {
            val remote = fetchRemoteSuggestions(trimmed, searchEngine)
            remote.take(5).forEach { phrase ->
                if (results.none { it.title.equals(phrase, ignoreCase = true) }) {
                    results.add(
                        OmniboxSuggestion(
                            title = phrase,
                            urlOrQuery = phrase,
                            kind = SuggestionKind.SEARCH_WEB
                        )
                    )
                }
            }
        }

        results
    }

    private fun fetchRemoteSuggestions(query: String, engine: SearchEngine): List<String> {
        val template = engine.suggestUrlTemplate ?: return emptyList()
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        val requestUrl = template.replace("{query}", encoded)

        val request = Request.Builder()
            .url(requestUrl)
            .header("User-Agent", "ModLUBrowserX/1.0")
            .get()
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                parseOpenSearchJson(body)
            }
        }.getOrDefault(emptyList())
    }

    internal fun parseOpenSearchJson(jsonBody: String): List<String> {
        val parsed = mutableListOf<String>()
        val root = JSONArray(jsonBody)
        if (root.length() >= 2) {
            val suggestionsArray = root.optJSONArray(1)
            if (suggestionsArray != null) {
                for (i in 0 until suggestionsArray.length()) {
                    val value = suggestionsArray.optString(i)
                    if (value.isNotBlank()) parsed.add(value)
                }
            }
        }
        return parsed
    }
}
