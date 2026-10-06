package sc.fawanews.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import sc.fawanews.app.data.ScheduleItem
import sc.fawanews.app.ui.TvFocusStyles
import sc.fawanews.app.ui.theme.PlexColors
import sc.fawanews.app.ui.rememberTvFocused
import sc.fawanews.app.ui.tvClickOnCenter

@Composable
fun EventCard(
    item: ScheduleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    val (interactionSource, focused) = rememberTvFocused()
    LaunchedEffect(focused) {
        onFocusChange?.invoke(focused)
    }
    val cardPadding = if (compact) 8.dp else 12.dp
    val titleStyle = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall
    val subtitleStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val corner = if (compact) 14.dp else 16.dp
    val shape = RoundedCornerShape(corner)

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.tvClickOnCenter(onClick),
        shape = shape,
        border = BorderStroke(
            width = if (focused) TvFocusStyles.focusBorder else 1.dp,
            color = if (focused) {
                TvFocusStyles.focusGlow
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            },
        ),
        colors = CardDefaults.cardColors(
            containerColor = PlexColors.card,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (focused) 10.dp else 1.dp,
        ),
    ) {
        Column {
            Box {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = corner, topEnd = corner)),
                )
                if (focused) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        TvFocusStyles.focusGlow.copy(alpha = 0.35f),
                                    ),
                                ),
                            ),
                    )
                }
            }
            Column(Modifier.padding(cardPadding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isLive) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                "LIVE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }
                Text(
                    text = item.title,
                    style = titleStyle,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = if (focused) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
                    },
                )
                item.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = subtitleStyle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
