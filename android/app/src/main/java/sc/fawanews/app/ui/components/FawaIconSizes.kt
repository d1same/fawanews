package sc.fawanews.app.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared icon dimensions — use everywhere so TV and phone match. */
object FawaIconSizes {
    /** Toolbar, player, search field, drawer row icons. */
    val standard: Dp = 24.dp

    /** Material icon button / TV D-pad focus target. */
    val touchTarget: Dp = 48.dp

    /** Team logos in score rows. */
    val teamLogo: Dp = 32.dp

    /** Empty states (coming soon, etc.). */
    val hero: Dp = 64.dp

    /** Height of the Clutch wordmark in the TV header and phone drawer. */
    val brandWordmark: Dp = 26.dp

    /** Width of the Clutch wordmark on the cold start / schedule load screen. */
    val loadingWordmarkTv: Dp = 300.dp
    val loadingWordmarkPhone: Dp = 220.dp

    /** Stream fetch + player buffer overlay. */
    val loadingLogoStreamTv: Dp = 64.dp
    val loadingLogoStreamPhone: Dp = 52.dp
}
