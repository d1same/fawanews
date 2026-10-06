package sc.fawanews.app.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class FawaRepositoryArticleTest {

    @Test
    fun parseArticle_fetchesNewsParagraphs() {
        val scheduleHtml = URL("http://www.fawanews.sc/").readText()
        val items = FawaRepository.parseSchedule(scheduleHtml)
        val news = items.first { !it.isLive }
        val encodedPath = news.pagePath.split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20")
        }
        val pageUrl = URL("http://www.fawanews.sc/").toURI().resolve(encodedPath).toASCIIString()
        val articleHtml = URL(pageUrl).readText()
        val article = FawaRepository.parseArticle(articleHtml, pageUrl)
        assertTrue(
            "Expected article text for ${news.title}, got ${article.paragraphs.size} blocks",
            article.paragraphs.size >= 2,
        )
    }
}
