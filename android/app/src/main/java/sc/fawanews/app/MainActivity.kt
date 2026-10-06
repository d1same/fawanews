package sc.fawanews.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import sc.fawanews.app.ui.FawaViewModel
import sc.fawanews.app.ui.MobileHomeScreen
import sc.fawanews.app.ui.PhonePlayerOrientationEffect
import sc.fawanews.app.ui.PlayerScreen
import sc.fawanews.app.ui.TvHomeScreen
import sc.fawanews.app.ui.components.FawaLoadingScreen
import sc.fawanews.app.ui.theme.FawaNewsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val isTv = applicationContext.isTelevision()
        if (isTv) {
            setTheme(R.style.Theme_FawaNews_Tv)
            // Portrait on a landscape TV is letterboxed into a ~9:16 strip.
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        if (isTv) {
            applyTelevisionWindowPolicy()
        } else {
            enableEdgeToEdge()
        }
        val app = application as FawaNewsApp

        setContent {
            val activity = LocalContext.current as MainActivity
            DisposableEffect(Unit) {
                if (isTv) activity.applyTelevisionWindowPolicy()
                onDispose { }
            }
            FawaNewsTheme(tv = isTv) {
                val vm: FawaViewModel = viewModel(
                    factory = FawaViewModel.Factory(app.repository, app.scoreRepository),
                )
                val schedule by vm.scheduleState.collectAsState()
                val scores by vm.scoresState.collectAsState()
                val player by vm.playerState.collectAsState()
                var playerFullscreen by remember { mutableStateOf(false) }
                val phoneVideoPlayer =
                    !isTv &&
                        player != null &&
                        player!!.streamUrl != null &&
                        !player!!.isLoading
                if (!isTv) {
                    PhonePlayerOrientationEffect(
                        playerActive = phoneVideoPlayer,
                        fullscreen = playerFullscreen,
                    )
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, vm) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            vm.onAppResumed()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val showStartupLoading =
                    player == null &&
                        schedule.items.isEmpty() &&
                        schedule.error == null &&
                        (schedule.isLoading || schedule.lastUpdatedMillis == null)

                if (showStartupLoading) {
                    FawaLoadingScreen(tvMode = isTv)
                } else if (player != null) {
                    PlayerScreen(
                        state = player!!,
                        tvMode = isTv,
                        onBack = vm::closePlayer,
                        onRefreshStream = { vm.refreshPlayerStream(showOverlay = true) },
                        onSelectStream = vm::selectStream,
                        onPlayerReady = vm::onPlayerReady,
                        onPlayerError = vm::onPlayerError,
                        onPlayerFullscreenChange = { playerFullscreen = it },
                    )
                } else if (isTv) {
                    TvHomeScreen(
                        state = schedule,
                        scoresState = scores,
                        onRefresh = { vm.refreshSchedule(showSpinner = true) },
                        onScoresRefresh = { vm.refreshScores(showSpinner = true) },
                        onTabSelect = vm::selectTab,
                        onCategorySelect = vm::selectCategory,
                        onScoreLeagueSelect = vm::selectScoreLeague,
                        onSearchQueryChange = vm::setSearchQuery,
                        onItemClick = vm::openItem,
                        onTvItemFocused = vm::onTvItemFocused,
                        onTvMenuFocused = vm::onTvMenuFocused,
                        onFocusContentFromMenu = vm::focusContentFromMenuTv,
                        onTvLeftFromContent = vm::onTvLeftFromContent,
                        onTvScoreGameFocused = vm::onTvScoreGameFocused,
                        onClearFocusRequest = vm::clearTvFocusRequest,
                    )
                } else {
                    MobileHomeScreen(
                        state = schedule,
                        scoresState = scores,
                        onRefresh = { vm.refreshSchedule(showSpinner = true) },
                        onScoresRefresh = { vm.refreshScores(showSpinner = true) },
                        onTabSelect = vm::selectTab,
                        onCategorySelect = vm::selectCategory,
                        onScoreLeagueSelect = vm::selectScoreLeague,
                        onSearchQueryChange = vm::setSearchQuery,
                        onItemClick = vm::openItem,
                    )
                }
            }
        }
    }
}
