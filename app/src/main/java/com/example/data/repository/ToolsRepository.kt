package com.example.data.repository

import android.content.Context
import com.example.engine.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WeatherSnapshot(
    val city: String,
    val temperatureC: Double,
    val windKmh: Double,
    val weatherCode: Int
)

data class DictionaryEntryItem(
    val id: Long = 0,
    val term: String,
    val translations: Map<String, String>,
    val isUserAdded: Boolean = false
)

data class ReaderArticle(
    val url: String,
    val title: String,
    val paragraphs: List<String>
)

class ToolsRepository(private val context: Context) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchWeather(city: String, lat: Double, lon: Double): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,wind_speed_10m"
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val body = response.body?.string() ?: return@runCatching null
                    val current = JSONObject(body).getJSONObject("current")
                    WeatherSnapshot(
                        city = city,
                        temperatureC = current.getDouble("temperature_2m"),
                        windKmh = current.optDouble("wind_speed_10m", 0.0),
                        weatherCode = current.optInt("weather_code", 0)
                    )
                }
            }.getOrNull()
        }

    suspend fun extractReaderArticle(url: String): ReaderArticle? = withContext(Dispatchers.IO) {
        if (!UrlUtils.isSafeExternalNavigationUri(url)) return@withContext null
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ModLUBrowserX/1.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val html = response.body?.string()?.take(600_000) ?: return@runCatching null
                val title = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .find(html)?.groupValues?.getOrNull(1)?.let(::stripHtml)
                    ?.ifBlank { UrlUtils.formatDisplayHost(url) }
                    ?: UrlUtils.formatDisplayHost(url)

                val cleanedHtml = html
                    .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                    .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                    .replace(Regex("<nav[^>]*>.*?</nav>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")

                val paragraphMatches = Regex("<(p|h1|h2|h3|li)[^>]*>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .findAll(cleanedHtml)
                    .map { stripHtml(it.groupValues[2]) }
                    .filter { it.length >= 25 }
                    .distinct()
                    .take(80)
                    .toList()

                val bodyParagraphs = if (paragraphMatches.isNotEmpty()) {
                    paragraphMatches
                } else {
                    stripHtml(cleanedHtml)
                        .split(Regex("\\s{2,}|\n"))
                        .map { it.trim() }
                        .filter { it.length >= 30 }
                        .take(40)
                }

                ReaderArticle(
                    url = url,
                    title = title,
                    paragraphs = bodyParagraphs
                )
            }
        }.getOrNull()
    }

    fun loadBundledDictionary(): List<DictionaryEntryItem> {
        return runCatching {
            val raw = context.assets.open("offline_dictionary.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val term = obj.optString("term")
                    val map = mutableMapOf<String, String>()
                    for (lang in listOf("tr", "en", "de", "fr", "ar", "ru", "es", "pt", "zh", "ja")) {
                        val v = obj.optString(lang)
                        if (v.isNotBlank()) map[lang] = v
                    }
                    if (term.isNotBlank()) {
                        add(DictionaryEntryItem(term = term, translations = map, isUserAdded = false))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadAdBlockHosts(): Set<String> {
        return runCatching {
            context.assets.open("adblock_hosts.txt").bufferedReader().use { reader ->
                reader.readLines()
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .toSet()
            }
        }.getOrDefault(emptySet())
    }

    private fun stripHtml(input: String): String {
        return input
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
