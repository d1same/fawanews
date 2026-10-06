package sc.fawanews.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object MobileLayout {
    /** Target columns: 2 on phones, 3 on wide / tablet. */
    @Composable
    fun eventGridMinCellSize(): Dp {
        val widthDp = LocalConfiguration.current.screenWidthDp
        return when {
            widthDp >= 720 -> 220.dp
            widthDp >= 520 -> 180.dp
            else -> 168.dp
        }
    }

    @Composable
    fun eventGridColumnsHint(): Int = when {
        LocalConfiguration.current.screenWidthDp >= 720 -> 3
        else -> 2
    }
}
