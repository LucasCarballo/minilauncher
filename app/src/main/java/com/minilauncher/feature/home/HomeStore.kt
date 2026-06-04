package com.minilauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minilauncher.data.datastore.LauncherPrefs
import com.minilauncher.data.model.AppDisplayModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeStore @Inject constructor(
    private val appRepository: AppRepository,
    private val launcherPrefs: LauncherPrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadApps()
        startTimeUpdates()
        observeUserName()
        observePinnedApps()
    }

    fun send(intent: HomeIntent) {
        val newState = HomeReducer(_state.value, intent)
        _state.value = newState
        handleSideEffects(intent)
    }

    private fun handleSideEffects(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.AppClicked -> {
                _effects.trySend(
                    HomeEffect.LaunchApp(
                        intent.app.packageName,
                        intent.app.activityName,
                    )
                )
            }
            is HomeIntent.AppInfoClicked -> {
                _effects.trySend(HomeEffect.ShowAppInfo(intent.packageName))
            }
            is HomeIntent.PinApp -> {
                viewModelScope.launch {
                    val currentPins = _state.value.pinnedPackageNames.toSet()
                    launcherPrefs.setPinnedApps(currentPins + intent.packageName)
                }
            }
            is HomeIntent.UnpinApp -> {
                viewModelScope.launch {
                    val currentPins = _state.value.pinnedPackageNames.toSet()
                    launcherPrefs.setPinnedApps(currentPins - intent.packageName)
                }
            }
            is HomeIntent.RetryClicked -> loadApps()
            else -> { /* Pure state transitions */ }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            appRepository.getInstalledApps()
                .onSuccess { apps ->
                    val displayModels = apps.map { it.toDisplayModel() }.toImmutableList()
                    send(HomeIntent.AppsLoaded(displayModels))
                }
                .onFailure { error ->
                    val appError = when {
                        error.message?.contains("PackageManagerDead") == true ->
                            AppListError.PackageManagerDead
                        error.message?.contains("SecurityDenied") == true ->
                            AppListError.SecurityDenied
                        else -> AppListError.Unknown(error.message ?: "Unknown error")
                    }
                    send(HomeIntent.AppsLoadFailed(appError))
                }
        }
    }

    private fun startTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val greeting = getGreeting(now)
                val date = formatDate(now)
                val time = formatTime(now)
                send(HomeIntent.TimeUpdated(greeting, date, time))
                kotlinx.coroutines.delay(60_000L)
            }
        }
    }

    private fun observeUserName() {
        viewModelScope.launch {
            launcherPrefs.userName.collect { name ->
                send(HomeIntent.UserNameLoaded(name))
            }
        }
    }

    private fun observePinnedApps() {
        viewModelScope.launch {
            launcherPrefs.pinnedApps.collect { pinnedSet ->
                send(HomeIntent.PinnedAppsLoaded(pinnedSet))
            }
        }
    }

    private fun getGreeting(timestamp: Long): String {
        val hour = SimpleDateFormat("HH", Locale.getDefault()).format(Date(timestamp)).toInt()
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    private fun AppModel.toDisplayModel() = AppDisplayModel(
        label = label,
        packageName = packageName,
        activityName = activityName,
    )
}