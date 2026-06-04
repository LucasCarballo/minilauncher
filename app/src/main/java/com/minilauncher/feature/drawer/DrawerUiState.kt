package com.minilauncher.feature.drawer

import androidx.compose.runtime.Immutable
import com.minilauncher.data.model.AppListError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class DrawerUiState(
    val query: String = "",
    val allApps: ImmutableList<AppDisplayModel> = persistentListOf(),
    val filteredApps: ImmutableList<AppDisplayModel> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: AppListError? = null,
)

@Immutable
data class AppDisplayModel(
    val label: String,
    val packageName: String,
    val activityName: String,
)

sealed interface DrawerIntent {
    data class QueryChanged(val query: String) : DrawerIntent
    data class AppsLoaded(val apps: ImmutableList<AppDisplayModel>) : DrawerIntent
    data class AppsLoadFailed(val error: AppListError) : DrawerIntent
    data class AppClicked(val app: AppDisplayModel) : DrawerIntent
    data object RetryClicked : DrawerIntent
}

sealed interface DrawerEffect {
    data class LaunchApp(val packageName: String, val activityName: String) : DrawerEffect
    data class ShowToast(val message: String) : DrawerEffect
}