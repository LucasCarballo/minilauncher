package com.minilauncher.feature.drawer

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
fun DrawerRoute(
    onBack: () -> Unit,
    onLaunchApp: (packageName: String, activityName: String) -> Unit,
    viewModel: DrawerStore = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        viewModel.effects
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
                when (effect) {
                    is DrawerEffect.LaunchApp -> onLaunchApp(effect.packageName, effect.activityName)
                    is DrawerEffect.ShowToast -> { /* TODO: Toast */ }
                }
            }
    }

    DrawerScreen(
        state = state,
        onIntent = viewModel::send,
        onBack = onBack,
    )
}