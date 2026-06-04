package com.minilauncher.feature.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minilauncher.data.model.AppListError
import com.minilauncher.data.model.AppModel
import com.minilauncher.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawerStore @Inject constructor(
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DrawerUiState())
    val state: StateFlow<DrawerUiState> = _state.asStateFlow()

    private val _effects = Channel<DrawerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadApps()
    }

    fun send(intent: DrawerIntent) {
        val newState = DrawerReducer(_state.value, intent)
        _state.value = newState
        handleSideEffects(intent)
    }

    private fun handleSideEffects(intent: DrawerIntent) {
        when (intent) {
            is DrawerIntent.AppClicked -> {
                _effects.trySend(
                    DrawerEffect.LaunchApp(
                        intent.app.packageName,
                        intent.app.activityName,
                    )
                )
            }
            is DrawerIntent.RetryClicked -> loadApps()
            else -> { /* Pure state transitions */ }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            appRepository.getInstalledApps()
                .onSuccess { apps ->
                    val displayModels = apps.map { it.toDisplayModel() }.toImmutableList()
                    send(DrawerIntent.AppsLoaded(displayModels))
                }
                .onFailure { error ->
                    val appError = when {
                        error.message?.contains("PackageManagerDead") == true ->
                            AppListError.PackageManagerDead
                        error.message?.contains("SecurityDenied") == true ->
                            AppListError.SecurityDenied
                        else -> AppListError.Unknown(error.message ?: "Unknown error")
                    }
                    send(DrawerIntent.AppsLoadFailed(appError))
                }
        }
    }

    private fun AppModel.toDisplayModel() = AppDisplayModel(
        label = label,
        packageName = packageName,
        activityName = activityName,
    )
}