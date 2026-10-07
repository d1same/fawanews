package sc.fawanews.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import sc.fawanews.app.R
import sc.fawanews.app.data.ScoreGame
import sc.fawanews.app.ui.TvFocusStyles
import sc.fawanews.app.ui.theme.PlexColors
import sc.fawanews.app.ui.tvClickOnCenter

@Composable
fun ScoreGameCard(
    game: ScoreGame,
    modifier: Modifier = Modifier,
    focusable: Boolean = false,
    compact: Boolean = false,
    showLeague: Boolean = false,
    onClick: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val pad = if (compact) 10.dp else 14.dp
    val canWatch = onClick != null
    Card(
        modifier = modifier
            .then(
                if (canWatch) {
                    Modifier.clickable(onClick = onClick)
                } else if (focusable) {
                    Modifier.focusable()
                } else {
                    Modifier
                },
            )
            .then(if (canWatch && focusable) Modifier.tvClickOnCenter(onClick) else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                if (focusable || canWatch) onFocusChange?.invoke(it.isFocused)
            },
        shape = RoundedCornerShape(if (compact) 12.dp else 16.dp),
        border = if (focusable && focused) {
            BorderStroke(TvFocusStyles.focusBorder, TvFocusStyles.focusGlow)
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = if (focusable) PlexColors.card else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (focused && focusable) 6.dp else 1.dp),
    ) {
        Column(
            Modifier.padding(pad),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when {
                        showLeague -> game.leagueLabel
                        game.startTimeLabel != null -> game.startTimeLabel
                        else -> game.statusLabel
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = PlexColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (game.streamPagePath != null) {
                    Surface(color = PlexColors.accent, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            stringResource(R.string.score_watch),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PlexColors.canvas,
                        )
                    }
                }
                if (game.isLive) {
                    Surface(color = PlexColors.liveRed, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "LIVE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
            }
            ScoreTeamRow(
                name = game.awayTeam,
                score = game.awayScore,
                logoUrl = game.awayLogoUrl,
                record = game.awayRecord,
                compact = compact,
            )
            ScoreTeamRow(
                name = game.homeTeam,
                score = game.homeScore,
                logoUrl = game.homeLogoUrl,
                record = game.homeRecord,
                compact = compact,
            )
            val footer = buildList {
                if (game.isLive) add(game.statusLabel)
                game.venueName?.let { add(it) }
                game.venueLocation?.let { add(it) }
                game.broadcastLabel?.let { add(it) }
            }.distinct().joinToString(" · ")
            if (footer.isNotBlank()) {
                Text(
                    footer,
                    style = MaterialTheme.typography.labelSmall,
                    color = PlexColors.textSecondary.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ScoreTeamRow(
    name: String,
    score: String,
    logoUrl: String?,
    record: String?,
    compact: Boolean,
) {
    val logoSize = if (compact) 28.dp else FawaIconSizes.teamLogo
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                modifier = Modifier.size(logoSize),
            )
            Column(Modifier.padding(start = 8.dp)) {
                Text(
                    name,
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = PlexColors.textPrimary,
                )
                if (!record.isNullOrBlank()) {
                    Text(
                        record,
                        style = MaterialTheme.typography.labelSmall,
                        color = PlexColors.textSecondary,
                    )
                }
            }
        }
        Text(
            score,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PlexColors.accentBright,
        )
    }
}
