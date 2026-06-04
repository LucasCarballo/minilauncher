package com.minilauncher.feature.settings

object SettingsReducer {

    operator fun invoke(state: SettingsUiState, intent: SettingsIntent): SettingsUiState =
        when (intent) {
            is SettingsIntent.UserNameLoaded -> state.copy(userName = intent.name)

            is SettingsIntent.TimeFormatLoaded -> state.copy(timeFormat = intent.format)

            is SettingsIntent.SuggestedNameLoaded -> state.copy(suggestedName = intent.name)

            is SettingsIntent.EditNameClicked -> state.copy(
                isEditingName = true,
                nameInput = state.userName.ifBlank { state.suggestedName },
            )

            is SettingsIntent.NameChanged -> state.copy(nameInput = intent.name)

            is SettingsIntent.SaveNameClicked -> state.copy(
                userName = state.nameInput,
                isEditingName = false,
            )

            is SettingsIntent.UseSuggestedName -> state.copy(
                nameInput = state.suggestedName,
            )

            is SettingsIntent.TimeFormatToggled -> state.copy(
                timeFormat = if (state.timeFormat == "24h") "12h" else "24h",
            )

            is SettingsIntent.BackClicked -> state // Side effect handled in Store
        }
}