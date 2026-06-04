package com.minilauncher.feature.settings

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsUiState(
    val userName: String = "",
    val timeFormat: String = "24h",
    val isEditingName: Boolean = false,
    val nameInput: String = "",
    val suggestedName: String = "",
)

sealed interface SettingsIntent {
    data class UserNameLoaded(val name: String) : SettingsIntent
    data class TimeFormatLoaded(val format: String) : SettingsIntent
    data class SuggestedNameLoaded(val name: String) : SettingsIntent
    data object EditNameClicked : SettingsIntent
    data class NameChanged(val name: String) : SettingsIntent
    data object SaveNameClicked : SettingsIntent
    data object UseSuggestedName : SettingsIntent
    data object TimeFormatToggled : SettingsIntent
    data object BackClicked : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
}