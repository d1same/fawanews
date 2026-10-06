package sc.fawanews.app.ui

object TvMenuFocusKeys {
    const val LIVE = "tab_live"
    const val SCORES = "tab_scores"
    const val NEWS = "tab_news"
    const val CATEGORY_ALL = "cat_all"

    fun category(name: String): String = "cat_$name"

    fun scoreLeague(name: String): String = "score_league_$name"
}
