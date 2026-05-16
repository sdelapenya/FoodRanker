package com.app.foodranker.ui.screens.challenge

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.foodranker.ui.theme.*
import com.app.foodranker.viewmodel.ChallengeViewModel

@Composable
fun ChallengeBanner(
    onParticipate: () -> Unit,
    viewModel: ChallengeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val challenge = uiState.currentChallenge ?: return

    // Snackbar de completado
    if (uiState.justCompleted) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearJustCompleted()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF1A0A00), OrangePrimary))
                )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(challenge.emoji, fontSize = 32.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(8.dp), color = OrangePrimary) {
                            Text(
                                "RETO SEMANAL",
                                fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("${challenge.daysLeft}d restantes", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(challenge.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text(challenge.description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (uiState.isParticipating) {
                        Surface(shape = RoundedCornerShape(8.dp), color = SuccessGreen) {
                            Text("✓ Completado", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.participate(); onParticipate() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+${challenge.xpReward} XP", color = OrangePrimary,
                                fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                    Text("${challenge.participantCount} participantes",
                        fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 2.dp))
                }
            }

            // Celebración al completar
            if (uiState.justCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SuccessGreen.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🎉 +${challenge.xpReward} XP ganados! ¡Reto completado!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
