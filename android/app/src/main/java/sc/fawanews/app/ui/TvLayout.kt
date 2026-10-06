package sc.fawanews.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Layout breakpoints for TV (dp width from [LocalConfiguration]). */
object TvLayout {
    /** 2560×1440-class panels (~1280dp wide); 1080p Shield is ~960dp and stays compact. */
    const val WIDTH_2K_DP = 1280

    @Composable
    fun screenWidthDp(): Int = LocalConfiguration.current.screenWidthDp

    @Composable
    fun is2KPanel(): Boolean = screenWidthDp() >= WIDTH_2K_DP

    @Composable
    fun sideMenuWidth(): Dp = if (is2KPanel()) 280.dp else 228.dp

    @Composable
    fun contentPadding(): Dp = if (is2KPanel()) 28.dp else 18.dp

    /** Fixed columns so 1080p TV never collapses to a single column. */
    @Composable
    fun gridColumns(): Int = when {
        is2KPanel() -> 5
        screenWidthDp() >= 840 -> 4
        else -> 3
    }

    @Composable
    fun gridGap(): Dp = if (is2KPanel()) 14.dp else 10.dp
}
