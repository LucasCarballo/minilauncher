package com.minilauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.minilauncher.data.model.AppListError
import com.minilauncher.data.model.AppModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for querying and launching installed applications.
 *
 * Uses LauncherApps API to query apps across all user profiles (personal + work).
 * Falls back to PackageManager if LauncherApps is unavailable.
 *
 * Caches the app list so Home and Drawer stores share a single query.
 * All queries run on [Dispatchers.IO] to avoid blocking the main thread.
 *
 * Defensive: all calls are wrapped in try/catch.
 * Never lets one bad package crash the launcher.
 */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val packageManager: PackageManager = context.packageManager
    private val launcherApps: LauncherApps? =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val userManager: UserManager? =
        context.getSystemService(Context.USER_SERVICE) as? UserManager

    /**
     * Maps component keys ("packageName/activityName") to their UserHandle
     * for launching work profile apps via LauncherApps.
     */
    private val userHandleMap = mutableMapOf<String, UserHandle>()

    /** Cache: shared between Home and Drawer stores to avoid duplicate PM queries. */
    private val _cachedApps = MutableStateFlow<List<AppModel>?>(null)
    val cachedApps: StateFlow<List<AppModel>?> = _cachedApps.asStateFlow()

    private val refreshMutex = Mutex()

    /**
     * Returns all launchable apps installed on the device, including work profile.
     * Uses LauncherApps API to query across all profiles.
     * Results are cached — subsequent calls return the cache.
     * Runs on [Dispatchers.IO] to avoid blocking the main thread.
     */
    suspend fun getInstalledApps(): Result<List<AppModel>> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        // Return cache if available
        _cachedApps.value?.let { return@withContext Result.success(it) }

        val result = try {
            val apps = queryLaunchableApps()
            Result.success(apps)
        } catch (e: android.os.DeadSystemException) {
            Result.failure(IllegalStateException(AppListError.PackageManagerDead.toString()))
        } catch (e: SecurityException) {
            Result.failure(IllegalStateException(AppListError.SecurityDenied.toString()))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(AppListError.Unknown(e.message ?: "Unknown error").toString()))
        }

        if (result.isSuccess) {
            _cachedApps.value = result.getOrThrow()
        }

        result
    }

    /**
     * Forces a fresh query, ignoring the cache.
     * Called when package change events are detected.
     */
    suspend fun refreshApps(): Result<List<AppModel>> = refreshMutex.withLock {
        _cachedApps.value = null
        getInstalledApps()
    }

    /**
     * Launch an app by its package, activity name, and profile.
     * Uses LauncherApps for work profile apps, falls back to Intent for personal apps.
     */
    fun launchApp(app: AppModel): Boolean {
        return try {
            val component = ComponentName(app.packageName, app.activityName)
            if (app.isWorkProfile && launcherApps != null) {
                val userHandle = userHandleMap[app.componentKey]
                    ?: return launchViaIntent(app)
                launcherApps.startMainActivity(component, userHandle, null, null)
                true
            } else {
                launchViaIntent(app)
            }
        } catch (e: Exception) {
            launchViaIntent(app)
        }
    }

    private fun launchViaIntent(app: AppModel): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun queryLaunchableApps(): List<AppModel> {
        userHandleMap.clear()

        // Try LauncherApps first — supports work profiles
        if (launcherApps != null) {
            return try {
                queryWithLauncherApps()
            } catch (e: SecurityException) {
                queryWithPackageManager()
            } catch (e: Exception) {
                queryWithPackageManager()
            }
        }

        return queryWithPackageManager()
    }

    private fun queryWithLauncherApps(): List<AppModel> {
        val myUserHandle = Process.myUserHandle()
        val apps = mutableListOf<AppModel>()

        // Get all user profiles (personal + work)
        val profiles = try {
            launcherApps?.profiles ?: listOf(myUserHandle)
        } catch (_: SecurityException) {
            listOf(myUserHandle)
        }

        for (profile in profiles) {
            val isWorkProfile = profile != myUserHandle

            // Query launchable activities for this profile
            val launcherActivities = try {
                launcherApps?.getActivityList(null, profile) ?: emptyList()
            } catch (_: SecurityException) {
                continue
            } catch (_: Exception) {
                continue
            }

            for (activityInfo in launcherActivities) {
                val packageName = activityInfo.applicationInfo.packageName
                // Skip self
                if (packageName == context.packageName) continue

                val label = activityInfo.label?.toString() ?: packageName
                val activityName = activityInfo.name

                // Store UserHandle for launching
                val key = "$packageName/$activityName"
                userHandleMap[key] = profile

                apps.add(
                    AppModel(
                        label = label,
                        packageName = packageName,
                        activityName = activityName,
                        isWorkProfile = isWorkProfile,
                    )
                )
            }
        }

        return apps.sortedWith(compareBy({ it.isWorkProfile }, { it.label.lowercase() }))
    }

    private fun queryWithPackageManager(): List<AppModel> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        @Suppress("DEPRECATION")
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        return resolveInfos
            .filterNot { it.activityInfo?.packageName == context.packageName }
            .mapNotNull { resolveInfo ->
                try {
                    val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                    AppModel(
                        label = resolveInfo.loadLabel(packageManager)?.toString()
                            ?: activityInfo.packageName,
                        packageName = activityInfo.packageName,
                        activityName = activityInfo.name ?: "",
                        isWorkProfile = false,
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    }

    private val AppModel.componentKey: String
        get() = "$packageName/$activityName"
}