package com.minilauncher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minilauncher.ui.theme.Black
import com.minilauncher.ui.theme.Spacing
import com.minilauncher.ui.theme.Surface2
import com.minilauncher.ui.theme.TextPrimary
import com.minilauncher.ui.theme.TextSecondary
import com.minilauncher.ui.theme.TextTertiary
import kotlin.math.abs

private val SwipeDownThreshold = 50.dp

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onSwipeDown: () -> Unit,
) {
    val currentOnSwipeDown by rememberUpdatedState(onSwipeDown)
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

                        if (verticalDisplacement > thresholdPx && verticalDisplacement > horizontalDisplacement) {
                            currentOnSwipeDown()
                        }
                    }
                }
            }
            .padding(start = Spacing.md, top = Spacing.md, end = Spacing.md),
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Name section
        SettingsSectionLabel("Name")
        if (state.isEditingName) {
            TextField(
                value = state.nameInput,
                onValueChange = { onIntent(SettingsIntent.NameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TextPrimary,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onIntent(SettingsIntent.SaveNameClicked) },
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Surface2,
                    unfocusedContainerColor = Surface2,
                    cursorColor = TextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            if (state.suggestedName.isNotBlank() && state.nameInput.isBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Use \"${state.suggestedName}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.clickable {
                        onIntent(SettingsIntent.UseSuggestedName)
                    },
                )
            }
        } else {
            SettingsRow(
                label = state.userName.ifBlank { "Not set" },
                onClick = { onIntent(SettingsIntent.EditNameClicked) },
            )
            if (state.userName.isBlank() && state.suggestedName.isNotBlank()) {
                Text(
                    text = "Suggestion: ${state.suggestedName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Time format section
        SettingsSectionLabel("Time format")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.appLineHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "24-hour",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.timeFormat == "12h",
                onCheckedChange = { onIntent(SettingsIntent.TimeFormatToggled) },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = TextPrimary,
                    checkedThumbColor = Color.Black,
                    uncheckedTrackColor = Surface2,
                    uncheckedThumbColor = TextSecondary,
                ),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = "12h",
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.timeFormat == "12h") TextPrimary else TextTertiary,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // About section
        SettingsSectionLabel("About")
        SettingsRow(
            label = "MiniLauncher",
            onClick = { /* No-op for now */ },
        )
        Text(
            text = "A minimal Android launcher",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
        )

        // Bottom hint
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = Spacing.md),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                text = "↓ swipe down or back to return",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = TextTertiary,
            )
        }
    }
}

@Composable
private fun SettingsSectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary,
        modifier = Modifier.padding(bottom = Spacing.xs),
    )
}

@Composable
private fun SettingsRow(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Spacing.appLineHeight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (label == "Not set") TextTertiary else TextPrimary,
        )
    }
}