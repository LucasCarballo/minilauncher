package com.minilauncher.feature.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minilauncher.data.datastore.LauncherPrefs
import com.minilauncher.data.model.AppDisplayModel
import com.minilauncher.data.model.AppListError
import com.minilauncher.data.model.AppModel
import com.minilauncher.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class DrawerStore @Inject constructor(
    private val appRepository: AppRepository,
    private val launcherPrefs: LauncherPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(DrawerUiState())
    val state: StateFlow<DrawerUiState> = _state.asStateFlow()

    private val _effects = Channel<DrawerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Conflated channel: only the latest query matters, drop stale keystrokes. */
    private val queryChannel = Channel<String>(Channel.CONFLATED)

    init {
        loadApps()
        observePinnedApps()
        observeQueryChanges()
    }

    fun send(intent: DrawerIntent) {
        when (intent) {
            is DrawerIntent.QueryChanged -> {
                // Update query text immediately for responsive typing
                _state.value = DrawerReducer(_state.value, intent)
                if (intent.query.isBlank()) {
                    // Clear filter immediately — no debounce needed
                    _state.value = DrawerReducer(_state.value, DrawerIntent.QueryFiltered(""))
                } else {
                    // Debounce the expensive filtering
                    queryChannel.trySend(intent.query)
                }
            }
            else -> {
                val newState = DrawerReducer(_state.value, intent)
                _state.value = newState
                handleSideEffects(intent)
            }
        }
    }

    private fun observeQueryChanges() {
        viewModelScope.launch {
            queryChannel.receiveAsFlow()
                .debounce(QUERY_DEBOUNCE_MS)
                .collect { debouncedQuery ->
                    // Skip stale debounced filters — the query may have been cleared
                    // (e.g., user clicked an app) since this filter was scheduled
                    if (_state.value.query == debouncedQuery) {
                        _state.value = DrawerReducer(
                            _state.value,
                            DrawerIntent.QueryFiltered(debouncedQuery),
                        )
                    }
                }
        }
    }

    private fun handleSideEffects(intent: DrawerIntent) {
        when (intent) {
            is DrawerIntent.AppClicked -> {
                _effects.trySend(
                    DrawerEffect.LaunchApp(
                        intent.app.packageName,
                        intent.app.activityName,
                        intent.app.isWorkProfile,
                    )
                )
                viewModelScope.launch {
                    launcherPrefs.recordAppLaunch(intent.app.packageName)
                }
            }
            is DrawerIntent.AppInfoClicked -> {
                _effects.trySend(DrawerEffect.ShowAppInfo(intent.packageName))
            }
            is DrawerIntent.PinApp -> {
                viewModelScope.launch {
                    val currentPins = _state.value.pinnedPackageNames.toSet()
                    launcherPrefs.setPinnedApps(currentPins + intent.packageName)
                }
            }
            is DrawerIntent.UnpinApp -> {
                viewModelScope.launch {
                    val currentPins = _state.value.pinnedPackageNames.toSet()
                    launcherPrefs.setPinnedApps(currentPins - intent.packageName)
                }
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

    private fun observePinnedApps() {
        viewModelScope.launch {
            launcherPrefs.pinnedApps.collect { pinnedSet ->
                send(DrawerIntent.PinnedAppsLoaded(pinnedSet))
            }
        }
    }

    private fun AppModel.toDisplayModel() = AppDisplayModel(
        label = label,
        packageName = packageName,
        activityName = activityName,
        isWorkProfile = isWorkProfile,
    )

    companion object {
        private const val QUERY_DEBOUNCE_MS = 150L
    }
}