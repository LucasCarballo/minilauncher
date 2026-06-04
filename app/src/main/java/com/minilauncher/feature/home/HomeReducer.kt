package com.minilauncher.feature.home

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

object HomeReducer {

    operator fun invoke(state: HomeUiState, intent: HomeIntent): HomeUiState =
        when (intent) {
            is HomeIntent.LoadApps -> state.copy(isLoading = true, error = null)

            is HomeIntent.AppsLoaded -> state.copy(
                allApps = intent.apps,
                pinnedApps = filterPinned(state, intent.apps),
                isLoading = false,
                error = null,
            )

            is HomeIntent.AppsLoadFailed -> state.copy(
                isLoading = false,
                error = intent.error,
            )

            is HomeIntent.AppClicked -> state // Side effect handled in Store

            is HomeIntent.RemoveFromPinned -> state.copy(
                pinnedApps = state.pinnedApps
                    .filterNot { it.packageName == intent.app.packageName }
                    .toImmutableList(),
            )

            is HomeIntent.TimeUpdated -> state.copy(
                greeting = intent.greeting,
                date = intent.date,
                time = intent.time,
            )

            is HomeIntent.UserNameLoaded -> state.copy(
                userName = intent.name,
            )

            is HomeIntent.RetryClicked -> state.copy(isLoading = true, error = null)
        }

    /**
     * Filters all apps to only those in the pinned set.
     * Preserves pinned order, falls back to first 5 apps if no pins configured.
     */
    private fun filterPinned(
        state: HomeUiState,
        apps: ImmutableList<AppDisplayModel>,
    ): ImmutableList<AppDisplayModel> {
        // If no pinned apps configured yet, show first 5
        if (state.pinnedApps.isEmpty() && apps.isNotEmpty()) {
            return apps.take(minOf(5, apps.size)).toImmutableList()
        }
        // Filter all apps to only pinned ones, preserving pinned order
        val pinnedSet = state.pinnedApps.map { it.packageName }.toSet()
        return apps.filter { it.packageName in pinnedSet }.toImmutableList()
    }
}