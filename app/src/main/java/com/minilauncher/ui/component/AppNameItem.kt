package com.minilauncher.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.minilauncher.ui.theme.Spacing
import com.minilauncher.ui.theme.TextPrimary
import com.minilauncher.ui.theme.TextTertiary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNameItem(
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    query: String? = null,
    isWorkProfile: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val displayText = if (!query.isNullOrBlank()) {
        buildAnnotatedString {
            val lowerLabel = label.lowercase()
            val lowerQuery = query.lowercase()
            val startIndex = lowerLabel.indexOf(lowerQuery)
            if (startIndex >= 0) {
                append(label.substring(0, startIndex))
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append(label.substring(startIndex, startIndex + query.length))
                }
                append(label.substring(startIndex + query.length))
            } else {
                append(label)
            }
        }
    } else {
        buildAnnotatedString { append(label) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.appLineHeight)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.combinedClickable(onClick = onClick)
                }
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
        )
        if (isWorkProfile) {
            Text(
                text = " Work",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}