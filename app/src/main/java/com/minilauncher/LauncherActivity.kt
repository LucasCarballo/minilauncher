package com.minilauncher

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.minilauncher.data.repository.AppRepository
import com.minilauncher.feature.drawer.AppDisplayModel
import com.minilauncher.feature.drawer.DrawerRoute
import com.minilauncher.feature.home.HomeRoute
import com.minilauncher.ui.theme.MiniLauncherTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed interface Screen {
    data object Home : Screen
    data object Drawer : Screen
}

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    @Inject
    lateinit var appRepository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MiniLauncherTheme {
                var currentScreen by androidx.compose.runtime.mutableStateOf<Screen>(Screen.Home)

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState == Screen.Drawer) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
                        } else {
                            slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
                        }
                    },
                    label = "screen_transition",
                ) { screen ->
                    when (screen) {
                        Screen.Home -> HomeRoute(
                            onSwipeRight = { currentScreen = Screen.Drawer },
                        )
                        Screen.Drawer -> DrawerRoute(
                            onBack = { currentScreen = Screen.Home },
                        )
                    }
                }
            }
        }
    }

    fun launchApp(packageName: String, activityName: String) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(packageName, activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // App not found or can't launch — silently handle
        }
    }
}