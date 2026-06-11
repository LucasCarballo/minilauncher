package com.minilauncher.feature.crashlog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minilauncher.data.repository.CrashLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Composable
fun CrashLogRoute(
    onBack: () -> Unit,
    viewModel: CrashLogViewModel = hiltViewModel(),
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    CrashLogScreen(
        logs = logs,
        onBack = onBack,
        onClear = { viewModel.clearLogs() },
    )
}

@HiltViewModel
class CrashLogViewModel @Inject constructor(
    private val crashLogRepository: CrashLogRepository,
) : androidx.lifecycle.ViewModel() {

    private val _logs = MutableStateFlow(crashLogRepository.getLogs())
    val logs: StateFlow<List<com.minilauncher.data.repository.CrashLogEntry>> = _logs.asStateFlow()

    fun clearLogs() {
        crashLogRepository.clearLogs()
        _logs.value = emptyList()
    }
}