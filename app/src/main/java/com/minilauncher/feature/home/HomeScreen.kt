package com.minilauncher.feature.home

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minilauncher.data.model.AppListError
import com.minilauncher.ui.component.AppNameItem
import com.minilauncher.ui.theme.Spacing
import com.minilauncher.ui.theme.TextPrimary
import com.minilauncher.ui.theme.TextSecondary
import com.minilauncher.ui.theme.TextTertiary

@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    onSwipeRight: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = Spacing.md, top = Spacing.md)
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50f) {
                        onSwipeRight()
                    }
                }
            },
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
                text = "← swipe right for all apps",
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
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
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
        androidx.compose.material3.TextButton(onClick = { onIntent(HomeIntent.RetryClicked) }) {
            Text("Retry", color = TextPrimary)
        }
    }
}