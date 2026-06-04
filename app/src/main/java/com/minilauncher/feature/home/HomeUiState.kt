package com.minilauncher.feature.home

import androidx.compose.runtime.Immutable
import com.minilauncher.data.model.AppDisplayModel
import com.minilauncher.data.model.AppListError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
data class HomeUiState(
    val greeting: String = "",
    val date: String = "",
    val time: String = "",
    val allApps: ImmutableList<AppDisplayModel> = persistentListOf(),
    val pinnedPackageNames: ImmutableSet<String> = persistentSetOf(),
    val pinnedApps: ImmutableList<AppDisplayModel> = persistentListOf(),
    val pinsCustomized: Boolean = false,
    val timeFormat: String = "24h",
    val isLoading: Boolean = true,
    val error: AppListError? = null,
    val userName: String = "",
)

sealed interface HomeIntent {
    data object LoadApps : HomeIntent
    data class AppsLoaded(val apps: ImmutableList<AppDisplayModel>) : HomeIntent
    data class AppsLoadFailed(val error: AppListError) : HomeIntent
    data class AppClicked(val app: AppDisplayModel) : HomeIntent
    data class PinApp(val packageName: String) : HomeIntent
    data class UnpinApp(val packageName: String) : HomeIntent
    data class PinnedAppsLoaded(val packageNames: Set<String>?) : HomeIntent
    data class AppInfoClicked(val packageName: String) : HomeIntent
    data class TimeFormatLoaded(val format: String) : HomeIntent
    data class TimeUpdated(val greeting: String, val date: String, val time: String) : HomeIntent
    data class UserNameLoaded(val name: String) : HomeIntent
    data object OpenSettings : HomeIntent
    data object RetryClicked : HomeIntent
}

sealed interface HomeEffect {
    data class LaunchApp(val packageName: String, val activityName: String) : HomeEffect
    data class ShowAppInfo(val packageName: String) : HomeEffect
    data object NavigateToSettings : HomeEffect
    data class ShowToast(val message: String) : HomeEffect
}