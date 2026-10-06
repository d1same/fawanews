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

    /** App mark beside titles on TV home. */
    val brandMark: Dp = 36.dp

    /** Cold start / schedule load (Compose). */
    val loadingLogoAppTv: Dp = 88.dp
    val loadingLogoAppPhone: Dp = 72.dp

    /** Stream fetch + player buffer overlay. */
    val loadingLogoStreamTv: Dp = 64.dp
    val loadingLogoStreamPhone: Dp = 52.dp
}
