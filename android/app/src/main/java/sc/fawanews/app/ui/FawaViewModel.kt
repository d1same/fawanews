package sc.fawanews.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sc.fawanews.app.data.FawaRepository
import sc.fawanews.app.data.ScheduleItem
import sc.fawanews.app.data.ScoreRepository
import sc.fawanews.app.data.filterScheduleBySearch
import sc.fawanews.app.data.filterScoresBySearch
import sc.fawanews.app.data.leagueLabel

class FawaViewModel(
    private val repository: FawaRepository,
    private val scoreRepository: ScoreRepository,
) : ViewModel() {

    private val _scheduleState = MutableStateFlow(ScheduleUiState())
    val scheduleState: StateFlow<ScheduleUiState> = _scheduleState.asStateFlow()

    private val _scoresState = MutableStateFlow(ScoresUiState())
    val scoresState: StateFlow<ScoresUiState> = _scoresState.asStateFlow()

    private val _playerState = MutableStateFlow<PlayerUiState?>(null)
    val playerState: StateFlow<PlayerUiState?> = _playerState.asStateFlow()

    private var scheduleRefreshJob: Job? = null
    private var streamRefreshJob: Job? = null
    private var scoresRefreshJob: Job? = null

    init {
        refreshSchedule(showSpinner = true)
        startScheduleAutoRefresh()
    }

    fun onAppResumed() {
        refreshSchedule(showSpinner = false)
        startScheduleAutoRefresh()
        if (_scheduleState.value.selectedTab == HomeTab.SCORES) {
            refreshScores(showSpinner = false)
            startScoresAutoRefresh()
        }
        val player = _playerState.value
        if (player?.pagePath != null && player.isLiveEvent) {
            refreshPlayerStream(showOverlay = false)
            if (player.playback != StreamPlayback.COMING_SOON) startStreamAutoRefresh()
        }
    }

    /** Nothing refreshes while the app is out of sight. */
    fun onAppStopped() {
        scheduleRefreshJob?.cancel()
        scoresRefreshJob?.cancel()
        stopStreamAutoRefresh()
    }

    fun onTvItemFocused(itemId: String) {
        _scheduleState.update { it.copy(tvLastFocusedItemId = itemId) }
    }

    fun onTvMenuFocused(menuKey: String) {
        _scheduleState.update { state ->
            val movedInMenu =
                state.tvRestoreContentOnMenuRight &&
                    menuKey != state.tvMenuRightRestoreAnchorKey
            state.copy(
                tvLastFocusedMenuKey = menuKey,
                tvRestoreContentOnMenuRight =
                    if (movedInMenu) false else state.tvRestoreContentOnMenuRight,
            )
        }
    }

    fun onTvLeftFromContent() {
        _scheduleState.update { state ->
            state.copy(
                tvRestoreContentOnMenuRight = true,
                tvMenuRightRestoreAnchorKey = state.tvLastFocusedMenuKey,
            )
        }
    }

    fun onTvScoreGameFocused(gameId: String) {
        _scoresState.update { it.copy(tvLastFocusedGameId = gameId) }
    }

    fun focusContentFromMenuTv() {
        val schedule = _scheduleState.value
        val restore = schedule.tvRestoreContentOnMenuRight
        val request = when (schedule.selectedTab) {
            HomeTab.SCORES -> {
                val games =
                    _scoresState.value.visibleGames(schedule.searchQuery)
                val id = _scoresState.value.tvLastFocusedGameId
                if (restore && id != null && games.any { it.id == id }) {
                    TvFocusRequest(TvFocusTarget.RestoreItem, id)
                } else {
                    TvFocusRequest(TvFocusTarget.FirstContentItem)
                }
            }
            HomeTab.LIVE -> {
                val id = schedule.tvLastFocusedItemId
                if (restore && id != null && schedule.filteredLiveItems.any { it.id == id }) {
                    TvFocusRequest(TvFocusTarget.RestoreItem, id)
                } else {
                    TvFocusRequest(TvFocusTarget.FirstContentItem)
                }
            }
            HomeTab.NEWS -> {
                val id = schedule.tvLastFocusedItemId
                if (restore && id != null && schedule.filteredNewsItems.any { it.id == id }) {
                    TvFocusRequest(TvFocusTarget.RestoreItem, id)
                } else {
                    TvFocusRequest(TvFocusTarget.FirstContentItem)
                }
            }
        }
        _scheduleState.update {
            it.copy(
                tvFocusRequest = request,
                tvRestoreContentOnMenuRight = false,
            )
        }
    }

    fun clearTvFocusRequest() {
        _scheduleState.update { it.copy(tvFocusRequest = null) }
    }

    fun requestTvFocusFirstContent() {
        _scheduleState.update {
            it.copy(tvFocusRequest = TvFocusRequest(TvFocusTarget.FirstContentItem))
        }
    }

    fun selectTab(tab: HomeTab) {
        _scheduleState.update {
            it.copy(selectedTab = tab, tvRestoreContentOnMenuRight = false)
        }
        if (tab == HomeTab.SCORES) {
            refreshScores(showSpinner = _scoresState.value.games.isEmpty())
            startScoresAutoRefresh()
        } else {
            scoresRefreshJob?.cancel()
        }
    }

    fun selectScoreLeague(league: String?) {
        _scheduleState.update { it.copy(tvRestoreContentOnMenuRight = false) }
        _scoresState.update { it.copy(selectedLeague = league) }
    }

    fun refreshScores(showSpinner: Boolean = true) {
        val matches = _scheduleState.value.liveItems
        viewModelScope.launch {
            if (showSpinner) {
                _scoresState.update { it.copy(isLoading = true, error = null) }
            }
            if (matches.isEmpty()) {
                _scoresState.update {
                    it.copy(
                        isLoading = false,
                        games = emptyList(),
                        selectedLeague = null,
                        lastUpdatedMillis = System.currentTimeMillis(),
                        error = null,
                    )
                }
                return@launch
            }
            runCatching { scoreRepository.fetchScoresForMatches(matches) }
                .onSuccess { games ->
                    _scoresState.update {
                        val league = it.selectedLeague?.takeIf { name ->
                            games.any { game -> game.leagueLabel == name }
                        }
                        it.copy(
                            isLoading = false,
                            games = games,
                            selectedLeague = league,
                            lastUpdatedMillis = System.currentTimeMillis(),
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _scoresState.update {
                        it.copy(
                            isLoading = false,
                            error = if (it.games.isEmpty()) error.message ?: "Scores unavailable" else it.error,
                        )
                    }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _scheduleState.update { state ->
            state.copy(
                searchQuery = query,
                filteredLiveItems = filterLive(state.liveItems, state.selectedCategory, query),
                filteredNewsItems = filterNews(state.newsItems, state.selectedCategory, query),
            )
        }
    }

    fun selectCategory(category: String?) {
        _scheduleState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredLiveItems = filterLive(state.liveItems, category, state.searchQuery),
                filteredNewsItems = filterNews(state.newsItems, category, state.searchQuery),
                tvRestoreContentOnMenuRight = false,
            )
        }
    }

    fun refreshSchedule(showSpinner: Boolean = true) {
        viewModelScope.launch {
            if (showSpinner) {
                _scheduleState.update { it.copy(isLoading = true, error = null) }
            }
            runCatching { repository.fetchSchedule() }
                .onSuccess { items -> applySchedule(items) }
                .onFailure { error ->
                    _scheduleState.update {
                        it.copy(
                            isLoading = false,
                            error = if (it.items.isEmpty()) error.message ?: "Unknown error" else it.error,
                        )
                    }
                }
        }
    }

    fun openScoreGame(game: sc.fawanews.app.data.ScoreGame) {
        val path = game.streamPagePath ?: return
        val item = _scheduleState.value.liveItems.firstOrNull { live ->
            live.pagePath == path || live.id == path
        } ?: return
        openItem(item)
        _scheduleState.update { it.copy(tvLastFocusedItemId = game.id) }
        _scoresState.update { it.copy(tvLastFocusedGameId = game.id) }
    }

    fun openItem(item: ScheduleItem) {
        _scheduleState.update { it.copy(tvLastFocusedItemId = item.id) }
        val schedule = _scheduleState.value
        val openAsArticle =
            !item.isLive ||
                schedule.selectedTab == HomeTab.NEWS ||
                schedule.newsItems.any { it.id == item.id }
        if (openAsArticle) {
            openArticleItem(item)
            return
        }
        viewModelScope.launch {
            _playerState.value = PlayerUiState(
                title = item.title,
                pagePath = item.pagePath,
                streamUrls = emptyList(),
                activeStreamIndex = 0,
                isLiveEvent = true,
                isLoading = true,
                playback = StreamPlayback.LOADING,
            )
            loadStreamsForPlayer(preserveIndex = false, showRefreshing = false)
            val state = _playerState.value
            if (state?.playback == StreamPlayback.COMING_SOON && state.streamUrls.isEmpty()) {
                loadArticleForPlayer(item.pagePath)
            } else if (state?.playback != StreamPlayback.COMING_SOON) {
                startStreamAutoRefresh()
            }
        }
    }

    private fun openArticleItem(item: ScheduleItem) {
        stopStreamAutoRefresh()
        _scheduleState.update { state ->
            state.copy(
                selectedTab = HomeTab.NEWS,
                tvLastFocusedItemId = item.id,
                tvLastFocusedMenuKey = TvMenuFocusKeys.NEWS,
                filteredNewsItems = filterNews(state.newsItems, state.selectedCategory, state.searchQuery),
            )
        }
        viewModelScope.launch {
            _playerState.value = PlayerUiState(
                title = item.title,
                pagePath = item.pagePath,
                isLiveEvent = false,
                isLoading = true,
            )
            loadArticleForPlayer(item.pagePath)
        }
    }

    fun closePlayer() {
        stopStreamAutoRefresh()
        val restoreId = _scheduleState.value.tvLastFocusedItemId
        _scheduleState.update { state ->
            state.copy(
                tvFocusRequest = TvFocusRequest(
                    target = TvFocusTarget.RestoreItem,
                    itemId = restoreId,
                ),
            )
        }
        _playerState.value = null
    }

    fun selectStream(index: Int) {
        val current = _playerState.value ?: return
        if (index !in current.streamUrls.indices) return
        _playerState.value = current.copy(
            activeStreamIndex = index,
            playback = StreamPlayback.LOADING,
            error = null,
            videoQualityLabel = null,
        )
    }

    fun refreshPlayerStream(showOverlay: Boolean = true) {
        val pagePath = _playerState.value?.pagePath ?: return
        viewModelScope.launch {
            if (showOverlay) {
                _playerState.update { it?.copy(isRefreshingStream = true) }
            }
            loadStreamsForPlayer(preserveIndex = true, showRefreshing = showOverlay)
        }
    }

    fun onPlayerReady(videoQualityLabel: String?) {
        _playerState.update {
            it?.copy(
                playback = StreamPlayback.PLAYING,
                videoQualityLabel = videoQualityLabel,
                error = null,
            )
        }
    }

    fun onPlayerError() {
        val state = _playerState.value ?: return
        val nextIndex = state.activeStreamIndex + 1
        if (nextIndex < state.streamUrls.size) {
            selectStream(nextIndex)
            return
        }
        val lastFetch = state.lastStreamFetchMillis ?: 0L
        if (state.pagePath != null && System.currentTimeMillis() - lastFetch > RELINK_AFTER_ERROR_MS) {
            _playerState.update {
                it?.copy(playAttempt = it.playAttempt + 1, playback = StreamPlayback.LOADING)
            }
            viewModelScope.launch {
                loadStreamsForPlayer(preserveIndex = false, showRefreshing = true)
            }
            return
        }
        _playerState.update {
            it?.copy(
                playback = StreamPlayback.COMING_SOON,
                error = "Stream unavailable right now",
            )
        }
    }

    private suspend fun loadArticleForPlayer(pagePath: String) {
        runCatching { repository.fetchArticleDetails(pagePath) }
            .onSuccess { article ->
                val textFirst = article.paragraphs.isNotEmpty()
                val useStreamOnly = !textFirst && article.streamUrls.isNotEmpty()
                _playerState.value = PlayerUiState(
                    title = article.title,
                    pagePath = pagePath,
                    streamUrls = if (useStreamOnly) article.streamUrls else emptyList(),
                    activeStreamIndex = 0,
                    isLiveEvent = false,
                    isLoading = false,
                    playback = if (useStreamOnly) StreamPlayback.LOADING else StreamPlayback.PLAYING,
                    articleParagraphs = article.paragraphs,
                    articleImageUrl = article.imageUrl,
                    articleUrl = article.pageUrl,
                )
            }
            .onFailure { error ->
                _playerState.update {
                    it?.copy(
                        isLoading = false,
                        error = error.message ?: "Article unavailable",
                    )
                }
            }
    }

    private suspend fun loadStreamsForPlayer(preserveIndex: Boolean, showRefreshing: Boolean) {
        val current = _playerState.value ?: return
        val pagePath = current.pagePath ?: return
        runCatching { repository.fetchStreams(pagePath) }
            .onSuccess { details ->
                if (details.streamUrls.isEmpty()) {
                    loadArticleForPlayer(pagePath)
                    if (_playerState.value?.articleParagraphs?.isNotEmpty() == true) {
                        return@onSuccess
                    }
                }
                applyStreamDetails(details, preserveIndex)
            }
            .onFailure { error ->
                _playerState.update {
                    it?.copy(
                        isLoading = false,
                        isRefreshingStream = false,
                        playback = if (it.isLiveEvent) StreamPlayback.COMING_SOON else it.playback,
                        error = error.message,
                    )
                }
            }
        if (showRefreshing) {
            _playerState.update { it?.copy(isRefreshingStream = false) }
        }
    }

    private fun applySchedule(items: List<ScheduleItem>) {
        val live = items.filter { it.isLive }
        val categories = live.map { it.leagueLabel() }.distinct().sorted()
        _scheduleState.update { state ->
            val category = state.selectedCategory?.takeIf { it in categories }
            state.copy(
                isLoading = false,
                items = items,
                liveItems = live,
                newsItems = items.filter { !it.isLive },
                liveCategories = categories,
                selectedCategory = category,
                filteredLiveItems = filterLive(live, category, state.searchQuery),
                filteredNewsItems = filterNews(
                    items.filter { !it.isLive },
                    category,
                    state.searchQuery,
                ),
                lastUpdatedMillis = System.currentTimeMillis(),
                error = null,
            )
        }
        refreshScores(showSpinner = false)
    }

    private fun filterLive(
        live: List<ScheduleItem>,
        category: String?,
        query: String,
    ): List<ScheduleItem> {
        val byCategory = if (category == null) live else live.filter { it.leagueLabel() == category }
        return byCategory.filterScheduleBySearch(query)
    }

    private fun filterNews(
        news: List<ScheduleItem>,
        category: String?,
        query: String,
    ): List<ScheduleItem> {
        val byCategory = if (category == null) news else news.filter { it.leagueLabel() == category }
        return byCategory.filterScheduleBySearch(query)
    }

    private fun applyStreamDetails(
        details: sc.fawanews.app.data.StreamDetails,
        preserveIndex: Boolean,
    ) {
        val current = _playerState.value
        val urls = details.streamUrls.distinct()
        if (urls.isEmpty()) {
            _playerState.value = PlayerUiState(
                title = details.title,
                pagePath = current?.pagePath,
                streamUrls = emptyList(),
                activeStreamIndex = 0,
                isLiveEvent = current?.isLiveEvent == true,
                articleUrl = details.pageUrl,
                isLoading = false,
                isRefreshingStream = false,
                playback = StreamPlayback.COMING_SOON,
                lastStreamFetchMillis = System.currentTimeMillis(),
            )
            return
        }
        val index = when {
            preserveIndex && current != null -> {
                val prev = current.streamUrls.getOrNull(current.activeStreamIndex)
                urls.indexOf(prev).takeIf { it >= 0 } ?: 0
            }
            else -> 0
        }
        _playerState.value = PlayerUiState(
            title = details.title,
            pagePath = current?.pagePath,
            streamUrls = urls,
            activeStreamIndex = index,
            isLiveEvent = current?.isLiveEvent == true,
            articleUrl = null,
            isLoading = false,
            isRefreshingStream = false,
            playback = StreamPlayback.LOADING,
            lastStreamFetchMillis = System.currentTimeMillis(),
            playAttempt = current?.playAttempt ?: 0,
        )
    }

    private fun startScoresAutoRefresh() {
        scoresRefreshJob?.cancel()
        scoresRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(SCORES_REFRESH_MS)
                if (_scheduleState.value.selectedTab == HomeTab.SCORES && _playerState.value == null) {
                    refreshScores(showSpinner = false)
                }
            }
        }
    }

    private fun startScheduleAutoRefresh() {
        scheduleRefreshJob?.cancel()
        scheduleRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(SCHEDULE_REFRESH_MS)
                if (_playerState.value == null) {
                    refreshSchedule(showSpinner = false)
                }
            }
        }
    }

    private fun startStreamAutoRefresh() {
        streamRefreshJob?.cancel()
        streamRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(STREAM_REFRESH_MS)
                if (_playerState.value?.pagePath != null &&
                    _playerState.value?.playback != StreamPlayback.COMING_SOON
                ) {
                    refreshPlayerStream(showOverlay = false)
                }
            }
        }
    }

    private fun stopStreamAutoRefresh() {
        streamRefreshJob?.cancel()
        streamRefreshJob = null
    }

    data class ScheduleUiState(
        val isLoading: Boolean = true,
        val items: List<ScheduleItem> = emptyList(),
        val liveItems: List<ScheduleItem> = emptyList(),
        val newsItems: List<ScheduleItem> = emptyList(),
        val filteredNewsItems: List<ScheduleItem> = emptyList(),
        val filteredLiveItems: List<ScheduleItem> = emptyList(),
        val searchQuery: String = "",
        val liveCategories: List<String> = emptyList(),
        val selectedTab: HomeTab = HomeTab.LIVE,
        val selectedCategory: String? = null,
        val lastUpdatedMillis: Long? = null,
        val error: String? = null,
        val tvLastFocusedItemId: String? = null,
        val tvLastFocusedMenuKey: String = TvMenuFocusKeys.LIVE,
        val tvRestoreContentOnMenuRight: Boolean = false,
        val tvMenuRightRestoreAnchorKey: String = TvMenuFocusKeys.LIVE,
        val tvFocusRequest: TvFocusRequest? = null,
    )

    data class PlayerUiState(
        val title: String,
        val pagePath: String? = null,
        val streamUrls: List<String> = emptyList(),
        val activeStreamIndex: Int = 0,
        val isLiveEvent: Boolean = true,
        val articleUrl: String? = null,
        val articleParagraphs: List<String> = emptyList(),
        val articleImageUrl: String? = null,
        val isLoading: Boolean = false,
        val isRefreshingStream: Boolean = false,
        val playback: StreamPlayback = StreamPlayback.LOADING,
        val videoQualityLabel: String? = null,
        val error: String? = null,
        val lastStreamFetchMillis: Long? = null,
        val playAttempt: Int = 0,
    ) {
        val streamUrl: String? get() = streamUrls.getOrNull(activeStreamIndex)
    }

    data class ScoresUiState(
        val selectedLeague: String? = null,
        val games: List<sc.fawanews.app.data.ScoreGame> = emptyList(),
        val isLoading: Boolean = false,
        val lastUpdatedMillis: Long? = null,
        val error: String? = null,
        val tvLastFocusedGameId: String? = null,
    ) {
        val leagues: List<String>
            get() = games.map { it.leagueLabel }.distinct().sorted()

        fun visibleGames(query: String): List<sc.fawanews.app.data.ScoreGame> =
            games
                .filter { selectedLeague == null || it.leagueLabel == selectedLeague }
                .filterScoresBySearch(query)
    }

    class Factory(
        private val repository: FawaRepository,
        private val scoreRepository: ScoreRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FawaViewModel(repository, scoreRepository) as T
        }
    }

    companion object {
        private const val SCHEDULE_REFRESH_MS = 3 * 60 * 1000L
        private const val STREAM_REFRESH_MS = 2 * 60 * 1000L
        private const val RELINK_AFTER_ERROR_MS = 20 * 1000L
        private const val SCORES_REFRESH_MS = 90 * 1000L
    }
}
