package com.app.foodranker.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.ui.components.BannerAdView
import com.app.foodranker.ui.components.EmptyStateCentered
import com.app.foodranker.ui.components.PlateCard
import com.app.foodranker.ui.components.PlateCardHorizontal
import com.app.foodranker.ui.components.PlateCardSkeleton
import com.app.foodranker.ui.components.PlateCardHorizontalSkeleton
import com.app.foodranker.ui.components.SectionHeader
import com.app.foodranker.ui.theme.AppElevation
import com.app.foodranker.ui.theme.AppSpacing
import com.app.foodranker.ui.theme.BackgroundLight
import com.app.foodranker.ui.theme.DividerColor
import com.app.foodranker.ui.theme.OrangePrimary
import com.app.foodranker.ui.theme.SurfaceWhite
import com.app.foodranker.ui.theme.TextPrimary
import com.app.foodranker.ui.theme.TextSecondary
import com.app.foodranker.viewmodel.AuthViewModel
import com.app.foodranker.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExplore: () -> Unit,
    onNavigateToAddPlate: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPlateDetail: (String) -> Unit,
    onNavigateToTrending: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val currentUser = authViewModel.currentUser
    val currentUserId = homeViewModel.currentUserId

    // Pedir permiso de notificaciones en Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* el usuario acepta o no, seguimos igual */ }
        LaunchedEffect(Unit) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Recargar al volver a HomeScreen (solo si han pasado más de 60s)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) homeViewModel.loadHomeDataIfStale()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Iniciar listener de notificaciones cuando el usuario esté disponible
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            homeViewModel.startListeningForNotifications(currentUserId)
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            Column {
                BannerAdView()
                NavigationBar(
                    containerColor = SurfaceWhite,
                    tonalElevation = AppElevation.navBar
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Inicio") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrangePrimary,
                            selectedTextColor = OrangePrimary,
                            indicatorColor = OrangePrimary.copy(alpha = 0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToExplore,
                        icon = { Icon(Icons.Default.Search, contentDescription = "Explorar") },
                        label = { Text("Explorar") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateToAddPlate,
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(OrangePrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White)
                            }
                        },
                        label = { Text("Añadir") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            currentUser?.uid?.let { uid ->
                                homeViewModel.markNotificationsRead(uid)
                                onNavigateToProfile(uid)
                            }
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.unreadNotificationCount > 0) {
                                        Badge(containerColor = OrangePrimary) {
                                            Text(
                                                text = if (uiState.unreadNotificationCount > 9) "9+"
                                                       else "${uiState.unreadNotificationCount}",
                                                color = Color.White,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Perfil")
                            }
                        },
                        label = { Text("Perfil") }
                    )
                }
            }
        }
    ) { paddingValues ->
        @OptIn(ExperimentalMaterial3Api::class)
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { homeViewModel.loadHomeData() },
            state = pullState,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                HomeHeader(
                    userName = currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Foodie",
                    userPhotoUrl = currentUser?.photoUrl?.toString() ?: "",
                    onProfileClick = { currentUser?.uid?.let { onNavigateToProfile(it) } }
                )
            }

            item { TrendingBanner(onClick = onNavigateToTrending) }

            item {
                CategorySection(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { homeViewModel.selectCategory(it) }
                )
            }

            item {
                SectionHeader(
                    title = "Top platos del mundo",
                    subtitle = "Los mejor valorados por la comunidad",
                    icon = Icons.Outlined.EmojiEvents
                )
            }

            if (uiState.isLoading) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = AppSpacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(4) { PlateCardSkeleton() }
                    }
                }
            } else if (uiState.topPlates.isEmpty()) {
                item {
                    EmptyStateCentered(
                        title = "Aquí están los grandes platos",
                        message = "Aún nadie ocupa el podio en esta vista. ¡Añade el tuyo y lidera el ranking!",
                        actionLabel = "Añadir plato",
                        onAction = onNavigateToAddPlate
                    )
                }
            } else {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = AppSpacing.screenHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.topPlates.size) { index ->
                            val plate = uiState.topPlates[index]
                            PlateCard(
                                plate = plate,
                                onClick = { onNavigateToPlateDetail(plate.id) },
                                isLiked = currentUserId.isNotEmpty() && currentUserId in plate.likedByUsers,
                                onLike = { homeViewModel.toggleLike(plate.id) }
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Ranking global",
                    subtitle = "Actualizado en tiempo real según valoraciones",
                    icon = Icons.Outlined.BarChart
                )
            }

            if (uiState.isLoading) {
                items(5) { index ->
                    PlateCardHorizontalSkeleton(
                        modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal, vertical = 4.dp)
                    )
                }
            } else if (uiState.recentPlates.isEmpty()) {
                item {
                    EmptyStateCentered(
                        title = "Ranking en construcción",
                        message = "Cuando la comunidad valore más platos, verás aquí la clasificación al instante.",
                        actionLabel = "Añadir plato",
                        onAction = onNavigateToAddPlate
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.recentPlates,
                    key = { _, plate -> plate.id }
                ) { index, plate ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(plate.id) {
                        kotlinx.coroutines.delay(index * 60L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInHorizontally(
                            initialOffsetX = { 120 },
                            animationSpec = tween(350)
                        ) + fadeIn(tween(350))
                    ) {
                        PlateCardHorizontal(
                            plate = plate,
                            position = index + 1,
                            onClick = { onNavigateToPlateDetail(plate.id) },
                            isLiked = currentUserId.isNotEmpty() && currentUserId in plate.likedByUsers,
                            onLike = { homeViewModel.toggleLike(plate.id) },
                            modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        } // cierre PullToRefreshBox
    }
}

@Composable
fun HomeHeader(
    userName: String,
    userPhotoUrl: String,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.sectionVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "Hola, $userName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¿Qué plato descubres hoy?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DividerColor)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                if (userPhotoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = userPhotoUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
                }
            }
        }

    }
}

@Composable
fun CategorySection(
    selectedCategory: PlateCategory?,
    onCategorySelected: (PlateCategory?) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Categorías",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal, vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("Todos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary,
                    selectedLabelColor = SurfaceWhite
                )
            )
            PlateCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text("${category.emoji} ${category.displayName}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary,
                        selectedLabelColor = SurfaceWhite
                    )
                )
            }
        }
    }
}

@Composable
fun TrendingBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenHorizontal, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.cardRaised)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(OrangePrimary, Color(0xFFFF8C42))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "En tendencia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Los platos del momento en la comunidad",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = Color.White.copy(alpha = 0.22f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Ver",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

