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
    val isFinished: Boolean = false,
    val venueName: String?,
    val venueLocation: String?,
    val broadcastLabel: String?,
    val startTimeLabel: String?,
    val streamPagePath: String? = null,
)

/**
 * The first league whose keyword appears in the site label wins, so a more specific league
 * ("brazil serie a") must come before a broader one ("serie a"). [exactLabels] are for names
 * many countries share ("premier league"): they only match the whole label.
 * [byDate] boards list the whole week unless asked for one day.
 */
enum class ScoreLeague(
    val label: String,
    val espnPath: String,
    val keywords: List<String>,
    val byDate: Boolean = false,
    val exactLabels: List<String> = emptyList(),
) {
    NFL("NFL", "football/nfl", listOf("nfl")),
    NBA("NBA", "basketball/nba", listOf("nba")),
    NHL("NHL", "hockey/nhl", listOf("nhl")),
    MLB("MLB", "baseball/mlb", listOf("mlb")),
    WNBA("WNBA", "basketball/wnba", listOf("wnba")),
    NCAAF(
        "College football",
        "football/college-football",
        listOf("college football", "ncaaf", "ncaa american football", "ncaa football"),
        byDate = true,
    ),
    NCAAB(
        "College basketball",
        "basketball/mens-college-basketball",
        listOf("college basketball", "ncaab", "ncaa basketball"),
        byDate = true,
    ),
    EPL(
        "Premier League",
        "soccer/eng.1",
        listOf("english premier", "england premier", "epl"),
        exactLabels = listOf("premier league"),
    ),
    RUSSIA("Russia Premier League", "soccer/rus.1", listOf("russia premier", "russian premier")),
    TURKEY("Turkey Super Lig", "soccer/tur.1", listOf("turkey super lig", "turkish super lig")),
    LA_LIGA("La Liga", "soccer/esp.1", listOf("la liga")),
    BRAZIL_B("Brazil Serie B", "soccer/bra.2", listOf("brazil serie b", "brasileirao serie b")),
    BRASILEIRAO("Brasileirão", "soccer/bra.1", listOf("brasileir", "brazil serie a")),
    SERIE_A("Serie A", "soccer/ita.1", listOf("serie a")),
    BUNDESLIGA_2("2. Bundesliga", "soccer/ger.2", listOf("german 2 bundesliga", "2. bundesliga", "bundesliga 2")),
    BUNDESLIGA("Bundesliga", "soccer/ger.1", listOf("bundesliga")),
    LIGUE_1("Ligue 1", "soccer/fra.1", listOf("ligue 1")),
    EREDIVISIE("Eredivisie", "soccer/ned.1", listOf("eredivisie")),
    PRIMEIRA("Primeira Liga", "soccer/por.1", listOf("primeira liga")),
    LIGA_MX("Liga MX", "soccer/mex.1", listOf("liga mx")),
    MLS("MLS", "soccer/usa.1", listOf("mls")),
    ARGENTINA("Argentina", "soccer/arg.1", listOf("argentina", "liga profesional")),
    COLOMBIA_B("Colombia Primera B", "soccer/col.2", listOf("primera b", "colombia b")),
    COLOMBIA_A("Colombia Primera A", "soccer/col.1", listOf("primera a", "colombia")),
    BOLIVIA_COPA("Bolivia Copa", "soccer/bol.copa", listOf("bolivia copa")),
    BOLIVIA("Bolivia", "soccer/bol.1", listOf("bolivia")),
    UEFA_NATIONS("UEFA Nations League", "soccer/uefa.nations", listOf("uefa nations")),
    CONCACAF("CONCACAF", "soccer/concacaf.nations.league", listOf("concacaf", "nations league")),
    UCL(
        "Champions League",
        "soccer/uefa.champions",
        listOf("uefa champions"),
        exactLabels = listOf("champions league"),
    ),
    UEL("Europa League", "soccer/uefa.europa", listOf("uefa europa"), exactLabels = listOf("europa league")),
    WOMEN_WORLD_CUP_QUALIFIERS("Women's World Cup qualifying", "soccer/fifa.wworldq.uefa", listOf("women world cup")),
    WORLD_CUP("World Cup", "soccer/fifa.world", listOf("world cup")),
    FRIENDLY("International friendly", "soccer/fifa.friendly", listOf("friendly match", "international friendly")),
    UFC("UFC", "mma/ufc", listOf("ufc")),
    ;

    companion object {
        fun fromSiteLabel(label: String): ScoreLeague? {
            val text = label.lowercase().trim()
            return entries.firstOrNull { league ->
                text in league.exactLabels || league.keywords.any { keyword -> text.contains(keyword) }
            }
        }

        private val notSoccer = Regex(
            "cricket|hockey|rugby|basketball|tennis|golf|darts|racing|motogp|formula|f1|nfl|nba|nhl|" +
                "mlb|wnba|ufc|mma|boxing|wrestling|baseball|american football|volleyball|handball|snooker",
        )

        /** Soccer labels get a second look on the all-soccer scoreboard. */
        fun looksLikeSoccer(label: String): Boolean = !notSoccer.containsMatchIn(label.lowercase())
    }
}
