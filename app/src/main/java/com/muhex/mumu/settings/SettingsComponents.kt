package com.muhex.mumu.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.muhex.mumu.AppModel
import com.muhex.mumu.AppRepository

val colorPalette = listOf(
    Color.White, Color.Black, Color(0xFF8E8E93), Color(0xFFFF3B30), Color(0xFFFF2D55), Color(0xFFAF52DE), Color(0xFF5856D6), Color(0xFF007AFF),
    Color(0xFF34C759), Color(0xFF5AC8FA), Color(0xFF00C7BE), Color(0xFF30B0C7), Color(0xFF4CD964), Color(0xFF8BC34A), Color(0xFFFFCC00), Color(0xFFFF9500)
)

@Composable
fun HeaderHandle(
    contentColor: Color,
    sheetOpacity: Float,
    onOpacityChange: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount -> onDrag(dragAmount) },
                    onDragEnd = { onDragEnd() }
                )
            }
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.15f))
        )
        
        Spacer(Modifier.height(20.dp))
        
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Sheet Transparency",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = contentColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            )
            Slider(
                value = sheetOpacity,
                onValueChange = onOpacityChange,
                valueRange = 0.5f..1f,
                modifier = Modifier.width(160.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF4CAF50),
                    activeTrackColor = Color(0xFF4CAF50),
                    inactiveTrackColor = contentColor.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    contentColor: Color,
    isExpanded: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable (() -> Unit)? = null
) {
    val containerColor = contentColor.copy(alpha = if (isExpanded) 0.08f else 0.04f)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Color(0xFF4CAF50).copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(
                    title, 
                    color = contentColor, 
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                )
                if (value.isNotEmpty()) {
                    Text(
                        value, 
                        color = contentColor.copy(alpha = 0.5f), 
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (content != null) {
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    tint = contentColor.copy(alpha = 0.3f)
                )
            }
        }
        if (isExpanded && content != null) {
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.padding(start = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingToggle(
    title: String, 
    checked: Boolean, 
    contentColor: Color, 
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(contentColor.copy(alpha = 0.04f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon, 
                null, 
                tint = if (checked) Color(0xFF4CAF50) else contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
        }
        Text(
            title, 
            modifier = Modifier.weight(1f), 
            color = contentColor, 
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFF4CAF50),
                checkedThumbColor = Color.White,
                uncheckedTrackColor = contentColor.copy(alpha = 0.1f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SliderSettingFloat(
    title: String, 
    value: Float, 
    range: ClosedFloatingPointRange<Float>, 
    contentColor: Color, 
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                title, 
                color = contentColor.copy(alpha = 0.7f), 
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                String.format("%.1f", value), 
                color = Color(0xFF4CAF50), 
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4CAF50),
                activeTrackColor = Color(0xFF4CAF50),
                inactiveTrackColor = contentColor.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun ColorSection(title: String, colors: List<Color>, selectedColor: Int, onColorSelected: (Color) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.take(8).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(if (selectedColor == color.toArgb()) 2.dp else 0.dp, Color.White, CircleShape)
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
fun DropdownSetting(
    label: String,
    currentValue: String,
    options: Map<String, String>,
    contentColor: Color,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = contentColor.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(contentColor.copy(alpha = 0.08f))
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = options[currentValue] ?: currentValue,
                    color = contentColor,
                    fontSize = 14.sp
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.5f)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFF2C2C2E))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            ) {
                options.forEach { (key, display) ->
                    DropdownMenuItem(
                        text = { Text(display, color = Color.White) },
                        onClick = {
                            onSelected(key)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accentColor = Color(0xFF4CAF50)
    val borderColor = if (isSelected) accentColor else contentColor.copy(alpha = 0.08f)
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.1f) else contentColor.copy(alpha = 0.04f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (isSelected) accentColor.copy(alpha = 0.2f) else contentColor.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else contentColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            color = contentColor,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            subtitle,
            color = contentColor.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp)
        )
    }
}

@Composable
fun AppSelectionContent(
    allApps: List<AppModel>,
    selectedIds: List<String>,
    repository: AppRepository,
    contentColor: Color,
    backgroundColor: Color,
    isPinnedApps: Boolean = false,
    onUpdate: (List<String>) -> Unit
) {
    Column {
        allApps.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { app ->
                    val id = repository.getAppUniqueId(app)
                    val isSelected = selectedIds.contains(id)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (isPinnedApps) repository.togglePin(app) else repository.toggleDock(app)
                                onUpdate(if (isPinnedApps) repository.getPinnedIds() else repository.getDockIds())
                            }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Image(
                                painter = rememberAsyncImagePainter(app.icon),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            if (isSelected) Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color(0xFF4CAF50), CircleShape)
                                    .border(2.dp, backgroundColor, CircleShape)
                            )
                        }
                        Text(
                            app.label,
                            color = contentColor,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (row.size < 4) {
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    )
}

data class UnifiedHomeItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val prefKey: String
)

