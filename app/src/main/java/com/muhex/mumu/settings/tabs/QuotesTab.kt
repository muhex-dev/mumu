package com.muhex.mumu.settings.tabs

import androidx.core.content.edit
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muhex.mumu.settings.SettingToggle

@Composable
fun QuotesTab(prefs: SharedPreferences, contentColor: Color) {
    var quotesEnabled by remember { mutableStateOf(prefs.getBoolean("quotes_enabled", true)) }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Quotes Settings",
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Inspirational quotes are displayed on your home screen to keep you motivated throughout the day.",
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        item {
            SettingToggle("Show Quotes", quotesEnabled, contentColor) {
                quotesEnabled = it
                prefs.edit { putBoolean("quotes_enabled", it) }
            }
        }
    }
}
