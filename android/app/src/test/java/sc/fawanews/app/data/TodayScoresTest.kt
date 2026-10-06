package sc.fawanews.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayScoresTest {
    @Test
    fun leagueLabelMapsSiteNames() {
        assertEquals(ScoreLeague.NFL, ScoreLeague.fromSiteLabel("NFL American Football"))
        assertEquals(ScoreLeague.COLOMBIA_B, ScoreLeague.fromSiteLabel("Colombia Primera B"))
        assertEquals(ScoreLeague.COLOMBIA_A, ScoreLeague.fromSiteLabel("Colombia Primera A"))
        assertNull(ScoreLeague.fromSiteLabel("Local club friendly"))
    }

    @Test
    fun channelCopiesCollapseToOneMatch() {
        val items = listOf(
            item("Saints vs Falcons --- CH 1", "NFL American Football 01:15"),
            item("Saints vs Falcons --- CH 2", "NFL American Football 01:15"),
        )
        assertEquals(1, dedupeMatchups(items).size)
    }

    @Test
    fun espnScoreAttachesToTodaysMatch() {
        val match = item("New Orleans Saints vs Atlanta Falcons --- CH 1", "NFL American Football 01:15")
        val board = listOf(
            ScoreGame(
                id = "espn-1",
                leagueLabel = "NFL",
                homeTeam = "Falcons",
                awayTeam = "Saints",
                homeScore = "17",
                awayScore = "14",
                homeLogoUrl = null,
                awayLogoUrl = null,
                homeRecord = null,
                awayRecord = null,
                statusLabel = "Q2",
                isLive = true,
                venueName = null,
                venueLocation = null,
                broadcastLabel = null,
                startTimeLabel = null,
            ),
        )
        val scored = matchScore(match, board)
        assertEquals("14", scored?.awayScore)
        assertEquals("NFL American Football", scored?.leagueLabel)
        assertEquals(match.id, scored?.id)
    }

    @Test
    fun missingStreamStillShowsTheRestOfThatSport() {
        val onlySaints = item(
            "New Orleans Saints vs Atlanta Falcons --- CH 1",
            "NFL American Football 01:15",
        )
        val saints = score("espn-saints", "Falcons", "Saints", "17", "14")
        val bills = score("espn-bills", "Bills", "Dolphins", "10", "7")
        val day = scoresForLiveDay(
            listOf(onlySaints),
            mapOf(ScoreLeague.NFL to listOf(saints, bills)),
        )
        assertEquals(listOf("Falcons", "Bills"), day.map { it.homeTeam })
        assertEquals(onlySaints.pagePath, day.first { it.homeTeam == "Falcons" }.streamPagePath)
        assertEquals(null, day.first { it.homeTeam == "Bills" }.streamPagePath)
    }

    private fun score(id: String, home: String, away: String, homeScore: String, awayScore: String) = ScoreGame(
        id = id,
        leagueLabel = "NFL",
        homeTeam = home,
        awayTeam = away,
        homeScore = homeScore,
        awayScore = awayScore,
        homeLogoUrl = null,
        awayLogoUrl = null,
        homeRecord = null,
        awayRecord = null,
        statusLabel = "Q2",
        isLive = true,
        venueName = null,
        venueLocation = null,
        broadcastLabel = null,
        startTimeLabel = null,
    )

    private fun item(title: String, subtitle: String) = ScheduleItem(
        id = title,
        title = title,
        subtitle = subtitle,
        imageUrl = null,
        pagePath = title,
        isLive = true,
    )
}
