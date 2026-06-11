package com.minilauncher.feature.drawer

import com.minilauncher.data.model.AppDisplayModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

object DrawerReducer {

    operator fun invoke(state: DrawerUiState, intent: DrawerIntent): DrawerUiState =
        when (intent) {
            is DrawerIntent.QueryChanged -> state.copy(query = intent.query)

            is DrawerIntent.QueryFiltered -> {
                val filtered = filterApps(state.allApps, intent.query)
                state.copy(filteredApps = filtered)
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

            is DrawerIntent.AppClicked -> state.copy(query = "", filteredApps = state.allApps)

            is DrawerIntent.PinApp -> state.copy(
                pinnedPackageNames = (state.pinnedPackageNames + intent.packageName).toImmutableSet(),
            )

            is DrawerIntent.UnpinApp -> state.copy(
                pinnedPackageNames = (state.pinnedPackageNames - intent.packageName).toImmutableSet(),
            )

            is DrawerIntent.PinnedAppsLoaded -> state.copy(
                pinnedPackageNames = intent.packageNames?.toImmutableSet() ?: persistentSetOf(),
            )

            is DrawerIntent.AppInfoClicked -> state // Side effect handled in Store

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