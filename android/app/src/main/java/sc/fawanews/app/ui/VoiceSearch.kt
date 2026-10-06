package sc.fawanews.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import sc.fawanews.app.R

@Composable
fun rememberVoiceSearchLauncher(
    onResult: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val onResultState = rememberUpdatedState(onResult)

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
        if (!text.isNullOrEmpty()) {
            onResultState.value(text)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer(context, speechLauncher)
        } else {
            toast(context, R.string.voice_permission_denied)
        }
    }

    return remember(context, speechLauncher, permissionLauncher) {
        {
            when {
                !isSpeechAvailable(context) -> toast(context, R.string.voice_not_available)
                needsRecordAudioPermission(context) -> {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                else -> launchSpeechRecognizer(context, speechLauncher)
            }
        }
    }
}

private fun needsRecordAudioPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) != PackageManager.PERMISSION_GRANTED
}

private fun isSpeechAvailable(context: Context): Boolean {
    return buildSpeechIntent().resolveActivity(context.packageManager) != null
}

private fun launchSpeechRecognizer(
    context: Context,
    launcher: ActivityResultLauncher<Intent>,
) {
    val intent = buildSpeechIntent()
    if (intent.resolveActivity(context.packageManager) == null) {
        toast(context, R.string.voice_not_available)
        return
    }
    val activity = context.findComponentActivity()
    if (activity == null) {
        toast(context, R.string.voice_not_available)
        return
    }
    runCatching { launcher.launch(intent) }
        .onFailure { toast(context, R.string.voice_not_available) }
}

private fun buildSpeechIntent(): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a team or league")
    }

private fun Context.findComponentActivity(): ComponentActivity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}

private fun toast(context: Context, messageRes: Int) {
    Toast.makeText(context.applicationContext, messageRes, Toast.LENGTH_SHORT).show()
}
