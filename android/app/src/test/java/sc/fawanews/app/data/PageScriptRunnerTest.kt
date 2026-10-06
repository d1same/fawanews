package sc.fawanews.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageScriptRunnerTest {
    private fun page(script: String) = """
        <html><body>
        <script id="aclib" type="text/javascript" src="//acscdn.com/script/aclib.js"></script>
        <script type="text/javascript">aclib.runPop({ zoneId: '8630206' });</script>
        <script type="text/javascript" src="https://cdn.jsdelivr.net/clappr/latest/clappr.min.js"></script>
        <div id="player"></div>
        <script type="text/javascript">$script</script>
        </body></html>
    """.trimIndent()

    private fun clappr(list: String) =
        "var player = new Clappr.Player({ source: $list[Math.floor(Math.random() * $list.length)], parentId: '#player' });"

    @Test
    fun renamedSplitLink() {
        val html = page(
            """
            var g1 = "http://193.47.62.55/hls/";
            var g2 = "roooaaa";
            var g3 = ".m3u8";
            var looo = [g1 + g2 + g3];
            ${clappr("looo")}
            """,
        )
        assertEquals(listOf("http://193.47.62.55/hls/roooaaa.m3u8"), PageScriptRunner.findStreamUrls(html))
    }

    @Test
    fun loopOverServersKeepsEveryServer() {
        val html = page(
            """
            var S1 = ["http://193.47.62.59/hls/", "http://193.47.62.50/hls/", "http://193.47.62.44/hls/"];
            var S2 = "HUHUHU";
            var S3 = ".m3u8";
            var looo = [];
            for (var i = 0; i < S1.length; i++) { looo.push(S1[i] + S2 + S3); }
            ${clappr("looo")}
            """,
        )
        val urls = PageScriptRunner.findStreamUrls(html)
        assertEquals(
            setOf(
                "http://193.47.62.59/hls/HUHUHU.m3u8",
                "http://193.47.62.50/hls/HUHUHU.m3u8",
                "http://193.47.62.44/hls/HUHUHU.m3u8",
            ),
            urls.toSet(),
        )
    }

    @Test
    fun reversedAndEncodedLinks() {
        val reversed = "http://a.example/hls/rev.m3u8".reversed()
        val encoded = java.util.Base64.getEncoder().encodeToString("http://b.example/hls/b64.m3u8".toByteArray())
        val html = page(
            """
            var r = "$reversed".split("").reverse().join("");
            var b = atob("$encoded");
            var list = [r, b];
            ${clappr("list")}
            """,
        )
        assertEquals(
            setOf("http://a.example/hls/rev.m3u8", "http://b.example/hls/b64.m3u8"),
            PageScriptRunner.findStreamUrls(html).toSet(),
        )
    }

    @Test
    fun linkBuiltInsideFunctionAndTimer() {
        val html = page(
            """
            function build(host, id) { return "http://" + host + "/hls/" + id + ".m3u8"; }
            setTimeout(function () {
                new Clappr.Player({ source: build("193.47.62.41", "DBBBQQQ"), parentId: '#player' });
            }, 500);
            """,
        )
        assertEquals(listOf("http://193.47.62.41/hls/DBBBQQQ.m3u8"), PageScriptRunner.findStreamUrls(html))
    }

    @Test
    fun otherPlayers() {
        assertEquals(
            listOf("https://c.example/live/index.m3u8"),
            PageScriptRunner.findStreamUrls(page("""jwplayer("p").setup({ file: "https://c.example/live/index.m3u8" });""")),
        )
        assertEquals(
            listOf("https://d.example/x.m3u8"),
            PageScriptRunner.findStreamUrls(page("""var h = new Hls(); h.loadSource("https://d.example/x.m3u8");""")),
        )
        assertEquals(
            listOf("https://e.example/x.m3u8"),
            PageScriptRunner.findStreamUrls(
                page("""var v = document.createElement("video"); v.src = "https://e.example/x.m3u8";"""),
            ),
        )
    }

    @Test
    fun endlessLoopDoesNotHang() {
        val started = System.currentTimeMillis()
        val urls = PageScriptRunner.findStreamUrls(page("""var u = "http://f.example/a.m3u8"; while (true) {}"""))
        assertTrue(System.currentTimeMillis() - started < 10_000)
        assertEquals(listOf("http://f.example/a.m3u8"), urls)
    }

    @Test
    fun pageCannotReachJava() {
        val html = page(
            """
            var leaked = "";
            try { leaked = String(java.lang.System.getProperty("user.home")); } catch (e) {}
            var looo = ["http://g.example/hls/" + (leaked ? "LEAK" : "ok") + ".m3u8"];
            ${clappr("looo")}
            """,
        )
        assertEquals(listOf("http://g.example/hls/ok.m3u8"), PageScriptRunner.findStreamUrls(html))
    }

    @Test
    fun newsPageHasNoLinks() {
        assertEquals(emptyList<String>(), PageScriptRunner.findStreamUrls("<p>Klopp says he has no plans to leave.</p>"))
    }
}
