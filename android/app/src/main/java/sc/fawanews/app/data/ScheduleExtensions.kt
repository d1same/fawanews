package sc.fawanews.app.data

private val timeSuffix = Regex("""\s+\d{1,2}:\d{2}$""")

fun ScheduleItem.leagueLabel(): String {
    val sub = subtitle ?: return "Other"
    return timeSuffix.replace(sub, "").trim().ifBlank { "Other" }
}
