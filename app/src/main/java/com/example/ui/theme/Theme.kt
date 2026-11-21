package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.domain.model.AppFontFamily
import com.example.domain.model.ThemeMode

@Immutable
data class FlowStudioColors(
    val canvasBackground: Color,
    val canvasGrid: Color,
    val floatingPillSurface: Color,
    val floatingPillBorder: Color,
    val selectionAccent: Color,
    val guideLine: Color,
    val warningColor: Color,
    val errorColor: Color,
    val successColor: Color,
    val codeBackground: Color,
    val codeHeader: Color,
    val isDark: Boolean,
)

private val LightStudioColors = FlowStudioColors(
    canvasBackground = StudioCanvasLight,
    canvasGrid = StudioGridLight,
    floatingPillSurface = Color(0xF7FFFFFF),
    floatingPillBorder = SlateBorderLight,
    selectionAccent = FlowTeal600,
    guideLine = FlowCoral500,
    warningColor = FlowAmber500,
    errorColor = FlowCoral500,
    successColor = FlowEmerald500,
    codeBackground = Color(0xFFFFFFFF),
    codeHeader = Color(0xFFE6F6F2),
    isDark = false,
)

private val DarkStudioColors = FlowStudioColors(
    canvasBackground = StudioCanvasDark,
    canvasGrid = StudioGridDark,
    floatingPillSurface = Color(0xF5151E2C),
    floatingPillBorder = SlateBorderDark,
    selectionAccent = FlowTeal400,
    guideLine = FlowCoral500,
    warningColor = FlowAmber500,
    errorColor = FlowCoral500,
    successColor = FlowTeal400,
    codeBackground = Color(0xFF101722),
    codeHeader = Color(0xFF15292B),
    isDark = true,
)

val LocalFlowStudioColors = staticCompositionLocalOf { LightStudioColors }
val LocalAppFontFamily = staticCompositionLocalOf { AppFontFamily.PLUS_JAKARTA_SANS }

private val LightColorScheme = lightColorScheme(
    primary = FlowTeal600,
    onPrimary = Color.White,
    primaryContainer = FlowTeal100,
    onPrimaryContainer = FlowTeal900,
    secondary = FlowEmerald500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = FlowCoral500,
    onTertiary = Color.White,
    tertiaryContainer = FlowCoral100,
    onTertiaryContainer = Color(0xFF881337),
    background = StudioCanvasLight,
    onBackground = SlateInk900,
    surface = StudioSurfaceLight,
    onSurface = SlateInk900,
    surfaceVariant = StudioSurfaceVariantLight,
    onSurfaceVariant = SlateMuted500,
    outline = SlateBorderLight,
    outlineVariant = Color(0xFFEDF2F7),
    error = FlowCoral500,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = FlowTeal400,
    onPrimary = SlateInk950,
    primaryContainer = FlowTeal900,
    onPrimaryContainer = FlowTeal100,
    secondary = FlowEmerald500,
    onSecondary = SlateInk950,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = FlowCoral500,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5F1426),
    onTertiaryContainer = FlowCoral100,
    background = StudioCanvasDark,
    onBackground = Color(0xFFF1F5F9),
    surface = StudioSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = StudioSurfaceVariantDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = SlateBorderDark,
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFFB7185),
    onError = SlateInk950,
)

@Composable
fun FlowBuilderTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontFamily: AppFontFamily = AppFontFamily.PLUS_JAKARTA_SANS,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val studioColors = if (isDark) DarkStudioColors else LightStudioColors
    val typography = remember(fontFamily) { buildFlowTypography(fontFamily) }

    CompositionLocalProvider(
        LocalFlowStudioColors provides studioColors,
        LocalAppFontFamily provides fontFamily,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
