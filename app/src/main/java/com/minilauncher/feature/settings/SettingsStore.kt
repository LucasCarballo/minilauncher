package com.minilauncher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minilauncher.data.datastore.LauncherPrefs
import com.minilauncher.data.repository.DeviceOwnerNameProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsStore @Inject constructor(
    private val launcherPrefs: LauncherPrefs,
    private val deviceOwnerNameProvider: DeviceOwnerNameProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadSuggestedName()
        observeUserName()
        observeTimeFormat()
    }

    fun send(intent: SettingsIntent) {
        val newState = SettingsReducer(_state.value, intent)
        _state.value = newState
        handleSideEffects(intent)
    }

    private fun handleSideEffects(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SaveNameClicked -> {
                viewModelScope.launch {
                    launcherPrefs.setUserName(_state.value.userName)
                }
            }
            is SettingsIntent.TimeFormatToggled -> {
                viewModelScope.launch {
                    launcherPrefs.setTimeFormat(_state.value.timeFormat)
                }
            }
            is SettingsIntent.BackClicked -> {
                _effects.trySend(SettingsEffect.NavigateBack)
            }
            else -> { /* Pure state transitions */ }
        }
    }

    private fun loadSuggestedName() {
        viewModelScope.launch {
            val suggestedName = deviceOwnerNameProvider.getOwnerName()
            send(SettingsIntent.SuggestedNameLoaded(suggestedName))
        }
    }

    private fun observeUserName() {
        viewModelScope.launch {
            launcherPrefs.userName.collect { name ->
                send(SettingsIntent.UserNameLoaded(name))
            }
        }
    }

    private fun observeTimeFormat() {
        viewModelScope.launch {
            launcherPrefs.timeFormat.collect { format ->
                send(SettingsIntent.TimeFormatLoaded(format))
            }
        }
    }
}