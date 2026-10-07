package sc.fawanews.app.data

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class LiveScoresProbe {
    @Test
    fun report() {
        assumeTrue(System.getenv("LIVE_SCORES_PROBE") == "1")
        val schedule = runBlocking { FawaRepository().fetchSchedule() }.filter { it.isLive }
        val unique = dedupeMatchups(schedule)
        val out = StringBuilder()
        out.appendLine("SITE GAMES: ${unique.size}")
        for (item in unique) {
            val league = ScoreLeague.fromSiteLabel(item.leagueLabel())
            out.appendLine("[${item.leagueLabel()}] -> ${league?.name ?: "NO LEAGUE"} | ${item.title} | sub=${item.subtitle}")
        }
        val scores = runBlocking { ScoreRepository().fetchScoresForMatches(schedule) }
        out.appendLine()
        out.appendLine("SCORES: ${scores.size}")
        for (g in scores) {
            out.appendLine("[${g.leagueLabel}] ${g.awayTeam} ${g.awayScore} @ ${g.homeTeam} ${g.homeScore} | ${g.statusLabel} | live=${g.isLive} | logo=${g.homeLogoUrl != null} | watch=${g.streamPagePath != null}")
        }
        File("build/live-scores-probe.txt").writeText(out.toString())
    }
}
