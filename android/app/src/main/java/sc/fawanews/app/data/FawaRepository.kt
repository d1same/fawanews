package sc.fawanews.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class FawaRepository(
    private val baseUrl: String = BASE_URL,
    private val client: OkHttpClient = defaultClient(),
) {
    suspend fun fetchSchedule(): List<ScheduleItem> = withContext(Dispatchers.IO) {
        val html = getHtml(baseUrl)
        parseSchedule(html)
    }

    suspend fun fetchStreams(pagePath: String): StreamDetails = withContext(Dispatchers.IO) {
        val absolute = resolveUrl(baseUrl, pagePath)
        val html = getHtml(absolute)
        val title = pageTitle(html)
        val urls = validateStreams(parseVideoUrls(html))
        StreamDetails(
            title = title.ifBlank { "Clutch" },
            streamUrls = urls,
            pageUrl = absolute,
        )
    }

    suspend fun fetchArticleDetails(pagePath: String): ArticleDetails = withContext(Dispatchers.IO) {
        val absolute = resolveUrl(baseUrl, pagePath)
        val html = getHtml(absolute)
        val streams = validateStreams(parseVideoUrls(html))
        parseArticle(html, absolute, streams)
    }

    fun validateStreams(urls: List<String>): List<String> {
        val checks = urls.distinct().associateWith { url -> checkStream(url, client) }
        val working = checks.filterValues { it == LinkCheck.WORKING }.keys.toList()
        // A link the server refuses (404, "channel stopped") is not live yet or is over.
        // Only links we could not judge, such as a timeout, are worth handing to the player.
        return working.ifEmpty { checks.filterValues { it == LinkCheck.UNKNOWN }.keys.toList() }
    }

    private fun getHtml(url: String): String {
        val bustUrl = if (url.contains("?")) "$url&_=${System.currentTimeMillis()}" else "$url?_=${System.currentTimeMillis()}"
        val request = Request.Builder()
            .url(bustUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code} for $url")
            }
            return response.body?.string().orEmpty()
        }
    }

    companion object {
        const val BASE_URL = "http://www.fawanews.sc/"

        // The site keeps renaming the link list (`videos`, `videos_1`, `videosv`, `looo`),
        // so follow whichever array the player reads: `source: looo[...]`.
        private val arrayVarRegex = Regex("""var\s+(\w+)\s*=\s*(\[[^\]]*])""")
        private val playerSourceRegex = Regex("""source\s*:\s*(\w+)\s*\[""")
        private val mediaUrlRegex = Regex("""\.(m3u8|mpd|mp4)(\?|$)""", RegexOption.IGNORE_CASE)
        private val pushRegex = Regex("""(\w+)\.push\(([^)]*)\)""")
        private val indexedRegex = Regex("""(\w+)\[\s*\w+\s*]""")

        // Links can also be built from pieces: `var g1 = "http://..."; ... [g1 + g2 + g3]`.
        private val stringVarRegex = Regex("""var\s+(\w+)\s*=\s*(["'])(.*?)\2""")
        private val quotedRegex = Regex("""(["'])(.*)\1""")

        private val playlistUrlRegex =
            Regex("""https?://[^"'\s<>]+\.m3u8[^"'\s<>]*""", RegexOption.IGNORE_CASE)

        // One client app-wide: each OkHttpClient owns its own connection pool and threads.
        private val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }

        fun defaultClient(): OkHttpClient = sharedClient

        fun parseSchedule(html: String): List<ScheduleItem> {
            val doc = Jsoup.parse(html)
            val seen = linkedSetOf<String>()
            return doc.select("div.user-item").mapNotNull { item ->
                val link = item.select("a[href]").firstNotNullOfOrNull { anchor ->
                    anchor.attr("href").takeIf { it.endsWith(".html", ignoreCase = true) }
                } ?: return@mapNotNull null

                if (!seen.add(link)) return@mapNotNull null

                val title = item.select(".user-item__name").text().trim()
                if (title.isBlank()) return@mapNotNull null

                val playing = item.select(".user-item__playing").text().trim()
                val imageRaw = item.select("img").first()?.attr("src")?.takeIf { it.isNotBlank() }
                val image = imageRaw?.let { resolveUrl(BASE_URL, it) }
                val isLive = playing.isNotBlank()

                ScheduleItem(
                    id = link,
                    title = title,
                    subtitle = playing.ifBlank { null },
                    imageUrl = image,
                    pagePath = link,
                    isLive = isLive,
                )
            }
        }

        private val articleNoise = listOf(
            "please note",
            "press to refresh",
            "share this game",
            "bookmark the site",
            "if lags , refresh",
            "twitter-share",
            "back to home page",
            "back to homepage",
            "sports news",
        )

        private fun isArticleNoise(text: String, title: String): Boolean {
            val trimmed = text.trim()
            if (trimmed.length < 3) return true
            if (trimmed.equals(title.trim(), ignoreCase = true)) return true
            val lower = trimmed.lowercase()
            if (articleNoise.any { lower == it || lower.contains(it) }) return true
            if (lower.startsWith("back to home")) return true
            return false
        }

        fun pageTitle(html: String): String {
            val doc = Jsoup.parse(html)
            return doc.select("ul.uk-breadcrumb li span").lastOrNull()?.text()?.trim()
                ?: doc.title().substringAfter(" - ").substringBefore(" - ").trim()
        }

        fun parseArticle(
            html: String,
            pageUrl: String,
            streamUrls: List<String> = emptyList(),
        ): ArticleDetails {
            val doc = Jsoup.parse(html)
            val title = pageTitle(html).ifBlank { "Clutch" }
            val main = doc.select(".uk-width-2-3\\@l").firstOrNull()
                ?: doc.select("main.page-main").firstOrNull()
                ?: doc.body()

            main.select(
                "script, style, button, #player, .creat-list-btn, .twitter-share-button, " +
                    "ul.uk-breadcrumb, nav, .breadcrumb, .uk-breadcrumb",
            ).remove()

            val blocks = linkedSetOf<String>()
            main.select("p, li, h2, h3, h4, blockquote").forEach { element ->
                val text = element.text().trim()
                if (isArticleNoise(text, title)) return@forEach
                blocks.add(text)
            }

            if (blocks.isEmpty()) {
                main.text()
                    .lines()
                    .map { it.trim() }
                    .filter { it.length > 40 }
                    .filter { line -> !isArticleNoise(line, title) }
                    .take(12)
                    .forEach { blocks.add(it) }
            }

            val hero = main.select("img").firstOrNull { img ->
                val src = img.attr("src")
                src.isNotBlank() &&
                    !src.contains("profile_images", ignoreCase = true) &&
                    !src.contains("favicon", ignoreCase = true)
            }?.attr("src")?.let { resolveUrl(BASE_URL, it) }

            return ArticleDetails(
                title = title,
                paragraphs = blocks.toList(),
                imageUrl = hero,
                pageUrl = pageUrl,
                streamUrls = streamUrls,
            )
        }

        fun parseVideoUrls(html: String): List<String> =
            (PageScriptRunner.findStreamUrls(html) + parseVideoUrlsByPattern(html)).distinct()

        // Still needed when the page's own script is broken, e.g. `p1` assigned three times.
        private fun parseVideoUrlsByPattern(html: String): List<String> {
            val sourceNames = playerSourceRegex.findAll(html).map { it.groupValues[1] }.toSet()
            val arrays = mutableListOf<Pair<String, List<String>>>()
            var previousEnd = 0
            for (match in arrayVarRegex.findAll(html)) {
                val scriptStart = html.lastIndexOf("<script", match.range.first, ignoreCase = true)
                val declarations = html.substring(maxOf(scriptStart, previousEnd, 0), match.range.first)
                arrays += match.groupValues[1] to parseUrlArray(match.groupValues[2], declarations)
                previousEnd = match.range.last + 1
            }
            val fromPlayer = arrays
                .filter { (name, _) -> name in sourceNames || name.startsWith("videos", ignoreCase = true) }
                .flatMap { it.second } + expandPushes(html, sourceNames, arrays.toMap())
            val fromArrays = fromPlayer.ifEmpty {
                arrays.flatMap { it.second }.filter { mediaUrlRegex.containsMatchIn(it) }
            }
            val urls = fromArrays.ifEmpty {
                playlistUrlRegex.findAll(html).map { it.value }.toList()
            }
            return urls.distinct()
        }

        /** Lists filled in a loop: `for (...) looo.push(S1[i] + S2 + S3)` with S1 an array of hosts. */
        private fun expandPushes(
            html: String,
            sourceNames: Set<String>,
            arrays: Map<String, List<String>>,
        ): List<String> = pushRegex.findAll(html)
            .filter { it.groupValues[1] in sourceNames }
            .flatMap { match ->
                val scriptStart = html.lastIndexOf("<script", match.range.first, ignoreCase = true)
                val strings = stringVarRegex.findAll(html.substring(maxOf(scriptStart, 0), match.range.first))
                    .associate { it.groupValues[1] to it.groupValues[3] }
                val choices = match.groupValues[2].split("+").map { it.trim() }.map { part ->
                    indexedRegex.matchEntire(part)?.let { arrays[it.groupValues[1]] }
                        ?: quotedRegex.matchEntire(part)?.let { listOf(it.groupValues[2]) }
                        ?: strings[part]?.let { listOf(it) }
                        ?: return@flatMap emptySequence()
                }
                choices.fold(listOf("")) { urls, values -> urls.flatMap { url -> values.map { url + it } } }
                    .asSequence()
            }
            .filter { it.startsWith("http") }
            .toList()

        private fun parseUrlArray(array: String, declarations: String): List<String> =
            runCatching {
                Json.decodeFromString<List<String>>(array)
            }.getOrElse {
                val strings = stringVarRegex.findAll(declarations)
                    .map { it.groupValues[1] to it.groupValues[3] }
                    .toList()
                array
                    .removePrefix("[")
                    .removeSuffix("]")
                    .split(",")
                    .mapNotNull { joinUrlParts(it, strings) }
            }.filter { it.startsWith("http") }

        private fun joinUrlParts(expression: String, strings: List<Pair<String, String>>): String? {
            val values = strings.toMap()
            val parts = expression.split("+").map { it.trim() }.filter { it.isNotEmpty() }
            val joined = parts.map { part -> quotedRegex.matchEntire(part)?.groupValues?.get(2) ?: values[part] }
            if (joined.isNotEmpty() && joined.all { it != null }) {
                val url = joined.joinToString("")
                if (url.startsWith("http")) return url
            }
            // Some pages assign `p1` three times instead of p1/p2/p3. Browsers fail there,
            // but the pieces are still declared in order.
            return strings.joinToString("") { it.second }.takeIf { it.startsWith("http") }
        }

        fun resolveUrl(base: String, path: String): String {
            if (path.startsWith("http", ignoreCase = true)) return path
            val encodedPath = path.split("/").joinToString("/") { segment ->
                @Suppress("DEPRECATION")
                URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
            }
            return URI(base).resolve(encodedPath).toASCIIString()
        }

        fun probeStream(url: String, client: OkHttpClient = defaultClient()): Boolean =
            checkStream(url, client) == LinkCheck.WORKING

        fun checkStream(url: String, client: OkHttpClient = defaultClient()): LinkCheck {
            val request = Request.Builder()
                .url(url)
                .get()
                .apply {
                    StreamRequestHeaders.properties.forEach { (key, value) -> header(key, value) }
                }
                .build()
            return runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return LinkCheck.DEAD
                    val snippet = response.body?.string()?.take(8192).orEmpty()
                    val hasVideo = snippet.contains("#EXT-X-STREAM-INF") || snippet.contains("#EXTINF")
                    when {
                        hasVideo -> LinkCheck.WORKING
                        // Before kickoff some hosts serve a bare `#EXTM3U` with nothing to play.
                        snippet.trimStart().startsWith("#EXTM3U") -> LinkCheck.DEAD
                        else -> LinkCheck.UNKNOWN
                    }
                }
            }.getOrDefault(LinkCheck.UNKNOWN)
        }
    }

    enum class LinkCheck { WORKING, DEAD, UNKNOWN }
}
