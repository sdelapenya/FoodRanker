package com.app.foodranker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
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
            // Nada de window.statusBarColor: está deprecado y en Android 15+ se ignora
            // (Play Console lo marca como "API obsoleta para la vista de extremo a
            // extremo"). Con enableEdgeToEdge() la barra es transparente y el color lo
            // pone el propio contenido; aquí solo se elige el tono de los iconos.
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

/**
 * Fuerza iconos claros en la barra de estado mientras la pantalla está visible, y
 * restaura los del tema al salir.
 *
 * Hace falta en las pantallas cuya cabecera es oscura (perfil, liga, login): con
 * `enableEdgeToEdge()` la barra de estado es transparente y el contenido se dibuja
 * debajo, así que los iconos oscuros del tema claro quedan ilegibles sobre ellas.
 */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(Unit) {
        val controller = WindowCompat.getInsetsController(
            (view.context as Activity).window, view
        )
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}
