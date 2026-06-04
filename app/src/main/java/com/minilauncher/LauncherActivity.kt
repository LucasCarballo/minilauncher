package com.minilauncher

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.minilauncher.feature.drawer.DrawerRoute
import com.minilauncher.feature.home.HomeRoute
import com.minilauncher.ui.theme.MiniLauncherTheme
import dagger.hilt.android.AndroidEntryPoint

sealed interface Screen {
    data object Home : Screen
    data object Drawer : Screen
}

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MiniLauncherTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState == Screen.Drawer) {
                            slideInVertically { it } togetherWith slideOutVertically { -it / 3 }
                        } else {
                            slideInVertically { -it / 3 } togetherWith slideOutVertically { it }
                        }
                    },
                    label = "screen_transition",
                ) { screen ->
                    when (screen) {
                        Screen.Home -> HomeRoute(
                            onSwipeUp = { currentScreen = Screen.Drawer },
                            onLaunchApp = { packageName, activityName ->
                                launchApp(packageName, activityName)
                            },
                        )
                        Screen.Drawer -> DrawerRoute(
                            onBack = { currentScreen = Screen.Home },
                            onLaunchApp = { packageName, activityName ->
                                launchApp(packageName, activityName)
                            },
                        )
                    }
                }
            }
        }
    }

    private fun launchApp(packageName: String, activityName: String) {
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
}