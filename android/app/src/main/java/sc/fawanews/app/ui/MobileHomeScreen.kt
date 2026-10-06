package sc.fawanews.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sc.fawanews.app.R
import sc.fawanews.app.data.ScheduleItem
import sc.fawanews.app.ui.components.EventCard
import sc.fawanews.app.ui.components.FawaIcon
import sc.fawanews.app.ui.components.FawaIconSizes
import sc.fawanews.app.ui.components.FawaSearchBar
import sc.fawanews.app.ui.components.LastUpdatedLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileHomeScreen(
    state: FawaViewModel.ScheduleUiState,
    scoresState: FawaViewModel.ScoresUiState,
    onRefresh: () -> Unit,
    onScoresRefresh: () -> Unit,
    onTabSelect: (HomeTab) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onScoreLeagueSelect: (sc.fawanews.app.data.ScoreLeague) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onItemClick: (ScheduleItem) -> Unit,
) {
    val listItems = when (state.selectedTab) {
        HomeTab.LIVE -> state.filteredLiveItems
        HomeTab.NEWS -> state.filteredNewsItems
        HomeTab.SCORES -> emptyList()
    }
    val isScores = state.selectedTab == HomeTab.SCORES
    val refreshing = if (isScores) scoresState.isLoading else state.isLoading
    val gridCellMin = MobileLayout.eventGridMinCellSize()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MobileNavigationDrawer(
                selectedTab = state.selectedTab,
                liveCategories = state.liveCategories,
                selectedCategory = state.selectedCategory,
                selectedScoreLeague = scoresState.selectedLeague,
                onTabSelect = onTabSelect,
                onCategorySelect = onCategorySelect,
                onScoreLeagueSelect = onScoreLeagueSelect,
                onNavigate = closeDrawer,
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(FawaIconSizes.touchTarget),
                        ) {
                            FawaIcon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.open_menu),
                            )
                        }
                    },
                    title = {
                        Text(
                            when (state.selectedTab) {
                                HomeTab.LIVE -> stringResource(R.string.menu_live)
                                HomeTab.SCORES -> stringResource(R.string.menu_scores)
                                HomeTab.NEWS -> stringResource(R.string.menu_news)
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { if (isScores) onScoresRefresh() else onRefresh() },
                            modifier = Modifier.size(FawaIconSizes.touchTarget),
                        ) {
                            FawaIcon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                FawaSearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChange,
                )
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = { if (isScores) onScoresRefresh() else onRefresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                if (isScores) {
                    ScoresScreen(
                        state = scoresState,
                        searchQuery = state.searchQuery,
                        onLeagueSelect = onScoreLeagueSelect,
                    )
                } else when {
                    state.error != null && state.items.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.error_load),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(24.dp),
                            )
                        }
                    }
                    state.isLoading && state.items.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = gridCellMin),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 4.dp,
                                bottom = 24.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    LastUpdatedLabel(
                                        updatedAtMillis = state.lastUpdatedMillis,
                                    )
                                    Text(
                                        stringResource(R.string.domain_notice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                    )
                                    state.selectedCategory?.let { category ->
                                        Text(
                                            stringResource(R.string.filter_active, category),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            if (listItems.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                    if (state.searchQuery.isNotBlank()) {
                                        stringResource(R.string.search_empty)
                                    } else if (state.selectedTab == HomeTab.LIVE) {
                                        stringResource(R.string.empty_live)
                                    } else {
                                        stringResource(R.string.empty_news)
                                    },
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    )
                                }
                            } else {
                                items(listItems, key = { it.id }) { item ->
                                    EventCard(
                                        item = item,
                                        onClick = { onItemClick(item) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
