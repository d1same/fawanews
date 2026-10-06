package sc.fawanews.app.player

import android.view.KeyEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as MediaUiR
import sc.fawanews.app.R
import sc.fawanews.app.isTelevision
import sc.fawanews.app.ui.components.FawaIcon
import sc.fawanews.app.ui.components.FawaIconSizes
import sc.fawanews.app.ui.theme.PlexColors
import sc.fawanews.app.ui.tvClickOnCenter

@OptIn(UnstableApi::class)
@Composable
fun NativePlayer(
    title: String,
    streamUrl: String,
    streamUrls: List<String>,
    activeStreamIndex: Int,
    isRefreshing: Boolean,
    videoQualityLabel: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onRefreshStream: () -> Unit = {},
    onSelectStream: (Int) -> Unit = {},
    onPlayerReady: (String?) -> Unit = {},
    onPlayerError: () -> Unit = {},
    onFullscreenChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var isFullscreen by remember { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    val preferTvQuality = context.isTelevision()
    val backInteraction = remember { MutableInteractionSource() }

    DisposableEffect(Unit) {
        onDispose {
            onFullscreenChange(false)
        }
    }

    BackHandler(enabled = !preferTvQuality && isFullscreen) {
        isFullscreen = false
        playerViewRef?.setFullscreenButtonState(false)
        onFullscreenChange(false)
    }

    val exoPlayer = remember(preferTvQuality) {
        FawaPlayerFactory.create(context, preferTvQuality = preferTvQuality)
    }
    var playbackState by remember(exoPlayer) { mutableIntStateOf(exoPlayer.playbackState) }

    LaunchedEffect(streamUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                if (preferTvQuality) exoPlayer.preferHighestVideo()
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    if (preferTvQuality) exoPlayer.preferHighestVideo()
                    onPlayerReady(exoPlayer.videoFormat?.qualityLabel())
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlayerError()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK &&
                    event.nativeKeyEvent.action == KeyEvent.ACTION_UP
                ) {
                    onBack()
                    true
                } else {
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        playerViewRef?.showController()
                    }
                    false
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    controllerShowTimeoutMs = 3_000
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = visibility == View.VISIBLE
                        },
                    )
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowRewindButton(false)
                    setShowFastForwardButton(false)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setArtworkDisplayMode(PlayerView.ARTWORK_DISPLAY_MODE_OFF)
                    if (preferTvQuality) {
                        setShowSubtitleButton(false)
                    } else {
                        setFullscreenButtonClickListener { enteringFullscreen ->
                            isFullscreen = enteringFullscreen
                            onFullscreenChange(enteringFullscreen)
                        }
                    }
                    trimPlayerController(this, preferTvQuality)
                    playerViewRef = this
                }
            },
            update = { view ->
                view.player = exoPlayer
                trimPlayerController(view, preferTvQuality)
                if (!preferTvQuality) {
                    view.setFullscreenButtonState(isFullscreen)
                }
            },
        )

        if (playbackState == Player.STATE_BUFFERING || isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = PlexColors.amberBright,
                strokeWidth = 2.5.dp,
            )
        }

        if (!isFullscreen && controlsVisible) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(FawaIconSizes.touchTarget)
                    .then(
                        if (preferTvQuality) {
                            Modifier
                                .focusable(interactionSource = backInteraction)
                                .tvClickOnCenter(onBack)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                FawaIcon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(UnstableApi::class)
private fun trimPlayerController(playerView: PlayerView, tv: Boolean) {
    listOf(
        MediaUiR.id.exo_settings,
        MediaUiR.id.exo_ffwd,
        MediaUiR.id.exo_rew,
        MediaUiR.id.exo_ffwd_with_amount,
        MediaUiR.id.exo_rew_with_amount,
        MediaUiR.id.exo_subtitle,
        MediaUiR.id.exo_vr,
        MediaUiR.id.exo_artwork,
        MediaUiR.id.exo_main_text,
        MediaUiR.id.exo_sub_text,
        MediaUiR.id.exo_duration,
        MediaUiR.id.exo_position,
        MediaUiR.id.exo_time,
        MediaUiR.id.exo_progress,
        MediaUiR.id.exo_progress_placeholder,
        MediaUiR.id.exo_playback_speed,
        MediaUiR.id.exo_repeat_toggle,
        MediaUiR.id.exo_shuffle,
        MediaUiR.id.exo_overflow_show,
        MediaUiR.id.exo_overflow_hide,
        MediaUiR.id.exo_extra_controls,
        MediaUiR.id.exo_extra_controls_scroll_view,
        MediaUiR.id.exo_next,
        MediaUiR.id.exo_prev,
    ).forEach { id ->
        playerView.findViewById<View>(id)?.visibility = View.GONE
    }
    if (tv) {
        playerView.findViewById<View>(MediaUiR.id.exo_fullscreen)?.visibility = View.GONE
    }
}

private fun Format.qualityLabel(): String {
    val heightPart = if (height > 0) "${height}p" else null
    val bitratePart = if (bitrate > 0) "${bitrate / 1000} kbps" else null
    return listOfNotNull(heightPart, bitratePart).joinToString(" · ").ifBlank { label ?: "Auto" }
}
