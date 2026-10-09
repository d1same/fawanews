package sc.fawanews.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sc.fawanews.app.ui.TvFocusStyles
import sc.fawanews.app.ui.theme.PlexColors
import sc.fawanews.app.ui.tvClickOnCenter

@Composable
fun TvMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDpadRight: (() -> Unit)? = null,
    onFocused: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(focused) {
        if (focused) onFocused?.invoke()
    }
    val shape = RoundedCornerShape(TvFocusStyles.menuItemCorner)

    // One layer only: fill when focused. Selected = bar + text, no second wash.
    val rowBg = if (focused) PlexColors.accent.copy(alpha = 0.17f) else Color.Transparent
    val barColor = when {
        focused -> PlexColors.accentBright
        selected -> PlexColors.accent.copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    val textColor = when {
        focused -> PlexColors.accentBright
        selected -> PlexColors.textPrimary
        else -> PlexColors.textSecondary.copy(alpha = 0.88f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(rowBg)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .tvClickOnCenter(onClick)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> {
                        onDpadRight?.invoke()
                        true
                    }
                    Key.DirectionLeft -> true
                    else -> false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .padding(start = 6.dp)
                .width(2.dp)
                .height(if (focused) 16.dp else 14.dp)
                .background(barColor, RoundedCornerShape(1.dp)),
        )
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = when {
                focused -> FontWeight.SemiBold
                selected -> FontWeight.Medium
                else -> FontWeight.Normal
            },
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
