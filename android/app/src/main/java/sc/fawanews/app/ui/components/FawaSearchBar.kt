package sc.fawanews.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import sc.fawanews.app.R
import sc.fawanews.app.ui.TvFocusStyles
import sc.fawanews.app.ui.theme.PlexColors
import sc.fawanews.app.ui.tvClickOnCenter

@Composable
fun FawaSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    tvMode: Boolean = false,
    menuFocusRequester: FocusRequester? = null,
    micFocusRequester: FocusRequester? = null,
    gridFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    if (tvMode) {
        TvSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = modifier,
            menuFocusRequester = menuFocusRequester,
            searchFocusRequester = micFocusRequester,
            gridFocusRequester = gridFocusRequester,
            downFocusRequester = downFocusRequester,
        )
        return
    }

    val keyboard = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        placeholder = {
            Text(stringResource(R.string.search_hint))
        },
        leadingIcon = {
            FawaIcon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(FawaIconSizes.touchTarget),
                ) {
                    FawaIcon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        ),
    )
}

@Composable
private fun TvSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    menuFocusRequester: FocusRequester? = null,
    searchFocusRequester: FocusRequester? = null,
    gridFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
) {
    val fieldFocusRequester = remember { FocusRequester() }
    val attachFocus = searchFocusRequester ?: fieldFocusRequester
    val fieldShape = RoundedCornerShape(12.dp)
    val searchDown = downFocusRequester ?: gridFocusRequester
    val keyboard = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .focusRequester(attachFocus)
            .focusProperties {
                if (menuFocusRequester != null) {
                    left = menuFocusRequester
                }
                if (gridFocusRequester != null) {
                    right = gridFocusRequester
                }
                if (searchDown != null) {
                    down = searchDown
                }
            },
        placeholder = {
            Text(
                stringResource(R.string.search_hint),
                color = PlexColors.textSecondary,
            )
        },
        leadingIcon = {
            FawaIcon(
                Icons.Default.Search,
                contentDescription = null,
                tint = PlexColors.accent,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                TvClearButton(onClick = { onQueryChange("") }) {
                    FawaIcon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.search_clear),
                        tint = PlexColors.accentBright,
                    )
                }
            }
        },
        singleLine = true,
        shape = fieldShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PlexColors.textPrimary,
            unfocusedTextColor = PlexColors.textPrimary,
            focusedBorderColor = PlexColors.accentBright,
            unfocusedBorderColor = PlexColors.divider,
            cursorColor = PlexColors.accent,
            focusedContainerColor = PlexColors.card,
            unfocusedContainerColor = PlexColors.card.copy(alpha = 0.65f),
        ),
    )
}

@Composable
private fun TvClearButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier
            .size(40.dp)
            .tvClickOnCenter(onClick)
            .border(
                width = if (focused) TvFocusStyles.focusBorder else 1.dp,
                color = if (focused) PlexColors.accentBright else PlexColors.accent,
                shape = shape,
            ),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}
