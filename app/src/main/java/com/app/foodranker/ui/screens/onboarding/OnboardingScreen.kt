package com.app.foodranker.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.foodranker.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val gradientStart: Color,
    val gradientEnd: Color
)

private val pages = listOf(
    OnboardingPage(
        emoji = "🍽️",
        title = "Descubre los mejores\nplatos del mundo",
        description = "Explora un feed infinito de platos increíbles publicados por foodies como tú, de cualquier rincón del mundo.",
        gradientStart = Color(0xFF1A0A00),
        gradientEnd = OrangePrimary
    ),
    OnboardingPage(
        emoji = "⭐",
        title = "Valora, comenta\ny comparte",
        description = "Puntúa cada plato por sabor, presentación y precio. Deja tu opinión y comparte tus descubrimientos con tu red.",
        gradientStart = Color(0xFF0A1628),
        gradientEnd = Color(0xFF2D7AAF)
    ),
    OnboardingPage(
        emoji = "🏆",
        title = "Sube de nivel\ny gana badges",
        description = "Cuanto más activo seas, más XP ganas. Desbloquea badges exclusivos y llega a ser un 💎 Leyenda Foodie.",
        gradientStart = Color(0xFF1A0A00),
        gradientEnd = Color(0xFFBF3D10)
    )
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            OnboardingPage(page = pages[index])
        }

        // Controles inferiores
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Dots indicadores
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { i ->
                    val isActive = pagerState.currentPage == i
                    val width by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .background(
                                if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            // Botón principal
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = OrangePrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    if (pagerState.currentPage < pages.size - 1) "Siguiente →" else "¡Empezar a descubrir! 🚀",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }

            // Skip
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = onFinish) {
                    Text("Saltar", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(page.gradientStart, page.gradientEnd))),
        contentAlignment = Alignment.Center
    ) {
        // Círculos decorativos
        Box(modifier = Modifier.size(300.dp).offset(x = 100.dp, y = (-150).dp)
            .background(Color.White.copy(alpha = 0.04f), CircleShape))
        Box(modifier = Modifier.size(200.dp).offset(x = (-80).dp, y = 150.dp)
            .background(Color.White.copy(alpha = 0.04f), CircleShape))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp).offset(y = (-60).dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Emoji con círculo
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(page.emoji, fontSize = 64.sp)
            }

            Text(
                page.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Text(
                page.description,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}
