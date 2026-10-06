package sc.fawanews.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextAlign.Companion.End
import kotlinx.coroutines.delay
import sc.fawanews.app.R
import kotlin.math.max

@Composable
fun LastUpdatedLabel(
    updatedAtMillis: Long?,
    modifier: Modifier = Modifier,
    textAlignEnd: Boolean = false,
) {
    if (updatedAtMillis == null) return
    var label by remember(updatedAtMillis) { mutableStateOf(formatAgo(updatedAtMillis)) }
    LaunchedEffect(updatedAtMillis) {
        while (true) {
            delay(30_000)
            label = formatAgo(updatedAtMillis)
        }
    }
    Text(
        text = stringResource(R.string.updated_ago, label),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        textAlign = if (textAlignEnd) End else TextAlign.Start,
        modifier = if (textAlignEnd) modifier.fillMaxWidth() else modifier,
    )
}

private fun formatAgo(updatedAtMillis: Long): String {
    val minutes = max(0, (System.currentTimeMillis() - updatedAtMillis) / 60_000).toInt()
    return when {
        minutes < 1 -> "just now"
        minutes == 1 -> "1 min ago"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60} hr ago"
    }
}
