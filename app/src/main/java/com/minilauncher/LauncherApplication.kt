package com.minilauncher

import android.app.Application
import com.minilauncher.data.repository.CrashLogHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Install crash log handler first — before any other code runs.
        // This ensures uncaught exceptions are persisted before the process dies.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(CrashLogHandler(this, defaultHandler))
    }
}