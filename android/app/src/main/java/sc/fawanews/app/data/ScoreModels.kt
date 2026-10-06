package sc.fawanews.app.data

data class ScoreGame(
    val id: String,
    val leagueLabel: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String,
    val awayScore: String,
    val homeLogoUrl: String?,
    val awayLogoUrl: String?,
    val homeRecord: String?,
    val awayRecord: String?,
    val statusLabel: String,
    val isLive: Boolean,
    val venueName: String?,
    val venueLocation: String?,
    val broadcastLabel: String?,
    val startTimeLabel: String?,
)

enum class ScoreLeague(val label: String, val espnPath: String) {
    NFL("NFL", "football/nfl"),
    NBA("NBA", "basketball/nba"),
    NHL("NHL", "hockey/nhl"),
    MLB("MLB", "baseball/mlb"),
    EPL("Premier League", "soccer/eng.1"),
    UCL("Champions League", "soccer/uefa.champions"),
}
