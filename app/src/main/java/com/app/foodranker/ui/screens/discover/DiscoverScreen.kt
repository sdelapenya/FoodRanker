package com.app.foodranker.ui.screens.discover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.ui.components.BannerAdView
import com.app.foodranker.ui.theme.*
import com.app.foodranker.ui.theme.categoryGradient
import com.app.foodranker.utils.formatCompact
import com.app.foodranker.viewmodel.AuthViewModel
import com.app.foodranker.viewmodel.DiscoverViewModel
import com.app.foodranker.viewmodel.LeagueViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onNavigateToExplore: () -> Unit,
    onNavigateToExploreUsers: () -> Unit = {},
    onNavigateToAddPlate: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPlateDetail: (String) -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToLeague: () -> Unit = {},
    viewModel: DiscoverViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = authViewModel.currentUser
    val currentUserId = viewModel.currentUserId
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isOnline by viewModel.isOnline.collectAsState()

    // Countdown to ranking reset (next Monday)
    var remainingMs by remember { mutableLongStateOf(LeagueViewModel.millisUntilNextMonday()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            remainingMs = LeagueViewModel.millisUntilNextMonday()
        }
    }
    val totalSecs = remainingMs / 1_000L
    val days    = totalSecs / 86_400L
    val hours   = (totalSecs % 86_400L) / 3_600L
    val minutes = (totalSecs % 3_600L) / 60L
    val seconds = totalSecs % 60L
    val countdownText = if (days > 0L)
        "${days}d %02d:%02d:%02d".format(hours, minutes, seconds)
    else
        "%02d:%02d:%02d".format(hours, minutes, seconds)

    // Rotating ticker
    val tickerMessages = remember {
        listOf(
            "El ranking se actualiza con cada voto",
            "Vota 3 platos hoy y sube en la liga",
            "Top 3 de la semana gana badge especial",
            "¿Hay un récord en tu ciudad hoy?",
            "¡Nuevos platos añadidos cerca de ti!"
        )
    }
    var tickerIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            tickerIdx = (tickerIdx + 1) % tickerMessages.size
        }
    }

    // Snackbars
    LaunchedEffect(uiState.ratingFeedback) {
        val msg = uiState.ratingFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long)
        viewModel.clearRatingFeedback()
    }
    LaunchedEffect(uiState.reportFeedback) {
        val msg = uiState.reportFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long)
        viewModel.clearReportFeedback()
    }

    // Save feedback
    var prevSavedIds by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(uiState.savedPlateIds) {
        val prev = prevSavedIds
        prevSavedIds = uiState.savedPlateIds
        if (prev == null) return@LaunchedEffect
        val added   = uiState.savedPlateIds - prev
        val removed = prev - uiState.savedPlateIds
        when {
            added.isNotEmpty()   -> snackbarHostState.showSnackbar("🔖 Guardado en tu colección", duration = SnackbarDuration.Short)
            removed.isNotEmpty() -> snackbarHostState.showSnackbar("Eliminado de guardados", duration = SnackbarDuration.Short)
        }
    }

    // Reload on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadPlatesIfStale()
                currentUser?.uid?.let { viewModel.startListeningForNotifications(it) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) viewModel.startListeningForNotifications(currentUserId)
    }

    val currentTabPlates = when (uiState.selectedTab) {
        1    -> uiState.nearbyPlates
        2    -> uiState.followingPlates
        else -> uiState.plates
    }
    val topScore = uiState.plates.maxOfOrNull { it.averageScore } ?: 10.0

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF2A2A2A),
                    contentColor = Color.White
                )
            }
        },
        containerColor = BackgroundLight,
        topBar = {
            RankingTopBar(
                unreadCount   = uiState.unreadNotificationCount,
                selectedTab   = uiState.selectedTab,
                isOnline      = isOnline,
                onSearch      = onNavigateToExplore,
                onNotifications = onNavigateToNotifications,
                onTabSelected = { viewModel.setTab(it) }
            )
        },
        bottomBar = {
            Column {
                BannerAdView()
                RankingNavBar(
                    onAddPlate = onNavigateToAddPlate,
                    onLeague   = onNavigateToLeague,
                    onProfile  = { onNavigateToProfile(currentUser?.uid ?: "") }
                )
            }
        }
    ) { padding ->

        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh    = viewModel::loadPlates,
            state        = pullState,
            modifier     = Modifier.fillMaxSize().padding(padding)
        ) {
            // Seeding progress overlay
            val seeding = uiState.seedingProgress
            if (seeding != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("🌍", fontSize = 52.sp)
                        Text("Cargando platos del mundo...", fontWeight = FontWeight.Bold, color = TextPrimary)
                        LinearProgressIndicator(
                            progress = { if (seeding.second > 0) seeding.first.toFloat() / seeding.second else 0f },
                            modifier = Modifier.fillMaxWidth(0.7f),
                            color = OrangePrimary
                        )
                        Text("${seeding.first} / ${seeding.second}", color = TextSecondary, fontSize = 13.sp)
                    }
                }
                return@PullToRefreshBox
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Mission card — only while in progress
                val progress = uiState.dailyMissionProgress
                val goal     = uiState.dailyMissionGoal
                if (progress < goal) {
                    item(key = "mission") {
                        MissionCard(progress = progress, goal = goal, streak = uiState.voteStreak)
                    }
                }

                // Countdown + ticker
                item(key = "countdown") {
                    CountdownTickerRow(
                        countdownText  = countdownText,
                        tickerMessage  = tickerMessages[tickerIdx],
                        voteStreak     = uiState.voteStreak
                    )
                }

                // Challenge banner
                if (uiState.selectedTab == 0 && uiState.plates.isNotEmpty()) {
                    item(key = "challenge") {
                        com.app.foodranker.ui.screens.challenge.ChallengeBanner(
                            onParticipate = onNavigateToAddPlate
                        )
                    }
                }

                // Empty / loading state
                if (currentTabPlates.isEmpty()) {
                    item(key = "empty") {
                        if (uiState.isLoading || uiState.isLoadingFollowing) {
                            Box(
                                Modifier.fillMaxWidth().padding(80.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(color = OrangePrimary) }
                        } else {
                            RankingEmptyState(
                                tab           = uiState.selectedTab,
                                onExplore     = onNavigateToExplore,
                                onExploreUsers = onNavigateToExploreUsers,
                                userCity      = uiState.userCity
                            )
                        }
                    }
                    return@LazyColumn
                }

                // Ranked plate cards
                itemsIndexed(currentTabPlates, key = { _, p -> p.id }) { index, plate ->
                    val gapToTop = if (index == 0) null else {
                        val g = topScore - plate.averageScore
                        if (g < 0.05) null else g
                    }
                    RankedPlateCard(
                        rank     = index + 1,
                        plate    = plate,
                        gapToTop = gapToTop,
                        onClick  = { onNavigateToPlateDetail(plate.id) }
                    )
                }

                // Load more (Top tab only)
                if (uiState.hasMorePlates && uiState.selectedTab == 0 && currentTabPlates.isNotEmpty()) {
                    item(key = "loadmore") {
                        LaunchedEffect(Unit) { viewModel.loadMorePlates() }
                        Box(
                            Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OrangePrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Top bar ──────────────────────────────────────────────────────────────────

@Composable
private fun RankingTopBar(
    unreadCount: Int,
    selectedTab: Int,
    isOnline: Boolean,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onTabSelected: (Int) -> Unit
) {
    Surface(shadowElevation = 2.dp, color = SurfaceWhite) {
        Column {
            if (!isOnline) {
                Surface(color = Color(0xFFB71C1C), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Sin conexión", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo
                Surface(shape = RoundedCornerShape(10.dp), color = OrangePrimary, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("F", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "FoodRanker",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = TextSecondary)
                }
                Box {
                    IconButton(onClick = onNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = TextSecondary)
                    }
                    if (unreadCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp),
                            containerColor = OrangePrimary
                        ) {
                            Text(unreadCount.coerceAtMost(99).toString(), fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = SurfaceWhite,
                contentColor     = OrangePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color    = OrangePrimary
                    )
                }
            ) {
                listOf("Top semana", "Cerca", "Siguiendo").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { onTabSelected(index) },
                        text = {
                            Text(
                                label,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (selectedTab == index) OrangePrimary else TextSecondary
                            )
                        }
                    )
                }
            }
        }
    }
}

// ── Bottom nav ────────────────────────────────────────────────────────────────

@Composable
private fun RankingNavBar(
    onAddPlate: () -> Unit,
    onLeague: () -> Unit,
    onProfile: () -> Unit
) {
    NavigationBar(containerColor = SurfaceWhite, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = true,
            onClick  = {},
            icon     = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
            label    = { Text("Ranking", fontSize = 11.sp) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangePrimary,
                selectedTextColor = OrangePrimary,
                indicatorColor    = OrangePrimary.copy(alpha = 0.12f)
            )
        )
        NavigationBarItem(
            selected = false,
            onClick  = onAddPlate,
            icon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(OrangePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            },
            label  = { Text("Subir", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedTextColor = TextSecondary,
                indicatorColor      = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick  = onLeague,
            icon     = { Icon(Icons.Default.MilitaryTech, contentDescription = null) },
            label    = { Text("Liga", fontSize = 11.sp) },
            colors   = NavigationBarItemDefaults.colors(
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = false,
            onClick  = onProfile,
            icon     = { Icon(Icons.Default.Person, contentDescription = null) },
            label    = { Text("Perfil", fontSize = 11.sp) },
            colors   = NavigationBarItemDefaults.colors(
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}

// ── Mission card ──────────────────────────────────────────────────────────────

@Composable
private fun MissionCard(progress: Int, goal: Int, streak: Int = 0) {
    val fraction = (progress.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2419)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Misión diaria: mueve el ranking",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (streak >= 2) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF6D00)
                        ) {
                            Text(
                                "🔥 $streak días",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    "$progress de $goal votos hechos · +${goal * 5} XP si terminas hoy",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.sp
                )
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier  = Modifier.size(52.dp),
                    color     = Color(0xFF4CAF50),
                    trackColor = Color.White.copy(alpha = 0.15f),
                    strokeWidth = 4.dp
                )
                Text(
                    "$progress/$goal",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ── Countdown + ticker ────────────────────────────────────────────────────────

@Composable
private fun CountdownTickerRow(countdownText: String, tickerMessage: String, voteStreak: Int = 0) {
    Surface(color = SurfaceWhite) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Reset semanal del ranking", color = TextSecondary, fontSize = 12.sp)
                Text(countdownText, color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                if (voteStreak >= 2) {
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF6D00)
                    ) {
                        Text(
                            "🔥 $voteStreak días",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            AnimatedContent(
                targetState = tickerMessage,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                label = "ticker"
            ) { msg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(6.dp)) { drawCircle(Color(0xFF4CAF50)) }
                    Text(msg, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ── Ranked plate card ─────────────────────────────────────────────────────────

@Composable
private fun RankedPlateCard(
    rank: Int,
    plate: Plate,
    gapToTop: Double?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            // Background image or gradient
            if (plate.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = plate.imageUrl,
                    contentDescription = plate.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(plate.category.categoryGradient())),
                    contentAlignment = Alignment.Center
                ) {
                    Text(plate.category.emoji, fontSize = 52.sp)
                }
            }

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.30f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )

            // Rank badge — top left
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                shape    = RoundedCornerShape(8.dp),
                color    = Color.Black.copy(alpha = 0.65f)
            ) {
                Text(
                    when (rank) { 1 -> "🥇 #1"; 2 -> "🥈 #2"; 3 -> "🥉 #3"; else -> "#$rank" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Score badge — top right
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                shape    = RoundedCornerShape(8.dp),
                color    = OrangePrimary
            ) {
                Text(
                    "★ ${"%.1f".format(plate.averageScore)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Bottom info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    plate.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildString {
                    if (plate.restaurantName.isNotEmpty()) append("${plate.restaurantName} · ")
                    append("${plate.totalRatings.formatCompact()} votos")
                    if (!plate.city.isNullOrEmpty()) append(" · ${plate.city}")
                    if (gapToTop != null && gapToTop < 1.5) {
                        append(" · a ${"%.1f".format(gapToTop)} del récord")
                    }
                }
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun RankingEmptyState(tab: Int, onExplore: () -> Unit, onExploreUsers: () -> Unit = {}, userCity: String = "") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            when (tab) { 1 -> "📍"; 2 -> "👥"; else -> "🍽️" },
            fontSize = 52.sp
        )
        Text(
            when {
                tab == 1 && userCity.isEmpty() ->
                    "Configura tu ciudad en tu perfil para ver los platos de tu zona."
                tab == 1 ->
                    "No hay platos en $userCity todavía.\n¡Sé el primero en añadir uno!"
                tab == 2 -> "Sigue a otros foodies para ver sus platos aquí."
                else -> "¡Añade el primer plato!"
            },
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        if (tab == 2) {
            Button(
                onClick = onExploreUsers,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape  = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Descubrir foodies", fontWeight = FontWeight.Bold)
            }
        }
    }
}

