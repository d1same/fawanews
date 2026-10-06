package sc.fawanews.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import sc.fawanews.app.R
import sc.fawanews.app.ui.components.FawaIcon
import sc.fawanews.app.ui.components.FawaIconSizes
import sc.fawanews.app.ui.theme.PlexColors

@Composable
fun ArticleReaderScreen(
    title: String,
    paragraphs: List<String>,
    imageUrl: String?,
    tvMode: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val pad = if (tvMode) 32.dp else 20.dp
    val scrollStep = if (tvMode) 140.dp else 96.dp
    val thumbWidth = if (tvMode) 240.dp else 112.dp
    val thumbHeight = if (tvMode) 150.dp else 112.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(PlexColors.canvas),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (!tvMode || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown, Key.PageDown -> {
                            scope.launch { scrollState.animateScrollTo(scrollState.value + scrollStep.value.toInt()) }
                            true
                        }
                        Key.DirectionUp, Key.PageUp -> {
                            scope.launch {
                                scrollState.animateScrollTo(
                                    (scrollState.value - scrollStep.value.toInt()).coerceAtLeast(0),
                                )
                            }
                            true
                        }
                        else -> false
                    }
                }
                .verticalScroll(scrollState)
                .padding(
                    start = pad,
                    end = pad,
                    top = if (tvMode) pad else pad + 44.dp,
                    bottom = pad,
                ),
        ) {
        Text(
            title,
            style = if (tvMode) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PlexColors.textPrimary,
        )
        if (paragraphs.isEmpty()) {
            Text(
                stringResource(R.string.article_empty),
                modifier = Modifier.padding(top = 20.dp),
                color = PlexColors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (tvMode) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .width(thumbWidth)
                            .height(thumbHeight)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = if (imageUrl != null) 16.dp else 0.dp),
                ) {
                    ArticleParagraphs(paragraphs)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                ArticleParagraphs(
                    paragraphs = paragraphs,
                    modifier = Modifier.padding(top = if (imageUrl != null) 16.dp else 0.dp),
                )
            }
        }
        if (tvMode) {
            Text(
                stringResource(R.string.article_back_hint),
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = PlexColors.textSecondary,
            )
        }
        }

        if (!tvMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(FawaIconSizes.touchTarget),
                ) {
                    FawaIcon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White,
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun ArticleParagraphs(
    paragraphs: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        paragraphs.forEachIndexed { index, paragraph ->
            Text(
                paragraph,
                modifier = Modifier.padding(top = if (index == 0) 0.dp else 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = PlexColors.textPrimary.copy(alpha = 0.92f),
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            )
        }
    }
}
