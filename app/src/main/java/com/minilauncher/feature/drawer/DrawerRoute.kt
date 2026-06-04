package com.minilauncher.feature.drawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle

@Composable
fun DrawerRoute(
    onBack: () -> Unit,
    viewModel: DrawerStore = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.effects
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
                when (effect) {
                    is DrawerEffect.LaunchApp -> {
                        // Launch handled by activity
                    }
                    is DrawerEffect.ShowToast -> {
                        // Toast handled by activity
                    }
                }
            }
    }

    DrawerScreen(
        state = state,
        onIntent = viewModel::send,
        onBack = onBack,
    )
}