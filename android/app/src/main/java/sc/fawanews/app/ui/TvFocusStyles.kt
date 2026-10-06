package sc.fawanews.app.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sc.fawanews.app.ui.theme.PlexColors

object TvFocusStyles {
    val focusBorder: Dp = 1.5.dp
    /** Left nav rows — keep outline hairline so D-pad focus stays readable without bulk. */
    val menuFocusBorder: Dp = 1.dp
    val menuItemCorner: Dp = 6.dp
    val focusGlow: Color = PlexColors.amberBright
    val menuAccent: Color = PlexColors.amber
    val menuPanel: Color = PlexColors.sidebar
}

@Composable
fun rememberTvFocused(): Pair<MutableInteractionSource, Boolean> {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    return interactionSource to focused
}

fun Modifier.tvClickOnCenter(onClick: () -> Unit): Modifier = onKeyEvent { event ->
    if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
    val code = event.nativeKeyEvent.keyCode
    val activated = event.key == Key.DirectionCenter ||
        event.key == Key.Enter ||
        event.key == Key.NumPadEnter ||
        code == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        code == AndroidKeyEvent.KEYCODE_ENTER ||
        code == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ||
        code == AndroidKeyEvent.KEYCODE_BUTTON_A
    if (activated) {
        onClick()
        true
    } else {
        false
    }
}
