package sc.fawanews.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import sc.fawanews.app.R
import sc.fawanews.app.ui.theme.PlexColors

enum class FawaLoadingStyle {
    /** App open / first schedule fetch — logo once, here only */
    AppOpen,
    /** Stream URL load — spinner only, no logo */
    Stream,
}

@Composable
fun FawaLoadingScreen(
    tvMode: Boolean,
    modifier: Modifier = Modifier,
    style: FawaLoadingStyle = FawaLoadingStyle.AppOpen,
) {
    val showLogo = style == FawaLoadingStyle.AppOpen
    val logoWidth =
        if (tvMode) FawaIconSizes.loadingWordmarkTv else FawaIconSizes.loadingWordmarkPhone

    Box(
        modifier
            .fillMaxSize()
            .background(PlexColors.canvas),
        contentAlignment = Alignment.Center,
    ) {
        if (showLogo) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.clutch_wordmark),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.width(logoWidth),
                    contentScale = ContentScale.Fit,
                )
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 28.dp),
                    color = PlexColors.accent,
                    strokeWidth = if (tvMode) 2.5.dp else 2.dp,
                )
            }
        } else {
            CircularProgressIndicator(
                color = PlexColors.accent,
                strokeWidth = if (tvMode) 2.5.dp else 2.dp,
            )
        }
    }
}
