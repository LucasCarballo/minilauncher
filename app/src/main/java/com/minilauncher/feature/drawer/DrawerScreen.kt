package com.minilauncher.feature.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.minilauncher.data.model.AppListError
import com.minilauncher.ui.component.AppNameItem
import com.minilauncher.ui.component.SearchInput
import com.minilauncher.ui.theme.Spacing
import com.minilauncher.ui.theme.TextPrimary
import com.minilauncher.ui.theme.TextSecondary
import com.minilauncher.ui.theme.TextTertiary
import kotlin.math.abs

private val SwipeDownThreshold = 50.dp

@Composable
fun DrawerScreen(
    state: DrawerUiState,
    onIntent: (DrawerIntent) -> Unit,
    onBack: () -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val thresholdPx = with(LocalDensity.current) { SwipeDownThreshold.toPx() }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Wait for a down event — observe even if consumed by children
                        // Use Initial pass to see events BEFORE children process them
                        var downChange: androidx.compose.ui.input.pointer.PointerInputChange? = null
                        while (downChange == null) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            downChange = event.changes.firstOrNull {
                                it.pressed && !it.previousPressed
                            }
                        }

                        val startX = downChange.position.x
                        val startY = downChange.position.y
                        val pointerId = downChange.id
                        var endX = startX
                        var endY = startY

                        // Track until pointer is released
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null) break
                            endX = change.position.x
                            endY = change.position.y
                            if (!change.pressed) break
                        }

                        val verticalDisplacement = endY - startY
                        val horizontalDisplacement = abs(startX - endX)

                        // Only trigger swipe-down if:
                        // 1. Vertical displacement exceeds threshold
                        // 2. Swipe is primarily vertical
                        // 3. LazyColumn is at the top
                        val isAtTop = listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset == 0

                        if (verticalDisplacement > thresholdPx &&
                            verticalDisplacement > horizontalDisplacement &&
                            isAtTop
                        ) {
                            currentOnBack()
                        }
                    }
                }
            }
            .padding(start = Spacing.md, top = Spacing.md)
            .statusBarsPadding(),
    ) {
        // Search input
        SearchInput(
            query = state.query,
            onQueryChanged = { onIntent(DrawerIntent.QueryChanged(it)) },
            modifier = Modifier.focusRequester(focusRequester),
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TextTertiary.copy(alpha = 0.2f)),
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // App list
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = TextSecondary)
                }
            }
            state.error != null -> {
                DrawerErrorState(state.error, onIntent)
            }
            else -> {
                AppList(
                    apps = state.filteredApps,
                    query = state.query,
                    onAppClick = { onIntent(DrawerIntent.AppClicked(it)) },
                    listState = listState,
                )
            }
        }
    }

    // Auto-focus search on open
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun AppList(
    apps: kotlinx.collections.immutable.ImmutableList<AppDisplayModel>,
    query: String,
    onAppClick: (AppDisplayModel) -> Unit,
    listState: LazyListState,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
    ) {
        items(
            count = apps.size,
            key = { apps[it].packageName },
        ) { index ->
            val app = apps[index]
            AppNameItem(
                label = app.label,
                query = query,
                onClick = { onAppClick(app) },
            )
        }
    }
}

@Composable
private fun DrawerErrorState(
    error: AppListError,
    onIntent: (DrawerIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = when (error) {
                is AppListError.PackageManagerDead -> "System error loading apps"
                is AppListError.SecurityDenied -> "Permission denied"
                is AppListError.Unknown -> "Something went wrong"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        TextButton(onClick = { onIntent(DrawerIntent.RetryClicked) }) {
            Text("Retry", color = TextPrimary)
        }
    }
}