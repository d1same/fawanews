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

    @Test
    fun siteLabelsSeenLiveMapToTheirScoreboards() {
        assertEquals(ScoreLeague.FRIENDLY, ScoreLeague.fromSiteLabel("Friendly Match"))
        assertEquals(ScoreLeague.NCAAF, ScoreLeague.fromSiteLabel("NCAA American Football"))
        assertEquals(ScoreLeague.BRAZIL_B, ScoreLeague.fromSiteLabel("Brazil Serie B"))
        assertEquals(ScoreLeague.BRASILEIRAO, ScoreLeague.fromSiteLabel("Brazil Serie A"))
        assertEquals(ScoreLeague.SERIE_A, ScoreLeague.fromSiteLabel("Italy Serie A"))
        assertEquals(ScoreLeague.BOLIVIA_COPA, ScoreLeague.fromSiteLabel("Bolivia Copa"))
        assertEquals(ScoreLeague.UEFA_NATIONS, ScoreLeague.fromSiteLabel("UEFA Nations League"))
        assertEquals(ScoreLeague.CONCACAF, ScoreLeague.fromSiteLabel("CONCACAF Nations League"))
    }

    @Test
    fun languageCopiesCollapseToOneMatch() {
        val items = listOf(
            item("Argentina vs Benin --- ENG", "Friendly Match 00:00"),
            item("Argentina vs Benin --- ES", "Friendly Match 00:00"),
            item("Argentina vs Benin --- CH 2", "Friendly Match 00:00"),
        )
        assertEquals(1, dedupeMatchups(items).size)
    }

    @Test
    fun accentsAndShortNamesStillMatch() {
        val aurora = score("espn-aur", "Aurora", "ABB", "2", "0")
        val oruro = score("espn-oru", "Real Oruro", "Tomayapo", "4", "0")
        val match = item("Aurora vs Academia del Balompie", "Bolivia Copa 01:00")
        assertEquals("espn-aur", findBoardGame(match, listOf(oruro, aurora))?.id)
        assertEquals(true, teamNamesMatch("Real Potosi", "Real Potosí"))
    }

    @Test
    fun oneSidedMatchIsSkippedWhenTwoGamesFit() {
        val a = score("a", "Russia", "Nigeria", "3", "3")
        val b = score("b", "Algeria", "Niger", "2", "1")
        assertNull(findBoardGame(item("Nigeria vs Somewhere", "Friendly Match 00:00"), listOf(a, b)))
    }

    @Test
    fun watchableGamesComeFirstThenLiveThenUpcomingThenFinished() {
        val finishedNotOnSite = score("korea", "South Korea", "Uzbekistan", "2", "0").copy(isLive = false, isFinished = true)
        val liveNotOnSite = score("bills", "Bills", "Dolphins", "10", "7")
        val finishedOnSite = score("argentina", "Argentina", "Benin", "3", "0").copy(isLive = false, isFinished = true)
        val liveOnSite = score("usa", "USA", "Canada", "1", "0")
        val laterOnSite = score("mexico", "Mexico", "Chile", "0", "0").copy(isLive = false)
        val day = scoresForLiveDay(
            listOf(
                item("Argentina vs Benin", "Friendly Match 00:00"),
                item("USA vs Canada", "Friendly Match 01:00"),
                item("Mexico vs Chile", "Friendly Match 03:30"),
            ),
            mapOf(ScoreLeague.FRIENDLY to listOf(finishedNotOnSite, liveNotOnSite, finishedOnSite, liveOnSite, laterOnSite)),
        )
        assertEquals(listOf("usa", "mexico", "argentina", "bills", "korea"), day.map { it.id })
    }

    @Test
    fun unmatchedGameShowsKickoffInLocalTime() {
        val now = java.time.Instant.parse("2026-10-07T01:38:00Z")
        val placeholder = placeholderScore(
            item("Troy vs Southern Miss", "NCAA American Football 01:00"),
            now,
            java.time.ZoneId.of("America/New_York"),
        )
        assertEquals("8:00 PM", placeholder.startTimeLabel)
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
