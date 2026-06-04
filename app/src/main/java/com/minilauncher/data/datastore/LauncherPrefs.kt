package com.minilauncher.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_prefs",
)

/**
 * Persists user preferences: pinned apps and display name.
 * Uses DataStore Preferences for simple key-value storage.
 */
@Singleton
class LauncherPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_PINNED_APPS = stringSetPreferencesKey("pinned_apps")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_TIME_FORMAT = stringPreferencesKey("time_format")
    }

    /**
     * Emits the set of pinned app package names.
     * Returns null when the key doesn't exist (user hasn't customized pins yet),
     * allowing the UI to distinguish "not customized" from "customized to empty".
     */
    val pinnedApps: Flow<Set<String>?> = context.launcherDataStore.data.map { prefs ->
        if (KEY_PINNED_APPS in prefs) prefs[KEY_PINNED_APPS] else null
    }

    val userName: Flow<String> = context.launcherDataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: ""
    }

    val timeFormat: Flow<String> = context.launcherDataStore.data.map { prefs ->
        prefs[KEY_TIME_FORMAT] ?: "24h"
    }

    suspend fun setPinnedApps(apps: Set<String>) {
        context.launcherDataStore.edit { prefs ->
            prefs[KEY_PINNED_APPS] = apps
        }
    }

    suspend fun setUserName(name: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
        }
    }

    suspend fun setTimeFormat(format: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[KEY_TIME_FORMAT] = format
        }
    }
}