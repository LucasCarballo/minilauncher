package com.minilauncher.data.repository

import android.content.Context
import java.io.File

/**
 * Installs as the default [UncaughtExceptionHandler] to persist crash logs
 * before the process dies. Then delegates to the original handler so the
 * system can show the crash dialog or kill the process as usual.
 *
 * Must be installed early in [android.app.Application.onCreate] before
 * any other code runs.
 */
class CrashLogHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Persist the crash log synchronously — the process is about to die
        writeCrashLog(thread, throwable)

        // Delegate to the default handler so the system can show the crash dialog
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun writeCrashLog(thread: Thread, throwable: Throwable) {
        try {
            val logDir = File(context.filesDir, LOG_DIR_NAME)
            logDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date(timestamp))
            val filename = java.text.SimpleDateFormat(FILE_DATE_FORMAT, java.util.Locale.US)
                .format(java.util.Date(timestamp))

            val sb = StringBuilder()
            sb.appendLine("=== $date ===")
            sb.appendLine("Thread: ${thread.name}")
            sb.appendLine("Exception: ${throwable.javaClass.name}")
            sb.appendLine("Message: ${throwable.message ?: "null"}")
            sb.appendLine("--- Stack trace ---")
            sb.appendLine(throwable.stackTraceToString())

            // Include cause chain if present
            var cause = throwable.cause
            var depth = 0
            while (cause != null && depth < MAX_CAUSE_DEPTH) {
                sb.appendLine("--- Caused by: ${cause.javaClass.name} ---")
                sb.appendLine("Message: ${cause.message ?: "null"}")
                sb.appendLine(cause.stackTraceToString())
                cause = cause.cause
                depth++
            }

            val file = File(logDir, "$filename.log")
            file.writeText(sb.toString())

            // Trim old logs
            logDir.listFiles()
                ?.filter { it.extension == "log" }
                ?.sortedByDescending { it.name }
                ?.drop(MAX_LOGS)
                ?.forEach { it.delete() }
        } catch (_: Exception) {
            // If we can't write the crash log, don't make things worse.
            // The process is already dying.
        }
    }

    companion object {
        private const val LOG_DIR_NAME = "crash_logs"
        private const val FILE_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS"
        private const val MAX_LOGS = 10
        private const val MAX_CAUSE_DEPTH = 5
    }
}