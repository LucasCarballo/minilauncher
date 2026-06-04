package com.minilauncher.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle

@Composable
fun HomeRoute(
    onSwipeRight: () -> Unit,
    onLaunchApp: (packageName: String, activityName: String) -> Unit,
    viewModel: HomeStore = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.effects
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
                when (effect) {
                    is HomeEffect.LaunchApp -> onLaunchApp(effect.packageName, effect.activityName)
                    is HomeEffect.ShowToast -> { /* TODO: Toast */ }
                }
            }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::send,
        onSwipeRight = onSwipeRight,
    )
}