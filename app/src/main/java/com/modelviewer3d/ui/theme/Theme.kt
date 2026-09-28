package com.modelviewer3d.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ViewerCyan = Color(0xFF00DAF3)
val ViewerBackground = Color(0xFF121212)
val ViewerCard = Color(0xFF1E1E1E)
val ViewerControl = Color(0xFF272A32)

private val ViewerColors = darkColorScheme(
    primary = ViewerCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF173C40),
    onPrimaryContainer = ViewerCyan,
    secondary = ViewerCyan,
    secondaryContainer = ViewerControl,
    background = ViewerBackground,
    surface = ViewerCard,
    onBackground = Color(0xFFE1E2EC),
    onSurface = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFFBAC9CC),
    outlineVariant = Color(0xFF2B3031),
    error = Color(0xFFFFB4AB)
)

@Composable
fun ModelViewer3DTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ViewerColors, typography = Typography, content = content)
}
