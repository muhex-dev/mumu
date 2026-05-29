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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import com.muhex.mumu.settings.SectionHeader
import com.muhex.mumu.settings.SettingToggle

@Composable
fun QuotesTab(prefs: SharedPreferences, contentColor: Color) {
    var quotesEnabled by remember { mutableStateOf(prefs.getBoolean("quotes_enabled", true)) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 24.dp), 
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader("Inspiration")
            Text(
                "Inspirational quotes are displayed on your home screen to keep you motivated throughout the day.",
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
            )
        }
        item {
            SettingToggle(
                title = "Enable Quotes",
                checked = quotesEnabled,
                contentColor = contentColor,
                icon = Icons.Default.FormatQuote
            ) {
                quotesEnabled = it
                prefs.edit { putBoolean("quotes_enabled", it) }
            }
        }
    }
}
