package sc.fawanews.app.data

data class ScheduleItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val pagePath: String,
    val isLive: Boolean,
)

data class StreamDetails(
    val title: String,
    val streamUrls: List<String>,
    val pageUrl: String,
)
