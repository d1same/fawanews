package sc.fawanews.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import sc.fawanews.app.R
import sc.fawanews.app.player.NativePlayer
import sc.fawanews.app.ui.components.FawaLoadingScreen
import sc.fawanews.app.ui.components.FawaLoadingStyle

@Composable
fun PlayerScreen(
    state: FawaViewModel.PlayerUiState,
    tvMode: Boolean,
    onBack: () -> Unit,
    onRefreshStream: () -> Unit,
    onSelectStream: (Int) -> Unit,
    onPlayerReady: (String?) -> Unit,
    onPlayerError: () -> Unit,
    onPlayerFullscreenChange: (Boolean) -> Unit = {},
) {
    BackHandler(onBack = onBack)

    when {
        state.isLoading -> {
            FawaLoadingScreen(
                tvMode = tvMode,
                style = if (state.isLiveEvent) FawaLoadingStyle.Stream else FawaLoadingStyle.AppOpen,
            )
        }
        !state.isLiveEvent -> {
            if (state.error != null && state.articleParagraphs.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        state.error ?: stringResource(R.string.article_load_error),
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                }
            } else if (state.streamUrl != null && state.articleParagraphs.isEmpty()) {
                val streamUrl = state.streamUrl!!
                NativePlayer(
                    title = state.title,
                    streamUrl = streamUrl,
                    streamUrls = state.streamUrls,
                    activeStreamIndex = state.activeStreamIndex,
                    isRefreshing = state.isRefreshingStream,
                    videoQualityLabel = state.videoQualityLabel,
                    onBack = onBack,
                    onRefreshStream = onRefreshStream,
                    onSelectStream = onSelectStream,
                    onPlayerReady = onPlayerReady,
                    onPlayerError = onPlayerError,
                    onFullscreenChange = onPlayerFullscreenChange,
                )
            } else {
                ArticleReaderScreen(
                    title = state.title,
                    paragraphs = state.articleParagraphs,
                    imageUrl = state.articleImageUrl,
                    tvMode = tvMode,
                    onBack = onBack,
                )
            }
        }
        state.playback == StreamPlayback.COMING_SOON -> {
            ComingSoonScreen(
                title = state.title,
                onBack = onBack,
                onRefresh = onRefreshStream,
                isRefreshing = state.isRefreshingStream,
                tvMode = tvMode,
            )
        }
        state.streamUrl != null -> {
            val streamUrl = state.streamUrl!!
            NativePlayer(
                title = state.title,
                streamUrl = streamUrl,
                streamUrls = state.streamUrls,
                activeStreamIndex = state.activeStreamIndex,
                isRefreshing = state.isRefreshingStream,
                videoQualityLabel = state.videoQualityLabel,
                onBack = onBack,
                onRefreshStream = onRefreshStream,
                onSelectStream = onSelectStream,
                onPlayerReady = onPlayerReady,
                onPlayerError = onPlayerError,
                onFullscreenChange = onPlayerFullscreenChange,
            )
        }
        else -> {
            ComingSoonScreen(
                title = state.title,
                onBack = onBack,
                onRefresh = onRefreshStream,
                isRefreshing = state.isRefreshingStream,
                tvMode = tvMode,
            )
        }
    }
}
