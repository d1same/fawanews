package sc.fawanews.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sc.fawanews.app.R
import sc.fawanews.app.data.ScheduleItem
import sc.fawanews.app.ui.TvLayout.contentPadding
import sc.fawanews.app.ui.TvLayout.gridColumns
import sc.fawanews.app.ui.TvLayout.gridGap
import sc.fawanews.app.ui.TvLayout.sideMenuWidth
import sc.fawanews.app.ui.components.FawaIconSizes
import sc.fawanews.app.ui.components.FawaSearchBar
import sc.fawanews.app.ui.components.LastUpdatedLabel
import sc.fawanews.app.ui.components.TvEventGrid
import sc.fawanews.app.ui.components.TvMenuItem
import sc.fawanews.app.ui.theme.PlexColors

@Composable
fun TvHomeScreen(
    state: FawaViewModel.ScheduleUiState,
    scoresState: FawaViewModel.ScoresUiState,
    onRefresh: () -> Unit,
    onScoresRefresh: () -> Unit,
    onTabSelect: (HomeTab) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onScoreLeagueSelect: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onItemClick: (ScheduleItem) -> Unit,
    onTvItemFocused: (String) -> Unit,
    onTvMenuFocused: (String) -> Unit,
    onFocusContentFromMenu: () -> Unit,
    onTvLeftFromContent: () -> Unit,
    onTvScoreGameFocused: (String) -> Unit,
    onClearFocusRequest: () -> Unit,
) {
    val listItems = when (state.selectedTab) {
        HomeTab.LIVE -> state.filteredLiveItems
        HomeTab.NEWS -> state.filteredNewsItems
        HomeTab.SCORES -> emptyList()
    }
    val isScores = state.selectedTab == HomeTab.SCORES
    val menuWidth = sideMenuWidth()
    val outerPad = contentPadding()
    val columns = gridColumns()
    val gap = gridGap()
    val menuFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    fun menuFocusFor(key: String): FocusRequester =
        menuFocusRequesters.getOrPut(key) { FocusRequester() }
    val menuReturnFocus = menuFocusFor(state.tvLastFocusedMenuKey)

    val searchMicFocus = remember { FocusRequester() }
    val gridFirstFocus = remember { FocusRequester() }
    val scoresFocus = remember { FocusRequester() }

    val returnToMenu: () -> Unit = {
        runCatching { menuReturnFocus.requestFocus() }
    }

    val scope = rememberCoroutineScope()

    suspend fun requestFocusWithRetry(requester: FocusRequester): Boolean {
        repeat(8) { attempt ->
            withFrameMillis { }
            delay(40L + attempt * 60L)
            if (requester.requestFocus()) return true
        }
        return false
    }

    fun moveRightFromMenu() {
        if (state.tvRestoreContentOnMenuRight) {
            onFocusContentFromMenu()
            return
        }
        when {
            isScores -> onFocusContentFromMenu()
            listItems.isNotEmpty() -> {
                scope.launch {
                    if (!requestFocusWithRetry(gridFirstFocus)) {
                        onFocusContentFromMenu()
                    }
                }
            }
            state.isLoading -> onFocusContentFromMenu()
            else -> {
                scope.launch {
                    if (!requestFocusWithRetry(searchMicFocus)) {
                        onFocusContentFromMenu()
                    }
                }
            }
        }
    }

    LaunchedEffect(
        state.tvFocusRequest,
        isScores,
        listItems.size,
        state.isLoading,
        scoresState.games.size,
    ) {
        when (state.tvFocusRequest?.target) {
            TvFocusTarget.Menu -> {
                delay(80)
                runCatching { menuReturnFocus.requestFocus() }
                onClearFocusRequest()
            }
            TvFocusTarget.FirstContentItem -> {
                if (isScores) {
                    val scoreGames = scoresState.visibleGames(state.searchQuery)
                    if (scoreGames.isEmpty() && scoresState.isLoading) {
                        return@LaunchedEffect
                    }
                    delay(80)
                    if (scoreGames.isNotEmpty()) {
                        requestFocusWithRetry(scoresFocus)
                    }
                } else {
                    if (listItems.isEmpty()) {
                        if (state.isLoading) return@LaunchedEffect
                        requestFocusWithRetry(searchMicFocus)
                    } else {
                        delay(60)
                        requestFocusWithRetry(gridFirstFocus)
                    }
                }
                onClearFocusRequest()
            }
            TvFocusTarget.RestoreItem -> {
                // Grid / scores list handles RestoreItem; focus request cleared there.
            }
            null -> Unit
        }
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(PlexColors.canvas)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Menu) {
                    if (isScores) onScoresRefresh() else onRefresh()
                    true
                } else {
                    false
                }
            },
    ) {
        TvSideMenu(
            selectedTab = state.selectedTab,
            categories = state.liveCategories,
            selectedCategory = state.selectedCategory,
            selectedScoreLeague = scoresState.selectedLeague,
            scoreLeagues = scoresState.leagues,
            onTabSelect = onTabSelect,
            onCategorySelect = onCategorySelect,
            onScoreLeagueSelect = onScoreLeagueSelect,
            menuFocusFor = ::menuFocusFor,
            onMenuFocused = onTvMenuFocused,
            onDpadRight = ::moveRightFromMenu,
            modifier = Modifier
                .width(menuWidth)
                .fillMaxHeight()
                .padding(outerPad),
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = outerPad, top = outerPad, bottom = outerPad)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Back, Key.Escape -> {
                            returnToMenu()
                            true
                        }
                        else -> false
                    }
                },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.fawanews_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(FawaIconSizes.brandMark)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.padding(start = 10.dp)) {
                    Text(
                        "FawaNews",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PlexColors.textPrimary,
                    )
                    Text(
                        text = when (state.selectedTab) {
                            HomeTab.LIVE -> stringResource(R.string.menu_live)
                            HomeTab.SCORES -> stringResource(R.string.menu_scores)
                            HomeTab.NEWS -> stringResource(R.string.menu_news)
                        } + state.selectedCategory?.let { " · $it" }.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = PlexColors.amber,
                    )
                }
                LastUpdatedLabel(
                    updatedAtMillis = if (isScores) scoresState.lastUpdatedMillis else state.lastUpdatedMillis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    textAlignEnd = true,
                )
            }
            FawaSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange,
                tvMode = true,
                menuFocusRequester = menuReturnFocus,
                micFocusRequester = searchMicFocus,
                gridFocusRequester = gridFirstFocus,
                downFocusRequester = if (isScores) scoresFocus else gridFirstFocus,
            )
            if (isScores) {
                ScoresScreen(
                    state = scoresState,
                    searchQuery = state.searchQuery,
                    onLeagueSelect = onScoreLeagueSelect,
                    modifier = Modifier.fillMaxSize(),
                    forTv = true,
                    menuFocusRequester = menuReturnFocus,
                    firstCardFocusRequester = scoresFocus,
                    searchUpFocusRequester = searchMicFocus,
                    focusRequest = state.tvFocusRequest,
                    onFocusRequestHandled = onClearFocusRequest,
                    onLeftToMenu = {
                        onTvLeftFromContent()
                        returnToMenu()
                    },
                    onScoreGameFocused = onTvScoreGameFocused,
                )
            } else when {
                state.error != null && state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.error_load), color = MaterialTheme.colorScheme.error)
                    }
                }
                state.isLoading && state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PlexColors.amber)
                    }
                }
                listItems.isEmpty() -> {
                    Text(
                        when {
                            state.searchQuery.isNotBlank() -> stringResource(R.string.search_empty)
                            state.selectedTab == HomeTab.LIVE -> stringResource(R.string.empty_live)
                            else -> stringResource(R.string.empty_news)
                        },
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
                else -> {
                    TvEventGrid(
                        items = listItems,
                        columns = columns,
                        gap = gap,
                        menuFocus = menuReturnFocus,
                        firstItemFocus = gridFirstFocus,
                        searchUpFocus = searchMicFocus,
                        focusRequest = state.tvFocusRequest,
                        onItemClick = onItemClick,
                        onItemFocused = onTvItemFocused,
                        onFocusRequestHandled = onClearFocusRequest,
                        onLeftToMenu = {
                            onTvLeftFromContent()
                            returnToMenu()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    // Cold start only — never steal focus after RestoreItem clears (that was jumping to the menu).
    LaunchedEffect(Unit) {
        if (state.tvFocusRequest?.target == TvFocusTarget.RestoreItem) return@LaunchedEffect
        delay(50)
        if (state.tvFocusRequest == null) {
            runCatching { menuFocusFor(TvMenuFocusKeys.LIVE).requestFocus() }
        }
    }
}

@Composable
private fun TvSideMenu(
    selectedTab: HomeTab,
    categories: List<String>,
    selectedCategory: String?,
    selectedScoreLeague: String?,
    scoreLeagues: List<String>,
    onTabSelect: (HomeTab) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onScoreLeagueSelect: (String?) -> Unit,
    menuFocusFor: (String) -> FocusRequester,
    onMenuFocused: (String) -> Unit,
    onDpadRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(6.dp),
        color = TvFocusStyles.menuPanel,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                stringResource(R.string.menu_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            TvMenuItem(
                label = stringResource(R.string.menu_live),
                selected = selectedTab == HomeTab.LIVE,
                onClick = { onTabSelect(HomeTab.LIVE) },
                modifier = Modifier.focusRequester(menuFocusFor(TvMenuFocusKeys.LIVE)),
                onDpadRight = onDpadRight,
                onFocused = { onMenuFocused(TvMenuFocusKeys.LIVE) },
            )
            TvMenuItem(
                label = stringResource(R.string.menu_scores),
                selected = selectedTab == HomeTab.SCORES,
                onClick = { onTabSelect(HomeTab.SCORES) },
                modifier = Modifier.focusRequester(menuFocusFor(TvMenuFocusKeys.SCORES)),
                onDpadRight = onDpadRight,
                onFocused = { onMenuFocused(TvMenuFocusKeys.SCORES) },
            )
            TvMenuItem(
                label = stringResource(R.string.menu_news),
                selected = selectedTab == HomeTab.NEWS,
                onClick = { onTabSelect(HomeTab.NEWS) },
                modifier = Modifier.focusRequester(menuFocusFor(TvMenuFocusKeys.NEWS)),
                onDpadRight = onDpadRight,
                onFocused = { onMenuFocused(TvMenuFocusKeys.NEWS) },
            )
            if (selectedTab == HomeTab.SCORES) {
                Text(
                    stringResource(R.string.menu_leagues),
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
                )
                val allKey = TvMenuFocusKeys.scoreLeague("all")
                TvMenuItem(
                    label = "All today",
                    selected = selectedScoreLeague == null,
                    onClick = { onScoreLeagueSelect(null) },
                    modifier = Modifier.focusRequester(menuFocusFor(allKey)),
                    onDpadRight = onDpadRight,
                    onFocused = { onMenuFocused(allKey) },
                )
                scoreLeagues.forEach { league ->
                    val key = TvMenuFocusKeys.scoreLeague(league)
                    TvMenuItem(
                        label = league,
                        selected = selectedScoreLeague == league,
                        onClick = { onScoreLeagueSelect(league) },
                        modifier = Modifier.focusRequester(menuFocusFor(key)),
                        onDpadRight = onDpadRight,
                        onFocused = { onMenuFocused(key) },
                    )
                }
            }
            if (categories.isNotEmpty() && selectedTab != HomeTab.SCORES) {
                Text(
                    stringResource(R.string.menu_sports),
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
                )
                TvMenuItem(
                    label = "All sports",
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    modifier = Modifier.focusRequester(menuFocusFor(TvMenuFocusKeys.CATEGORY_ALL)),
                    onDpadRight = onDpadRight,
                    onFocused = { onMenuFocused(TvMenuFocusKeys.CATEGORY_ALL) },
                )
                categories.forEach { category ->
                    val key = TvMenuFocusKeys.category(category)
                    TvMenuItem(
                        label = category,
                        selected = selectedCategory == category,
                        onClick = { onCategorySelect(category) },
                        modifier = Modifier.focusRequester(menuFocusFor(key)),
                        onDpadRight = onDpadRight,
                        onFocused = { onMenuFocused(key) },
                    )
                }
            }
        }
    }
}
