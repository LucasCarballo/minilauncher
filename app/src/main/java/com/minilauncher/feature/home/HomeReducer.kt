package com.minilauncher.feature.home

import com.minilauncher.data.model.AppDisplayModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

object HomeReducer {

    operator fun invoke(state: HomeUiState, intent: HomeIntent): HomeUiState =
        when (intent) {
            is HomeIntent.LoadApps -> state.copy(isLoading = true, error = null)

            is HomeIntent.AppsLoaded -> {
                val newPinnedApps = computePinnedApps(
                    allApps = intent.apps,
                    pinnedPackageNames = state.pinnedPackageNames,
                    pinsCustomized = state.pinsCustomized,
                )
                state.copy(
                    allApps = intent.apps,
                    pinnedApps = newPinnedApps,
                    isLoading = false,
                    error = null,
                )
            }

            is HomeIntent.AppsLoadFailed -> state.copy(
                isLoading = false,
                error = intent.error,
            )

            is HomeIntent.AppClicked -> state // Side effect handled in Store

            is HomeIntent.PinApp -> {
                val newPinnedPackageNames = (state.pinnedPackageNames + intent.packageName).toImmutableSet()
                val newPinnedApps = computePinnedApps(
                    allApps = state.allApps,
                    pinnedPackageNames = newPinnedPackageNames,
                    pinsCustomized = true,
                )
                state.copy(
                    pinnedPackageNames = newPinnedPackageNames,
                    pinnedApps = newPinnedApps,
                    pinsCustomized = true,
                )
            }

            is HomeIntent.UnpinApp -> {
                val newPinnedPackageNames = (state.pinnedPackageNames - intent.packageName).toImmutableSet()
                val newPinnedApps = computePinnedApps(
                    allApps = state.allApps,
                    pinnedPackageNames = newPinnedPackageNames,
                    pinsCustomized = true,
                )
                state.copy(
                    pinnedPackageNames = newPinnedPackageNames,
                    pinnedApps = newPinnedApps,
                    pinsCustomized = true,
                )
            }

            is HomeIntent.PinnedAppsLoaded -> {
                val pinsCustomized = intent.packageNames != null
                val pinnedPackageNames = intent.packageNames?.toImmutableSet() ?: persistentSetOf()
                val newPinnedApps = computePinnedApps(
                    allApps = state.allApps,
                    pinnedPackageNames = pinnedPackageNames,
                    pinsCustomized = pinsCustomized,
                )
                state.copy(
                    pinnedPackageNames = pinnedPackageNames,
                    pinnedApps = newPinnedApps,
                    pinsCustomized = pinsCustomized,
                )
            }

            is HomeIntent.AppInfoClicked -> state // Side effect handled in Store

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
     * Computes the list of pinned apps from all apps and pinned package names.
     * If pins have not been customized yet, returns the first 5 apps as defaults.
     * If pins are customized, returns only the apps whose package names are in the pinned set,
     * preserving the order they appear in allApps.
     */
    private fun computePinnedApps(
        allApps: ImmutableList<AppDisplayModel>,
        pinnedPackageNames: ImmutableSet<String>,
        pinsCustomized: Boolean,
    ): ImmutableList<AppDisplayModel> {
        if (!pinsCustomized && allApps.isNotEmpty()) {
            return allApps.take(minOf(5, allApps.size)).toImmutableList()
        }
        if (pinnedPackageNames.isEmpty()) return persistentListOf()
        return allApps.filter { it.packageName in pinnedPackageNames }.toImmutableList()
    }
}