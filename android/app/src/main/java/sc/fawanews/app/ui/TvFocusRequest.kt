package sc.fawanews.app.ui

enum class TvFocusTarget {
    Menu,
    FirstContentItem,
    RestoreItem,
}

data class TvFocusRequest(
    val target: TvFocusTarget,
    val itemId: String? = null,
)
