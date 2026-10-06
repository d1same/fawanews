package sc.fawanews.app.data

private val channelSuffix = Regex("""\s+[-–—]+\s+ch\s*\d+.*$""", RegexOption.IGNORE_CASE)
private val versus = Regex("""\s+vs\.?\s+""", RegexOption.IGNORE_CASE)
private val clock = Regex("""\d{1,2}:\d{2}""")

/** "Saints vs Falcons --- CH 2" and CH 1 are the same match. */
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

fun teamNamesMatch(scheduleSide: String, espnName: String): Boolean {
    val side = scheduleSide.lowercase()
    val name = espnName.lowercase()
    if (name.length < 3 || side.length < 3) return false
    return side.contains(name) || name.contains(side)
}

fun matchScore(item: ScheduleItem, board: List<ScoreGame>): ScoreGame? {
    val (left, right) = parseMatchupSides(item.title) ?: return null
    val found = board.firstOrNull { game ->
        (teamNamesMatch(left, game.homeTeam) && teamNamesMatch(right, game.awayTeam)) ||
            (teamNamesMatch(left, game.awayTeam) && teamNamesMatch(right, game.homeTeam))
    } ?: return null
    return found.copy(id = item.id, leagueLabel = item.leagueLabel())
}

/**
 * Sports on the live list get the full day's scoreboard.
 * A game the site is not streaming still shows. A site game with no scoreboard row still shows.
 */
fun scoresForLiveDay(
    matches: List<ScheduleItem>,
    boards: Map<ScoreLeague, List<ScoreGame>>,
): List<ScoreGame> {
    val unique = dedupeMatchups(matches)
    val scores = mutableListOf<ScoreGame>()
    for ((league, items) in unique.groupBy { ScoreLeague.fromSiteLabel(it.leagueLabel()) }) {
        val siteLabel = items.first().leagueLabel()
        if (league == null) {
            scores.addAll(items.map { placeholderScore(it) })
            continue
        }
        val board = boards[league].orEmpty()
        scores.addAll(board.map { game -> game.copy(leagueLabel = siteLabel) })
        for (item in items) {
            val alreadyListed = matchScore(item, board) != null
            if (!alreadyListed) scores.add(placeholderScore(item))
        }
    }
    return scores
}

fun placeholderScore(item: ScheduleItem): ScoreGame {
    val sides = parseMatchupSides(item.title)
    val time = item.subtitle?.let { clock.find(it)?.value }
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
    )
}
