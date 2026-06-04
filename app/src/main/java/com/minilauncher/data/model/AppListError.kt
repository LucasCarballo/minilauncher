package com.minilauncher.data.model

/**
 * Typed errors for repository operations.
 * No exceptions thrown from repositories — errors are values.
 */
sealed interface AppListError {
    data object PackageManagerDead : AppListError
    data object SecurityDenied : AppListError
    data class Unknown(val message: String) : AppListError
}