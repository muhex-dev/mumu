package com.muhex.mumu

import android.graphics.drawable.Drawable
import android.os.UserHandle

/**
 * AppModel: A clean data representation of an installed application.
 * 
 * @property label The display name of the app (e.g., "Chrome").
 * @property packageName The unique package identifier (e.g., "com.android.chrome").
 * @property className The main activity class name.
 * @property userHandle Represents the user profile (e.g., Personal vs. Work).
 * @property lastUpdated A timestamp used to force UI refreshes via DiffUtil.
 */
data class AppModel(
    val label: String,
    val packageName: String,
    val className: String,
    val userHandle: UserHandle,
    val lastUpdated: Long = 0L
)
