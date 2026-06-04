package com.minilauncher.feature.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle

@Composable
fun HomeRoute(
    onSwipeUp: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchApp: (packageName: String, activityName: String) -> Unit,
    viewModel: HomeStore = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
                when (effect) {
                    is HomeEffect.LaunchApp -> onLaunchApp(effect.packageName, effect.activityName)
                    is HomeEffect.ShowAppInfo -> {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", effect.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                    is HomeEffect.NavigateToSettings -> onOpenSettings()
                    is HomeEffect.ShowToast -> { /* TODO: Toast */ }
                }
            }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::send,
        onSwipeUp = onSwipeUp,
    )
}