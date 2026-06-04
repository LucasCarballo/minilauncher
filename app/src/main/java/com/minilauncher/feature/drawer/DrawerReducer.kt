package com.minilauncher.feature.drawer

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

object DrawerReducer {

    operator fun invoke(state: DrawerUiState, intent: DrawerIntent): DrawerUiState =
        when (intent) {
            is DrawerIntent.QueryChanged -> {
                val filtered = filterApps(state.allApps, intent.query)
                state.copy(query = intent.query, filteredApps = filtered)
            }

            is DrawerIntent.AppsLoaded -> {
                val filtered = filterApps(intent.apps, state.query)
                state.copy(
                    allApps = intent.apps,
                    filteredApps = filtered,
                    isLoading = false,
                    error = null,
                )
            }

            is DrawerIntent.AppsLoadFailed -> state.copy(
                isLoading = false,
                error = intent.error,
            )

            is DrawerIntent.AppClicked -> state // Side effect handled in Store

            is DrawerIntent.RetryClicked -> state.copy(isLoading = true, error = null)
        }

    private fun filterApps(
        apps: ImmutableList<AppDisplayModel>,
        query: String,
    ): ImmutableList<AppDisplayModel> {
        if (query.isBlank()) return apps
        val lowerQuery = query.lowercase()
        return apps.filter { it.label.lowercase().contains(lowerQuery) }.toImmutableList()
    }
}