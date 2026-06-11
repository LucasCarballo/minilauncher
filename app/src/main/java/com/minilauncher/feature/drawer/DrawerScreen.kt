package com.minilauncher.feature.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.minilauncher.data.model.AppDisplayModel
import com.minilauncher.data.model.AppListError
import com.minilauncher.ui.component.AppNameItem
import com.minilauncher.ui.component.SearchInput
import com.minilauncher.ui.theme.Spacing
import com.minilauncher.ui.theme.TextPrimary
import com.minilauncher.ui.theme.TextSecondary
import com.minilauncher.ui.theme.Surface2
import com.minilauncher.ui.theme.TextTertiary
import kotlin.math.abs

private val SwipeDownThreshold = 50.dp
private val BottomEdgeThreshold = 50.dp

@Composable
fun DrawerScreen(
    state: DrawerUiState,
    onIntent: (DrawerIntent) -> Unit,
    onBack: () -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val thresholdPx = with(LocalDensity.current) { SwipeDownThreshold.toPx() }
    val bottomEdgeThresholdPx = with(LocalDensity.current) { BottomEdgeThreshold.toPx() }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val clearFocusOnAppClick: (DrawerIntent) -> Unit = { intent ->
        if (intent is DrawerIntent.AppClicked) {
            focusManager.clearFocus()
        }
        onIntent(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding()
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
                        val startHeight = size.height
                        // Capture scroll position at gesture start — prevents false swipe-down
                        // when the list flings to top during a scroll gesture
                        val isAtTopAtStart = listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset == 0
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

                        val verticalDisplacement = endY - startY
                        val horizontalDisplacement = abs(startX - endX)

                        val isSwipeDown = verticalDisplacement > thresholdPx &&
                            verticalDisplacement > horizontalDisplacement &&
                            isAtTopAtStart

                        val isSwipeUpFromBottom = (startY - endY) > thresholdPx &&
                            (startY - endY) > horizontalDisplacement &&
                            startY > startHeight - bottomEdgeThresholdPx

                        if (isSwipeDown || isSwipeUpFromBottom) {
                            currentOnBack()
                        }
                    }
                }
            }
            .padding(start = Spacing.md, top = Spacing.md),
    ) {
        // Search input
        SearchInput(
            query = state.query,
            onQueryChanged = { onIntent(DrawerIntent.QueryChanged(it)) },
            onClear = {
                onIntent(DrawerIntent.QueryChanged(""))
                focusManager.clearFocus()
            },
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
            state.filteredApps.isEmpty() && state.query.isNotBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No apps found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
            else -> {
                AppList(
                    apps = state.filteredApps,
                    query = state.query,
                    pinnedPackageNames = state.pinnedPackageNames,
                    onIntent = clearFocusOnAppClick,
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
    pinnedPackageNames: Set<String>,
    onIntent: (DrawerIntent) -> Unit,
    listState: LazyListState,
) {
    var contextMenuApp by remember { mutableStateOf<AppDisplayModel?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
    ) {
        items(
            count = apps.size,
            key = { apps[it].packageName },
        ) { index ->
            val app = apps[index]
            val isPinned = app.packageName in pinnedPackageNames

            Box {
                AppNameItem(
                    label = app.label,
                    onClick = { onIntent(DrawerIntent.AppClicked(app)) },
                    onLongClick = { contextMenuApp = app },
                    query = query,
                    isWorkProfile = app.isWorkProfile,
                )

                DropdownMenu(
                    expanded = contextMenuApp == app,
                    onDismissRequest = { contextMenuApp = null },
                    containerColor = Surface2,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ) {
                    if (isPinned) {
                        DropdownMenuItem(
                            text = { Text("Unpin from Home") },
                            onClick = {
                                onIntent(DrawerIntent.UnpinApp(app.packageName))
                                contextMenuApp = null
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TextPrimary,
                            ),
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Pin to Home") },
                            onClick = {
                                onIntent(DrawerIntent.PinApp(app.packageName))
                                contextMenuApp = null
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TextPrimary,
                            ),
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("App info") },
                        onClick = {
                            onIntent(DrawerIntent.AppInfoClicked(app.packageName))
                            contextMenuApp = null
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = TextSecondary,
                        ),
                    )
                }
            }
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