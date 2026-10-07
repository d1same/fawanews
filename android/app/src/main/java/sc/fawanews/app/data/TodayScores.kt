package sc.fawanews.app.data

import java.text.Normalizer
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val channelSuffix = Regex("""\s+(?:[-–—]{2,}.*|[-–—]+\s+ch\s*\d+.*)$""", RegexOption.IGNORE_CASE)
private val versus = Regex("""\s+vs\.?\s+""", RegexOption.IGNORE_CASE)
private val clock = Regex("""(\d{1,2}):(\d{2})""")
private val accents = Regex("""\p{Mn}+""")
private val punctuation = Regex("""[^\p{L}\p{N}]+""")
private val siteZone: ZoneId = ZoneId.of("Europe/London")

/** "Saints vs Falcons --- CH 2", "--- ENG" and "--- ES" are all the same match. */
fun ScheduleItem.matchupTitle(): String =
    channelSuffix.replace(title, "").trim().ifBlank { title.trim() }

fun dedupeMatchups(items: List<ScheduleItem>): List<ScheduleItem> {
    val seen = linkedSetOf<String>()
    return items.filter { item ->
        val key = "${item.leagueLabel().lowercase()}|${item.matchupTitle().lowercase()}"
        seen.add(key)
    }
}

fun parseMatchupSides(title: String): Pair<String, String>? {
    val parts = channelSuffix.replace(title, "").trim().split(versus, limit = 2)
    if (parts.size != 2) return null
    val left = parts[0].trim()
    val right = parts[1].trim()
    if (left.isBlank() || right.isBlank()) return null
    return left to right
}

private fun plainName(name: String): String =
    accents.replace(Normalizer.normalize(name, Normalizer.Form.NFD), "")
        .lowercase()
        .replace(punctuation, " ")
        .trim()

fun teamNamesMatch(scheduleSide: String, espnName: String): Boolean {
    val side = plainName(scheduleSide)
    val name = plainName(espnName)
    if (name.length < 3 || side.length < 3) return false
    return side.contains(name) || name.contains(side)
}

/**
 * Both teams must match. When the scoreboard spells one team very differently
 * ("Academia del Balompie" vs "ABB"), one matching team is enough if it plays in only one game.
 */
fun findBoardGame(item: ScheduleItem, board: List<ScoreGame>): ScoreGame? {
    val (left, right) = parseMatchupSides(item.title) ?: return null
    return board.firstOrNull { game ->
        (teamNamesMatch(left, game.homeTeam) && teamNamesMatch(right, game.awayTeam)) ||
            (teamNamesMatch(left, game.awayTeam) && teamNamesMatch(right, game.homeTeam))
    } ?: board.filter { game ->
        listOf(left, right).any { side ->
            teamNamesMatch(side, game.homeTeam) || teamNamesMatch(side, game.awayTeam)
        }
    }.singleOrNull()
}

fun matchScore(item: ScheduleItem, board: List<ScoreGame>): ScoreGame? =
    findBoardGame(item, board)?.copy(id = item.id, leagueLabel = item.leagueLabel())

/**
 * Sports on the live list get the full day's scoreboard.
 * A game the site is not streaming still shows. A site game with no scoreboard row still shows.
 */
fun scoresForLiveDay(
    matches: List<ScheduleItem>,
    boards: Map<ScoreLeague, List<ScoreGame>>,
    now: Instant = Instant.now(),
    localZone: ZoneId = ZoneId.systemDefault(),
): List<ScoreGame> {
    val unique = dedupeMatchups(matches)
    val scores = mutableListOf<ScoreGame>()
    for ((league, items) in unique.groupBy { ScoreLeague.fromSiteLabel(it.leagueLabel()) }) {
        val siteLabel = items.first().leagueLabel()
        val board = if (league == null) emptyList() else boards[league].orEmpty()
        val streamByGame = mutableMapOf<String, String>()
        val unmatched = mutableListOf<ScheduleItem>()
        for (item in items) {
            val game = findBoardGame(item, board)
            if (game == null) unmatched += item else streamByGame.putIfAbsent(game.id, item.pagePath)
        }
        scores.addAll(
            board.map { game ->
                game.copy(leagueLabel = siteLabel, streamPagePath = streamByGame[game.id])
            },
        )
        scores.addAll(unmatched.map { placeholderScore(it, now, localZone) })
    }
    return scores.sortedBy { it.listRank() }
}

/** Games you can watch come first; within each group, live, then upcoming, then finished. */
private fun ScoreGame.listRank(): Int {
    val stage = when {
        isLive -> 0
        isFinished -> 2
        else -> 1
    }
    return if (streamPagePath != null) stage else 3 + stage
}

fun placeholderScore(
    item: ScheduleItem,
    now: Instant = Instant.now(),
    localZone: ZoneId = ZoneId.systemDefault(),
): ScoreGame {
    val sides = parseMatchupSides(item.title)
    val time = item.subtitle?.let { siteTimeToLocal(it, now, localZone) }
    return ScoreGame(
        id = item.id,
        leagueLabel = item.leagueLabel(),
        homeTeam = sides?.first ?: item.matchupTitle(),
        awayTeam = sides?.second ?: "",
        homeScore = "-",
        awayScore = "-",
        homeLogoUrl = null,
        awayLogoUrl = null,
        homeRecord = null,
        awayRecord = null,
        statusLabel = time ?: "Today",
        isLive = false,
        venueName = null,
        venueLocation = null,
        broadcastLabel = null,
        startTimeLabel = time,
        streamPagePath = item.pagePath,
    )
}

/** The site lists kickoff in UK time with no date; pick the nearest such moment to now. */
fun siteTimeToLocal(subtitle: String, now: Instant, localZone: ZoneId): String? {
    val match = clock.find(subtitle) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    if (hour > 23 || minute > 59) return null
    val today = now.atZone(siteZone).toLocalDate()
    val kickoff = (-1L..1L)
        .map { today.plusDays(it).atTime(LocalTime.of(hour, minute)).atZone(siteZone).toInstant() }
        .minBy { Duration.between(now, it).abs() }
    return DateTimeFormatter.ofPattern("h:mm a", Locale.US).withZone(localZone).format(kickoff)
}
