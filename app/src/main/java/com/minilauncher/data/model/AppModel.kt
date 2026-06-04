package com.minilauncher.data.model

import android.graphics.drawable.Drawable

/**
 * Represents a launchable application.
 *
 * @property label         User-visible app name
 * @property packageName   Full package name (e.g. com.google.android.apps.messaging)
 * @property activityName  Fully qualified activity class name for launching
 * @property isWorkProfile Whether this app belongs to a work profile
 * @property icon          Lazy-loaded icon drawable — null until loaded on IO thread
 */
data class AppModel(
    val label: String,
    val packageName: String,
    val activityName: String,
    val isWorkProfile: Boolean = false,
    val icon: Drawable? = null,
) {
    val id: String get() = packageName
}