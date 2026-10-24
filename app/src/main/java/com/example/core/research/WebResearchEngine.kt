package com.example.core.research

import com.example.core.model.ResearchScrapeResult
import com.example.core.model.ScrapedMovieItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Authentic Web Research Engine.
 *
 * ZERO-FABRICATION POLICY:
 * 1. Executes real HTTP queries via OkHttpClient with strict timeouts.
 * 2. If the device is offline or the network is unreachable, returns an explicit failure
 *    result ([isOnline] = false, [isFailure] = true) stating the network is unreachable.
 * 3. Never returns canned movie records labeled as "scraped" or "Verified Online".
 * 4. Never fabricates a synthetic result when a query fails.
 */
class WebResearchEngine {

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
                movies = emptyList(),
                rawSummary = "Empty query provided.",
                isOnline = false,
                isFailure = true,
                provenance = "INVALID_EMPTY_QUERY"
            )
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
                movies = emptyList(),
                rawSummary = "[OFFLINE] Network unreachable or live search failed ($networkErrorMessage). No live results obtained.",
                isOnline = false,
                isFailure = true,
                provenance = "OFFLINE_UNREACHABLE_NO_FABRICATION"
            )
        }

        // Parse genuine search snippets from live HTTP HTML response
        val items = mutableListOf<ScrapedMovieItem>()
        val snippetPattern = Pattern.compile("<a[^>]+class=\"result__snippet\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val titlePattern = Pattern.compile("<a[^>]+class=\"result__url\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)

        val snippetMatcher = snippetPattern.matcher(scrapedHtml)
        var count = 0

        while (snippetMatcher.find() && count < 8) {
            val rawSnippet = snippetMatcher.group(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
            if (rawSnippet.isNotBlank()) {
                count++
                items.add(
                    ScrapedMovieItem(
                        title = "Search Result #$count: '$trimmed'",
                        year = "Current",
                        genre = "Live Web Result",
                        directorOrCast = rawSnippet.take(120),
                        status = "Live HTTP Fetch",
                        ratingOrNote = rawSnippet
                    )
                )
            }
        }

        if (items.isEmpty()) {
            return@withContext ResearchScrapeResult(
                query = trimmed,
                extractedCount = 0,
                movies = emptyList(),
                rawSummary = "Live search completed but 0 parsed snippets matched query '$trimmed'.",
                isOnline = true,
                isFailure = false,
                provenance = "LIVE_HTTP_EMPTY_MATCH"
            )
        }

        return@withContext ResearchScrapeResult(
            query = trimmed,
            extractedCount = items.size,
            movies = items,
            rawSummary = "Retrieved ${items.size} live search snippets for '$trimmed'.",
            isOnline = true,
            isFailure = false,
            provenance = "LIVE_HTTP_SCRAPE"
        )
    }
}
