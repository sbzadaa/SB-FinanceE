package com.sbfinance.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF7115FA)
val Violet = Color(0xFF8D42FF)
val Lilac = Color(0xFFDCC4FF)
val Indigo = Color(0xFF23034C)
val Ink = Color(0xFF190E28)
val Slate = Color(0xFF7E748E)
val CanvasBg = Color(0xFFF7F6FB)
val Positive = Color(0xFF0C956A)
val Negative = Color(0xFFE45B79)
val Hairline = Color(0xFFEEEAF4)

@Composable
fun SBTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Purple, onPrimary = Color.White, primaryContainer = Lilac,
        secondary = Indigo, background = CanvasBg, onBackground = Ink,
        surface = Color.White, onSurface = Ink, surfaceVariant = Hairline,
        onSurfaceVariant = Slate, outline = Hairline, error = Negative
    ), typography = Typography(), content = content)
}
