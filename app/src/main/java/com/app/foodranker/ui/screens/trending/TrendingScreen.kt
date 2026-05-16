package com.app.foodranker.ui.screens.trending

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import com.app.foodranker.ui.components.PlateCard
import com.app.foodranker.ui.components.PlateCardHorizontal
import com.app.foodranker.ui.theme.*
import com.app.foodranker.viewmodel.TrendingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingScreen(
    onNavigateBack: () -> Unit,
    onPlateClick: (String) -> Unit,
    viewModel: TrendingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🔥 Tendencias",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadTrending() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = OrangePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(6) { com.app.foodranker.ui.components.PlateCardHorizontalSkeleton(
                    modifier = Modifier.padding(horizontal = 16.dp)) }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Banner hero
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(OrangePrimary, Color(0xFFFF8C42))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥", fontSize = 32.sp)
                            Text(
                                "Los platos del momento",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                "Ranking en vivo de la comunidad",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Sección 1: Más likeados
                item {
                    TrendingSectionTitle(
                        emoji = "❤️",
                        title = "Más likeados",
                        subtitle = "Los que más amor reciben"
                    )
                }

                if (uiState.mostLiked.isEmpty()) {
                    item { TrendingEmpty() }
                } else {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(uiState.mostLiked) { index, plate ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(plate.id) {
                                    kotlinx.coroutines.delay(index * 60L)
                                    visible = true
                                }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(300)) + slideInVertically(
                                        initialOffsetY = { 40 },
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    PlateCard(
                                        plate = plate,
                                        onClick = { onPlateClick(plate.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Sección 2: Mejor valorados
                item {
                    TrendingSectionTitle(
                        emoji = "⭐",
                        title = "Mejor valorados",
                        subtitle = "Los que la comunidad recomienda"
                    )
                }

                if (uiState.topRated.isEmpty()) {
                    item { TrendingEmpty() }
                } else {
                    itemsIndexed(uiState.topRated) { index, plate ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(plate.id) {
                            kotlinx.coroutines.delay(index * 50L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(300)) + slideInVertically(
                                initialOffsetY = { 60 },
                                animationSpec = tween(300)
                            )
                        ) {
                            PlateCardHorizontal(
                                plate = plate,
                                position = index + 1,
                                onClick = { onPlateClick(plate.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Sección 3: Más activos
                item {
                    TrendingSectionTitle(
                        emoji = "📈",
                        title = "Más activos",
                        subtitle = "Mayor engagement de la comunidad"
                    )
                }

                if (uiState.mostActive.isEmpty()) {
                    item { TrendingEmpty() }
                } else {
                    itemsIndexed(uiState.mostActive) { index, plate ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(plate.id) {
                            kotlinx.coroutines.delay(index * 50L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(300)) + slideInVertically(
                                initialOffsetY = { 60 },
                                animationSpec = tween(300)
                            )
                        ) {
                            PlateCardHorizontal(
                                plate = plate,
                                position = index + 1,
                                onClick = { onPlateClick(plate.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingSectionTitle(emoji: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = OrangePrimary.copy(alpha = 0.12f)
            ) {
                Text(
                    emoji,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun TrendingEmpty() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌱", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Aún no hay datos suficientes",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Vuelve cuando haya más likes y valoraciones",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
