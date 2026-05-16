package com.app.foodranker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Tokens de color dinámicos accesibles desde cualquier Composable ──────
data class FoodRankerColorTokens(
    val background: Color,
    val surface:    Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider:    Color,
    val isDark:     Boolean
)

val LocalFoodColors = staticCompositionLocalOf {
    FoodRankerColorTokens(
        background   = BackgroundLight,
        surface      = SurfaceWhite,
        surfaceMuted = SurfaceMuted,
        textPrimary  = TextPrimary,
        textSecondary = TextSecondary,
        divider      = DividerColor,
        isDark       = false
    )
}

/** Acceso rápido desde cualquier Composable: `FoodTheme.colors.textPrimary` */
object FoodTheme {
    val colors: FoodRankerColorTokens
        @Composable get() = LocalFoodColors.current
}

// ── Esquemas Material3 ────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = OrangePrimary,
    onPrimary        = SurfaceWhite,
    primaryContainer = OrangeLight,
    secondary        = OrangeDark,
    background       = BackgroundLight,
    surface          = SurfaceWhite,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    error            = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary          = OrangePrimary,
    onPrimary        = Color.Black,
    primaryContainer = OrangeDark,
    secondary        = OrangeLight,
    background       = BackgroundDark,
    surface          = SurfaceDark,
    onBackground     = TextPrimaryDark,
    onSurface        = TextPrimaryDark,
    error            = ErrorRed,
    surfaceVariant   = SurfaceMutedDark,
    outline          = DividerDark
)

// ── Tema principal ────────────────────────────────────────────────────────
@Composable
fun FoodRankerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val foodColors = if (darkTheme) FoodRankerColorTokens(
        background    = BackgroundDark,
        surface       = SurfaceDark,
        surfaceMuted  = SurfaceMutedDark,
        textPrimary   = TextPrimaryDark,
        textSecondary = TextSecondaryDark,
        divider       = DividerDark,
        isDark        = true
    ) else FoodRankerColorTokens(
        background    = BackgroundLight,
        surface       = SurfaceWhite,
        surfaceMuted  = SurfaceMuted,
        textPrimary   = TextPrimary,
        textSecondary = TextSecondary,
        divider       = DividerColor,
        isDark        = false
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFoodColors provides foodColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            shapes      = FoodRankerShapes,
            content     = content
        )
    }
}
