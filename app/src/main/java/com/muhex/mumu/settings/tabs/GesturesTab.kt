package com.muhex.mumu.settings.tabs

import androidx.core.content.edit
import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.muhex.mumu.AppModel
import com.muhex.mumu.AppRepository
import com.muhex.mumu.settings.*

@Composable
fun GesturesTab(repository: AppRepository, prefs: SharedPreferences, contentColor: Color) {
    val gestureOptions = mapOf(
        "none" to "None",
        "app_drawer" to "App Drawer",
        "notifications" to "Notifications",
        "quick_settings" to "Quick Settings",
        "lock_screen" to "Lock Screen",
        "quick_menu" to "Quick Menu",
        "open_app" to "Open App"
    )
    var expandedItem by remember { mutableStateOf<String?>(null) }
    var selectingAppForKey by remember { mutableStateOf<String?>(null) }
    val allApps by produceState(initialValue = emptyList<AppModel>()) {
        value = repository.getAllApps()
    }

    val gestureKeys = remember {
        listOf("gesture_swipe_up", "gesture_swipe_down", "gesture_swipe_left", "gesture_swipe_right", "gesture_double_tap")
    }

    val gestureAssignments = remember {
        mutableStateMapOf<String, String>().also { map ->
            gestureKeys.forEach { key ->
                map[key] = prefs.getString(key, "none") ?: "none"
            }
        }
    }
    val gestureAppIds = remember {
        mutableStateMapOf<String, String>().also { map ->
            gestureKeys.forEach { key ->
                val appIdKey = "${key}_app_id"
                map[appIdKey] = prefs.getString(appIdKey, "") ?: ""
            }
        }
    }

    if (selectingAppForKey != null) {
        AlertDialog(
            onDismissRequest = { selectingAppForKey = null },
            title = { Text("Select App", color = contentColor) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        items(allApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val appId = repository.getAppUniqueId(app)
                                        val key = selectingAppForKey!!
                                        
                                        gestureAppIds["${key}_app_id"] = appId
                                        gestureAssignments[key] = "open_app"

                                        prefs.edit {
                                            putString("${key}_app_id", appId)
                                            putString(key, "open_app")
                                        }
                                        selectingAppForKey = null
                                        expandedItem = null
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(app.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    app.label,
                                    modifier = Modifier.padding(start = 12.dp),
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectingAppForKey = null }) {
                    Text("Cancel", color = Color(0xFF4CAF50))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = contentColor
        )
    }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            "Up" to "gesture_swipe_up",
            "Down" to "gesture_swipe_down",
            "Left" to "gesture_swipe_left",
            "Right" to "gesture_swipe_right",
            "Double Tap" to "gesture_double_tap"
        ).forEach { (label, key) ->
            item {
                val current = gestureAssignments[key] ?: "none"
                val assignedAppId = gestureAppIds["${key}_app_id"] ?: ""
                val assignedAppLabel = if (current == "open_app" && assignedAppId.isNotEmpty()) {
                    allApps.find { repository.getAppUniqueId(it) == assignedAppId }?.label ?: "Unknown App"
                } else null

                val displayValue = if (assignedAppLabel != null) "Open $assignedAppLabel" else gestureOptions[current] ?: "None"

                SettingItem(
                    icon = Icons.Default.TouchApp,
                    title = label,
                    value = displayValue,
                    contentColor = contentColor,
                    isExpanded = expandedItem == key,
                    onClick = { expandedItem = if (expandedItem == key) null else key }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gestureOptions.forEach { (optionKey, optionLabel) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (optionKey == "open_app") {
                                            selectingAppForKey = key
                                        } else {
                                            gestureAssignments[key] = optionKey
                                            prefs.edit { putString(key, optionKey) }
                                            expandedItem = null
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = current == optionKey, onClick = null)
                                Text(optionLabel, color = contentColor, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
