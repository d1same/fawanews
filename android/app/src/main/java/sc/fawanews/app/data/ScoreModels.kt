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
    val streamPagePath: String? = null,
)

enum class ScoreLeague(val label: String, val espnPath: String, val keywords: List<String>) {
    NFL("NFL", "football/nfl", listOf("nfl")),
    NBA("NBA", "basketball/nba", listOf("nba")),
    NHL("NHL", "hockey/nhl", listOf("nhl")),
    MLB("MLB", "baseball/mlb", listOf("mlb")),
    WNBA("WNBA", "basketball/wnba", listOf("wnba")),
    NCAAF("College football", "football/college-football", listOf("college football", "ncaaf")),
    NCAAB("College basketball", "basketball/mens-college-basketball", listOf("college basketball", "ncaab")),
    EPL("Premier League", "soccer/eng.1", listOf("premier league", "epl")),
    LA_LIGA("La Liga", "soccer/esp.1", listOf("la liga")),
    SERIE_A("Serie A", "soccer/ita.1", listOf("serie a")),
    BUNDESLIGA("Bundesliga", "soccer/ger.1", listOf("bundesliga")),
    LIGUE_1("Ligue 1", "soccer/fra.1", listOf("ligue 1")),
    EREDIVISIE("Eredivisie", "soccer/ned.1", listOf("eredivisie")),
    PRIMEIRA("Primeira Liga", "soccer/por.1", listOf("primeira liga")),
    BRASILEIRAO("Brasileirão", "soccer/bra.1", listOf("brasileir")),
    LIGA_MX("Liga MX", "soccer/mex.1", listOf("liga mx")),
    MLS("MLS", "soccer/usa.1", listOf("mls")),
    ARGENTINA("Argentina", "soccer/arg.1", listOf("argentina", "liga profesional")),
    COLOMBIA_B("Colombia Primera B", "soccer/col.2", listOf("primera b", "colombia b")),
    COLOMBIA_A("Colombia Primera A", "soccer/col.1", listOf("primera a", "colombia")),
    CONCACAF("CONCACAF", "soccer/concacaf.nations", listOf("concacaf", "nations league")),
    UCL("Champions League", "soccer/uefa.champions", listOf("champions league")),
    UEL("Europa League", "soccer/uefa.europa", listOf("europa league")),
    WORLD_CUP("World Cup", "soccer/fifa.world", listOf("world cup")),
    UFC("UFC", "mma/ufc", listOf("ufc")),
    ;

    companion object {
        fun fromSiteLabel(label: String): ScoreLeague? {
            val text = label.lowercase()
            return entries.firstOrNull { league ->
                league.keywords.any { keyword -> text.contains(keyword) }
            }
        }
    }
}
