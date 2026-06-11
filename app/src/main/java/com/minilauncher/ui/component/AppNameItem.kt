package com.minilauncher.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
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
    val highlightColor = MaterialTheme.colorScheme.primary
    val displayText = remember(label, query, highlightColor) {
        buildHighlightedText(label, query, highlightColor)
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

/**
 * Builds an [AnnotatedString] highlighting the matching portion of [label].
 *
 * Uses character-by-character mapping to handle Unicode case folding correctly.
 * Characters like ß expand to "ss" when lowercased, so indices in the lowercased
 * string don't map 1:1 to the original. This function walks both strings together
 * to find the correct highlight range in the original label.
 */
private fun buildHighlightedText(
    label: String,
    query: String?,
    highlightColor: androidx.compose.ui.graphics.Color,
): AnnotatedString {
    if (query.isNullOrBlank()) return AnnotatedString(label)

    val lowerLabel = label.lowercase()
    val lowerQuery = query.lowercase()
    val matchStartInLower = lowerLabel.indexOf(lowerQuery)
    if (matchStartInLower < 0) return AnnotatedString(label)

    val matchEndInLower = matchStartInLower + lowerQuery.length

    // Map lower-string indices back to original-string indices.
    // Case folding can change string length (e.g., ß → ss), so we walk
    // the original string and track cumulative lowercased length.
    val highlightStart = mapLowerIndexToOriginal(label, matchStartInLower)
    val highlightEnd = mapLowerIndexToOriginal(label, matchEndInLower)

    return buildAnnotatedString {
        append(label.substring(0, highlightStart))
        withStyle(SpanStyle(color = highlightColor)) {
            append(label.substring(highlightStart, highlightEnd))
        }
        append(label.substring(highlightEnd))
    }
}

/**
 * Maps an index in the lowercased version of a string back to the
 * corresponding index in the original string.
 *
 * Case folding can change string length (e.g., "ß" → "ss"), so indices
 * don't map 1:1. This function walks the original string, tracking how
 * many lowercased characters each original character produces, until we've
 * consumed enough to reach [lowerIndex].
 */
private fun mapLowerIndexToOriginal(original: String, lowerIndex: Int): Int {
    var originalIdx = 0
    var lowerIdx = 0
    while (lowerIdx < lowerIndex && originalIdx < original.length) {
        // toString().lowercase() handles multi-character case expansions (e.g., ß → ss)
        lowerIdx += original[originalIdx].toString().lowercase().length
        originalIdx++
    }
    return originalIdx
}