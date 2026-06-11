package com.minilauncher.feature.crashlog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.minilauncher.data.repository.CrashLogEntry
import com.minilauncher.ui.theme.Black
import com.minilauncher.ui.theme.Spacing
import com.minilauncher.ui.theme.TextPrimary
import com.minilauncher.ui.theme.TextSecondary
import com.minilauncher.ui.theme.TextTertiary
import kotlin.math.abs

private val SwipeDownThreshold = 50.dp

@Composable
fun CrashLogScreen(
    logs: List<CrashLogEntry>,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val thresholdPx = with(LocalDensity.current) { SwipeDownThreshold.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        var downChange: androidx.compose.ui.input.pointer.PointerInputChange? = null
                        while (downChange == null) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            downChange = event.changes.firstOrNull {
                                it.pressed && !it.previousPressed
                            }
                        }

                        val startX = downChange.position.x
                        val startY = downChange.position.y
                        val pointerId = downChange.id
                        var endX = startX
                        var endY = startY

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null) break
                            endX = change.position.x
                            endY = change.position.y
                            if (!change.pressed) break
                        }

                        val verticalDisplacement = endY - startY
                        val horizontalDisplacement = abs(startX - endX)

                        if (verticalDisplacement > thresholdPx &&
                            verticalDisplacement > horizontalDisplacement
                        ) {
                            currentOnBack()
                        }
                    }
                }
            }
            .padding(start = Spacing.md, top = Spacing.md, end = Spacing.md),
    ) {
        // Header
        Text(
            text = "Crash Logs",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No crash logs recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        } else {
            // Clear button
            Text(
                text = "Clear all logs",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.clickable(onClick = onClear),
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Log entries
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                logs.forEach { entry ->
                    Text(
                        text = entry.content.trimEnd(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.md),
                    )
                }
            }
        }

        // Bottom hint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.md),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                text = "↓ swipe down or back to return",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
            )
        }
    }
}