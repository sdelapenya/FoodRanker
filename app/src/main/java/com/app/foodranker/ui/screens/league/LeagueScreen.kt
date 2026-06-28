package com.app.foodranker.ui.screens.league

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.foodranker.data.model.LeagueEntry
import com.app.foodranker.ui.theme.*
import com.app.foodranker.viewmodel.LeagueViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: LeagueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = viewModel.currentUserId

    var remainingMs by remember { mutableLongStateOf(LeagueViewModel.millisUntilNextMonday()) }

    // Recargar ranking cada vez que el usuario vuelve a esta pantalla (ej: después de votar)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Countdown ticker
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            remainingMs = LeagueViewModel.millisUntilNextMonday()
        }
    }

    val days    = (remainingMs / 86_400_000L).toInt()
    val hours   = ((remainingMs / 3_600_000L) % 24).toInt()
    val minutes = ((remainingMs / 60_000L) % 60).toInt()
    val seconds = ((remainingMs / 1_000L) % 60).toInt()
    val countdownText = if (days > 0) {
        "${days}d %02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        val msg = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight
    ) { padding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Dark header ─────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF20241F), Color(0xFF2A3028))
                            )
                        )
                        .padding(horizontal = 16.dp)
                        .padding(top = 52.dp, bottom = 20.dp)
                ) {
                    Column {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.height(4.dp))

                        val weekLabel = remember(uiState.weekKey) {
                            uiState.weekKey.split("-W").let { parts ->
                                if (parts.size == 2) "Semana ${parts[1]}" else uiState.weekKey
                            }
                        }
                        Text(
                            "⚔️ Liga ${uiState.city.ifEmpty { "Local" }}  ·  $weekLabel",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Termina en",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                countdownText,
                                color = OrangePrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "· Top 3 gana badge 🏅",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (uiState.entries.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            LeaguePodium(entries = uiState.entries.take(3))
                        }
                    }
                }
            }

            // ── Loading ──────────────────────────────────────────────────────
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = OrangePrimary) }
                }
                return@LazyColumn
            }

            // ── No city ──────────────────────────────────────────────────────
            if (uiState.city.isBlank()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📍", fontSize = 52.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Añade tu ciudad en el perfil para participar en la liga local.",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToProfile,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ir a mi perfil", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                return@LazyColumn
            }

            // ── Empty league ──────────────────────────────────────────────────
            if (uiState.entries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚔️", fontSize = 52.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Nadie ha votado aún esta semana en ${uiState.city}.\n¡Sé el primero!",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToDiscover,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Votar ahora ⚡", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                return@LazyColumn
            }

            // ── Fuera del top visible ────────────────────────────────────────
            if (uiState.userOutsideTop) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        border = BorderStroke(1.dp, Color(0xFFFFCC80))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("📍", fontSize = 22.sp)
                            Column {
                                Text(
                                    "Estás fuera del Top 20",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF5D3A00)
                                )
                                Text(
                                    "Vota más platos esta semana para aparecer en el ranking.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF7A5020)
                                )
                            }
                        }
                    }
                }
            }

            // ── Progress card (user not in Top 3) ────────────────────────────
            val userEntry = uiState.currentUserEntry
            val userRank = uiState.currentUserRank
            if (userEntry != null && userRank > 3 && uiState.entries.size >= 3) {
                item {
                    val top3Xp = uiState.entries[2].xp
                    val gap = maxOf(1, top3Xp - userEntry.xp + 1)
                    val progress = (userEntry.xp.toFloat() / top3Xp.coerceAtLeast(1))
                        .coerceIn(0f, 1f)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF7EE)),
                        border = BorderStroke(1.dp, Color(0xFFCFE8D7))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Te faltan $gap XP para el Top 3",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF153B2A)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Vota platos para acumular XP y subir en la liga.",
                                fontSize = 11.sp,
                                color = Color(0xFF516D5E),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = Color(0xFF168963),
                                trackColor = Color(0xFF168963).copy(alpha = 0.18f)
                            )
                        }
                    }
                }
            }

            // ── League rows ──────────────────────────────────────────────────
            itemsIndexed(uiState.entries, key = { _, e -> e.userId }) { index, entry ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(entry.userId) {
                    delay(index * 60L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(300)
                    ) + fadeIn(tween(300))
                ) {
                    LeagueRow(
                        rank = index + 1,
                        entry = entry,
                        isCurrentUser = entry.userId == currentUserId
                    )
                }
            }

            // ── CTA dinámico según estado del usuario ─────────────────────────
            item {
                val userEntry = uiState.currentUserEntry
                val votedToday = userEntry != null && isToday(userEntry.updatedAt)

                val ctaText = when {
                    userEntry == null -> "¡Empieza a votar! ⚡"
                    votedToday && uiState.currentUserRank <= 3 -> "¡Sigue así, estás en el Top 3! 🏅"
                    votedToday -> "Vota más para subir posiciones 🚀"
                    else -> "Vota hoy para no perder tu posición ⚡"
                }
                val ctaColor = if (votedToday && uiState.currentUserRank <= 3)
                    Color(0xFF168963) else OrangePrimary

                Button(
                    onClick = onNavigateToDiscover,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ctaColor),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(ctaText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ── Podium ───────────────────────────────────────────────────────────────────

@Composable
private fun LeaguePodium(entries: List<LeagueEntry>) {
    val first = entries.getOrNull(0)
    val second = entries.getOrNull(1)
    val third = entries.getOrNull(2)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (second != null) {
            PodiumSlot(entry = second, medal = "🥈", height = 80.dp, modifier = Modifier.weight(1f))
        }
        if (first != null) {
            PodiumSlot(entry = first, medal = "🥇", height = 104.dp, modifier = Modifier.weight(1f),
                highlight = true)
        }
        if (third != null) {
            PodiumSlot(entry = third, medal = "🥉", height = 72.dp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumSlot(
    entry: LeagueEntry,
    medal: String,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(
                if (highlight)
                    Color(0xFFD79A1E).copy(alpha = 0.22f)
                else
                    Color.White.copy(alpha = 0.12f)
            )
            .then(
                if (highlight) Modifier else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(medal, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        if (entry.userPhotoUrl.isNotEmpty()) {
            AsyncImage(
                model = entry.userPhotoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 14.sp) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            entry.userName.take(8),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            "${entry.xp} XP",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun isToday(timestamp: Long): Boolean {
    val now = java.util.Calendar.getInstance()
    val then = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    return now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
           now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
}

// ── League row ───────────────────────────────────────────────────────────────

@Composable
private fun LeagueRow(
    rank: Int,
    entry: LeagueEntry,
    isCurrentUser: Boolean
) {
    val bgColor = if (isCurrentUser) Color(0xFFEAF7EE) else Color.White
    val borderColor = if (isCurrentUser) Color(0xFFB9DEC6) else DividerColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentUser) 3.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rank
            Text(
                when (rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "$rank"
                },
                fontWeight = FontWeight.Bold,
                fontSize = if (rank <= 3) 18.sp else 14.sp,
                color = TextPrimary,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )

            // Avatar
            if (entry.userPhotoUrl.isNotEmpty()) {
                AsyncImage(
                    model = entry.userPhotoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(DividerColor),
                    contentAlignment = Alignment.Center
                ) { Text("👤", fontSize = 16.sp) }
            }

            // Name + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 1
                )
                val subtitle = when {
                    isCurrentUser -> "Tu posición"
                    entry.updatedAt > 0L && isToday(entry.updatedAt) -> "Votó hoy"
                    else -> "Activo esta semana"
                }
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = if (isCurrentUser) Color(0xFF168963) else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // XP
            Text(
                "${entry.xp} XP",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isCurrentUser) Color(0xFF168963) else OrangePrimary
            )
        }
    }
}
