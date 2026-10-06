package sc.fawanews.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoUrlParseTest {
    @Test
    fun readsOriginalVideosArray() {
        val html = """var videos =  ["http://193.47.62.44/hls/a.m3u8"];"""
        assertEquals(listOf("http://193.47.62.44/hls/a.m3u8"), FawaRepository.parseVideoUrls(html))
    }

    @Test
    fun readsNumberedVideosArray() {
        val html = """
            var videos_1 = ["http://193.47.62.41/hls/VUUUAAAAAAA.m3u8"];
            var player = new Clappr.Player({ source: videos_1[0] });
        """.trimIndent()
        assertEquals(listOf("http://193.47.62.41/hls/VUUUAAAAAAA.m3u8"), FawaRepository.parseVideoUrls(html))
    }

    @Test
    fun readsEveryNumberedArray() {
        val html = """
            var videos_1 = ["http://a.example/one.m3u8"];
            var videos_2 = ['http://b.example/two.m3u8'];
        """.trimIndent()
        assertEquals(
            listOf("http://a.example/one.m3u8", "http://b.example/two.m3u8"),
            FawaRepository.parseVideoUrls(html),
        )
    }

    @Test
    fun fallsBackToAnyPlaylistLink() {
        val html = """new Clappr.Player({ source: "https://live.example/master.m3u8?token=1" });"""
        assertEquals(listOf("https://live.example/master.m3u8?token=1"), FawaRepository.parseVideoUrls(html))
    }

    @Test
    fun newsPageHasNoStreams() {
        assertEquals(emptyList<String>(), FawaRepository.parseVideoUrls("<p>Klopp says he has no plans to leave.</p>"))
    }
}
