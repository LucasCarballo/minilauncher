package com.minilauncher.feature.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.minilauncher.data.model.AppListError
import com.minilauncher.ui.component.AppNameItem
import com.minilauncher.ui.component.SearchInput
import com.minilauncher.ui.theme.Spacing
import com.minilauncher.ui.theme.TextPrimary
import com.minilauncher.ui.theme.TextSecondary
import com.minilauncher.ui.theme.TextTertiary

@Composable
fun DrawerScreen(
    state: DrawerUiState,
    onIntent: (DrawerIntent) -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
            .padding(start = Spacing.md, top = Spacing.md)
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -50f) {
                        onBack()
                    }
                }
            },
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
                )
            }
        }
    }

    // Auto-focus search on open
    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun AppList(
    apps: kotlinx.collections.immutable.ImmutableList<AppDisplayModel>,
    query: String,
    onAppClick: (AppDisplayModel) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
        androidx.compose.material3.TextButton(onClick = { onIntent(DrawerIntent.RetryClicked) }) {
            Text("Retry", color = TextPrimary)
        }
    }
}

