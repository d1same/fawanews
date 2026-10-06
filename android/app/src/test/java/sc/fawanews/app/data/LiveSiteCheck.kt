package sc.fawanews.app.data

import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Opens the real site and runs every live game page through the app's link finder.
 * Fails when a page clearly carries a link in its scripts but the app finds none, which
 * means the site changed its format. Dead links are reported but do not fail: those are
 * games that have not started or have ended.
 *
 * Runs only with LIVE_SITE_CHECK=1 (the scheduled GitHub workflow sets it).
 */
class LiveSiteCheck {
    private val scriptRegex = Regex("""<script\b[^>]*>(.*?)</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val linkHintRegex = Regex("""https?:|\.m3u8|\.mpd|hls/""", RegexOption.IGNORE_CASE)
    private val adHostRegex = Regex("""acscdn|twitter|google|facebook|cloudflare""", RegexOption.IGNORE_CASE)

    @Test
    fun everyLiveGamePageYieldsALink() {
        assumeTrue(System.getenv("LIVE_SITE_CHECK") == "1")

        val repository = FawaRepository()
        val client = FawaRepository.defaultClient()
        val schedule = runCatching { runBlocking { repository.fetchSchedule() } }
        val games = schedule.getOrDefault(emptyList()).filter { it.isLive }
        if (games.isEmpty()) {
            val why = schedule.exceptionOrNull()?.let { "the site did not load: ${it.message}" }
                ?: "the homepage loaded but no games were found, so its layout probably changed"
            File("build/live-site-report.md").apply { parentFile?.mkdirs() }
                .writeText("## Live site check\n\nNo live games: $why.\n")
            fail("No live games: $why")
        }
        val missed = mutableListOf<String>()
        val rows = mutableListOf<String>()

        for (game in games) {
            val url = FawaRepository.resolveUrl(FawaRepository.BASE_URL, game.pagePath)
            val loaded = runCatching {
                client.newCall(Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).execute()
                    .use { it.body?.string().orEmpty() }
            }
            val html = loaded.getOrNull()
            if (html == null) {
                rows += "| ${game.title} | page failed to load: ${loaded.exceptionOrNull()?.message} | |"
                continue
            }
            val links = FawaRepository.parseVideoUrls(html)
            if (links.isEmpty()) {
                val hinted = scriptRegex.findAll(html).map { it.groupValues[1] }
                    .any { linkHintRegex.containsMatchIn(it) && !adHostRegex.containsMatchIn(it) }
                if (hinted) missed += game.title
                rows += "| ${game.title} | ${if (hinted) "**NO LINK FOUND (format changed?)**" else "no link posted yet"} | |"
                continue
            }
            val status = links.joinToString("<br>") { link ->
                "${FawaRepository.checkStream(link, client)} `$link`"
            }
            rows += "| ${game.title} | ${links.size} link(s) | $status |"
        }

        val report = buildString {
            appendLine("## Live site check")
            appendLine()
            appendLine("${games.size} live game pages, ${missed.size} where the app found no link.")
            appendLine()
            appendLine("| Game | Result | Links |")
            appendLine("|---|---|---|")
            rows.forEach { appendLine(it) }
        }
        File("build/live-site-report.md").apply { parentFile?.mkdirs() }.writeText(report)
        println(report)

        if (missed.isNotEmpty()) {
            fail("The app found no link on: ${missed.joinToString()}")
        }
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
