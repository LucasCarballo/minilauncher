package com.minilauncher.data.model

import androidx.compose.runtime.Immutable

/**
 * Lightweight app model for UI display.
 * No Drawable reference — icons are loaded lazily.
 */
@Immutable
data class AppDisplayModel(
    val label: String,
    val packageName: String,
    val activityName: String,
    val isWorkProfile: Boolean = false,
)