package sc.fawanews.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class ScoreRepository(
    private val client: OkHttpClient = defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun fetchScores(league: ScoreLeague): List<ScoreGame> = withContext(Dispatchers.IO) {
        val url = "https://site.api.espn.com/apis/site/v2/sports/${league.espnPath}/scoreboard"
        val body = getJson(url)
        parseScoreboard(body, league.label)
    }

    /**
     * Leagues come from today's live list. Each of those leagues gets the full scoreboard,
     * including games the site is not streaming.
     */
    suspend fun fetchScoresForMatches(matches: List<ScheduleItem>): List<ScoreGame> =
        withContext(Dispatchers.IO) {
            val unique = dedupeMatchups(matches)
            val boards = mutableMapOf<ScoreLeague, List<ScoreGame>>()
            for (league in unique.mapNotNull { ScoreLeague.fromSiteLabel(it.leagueLabel()) }.distinct()) {
                boards[league] = runCatching { fetchScores(league) }.getOrDefault(emptyList())
            }
            scoresForLiveDay(unique, boards)
        }

    private fun getJson(url: String): JsonObject {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", StreamRequestHeaders.USER_AGENT)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Scores HTTP ${response.code}")
            val text = response.body?.string().orEmpty()
            return json.parseToJsonElement(text).jsonObject
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

        fun parseScoreboard(root: JsonObject, leagueLabel: String): List<ScoreGame> {
            val events = root["events"]?.jsonArray ?: return emptyList()
            return events.mapNotNull { eventEl -> parseEvent(eventEl, leagueLabel) }
        }

        private fun parseEvent(eventEl: JsonElement, leagueLabel: String): ScoreGame? {
            val event = eventEl.jsonObject
            val id = event.string("id") ?: return null
            val competition = event["competitions"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
            val competitors = competition["competitors"]?.jsonArray ?: return null
            val home = competitors.firstOrNull { it.jsonObject.string("homeAway") == "home" }?.jsonObject
            val away = competitors.firstOrNull { it.jsonObject.string("homeAway") == "away" }?.jsonObject
            if (home == null || away == null) return null

            val status = event["status"]?.jsonObject ?: competition["status"]?.jsonObject
            val state = status?.obj("type")?.string("state").orEmpty()
            val shortDetail = status?.obj("type")?.string("shortDetail")
                ?: status?.string("shortDetail")
                ?: "Scheduled"
            val isLive = state == "in"
            val venue = competition.obj("venue")
            val venueName = venue?.string("fullName")
            val venueLocation = venue?.obj("address")?.let { address ->
                listOfNotNull(address.string("city"), address.string("state"))
                    .joinToString(", ")
                    .takeIf { it.isNotBlank() }
            }
            val broadcastLabel = competition.parseBroadcast()
            val startTimeLabel = event.formatStartTime(state)

            return ScoreGame(
                id = id,
                leagueLabel = leagueLabel,
                homeTeam = home.teamName(),
                awayTeam = away.teamName(),
                homeScore = home.displayScore(),
                awayScore = away.displayScore(),
                homeLogoUrl = home.teamLogo(),
                awayLogoUrl = away.teamLogo(),
                homeRecord = home.overallRecord(),
                awayRecord = away.overallRecord(),
                statusLabel = shortDetail,
                isLive = isLive,
                venueName = venueName,
                venueLocation = venueLocation,
                broadcastLabel = broadcastLabel,
                startTimeLabel = startTimeLabel,
            )
        }

        private fun JsonObject.parseBroadcast(): String? {
            val names = jsonArray("broadcasts")
                ?.flatMap { broadcast ->
                    broadcast.jsonObject["names"]?.jsonArray.orEmpty().mapNotNull {
                        it.jsonPrimitive.contentOrNull
                    }
                }
                ?.distinct()
                .orEmpty()
            return names.take(2).joinToString(" · ").takeIf { it.isNotBlank() }
        }

        private fun JsonObject.formatStartTime(state: String): String? {
            if (state == "in" || state == "post") return null
            val raw = string("date") ?: return null
            return runCatching {
                val instant = Instant.parse(raw)
                DateTimeFormatter.ofPattern("EEE h:mm a", Locale.US)
                    .withZone(ZoneId.systemDefault())
                    .format(instant)
            }.getOrNull()
        }

        private fun JsonObject.overallRecord(): String? =
            jsonArray("records")
                ?.firstOrNull { it.jsonObject.string("type") == "total" }
                ?.jsonObject
                ?.string("summary")
                ?.takeIf { it.isNotBlank() && it != "0-0" }

        private fun JsonObject.teamName(): String =
            obj("team")?.string("shortDisplayName")
                ?: obj("team")?.string("displayName")
                ?: "Team"

        private fun JsonObject.teamLogo(): String? =
            obj("team")?.jsonArray("logos")?.firstOrNull()?.jsonObject?.string("href")
                ?: obj("team")?.string("logo")

        private fun JsonObject.displayScore(): String {
            val score = string("score")?.takeIf { it.isNotBlank() }
            if (score != null) return score
            return obj("score")?.string("displayValue") ?: "-"
        }

        private fun JsonObject.string(key: String): String? =
            this[key]?.jsonPrimitive?.contentOrNull

        private fun JsonObject.obj(key: String): JsonObject? =
            this[key]?.jsonObject

        private fun JsonObject.jsonArray(key: String): JsonArray? =
            this[key]?.jsonArray
    }
}
