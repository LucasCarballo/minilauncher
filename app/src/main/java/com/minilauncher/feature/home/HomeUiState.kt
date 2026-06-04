package com.minilauncher.feature.home

import androidx.compose.runtime.Immutable
import com.minilauncher.data.model.AppListError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class HomeUiState(
    val greeting: String = "",
    val date: String = "",
    val time: String = "",
    val pinnedApps: ImmutableList<AppDisplayModel> = persistentListOf(),
    val allApps: ImmutableList<AppDisplayModel> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: AppListError? = null,
    val userName: String = "",
)

/**
 * Lightweight app model for UI display.
 * No Drawable reference — icons are loaded lazily.
 */
@Immutable
data class AppDisplayModel(
    val label: String,
    val packageName: String,
    val activityName: String,
)

sealed interface HomeIntent {
    data object LoadApps : HomeIntent
    data class AppsLoaded(val apps: ImmutableList<AppDisplayModel>) : HomeIntent
    data class AppsLoadFailed(val error: AppListError) : HomeIntent
    data class AppClicked(val app: AppDisplayModel) : HomeIntent
    data class RemoveFromPinned(val app: AppDisplayModel) : HomeIntent
    data class TimeUpdated(val greeting: String, val date: String, val time: String) : HomeIntent
    data class UserNameLoaded(val name: String) : HomeIntent
    data object RetryClicked : HomeIntent
}

sealed interface HomeEffect {
    data class LaunchApp(val packageName: String, val activityName: String) : HomeEffect
    data class ShowToast(val message: String) : HomeEffect
}