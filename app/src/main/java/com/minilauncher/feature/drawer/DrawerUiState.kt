package com.minilauncher.feature.drawer

import androidx.compose.runtime.Immutable
import com.minilauncher.data.model.AppDisplayModel
import com.minilauncher.data.model.AppListError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
data class DrawerUiState(
    val query: String = "",
    val allApps: ImmutableList<AppDisplayModel> = persistentListOf(),
    val filteredApps: ImmutableList<AppDisplayModel> = persistentListOf(),
    val pinnedPackageNames: ImmutableSet<String> = persistentSetOf(),
    val isLoading: Boolean = true,
    val error: AppListError? = null,
)

sealed interface DrawerIntent {
    data class QueryChanged(val query: String) : DrawerIntent
    data class AppsLoaded(val apps: ImmutableList<AppDisplayModel>) : DrawerIntent
    data class AppsLoadFailed(val error: AppListError) : DrawerIntent
    data class AppClicked(val app: AppDisplayModel) : DrawerIntent
    data class PinApp(val packageName: String) : DrawerIntent
    data class UnpinApp(val packageName: String) : DrawerIntent
    data class PinnedAppsLoaded(val packageNames: Set<String>?) : DrawerIntent
    data class AppInfoClicked(val packageName: String) : DrawerIntent
    data object RetryClicked : DrawerIntent
}

sealed interface DrawerEffect {
    data class LaunchApp(val packageName: String, val activityName: String, val isWorkProfile: Boolean) : DrawerEffect
    data class ShowAppInfo(val packageName: String) : DrawerEffect
    data class ShowToast(val message: String) : DrawerEffect
}