package sc.fawanews.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import sc.fawanews.app.R
import sc.fawanews.app.ui.theme.PlexColors

@Composable
fun UpdateAvailableDialog(
    versionName: String,
    downloading: Boolean,
    error: String?,
    tvMode: Boolean,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    val updateFocus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = { if (!downloading) onLater() },
        title = { Text(stringResource(R.string.update_title)) },
        text = {
            Text(
                error ?: if (downloading) {
                    stringResource(R.string.update_downloading)
                } else {
                    stringResource(R.string.update_body, versionName)
                },
            )
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                enabled = !downloading,
                modifier = Modifier.then(
                    if (tvMode) {
                        Modifier
                            .focusRequester(updateFocus)
                            .tvClickOnCenter(onUpdate)
                    } else {
                        Modifier
                    },
                ),
            ) {
                if (downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = PlexColors.canvas,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.update_install))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLater,
                enabled = !downloading,
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Text(stringResource(R.string.update_later))
            }
        },
    )
    if (tvMode) {
        LaunchedEffect(Unit) {
            updateFocus.requestFocus()
        }
    }
}
