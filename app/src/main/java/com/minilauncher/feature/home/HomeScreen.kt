package com.minilauncher.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minilauncher.data.model.AppListError
import kotlin.math.abs
import com.minilauncher.ui.component.AppNameItem
import com.minilauncher.ui.theme.Spacing
import com.minilauncher.ui.theme.TextPrimary
import com.minilauncher.ui.theme.TextSecondary
import com.minilauncher.ui.theme.TextTertiary

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

                        val verticalDisplacement = startY - endY
                        val horizontalDisplacement = abs(startX - endX)

                        // Only trigger if swipe is primarily vertical and exceeds threshold
                        if (verticalDisplacement > thresholdPx && verticalDisplacement > horizontalDisplacement) {
                            currentOnSwipeUp()
                        }
                    }
                }
            }
            .padding(start = Spacing.md, top = Spacing.md)
            .statusBarsPadding(),
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

@Composable
private fun HomeContent(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Greeting
        val greetingText = if (state.userName.isNotBlank()) {
            "${state.greeting}, ${state.userName}"
        } else {
            state.greeting
        }
        Text(
            text = greetingText,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
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

        // Pinned apps
        state.pinnedApps.forEach { app ->
            AppNameItem(
                label = app.label,
                onClick = { onIntent(HomeIntent.AppClicked(app)) },
                onLongClick = { /* TODO: context menu */ },
            )
        }

        // Bottom hint
        Box(
            modifier = Modifier
                .fillMaxSize()
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