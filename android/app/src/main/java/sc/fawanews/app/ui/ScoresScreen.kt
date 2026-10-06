package sc.fawanews.app.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import sc.fawanews.app.R
import sc.fawanews.app.ui.components.LastUpdatedLabel
import sc.fawanews.app.ui.components.ScoreGameCard

@Composable
fun ScoresScreen(
    state: FawaViewModel.ScoresUiState,
    searchQuery: String,
    onLeagueSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    forTv: Boolean = false,
    menuFocusRequester: FocusRequester? = null,
    firstCardFocusRequester: FocusRequester? = null,
    searchUpFocusRequester: FocusRequester? = null,
    focusRequest: TvFocusRequest? = null,
    onFocusRequestHandled: () -> Unit = {},
    onLeftToMenu: (() -> Unit)? = null,
    onScoreGameFocused: (String) -> Unit = {},
) {
    val games = state.visibleGames(searchQuery)
    val scoreColumns = if (forTv) 2 else 1
    val gameFocus = remember(games.map { it.id }) {
        games.associate { it.id to FocusRequester() }
    }

    suspend fun requestWithRetry(requester: FocusRequester): Boolean {
        repeat(8) { attempt ->
            withFrameMillis { }
            delay(40L + attempt * 60L)
            if (requester.requestFocus()) return true
        }
        return false
    }

    if (forTv) {
        LaunchedEffect(focusRequest?.target, focusRequest?.itemId, games.map { it.id }) {
            when (focusRequest?.target) {
                TvFocusTarget.RestoreItem -> {
                    val id = focusRequest.itemId
                    val index = if (id != null) games.indexOfFirst { it.id == id } else -1
                    if (index >= 0) {
                        val target =
                            if (index == 0 && firstCardFocusRequester != null) {
                                firstCardFocusRequester
                            } else {
                                gameFocus.getValue(games[index].id)
                            }
                        requestWithRetry(target)
                    } else if (games.isNotEmpty() && firstCardFocusRequester != null) {
                        requestWithRetry(firstCardFocusRequester)
                    }
                    delay(80)
                    onFocusRequestHandled()
                }
                TvFocusTarget.FirstContentItem -> {
                    if (firstCardFocusRequester != null && games.isNotEmpty()) {
                        delay(80)
                        requestWithRetry(firstCardFocusRequester)
                    }
                    onFocusRequestHandled()
                }
                TvFocusTarget.Menu, null -> Unit
            }
        }
    }
    Box(modifier.fillMaxSize()) {
        when {
            state.isLoading && state.games.isEmpty() -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            state.error != null && state.games.isEmpty() -> {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = if (forTv) 8.dp else 16.dp,
                        end = if (forTv) 8.dp else 16.dp,
                        top = if (forTv) 4.dp else 16.dp,
                        bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (!forTv) {
                        item {
                            Text(
                                stringResource(R.string.scores_source_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            )
                            LastUpdatedLabel(
                                updatedAtMillis = state.lastUpdatedMillis,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                            )
                            Row(
                                Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(
                                    selected = state.selectedLeague == null,
                                    onClick = { onLeagueSelect(null) },
                                    label = { Text("All today") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                )
                                state.leagues.forEach { league ->
                                    FilterChip(
                                        selected = state.selectedLeague == league,
                                        onClick = { onLeagueSelect(league) },
                                        label = { Text(league) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    if (games.isEmpty()) {
                        item {
                            Text(
                                if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.search_empty)
                                } else {
                                    stringResource(R.string.scores_empty)
                                },
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            )
                        }
                    } else {
                        games.chunked(scoreColumns).forEachIndexed { rowIndex, row ->
                            item(key = row.joinToString("-") { it.id }) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    row.forEachIndexed { colIndex, game ->
                                        val isFirstCard = rowIndex == 0 && colIndex == 0
                                        val cardFocus =
                                            if (isFirstCard && firstCardFocusRequester != null) {
                                                firstCardFocusRequester
                                            } else if (forTv) {
                                                gameFocus.getValue(game.id)
                                            } else {
                                                null
                                            }
                                        ScoreGameCard(
                                            game = game,
                                            compact = forTv,
                                            showLeague = false,
                                            onFocusChange = if (forTv) {
                                                { focused ->
                                                    if (focused) onScoreGameFocused(game.id)
                                                }
                                            } else {
                                                null
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .then(
                                                    if (cardFocus != null) {
                                                        Modifier.focusRequester(cardFocus)
                                                    } else {
                                                        Modifier
                                                    },
                                                )
                                                .then(
                                                    if (forTv && menuFocusRequester != null && colIndex == 0) {
                                                        Modifier
                                                            .onPreviewKeyEvent { event ->
                                                                if (
                                                                    event.type == KeyEventType.KeyDown &&
                                                                    event.key == Key.DirectionLeft
                                                                ) {
                                                                    onLeftToMenu?.invoke()
                                                                    true
                                                                } else {
                                                                    false
                                                                }
                                                            }
                                                            .focusProperties { left = menuFocusRequester }
                                                    } else {
                                                        Modifier
                                                    },
                                                )
                                                .then(
                                                    if (forTv && isFirstCard && searchUpFocusRequester != null) {
                                                        Modifier.focusProperties { up = searchUpFocusRequester }
                                                    } else {
                                                        Modifier
                                                    },
                                                ),
                                            focusable = forTv,
                                        )
                                    }
                                    repeat(scoreColumns - row.size) {
                                        Spacer(Modifier.weight(1f))
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
