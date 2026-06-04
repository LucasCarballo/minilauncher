package com.minilauncher.data.model

/**
 * User-visible text that can be resolved from string resources or raw strings.
 * Avoids passing Context into ViewModels.
 */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Resource(val resId: Int) : UiText
}