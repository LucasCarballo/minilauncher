package com.minilauncher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val Shapes = androidx.compose.material3.Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
)

private val LauncherColorScheme = darkColorScheme(
    background = Black,
    surface = Surface1,
    surfaceVariant = Surface2,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    primary = TextPrimary,
    secondary = TextSecondary,
    tertiary = TextTertiary,
)

@Composable
fun MiniLauncherTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LauncherColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}