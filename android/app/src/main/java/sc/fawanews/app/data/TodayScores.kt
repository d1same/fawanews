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

/** Letters that do not split into a base letter plus accent are spelled out by hand. */
private val spelledOut = mapOf('æ' to "ae", 'ø' to "o", 'ß' to "ss", 'đ' to "d", 'ł' to "l", 'ı' to "i", 'œ' to "oe")

private fun plainName(name: String): String {
    val lower = name.lowercase()
    val spelled = buildString { lower.forEach { append(spelledOut[it] ?: it) } }
    return accents.replace(Normalizer.normalize(spelled, Normalizer.Form.NFD), "")
        .replace(punctuation, " ")
        .trim()
}

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
fun findBoardGame(item: ScheduleItem, board: List<ScoreGame>, allowOneTeam: Boolean = true): ScoreGame? {
    val (left, right) = parseMatchupSides(item.title) ?: return null
    val both = board.firstOrNull { game ->
        (teamNamesMatch(left, game.homeTeam) && teamNamesMatch(right, game.awayTeam)) ||
            (teamNamesMatch(left, game.awayTeam) && teamNamesMatch(right, game.homeTeam))
    }
    if (both != null || !allowOneTeam) return both
    return board.filter { game ->
        listOf(left, right).any { side ->
            teamNamesMatch(side, game.homeTeam) || teamNamesMatch(side, game.awayTeam)
        }
    }.singleOrNull()
}

fun matchScore(item: ScheduleItem, board: List<ScoreGame>): ScoreGame? =
    findBoardGame(item, board)?.copy(id = item.id, leagueLabel = item.leagueLabel())

/**
 * A league the site streams gets its full day's scoreboard, so games the site is not streaming
 * still show. That only happens once a site game matched the board, which keeps a wrongly
 * guessed league from filling the list. Soccer games with no league board are looked up on
 * the all-soccer board. A site game found nowhere still shows, without a score.
 */
fun scoresForLiveDay(
    matches: List<ScheduleItem>,
    boards: Map<ScoreLeague, List<ScoreGame>>,
    allSoccer: List<ScoreGame> = emptyList(),
    now: Instant = Instant.now(),
    localZone: ZoneId = ZoneId.systemDefault(),
): List<ScoreGame> {
    val unique = dedupeMatchups(matches)
    val scores = mutableListOf<ScoreGame>()
    val listedGameIds = mutableSetOf<String>()
    for ((siteLabel, items) in unique.groupBy { it.leagueLabel() }) {
        val league = ScoreLeague.fromSiteLabel(siteLabel)
        val board = if (league == null) emptyList() else boards[league].orEmpty()
        val streamByGame = mutableMapOf<String, String>()
        val unmatched = mutableListOf<ScheduleItem>()
        for (item in items) {
            val game = findBoardGame(item, board)
            if (game == null) unmatched += item else streamByGame.putIfAbsent(game.id, item.pagePath)
        }
        if (streamByGame.isNotEmpty()) {
            board.filter { listedGameIds.add(it.id) }.forEach { game ->
                scores += game.copy(leagueLabel = siteLabel, streamPagePath = streamByGame[game.id])
            }
        }
        for (item in unmatched) {
            val soccerGame = if (ScoreLeague.looksLikeSoccer(siteLabel)) {
                findBoardGame(item, allSoccer, allowOneTeam = false)
            } else {
                null
            }
            scores += if (soccerGame != null && listedGameIds.add(soccerGame.id)) {
                soccerGame.copy(leagueLabel = siteLabel, streamPagePath = item.pagePath)
            } else {
                placeholderScore(item, now, localZone)
            }
        }
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
