package com.minilauncher.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists crash logs to local storage so they survive process death.
 *
 * Crash logs are written synchronously by [CrashLogHandler] on the crashing thread
 * before the process dies. The viewer reads them on the main thread via Hilt injection.
 *
 * Keeps at most [MAX_LOGS] entries to avoid unbounded storage growth.
 */
@Singleton
class CrashLogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val logDir = File(context.filesDir, LOG_DIR_NAME)

    /** Queue used by [CrashLogHandler] to buffer logs before writing. */
    private val pendingLogs = ConcurrentLinkedQueue<String>()

    init {
        logDir.mkdirs()
        trimLogs()
    }

    /**
     * Writes a crash log entry synchronously. Safe to call from a crashing thread.
     * No coroutines — the process is about to die.
     */
    fun writeLog(timestamp: Long, threadName: String, throwable: Throwable) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
        val filename = SimpleDateFormat(FILE_DATE_FORMAT, Locale.US).format(Date(timestamp))

        val sb = StringBuilder()
        sb.appendLine("=== $date ===")
        sb.appendLine("Thread: $threadName")
        sb.appendLine("Exception: ${throwable.javaClass.name}")
        sb.appendLine("Message: ${throwable.message ?: "null"}")
        sb.appendLine("--- Stack trace ---")
        sb.appendLine(throwable.stackTraceToString())
        sb.appendLine()

        val file = File(logDir, "$filename.log")
        file.writeText(sb.toString())
        trimLogs()
    }

    /** Returns all crash logs, newest first. */
    fun getLogs(): List<CrashLogEntry> {
        return logDir.listFiles()
            ?.filter { it.extension == "log" }
            ?.sortedByDescending { it.name }
            ?.map { file ->
                CrashLogEntry(
                    filename = file.name,
                    content = file.readText(),
                    timestamp = file.lastModified(),
                )
            }
            ?: emptyList()
    }

    /** Deletes all crash logs. */
    fun clearLogs() {
        logDir.listFiles()?.forEach { it.delete() }
    }

    /** Keeps only the newest [MAX_LOGS] entries. */
    private fun trimLogs() {
        val files = logDir.listFiles()
            ?.filter { it.extension == "log" }
            ?.sortedByDescending { it.name }
            ?: return

        if (files.size > MAX_LOGS) {
            files.drop(MAX_LOGS).forEach { it.delete() }
        }
    }

    companion object {
        private const val LOG_DIR_NAME = "crash_logs"
        private const val FILE_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS"
        private const val MAX_LOGS = 10
    }
}

data class CrashLogEntry(
    val filename: String,
    val content: String,
    val timestamp: Long,
)