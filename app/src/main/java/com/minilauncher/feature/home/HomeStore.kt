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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
        observeTimeFormat()
        observeRecentApps()
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
                        intent.app.isWorkProfile,
                    )
                )
                viewModelScope.launch {
                    launcherPrefs.recordAppLaunch(intent.app.packageName)
                }
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
            is HomeIntent.OpenSettings -> {
                _effects.trySend(HomeEffect.NavigateToSettings)
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
                val time = formatTime(now, _state.value.timeFormat)
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

    private fun observeTimeFormat() {
        viewModelScope.launch {
            launcherPrefs.timeFormat.collect { format ->
                send(HomeIntent.TimeFormatLoaded(format))
                // Immediately update the time display with the new format
                val now = System.currentTimeMillis()
                send(HomeIntent.TimeUpdated(
                    getGreeting(now),
                    formatDate(now),
                    formatTime(now, format),
                ))
            }
        }
    }

    private fun observeRecentApps() {
        viewModelScope.launch {
            launcherPrefs.recentAppTimestamps.collect { timestamps ->
                send(HomeIntent.RecentAppsUpdated(timestamps))
            }
        }
    }

    private fun getGreeting(timestamp: Long): String {
        val hour = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .hour
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun formatDate(timestamp: Long): String {
        val date = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }

    private fun formatTime(timestamp: Long, timeFormat: String): String {
        val time = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val pattern = if (timeFormat == "12h") "h:mm a" else "HH:mm"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    private fun AppModel.toDisplayModel() = AppDisplayModel(
        label = label,
        packageName = packageName,
        activityName = activityName,
        isWorkProfile = isWorkProfile,
    )
}