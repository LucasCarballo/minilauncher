package com.minilauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import com.minilauncher.feature.drawer.DrawerRoute
import com.minilauncher.feature.home.HomeRoute
import com.minilauncher.feature.settings.SettingsRoute
import com.minilauncher.ui.theme.MiniLauncherTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

sealed interface Screen {
    data object Home : Screen
    data object Drawer : Screen
    data object Settings : Screen
}

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    private val currentScreen = MutableStateFlow<Screen>(Screen.Home)

    private val launcherApps: LauncherApps? by lazy {
        getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Hide the system status bar — the launcher shows its own clock.
        // Immersive sticky mode: status bar appears temporarily on swipe-down.
        hideStatusBar()

        setContent {
            MiniLauncherTheme {
                val screen by currentScreen.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    // Home screen is always in the composition — no recreation on transitions
                    HomeRoute(
                        onSwipeUp = { currentScreen.value = Screen.Drawer },
                        onOpenSettings = { currentScreen.value = Screen.Settings },
                        onLaunchApp = { packageName, activityName, isWorkProfile ->
                            launchApp(packageName, activityName, isWorkProfile)
                        },
                    )

                    // Drawer slides up from bottom, overlaying the home screen
                    AnimatedVisibility(
                        visible = screen == Screen.Drawer,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it },
                    ) {
                        DrawerRoute(
                            onBack = { currentScreen.value = Screen.Home },
                            onLaunchApp = { packageName, activityName, isWorkProfile ->
                                launchApp(packageName, activityName, isWorkProfile)
                            },
                        )
                    }

                    // Settings slides up from bottom, overlaying the home screen
                    AnimatedVisibility(
                        visible = screen == Screen.Settings,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it },
                    ) {
                        SettingsRoute(
                            onBack = { currentScreen.value = Screen.Home },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Returning to the launcher always goes to the home screen.
        currentScreen.value = Screen.Home
    }

    private fun hideStatusBar() {
        val windowInsetsController = window.insetsController
        windowInsetsController?.let { controller ->
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(android.view.WindowInsets.Type.statusBars())
        }

        // Fallback for older APIs
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
    }

    private fun launchApp(packageName: String, activityName: String, isWorkProfile: Boolean) {
        if (isWorkProfile) {
            launchWorkProfileApp(packageName, activityName)
        } else {
            launchPersonalApp(packageName, activityName)
        }
    }

    private fun launchPersonalApp(packageName: String, activityName: String) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(packageName, activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "App not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchWorkProfileApp(packageName: String, activityName: String) {
        val component = ComponentName(packageName, activityName)
        val apps = launcherApps ?: run {
            // Fallback to regular launch if LauncherApps unavailable
            launchPersonalApp(packageName, activityName)
            return
        }

        // Find the work profile UserHandle
        val profiles = try {
            apps.profiles
        } catch (_: SecurityException) {
            listOf(Process.myUserHandle())
        }

        val workProfile = profiles.firstOrNull { it != Process.myUserHandle() }
        if (workProfile != null) {
            try {
                apps.startMainActivity(component, workProfile, null, null)
                return
            } catch (_: Exception) {
                // Fall through to regular launch
            }
        }

        // Fallback: try launching via intent
        launchPersonalApp(packageName, activityName)
    }
}