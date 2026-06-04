package com.minilauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import com.minilauncher.data.model.AppListError
import com.minilauncher.data.model.AppModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for querying and launching installed applications.
 *
 * Defensive: all PackageManager calls are wrapped in try/catch.
 * Never lets one bad package crash the launcher.
 */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val packageManager: PackageManager = context.packageManager

    /**
     * Returns all launchable apps installed on the device.
     * Uses LauncherApps API on API 25+ for launcher-optimized queries,
     * falls back to queryIntentActivities.
     */
    fun getInstalledApps(): Result<List<AppModel>> = try {
        val apps = queryLaunchableApps()
        Result.success(apps)
    } catch (e: android.os.DeadSystemException) {
        Result.failure(IllegalStateException(AppListError.PackageManagerDead.toString()))
    } catch (e: SecurityException) {
        Result.failure(IllegalStateException(AppListError.SecurityDenied.toString()))
    } catch (e: Exception) {
        Result.failure(IllegalStateException(AppListError.Unknown(e.message ?: "Unknown error").toString()))
    }

    /**
     * Launch an app by its package and activity name.
     * Returns true if launch succeeded, false otherwise.
     */
    fun launchApp(app: AppModel): Boolean {
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
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    }
}