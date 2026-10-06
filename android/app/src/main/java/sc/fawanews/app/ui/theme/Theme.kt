package sc.fawanews.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** Plex-inspired: near-black canvas, warm amber accent (not blue/purple). */
object PlexColors {
    val canvas = Color(0xFF0B0B0B)
    val sidebar = Color(0xFF121212)
    val card = Color(0xFF1F1F1F)
    val cardElevated = Color(0xFF2A2A2A)
    val amber = Color(0xFFE5A00D)
    val amberBright = Color(0xFFF5C518)
    val amberMuted = Color(0xFFCC7B19)
    val textPrimary = Color(0xFFF3F3F3)
    val textSecondary = Color(0xFF9A9A9A)
    val divider = Color(0xFF333333)
    val liveRed = Color(0xFFE53935)
}

private val MobileDark = darkColorScheme(
    primary = PlexColors.amber,
    onPrimary = Color(0xFF1A1A1A),
    secondary = PlexColors.amberMuted,
    background = PlexColors.canvas,
    surface = PlexColors.card,
    onBackground = PlexColors.textPrimary,
    onSurface = PlexColors.textPrimary,
    surfaceVariant = PlexColors.cardElevated,
    outline = PlexColors.divider,
)

private val TvDark = darkColorScheme(
    primary = PlexColors.amber,
    onPrimary = Color(0xFF1A1A1A),
    secondary = PlexColors.amberBright,
    background = PlexColors.canvas,
    surface = PlexColors.card,
    onBackground = PlexColors.textPrimary,
    onSurface = PlexColors.textPrimary,
    surfaceVariant = PlexColors.sidebar,
    outline = PlexColors.divider,
    error = PlexColors.liveRed,
)

private val TvTypography = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, lineHeight = 50.sp),
    displayMedium = TextStyle(fontSize = 36.sp, lineHeight = 42.sp),
    displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontSize = 26.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp),
)

@Composable
fun FawaNewsTheme(
    tv: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (tv) TvDark else MobileDark,
        typography = if (tv) TvTypography else Typography(),
        content = content,
    )
}
