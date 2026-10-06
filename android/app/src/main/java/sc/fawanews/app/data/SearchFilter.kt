package sc.fawanews.app.data

fun List<ScheduleItem>.filterScheduleBySearch(query: String): List<ScheduleItem> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return filter { item ->
        item.title.contains(q, ignoreCase = true) ||
            item.subtitle?.contains(q, ignoreCase = true) == true ||
            item.leagueLabel().contains(q, ignoreCase = true)
    }
}

fun List<ScoreGame>.filterScoresBySearch(query: String): List<ScoreGame> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return filter { game ->
        game.homeTeam.contains(q, ignoreCase = true) ||
            game.awayTeam.contains(q, ignoreCase = true) ||
            game.leagueLabel.contains(q, ignoreCase = true) ||
            game.statusLabel.contains(q, ignoreCase = true) ||
            game.venueName?.contains(q, ignoreCase = true) == true ||
            game.venueLocation?.contains(q, ignoreCase = true) == true ||
            game.broadcastLabel?.contains(q, ignoreCase = true) == true
    }
}
