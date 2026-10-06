package sc.fawanews.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sc.fawanews.app.R
import sc.fawanews.app.ui.components.FawaIcon
import sc.fawanews.app.ui.components.FawaIconSizes
import sc.fawanews.app.ui.theme.PlexColors
import sc.fawanews.app.ui.tvClickOnCenter

@Composable
fun ComingSoonScreen(
    title: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean = false,
    tvMode: Boolean = false,
) {
    val refreshFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (tvMode) {
            refreshFocus.requestFocus()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = PlexColors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.stream_waiting_body),
                style = MaterialTheme.typography.bodyLarge,
                color = PlexColors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StreamActionButton(
                    label = stringResource(R.string.refresh_stream),
                    primary = true,
                    loading = isRefreshing,
                    modifier = Modifier
                        .then(if (tvMode) Modifier.focusRequester(refreshFocus) else Modifier),
                    onClick = onRefresh,
                )
                StreamActionButton(
                    label = stringResource(R.string.back),
                    primary = false,
                    loading = false,
                    onClick = onBack,
                )
            }
        }
    }
}

@Composable
private fun StreamActionButton(
    label: String,
    primary: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(TvFocusStyles.menuItemCorner)

    if (primary) {
        Button(
            onClick = onClick,
            enabled = !loading,
            modifier = modifier
                .focusable(interactionSource = interaction)
                .tvClickOnCenter(onClick),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = PlexColors.amber,
                contentColor = Color(0xFF1A1A1A),
                disabledContainerColor = PlexColors.amber.copy(alpha = 0.45f),
                disabledContentColor = Color(0xFF1A1A1A).copy(alpha = 0.7f),
            ),
            border = if (focused) {
                androidx.compose.foundation.BorderStroke(
                    TvFocusStyles.focusBorder,
                    TvFocusStyles.focusGlow,
                )
            } else {
                null
            },
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color(0xFF1A1A1A),
                    strokeWidth = 2.dp,
                )
            } else {
                FawaIcon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF1A1A1A),
                    size = FawaIconSizes.standard,
                )
            }
            Text(
                label,
                modifier = Modifier.padding(start = if (loading) 0.dp else 8.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .focusable(interactionSource = interaction)
                .tvClickOnCenter(onClick),
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PlexColors.textPrimary,
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = if (focused) TvFocusStyles.focusBorder else 1.dp,
                color = if (focused) TvFocusStyles.focusGlow else PlexColors.divider,
            ),
        ) {
            Text(label, color = PlexColors.textPrimary)
        }
    }
}
