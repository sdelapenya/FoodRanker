package com.app.foodranker.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.foodranker.ui.components.FoodRankerLogo
import com.app.foodranker.ui.theme.FoodTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    awaitAuthReady: suspend () -> Boolean,
    onNavigateToAuth: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val logoScale = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        textOffsetY.animateTo(0f, animationSpec = tween(400))
        delay(850)
        if (awaitAuthReady()) onNavigateToHome() else onNavigateToAuth()
    }

    // El degradado tiene dos versiones porque esta pantalla es lo PRIMERO que se ve. El
    // fondo del splash del sistema (values-night/colors.xml) ya es oscuro; si este se
    // quedara claro, quien use el movil en oscuro vería el arranque negro, un fogonazo
    // blanco y otra vez oscuro al entrar en la app. Son los mismos tonos calidos, uno
    // subiendo desde el blanco y el otro desde el negro.
    val esOscuro = FoodTheme.colors.isDark
    val degradado = if (esOscuro)
        listOf(Color(0xFF0D0D0D), Color(0xFF16100D), Color(0xFF231711))
    else
        listOf(Color.White, Color(0xFFFFF8F4), Color(0xFFFFECE0))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(degradado)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FoodRankerLogo(
                modifier = Modifier
                    .scale(logoScale.value)
                    .graphicsLayer {
                        translationY = textOffsetY.value
                    },
                markSize = 138.dp,
                // onDark NO es "tema oscuro", es "el logo va sobre un fondo oscuro". Aquí
                // coinciden porque el fondo de esta pantalla es el degradado de arriba.
                onDark = esOscuro,
                showTagline = true
            )
        }

        Text(
            // Se lee del build: escrita a mano se quedó en v1.0 mientras la app iba por la 1.2.
            text = "v" + com.app.foodranker.BuildConfig.VERSION_NAME,
            color = FoodTheme.colors.textSecondary.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }
}
