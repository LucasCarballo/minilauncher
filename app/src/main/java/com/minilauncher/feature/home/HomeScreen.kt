package com.minilauncher.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minilauncher.data.model.AppDisplayModel
import com.minilauncher.data.model.AppListError
import com.minilauncher.ui.component.AppNameItem
import com.minilauncher.ui.theme.Spacing
import com.minilauncher.ui.theme.TextPrimary
import com.minilauncher.ui.theme.TextSecondary
import com.minilauncher.ui.theme.Surface2
import com.minilauncher.ui.theme.TextTertiary
import kotlin.math.abs

private val SwipeUpThreshold = 50.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    onSwipeUp: () -> Unit,
) {
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val thresholdPx = with(LocalDensity.current) { SwipeUpThreshold.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
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

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null) break
                            endX = change.position.x
                            endY = change.position.y
                            if (!change.pressed) break
                        }

                        val verticalDisplacement = startY - endY
                        val horizontalDisplacement = abs(startX - endX)

                        if (verticalDisplacement > thresholdPx && verticalDisplacement > horizontalDisplacement) {
                            currentOnSwipeUp()
                        }
                    }
                }
            }
            .padding(start = Spacing.md, top = Spacing.md),
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = TextSecondary,
                )
            }
            state.error != null -> {
                ErrorState(state.error, onIntent)
            }
            else -> {
                HomeContent(state, onIntent)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
) {
    var contextMenuApp by remember { mutableStateOf<AppDisplayModel?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Greeting — long-press to open settings
        val greetingText = if (state.userName.isNotBlank()) {
            "${state.greeting}, ${state.userName}"
        } else {
            state.greeting
        }
        Text(
            text = greetingText,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { onIntent(HomeIntent.OpenSettings) },
            ),
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Date
        Text(
            text = state.date,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Time
        Text(
            text = state.time,
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Pinned apps with context menu
        state.pinnedApps.forEach { app ->
            Box {
                AppNameItem(
                    label = app.label,
                    onClick = { onIntent(HomeIntent.AppClicked(app)) },
                    onLongClick = { contextMenuApp = app },
                    isWorkProfile = app.isWorkProfile,
                )

                DropdownMenu(
                    expanded = contextMenuApp == app,
                    onDismissRequest = { contextMenuApp = null },
                    containerColor = Surface2,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove from Home") },
                        onClick = {
                            onIntent(HomeIntent.UnpinApp(app.packageName))
                            contextMenuApp = null
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = TextPrimary,
                        ),
                    )
                    DropdownMenuItem(
                        text = { Text("App info") },
                        onClick = {
                            onIntent(HomeIntent.AppInfoClicked(app.packageName))
                            contextMenuApp = null
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = TextSecondary,
                        ),
                    )
                }
            }
        }

        // Bottom hint
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = Spacing.md),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                text = "↑ swipe up for all apps",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = TextTertiary,
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: AppListError,
    onIntent: (HomeIntent) -> Unit,
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
        TextButton(onClick = { onIntent(HomeIntent.RetryClicked) }) {
            Text("Retry", color = TextPrimary)
        }
    }
}