package sc.fawanews.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import sc.fawanews.app.data.ScheduleItem
import sc.fawanews.app.ui.TvFocusRequest
import sc.fawanews.app.ui.TvFocusTarget

@Composable
fun TvEventGrid(
    items: List<ScheduleItem>,
    columns: Int,
    gap: Dp,
    menuFocus: FocusRequester,
    firstItemFocus: FocusRequester,
    searchUpFocus: FocusRequester,
    focusRequest: TvFocusRequest?,
    onItemClick: (ScheduleItem) -> Unit,
    onItemFocused: (String) -> Unit,
    onFocusRequestHandled: () -> Unit,
    onLeftToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val itemFocus = remember(items.map { it.id }) {
        items.associate { it.id to FocusRequester() }
    }

    suspend fun requestWithRetry(requester: FocusRequester): Boolean {
        repeat(8) { attempt ->
            withFrameMillis { }
            delay(40L + attempt * 60L)
            if (requester.requestFocus()) return true
        }
        return false
    }

    LaunchedEffect(focusRequest?.target, focusRequest?.itemId, items.map { it.id }) {
        when (focusRequest?.target) {
            TvFocusTarget.RestoreItem -> {
                val id = focusRequest.itemId
                val index = if (id != null) items.indexOfFirst { it.id == id } else -1
                if (index >= 0) {
                    val row = index / columns
                    scroll.animateScrollTo((row * 200).coerceAtMost(scroll.maxValue))
                    delay(150)
                    val target =
                        if (index == 0) firstItemFocus else itemFocus.getValue(items[index].id)
                    if (!requestWithRetry(target) && index != 0) {
                        requestWithRetry(firstItemFocus)
                    }
                } else if (items.isNotEmpty()) {
                    scroll.animateScrollTo(0)
                    requestWithRetry(firstItemFocus)
                }
                delay(80)
                onFocusRequestHandled()
            }
            TvFocusTarget.FirstContentItem -> {
                scroll.animateScrollTo(0)
                delay(80)
                requestWithRetry(firstItemFocus)
                onFocusRequestHandled()
            }
            TvFocusTarget.Menu, null -> Unit
        }
    }

    Column(
        modifier
            .verticalScroll(scroll)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        items.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEachIndexed { colIndex, item ->
                    val index = rowIndex * columns + colIndex
                    val requester = if (index == 0) firstItemFocus else itemFocus.getValue(item.id)
                    EventCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        compact = true,
                        onFocusChange = { focused ->
                            if (focused) onItemFocused(item.id)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(requester)
                            .onPreviewKeyEvent { event ->
                                if (
                                    colIndex == 0 &&
                                    event.type == KeyEventType.KeyDown &&
                                    event.key == Key.DirectionLeft
                                ) {
                                    onLeftToMenu()
                                    true
                                } else {
                                    false
                                }
                            }
                            .focusProperties {
                                if (colIndex == 0) {
                                    left = menuFocus
                                }
                                if (rowIndex == 0) {
                                    up = searchUpFocus
                                }
                            },
                    )
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
