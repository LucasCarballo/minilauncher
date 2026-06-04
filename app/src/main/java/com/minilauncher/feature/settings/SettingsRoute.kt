package com.minilauncher.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsStore = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(onBack = { viewModel.send(SettingsIntent.BackClicked) })

    LaunchedEffect(Unit) {
        viewModel.effects
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
                when (effect) {
                    is SettingsEffect.NavigateBack -> onBack()
                }
            }
    }

    SettingsScreen(
        state = state,
        onIntent = viewModel::send,
        onSwipeDown = onBack,
    )
}