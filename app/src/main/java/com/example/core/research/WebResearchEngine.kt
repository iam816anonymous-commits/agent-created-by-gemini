package com.example.core.research

import com.example.core.model.ResearchScrapeResult
import com.example.core.model.SearchHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Authentic Web Research Engine.
 *
 * ZERO-FABRICATION POLICY:
 * 1. Executes real HTTP queries via OkHttpClient with strict timeouts.
 * 2. Parses genuine search hit titles, URLs, and snippets into [SearchHit].
 * 3. Never returns canned movie records or fabricated synthetic data.
 * 4. Caches past research queries locally and reports the accurate cache age.
 * 5. If the network is unreachable, returns an explicit failure result with [isFailure] = true.
 */
class WebResearchEngine {

    companion object {
        private const val CACHE_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours
        private val queryCache = ConcurrentHashMap<String, Pair<Long, List<SearchHit>>>()

        fun clearCache() {
            queryCache.clear()
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun conductResearch(query: String, extractType: String = "web_search"): ResearchScrapeResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext ResearchScrapeResult(
                query = query,
                extractedCount = 0,
                hits = emptyList(),
                rawSummary = "Empty query provided.",
                isOnline = false,
                isFailure = true,
                provenance = "INVALID_EMPTY_QUERY"
            )
        }

        val cacheKey = trimmed.lowercase()
        val cached = queryCache[cacheKey]
        val now = System.currentTimeMillis()
        if (cached != null) {
            val (cachedAtMs, cachedHits) = cached
            val ageMs = now - cachedAtMs
            if (ageMs < CACHE_TTL_MS) {
                val ageSec = ageMs / 1000L
                return@withContext ResearchScrapeResult(
                    query = trimmed,
                    extractedCount = cachedHits.size,
                    hits = cachedHits,
                    rawSummary = "Retrieved ${cachedHits.size} cached search result(s) (cache age: ${ageSec}s) for '$trimmed'.",
                    scrapedAtMs = cachedAtMs,
                    isOnline = false,
                    isFailure = false,
                    provenance = "LOCAL_CACHE_HIT",
                    cacheAgeSeconds = ageSec
                )
            } else {
                queryCache.remove(cacheKey)
            }
        }

        var scrapedHtml: String? = null
        var networkErrorMessage: String? = null

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/109.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                scrapedHtml = response.body?.string()
            } else {
                networkErrorMessage = "HTTP error code: ${response.code}"
            }
        } catch (e: Exception) {
            networkErrorMessage = e.message ?: "Network unreachable (Offline)"
        }

        // If network failed, report honest offline failure without fabricating data
        if (scrapedHtml.isNullOrBlank()) {
            return@withContext ResearchScrapeResult(
                query = trimmed,
                extractedCount = 0,
                hits = emptyList(),
                rawSummary = "[OFFLINE] Network unreachable or live search failed ($networkErrorMessage). No live results obtained.",
                isOnline = false,
                isFailure = true,
                provenance = "OFFLINE_UNREACHABLE_NO_FABRICATION"
            )
        }

        // Parse genuine search snippets from live HTTP HTML response
        val items = mutableListOf<SearchHit>()
        val resultPattern = Pattern.compile(
            "<div[^>]+class=\"[^\"]*result[^\"]*web-result[^\"]*\"[^>]*>(.*?)</div>\\s*</div>",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        val titlePattern = Pattern.compile("<a[^>]+class=\"result__a\"[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val snippetPattern = Pattern.compile("<a[^>]+class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val urlPattern = Pattern.compile("<a[^>]+class=\"result__url\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)

        val resultMatcher = resultPattern.matcher(scrapedHtml)

        while (resultMatcher.find() && items.size < 8) {
            val block = resultMatcher.group(1) ?: continue

            val tMatcher = titlePattern.matcher(block)
            val sMatcher = snippetPattern.matcher(block)
            val uMatcher = urlPattern.matcher(block)

            var rawTitle = ""
            var targetUrl = ""
            if (tMatcher.find()) {
                val rawHref = tMatcher.group(1) ?: ""
                rawTitle = tMatcher.group(2)?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
                targetUrl = if (rawHref.contains("uddg=")) {
                    try {
                        val encoded = rawHref.substringAfter("uddg=").substringBefore("&")
                        URLDecoder.decode(encoded, "UTF-8")
                    } catch (_: Exception) {
                        rawHref
                    }
                } else rawHref
            }

            var snippet = ""
            if (sMatcher.find()) {
                snippet = sMatcher.group(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
            }

            if (targetUrl.isBlank() && uMatcher.find()) {
                targetUrl = uMatcher.group(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
            }

            if (rawTitle.isNotBlank() || snippet.isNotBlank()) {
                items.add(
                    SearchHit(
                        title = if (rawTitle.isNotBlank()) rawTitle else "Search result for '$trimmed'",
                        url = targetUrl,
                        snippet = snippet
                    )
                )
            }
        }

        // Fallback snippet matcher if deep div regex yielded few results
        if (items.isEmpty()) {
            val fallbackSnippetMatcher = snippetPattern.matcher(scrapedHtml)
            var count = 0
            while (fallbackSnippetMatcher.find() && count < 8) {
                val s = fallbackSnippetMatcher.group(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
                if (s.isNotBlank()) {
                    count++
                    items.add(
                        SearchHit(
                            title = "Result #$count: $trimmed",
                            url = "",
                            snippet = s
                        )
                    )
                }
            }
        }

        if (items.isEmpty()) {
            return@withContext ResearchScrapeResult(
                query = trimmed,
                extractedCount = 0,
                hits = emptyList(),
                rawSummary = "Live search completed but 0 parsed snippets matched query '$trimmed'.",
                isOnline = true,
                isFailure = false,
                provenance = "LIVE_HTTP_EMPTY_MATCH"
            )
        }

        // Cache the valid live results
        queryCache[cacheKey] = Pair(now, items)

        return@withContext ResearchScrapeResult(
            query = trimmed,
            extractedCount = items.size,
            hits = items,
            rawSummary = "Retrieved ${items.size} live search result(s) for '$trimmed'.",
            isOnline = true,
            isFailure = false,
            provenance = "LIVE_HTTP_SCRAPE"
        )
    }
}
