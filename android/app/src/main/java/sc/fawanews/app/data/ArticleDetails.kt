package sc.fawanews.app.data

data class ArticleDetails(
    val title: String,
    val paragraphs: List<String>,
    val imageUrl: String?,
    val pageUrl: String,
    val streamUrls: List<String> = emptyList(),
)
