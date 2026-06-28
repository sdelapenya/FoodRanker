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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.White, Color(0xFFFFF8F4), Color(0xFFFFECE0))
                )
            ),
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
                onDark = false,
                showTagline = true
            )
        }

        Text(
            text = "v1.0",
            color = Color(0xFF11122E).copy(alpha = 0.38f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }
}
