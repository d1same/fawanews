package sc.fawanews.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun PhonePlayerOrientationEffect(
    playerActive: Boolean,
    fullscreen: Boolean,
) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(playerActive, fullscreen) {
        val window = activity.window
        val insets = WindowInsetsControllerCompat(window, window.decorView)
        when {
            !playerActive -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                WindowCompat.setDecorFitsSystemWindows(window, true)
                insets.show(WindowInsetsCompat.Type.systemBars())
            }
            fullscreen -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                WindowCompat.setDecorFitsSystemWindows(window, false)
                insets.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insets.hide(WindowInsetsCompat.Type.systemBars())
            }
            else -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                WindowCompat.setDecorFitsSystemWindows(window, false)
                insets.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            WindowCompat.setDecorFitsSystemWindows(window, true)
            insets.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
