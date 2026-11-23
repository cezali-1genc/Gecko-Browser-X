package com.example

import com.example.data.preferences.SearchEngine
import com.example.engine.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `resolveInputToUri adds https to domain names`() {
        assertEquals(
            "https://mozilla.org",
            UrlUtils.resolveInputToUri("mozilla.org", SearchEngine.DUCKDUCKGO)
        )
    }

    @Test
    fun `resolveInputToUri keeps localhost as http`() {
        assertEquals(
            "http://localhost:8080/test",
            UrlUtils.resolveInputToUri("localhost:8080/test", SearchEngine.DUCKDUCKGO)
        )
    }

    @Test
    fun `resolveInputToUri blocks javascript scheme and treats as search`() {
        val resolved = UrlUtils.resolveInputToUri("javascript:alert(1)", SearchEngine.DUCKDUCKGO)
        assertTrue(resolved.startsWith("https://duckduckgo.com/?q="))
        assertFalse(resolved.startsWith("javascript:"))
    }

    @Test
    fun `resolveInputToUri treats search phrases as search engine queries`() {
        val resolved = UrlUtils.resolveInputToUri("geckoview android kotlin", SearchEngine.BRAVE)
        assertEquals("https://search.brave.com/search?q=geckoview+android+kotlin", resolved)
    }

    @Test
    fun `isSafeExternalNavigationUri only allows http and https`() {
        assertTrue(UrlUtils.isSafeExternalNavigationUri("https://example.com"))
        assertTrue(UrlUtils.isSafeExternalNavigationUri("http://example.com"))
        assertFalse(UrlUtils.isSafeExternalNavigationUri("javascript:void(0)"))
        assertFalse(UrlUtils.isSafeExternalNavigationUri("file:///etc/passwd"))
        assertFalse(UrlUtils.isSafeExternalNavigationUri("content://media/external"))
    }

    @Test
    fun `sanitizeDownloadFilename removes path traversal characters`() {
        val cleaned = UrlUtils.sanitizeDownloadFilename(
            url = "https://example.com/files/../../evil.apk",
            contentDisposition = null,
            mimeType = "application/octet-stream"
        )
        assertFalse(cleaned.contains(".."))
        assertFalse(cleaned.contains("/"))
    }
}
