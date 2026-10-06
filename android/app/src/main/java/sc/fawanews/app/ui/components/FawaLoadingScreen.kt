package sc.fawanews.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    val logoSize =
        if (tvMode) FawaIconSizes.loadingLogoAppTv else FawaIconSizes.loadingLogoAppPhone

    Box(
        modifier
            .fillMaxSize()
            .background(PlexColors.canvas),
        contentAlignment = Alignment.Center,
    ) {
        if (showLogo) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.fawanews_icon),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(logoSize)
                        .clip(RoundedCornerShape(if (tvMode) 12.dp else 10.dp)),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(top = if (tvMode) 14.dp else 12.dp),
                    style = if (tvMode) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    color = PlexColors.textPrimary,
                )
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 20.dp),
                    color = PlexColors.amber,
                    strokeWidth = if (tvMode) 2.5.dp else 2.dp,
                )
            }
        } else {
            CircularProgressIndicator(
                color = PlexColors.amber,
                strokeWidth = if (tvMode) 2.5.dp else 2.dp,
            )
        }
    }
}
