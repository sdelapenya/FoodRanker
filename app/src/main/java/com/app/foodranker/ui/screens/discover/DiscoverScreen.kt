package com.app.foodranker.ui.screens.discover

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.input.pointer.pointerInput
import com.app.foodranker.utils.formatCompact
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.ui.screens.platedetail.RatingBottomSheet
import com.app.foodranker.ui.theme.*
import com.app.foodranker.utils.ShareManager
import com.app.foodranker.ui.components.BannerAdView
import com.app.foodranker.viewmodel.AuthViewModel
import com.app.foodranker.viewmodel.DiscoverViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    onNavigateToExplore: () -> Unit,
    onNavigateToAddPlate: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPlateDetail: (String) -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    viewModel: DiscoverViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = authViewModel.currentUser
    val currentUserId = viewModel.currentUserId
    val context = LocalContext.current
    var showRatingFor by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val forYouPagerState = rememberPagerState(pageCount = { uiState.plates.size + 1 })
    val isOnline by viewModel.isOnline.collectAsState()

    // Mostrar feedback de reporte
    val reportFeedback = uiState.reportFeedback
    LaunchedEffect(reportFeedback) {
        if (reportFeedback != null) {
            snackbarHostState.showSnackbar(reportFeedback)
            viewModel.clearReportFeedback()
        }
    }

    // Snackbar al guardar/desguardar plato (se omite la carga inicial)
    var prevSavedIds by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(uiState.savedPlateIds) {
        val prev = prevSavedIds
        prevSavedIds = uiState.savedPlateIds
        if (prev == null) return@LaunchedEffect  // primera carga: no mostrar snackbar
        val added   = uiState.savedPlateIds - prev
        val removed = prev - uiState.savedPlateIds
        when {
            added.isNotEmpty()   -> snackbarHostState.showSnackbar("🔖 Guardado en tu colección")
            removed.isNotEmpty() -> snackbarHostState.showSnackbar("Eliminado de guardados")
        }
    }

    val ratingFeedback = uiState.ratingFeedback
    LaunchedEffect(ratingFeedback) {
        if (ratingFeedback != null) {
            snackbarHostState.showSnackbar(ratingFeedback)
            viewModel.clearRatingFeedback()
        }
    }

    // Recargar el feed cada vez que la pantalla vuelve al primer plano
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadPlatesIfStale()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }
        LaunchedEffect(Unit) { permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) viewModel.startListeningForNotifications(currentUserId)
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Column {
            if (!isOnline) {
                Surface(color = Color(0xFFB71C1C), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Sin conexión — mostrando datos en caché", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            TabRow(
                selectedTabIndex = if (uiState.feedMode == com.app.foodranker.viewmodel.FeedMode.FOR_YOU) 0 else 1,
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[if (uiState.feedMode == com.app.foodranker.viewmodel.FeedMode.FOR_YOU) 0 else 1]),
                        color = OrangePrimary
                    )
                }
            ) {
                Tab(
                    selected = uiState.feedMode == com.app.foodranker.viewmodel.FeedMode.FOR_YOU,
                    onClick = { viewModel.setFeedMode(com.app.foodranker.viewmodel.FeedMode.FOR_YOU) },
                    text = { Text("Para ti", fontWeight = FontWeight.Bold, color = Color.White) }
                )
                Tab(
                    selected = uiState.feedMode == com.app.foodranker.viewmodel.FeedMode.FOLLOWING,
                    onClick = { viewModel.setFeedMode(com.app.foodranker.viewmodel.FeedMode.FOLLOWING) },
                    text = { Text("Siguiendo", fontWeight = FontWeight.Bold, color = Color.White) }
                )
            }
            } // cierre Column del topBar
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF2A2A2A),
                    contentColor = Color.White
                )
            }
        },
        bottomBar = {
            Column {
            BannerAdView()
            NavigationBar(containerColor = Color.Black.copy(alpha = 0.85f), tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = true,
                    onClick = {
                        coroutineScope.launch {
                            forYouPagerState.animateScrollToPage(0)
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Descubrir") },
                    label = { Text("Descubrir", color = OrangePrimary, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangePrimary,
                        indicatorColor = OrangePrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToExplore,
                    icon = { Icon(Icons.Default.Search, contentDescription = "Explorar") },
                    label = { Text("Explorar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                        unselectedTextColor = Color.White.copy(alpha = 0.7f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToAddPlate,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(OrangePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir", tint = Color.White)
                        }
                    },
                    label = { Text("Añadir", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedTextColor = Color.White.copy(alpha = 0.7f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { currentUser?.uid?.let { onNavigateToProfile(it) } },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                        unselectedTextColor = Color.White.copy(alpha = 0.7f)
                    )
                )
            }
            } // close Column in bottomBar
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = uiState.feedMode,
            transitionSpec = {
                fadeIn(tween(250)) togetherWith fadeOut(tween(200))
            },
            label = "feed_mode"
        ) { feedMode ->
        // Feed Siguiendo
        if (feedMode == com.app.foodranker.viewmodel.FeedMode.FOLLOWING) {
            if (uiState.isLoadingFollowing) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.Black),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else if (uiState.followingPlates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.Black),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)) {
                        Text("👥", fontSize = 56.sp)
                        Text("Aún no sigues a nadie", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Sigue a otros foodies para ver sus platos aquí",
                            color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToExplore,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Descubrir foodies", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val followingPagerState = rememberPagerState(pageCount = { uiState.followingPlates.size })
                VerticalPager(state = followingPagerState, modifier = Modifier.fillMaxSize().padding(paddingValues)) { page ->
                    val plate = uiState.followingPlates[page]
                    val isLiked = currentUserId.isNotEmpty() && currentUserId in plate.likedByUsers
                    var showReportDialog by remember { mutableStateOf(false) }
                    DiscoverPage(
                        plate = plate, isLiked = isLiked,
                        isSaved = plate.id in uiState.savedPlateIds,
                        pageIndex = page + 1, totalPages = uiState.followingPlates.size,
                        pageOffset = 0f, unreadNotifications = 0,
                        onLike = { viewModel.toggleLike(plate.id) },
                        onSave = { viewModel.toggleSave(plate.id) },
                        onRate = { showRatingFor = plate.id },
                        onShare = { ShareManager.sharePlateText(context, plate) },
                        onDetail = { onNavigateToPlateDetail(plate.id) },
                        onReport = { showReportDialog = true }
                    )
                    if (showReportDialog) {
                        ReportDialog(onDismiss = { showReportDialog = false },
                            onReport = { reason -> viewModel.reportPlate(plate.id, reason); showReportDialog = false })
                    }
                }
            }
        } else if (uiState.seedingProgress != null) {
            val (current, total) = uiState.seedingProgress!!
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("🌍", fontSize = 56.sp)
                    Text("Cargando platos del mundo...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { if (total > 0) current.toFloat() / total else 0f },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        color = OrangePrimary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Text("$current / $total platos", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        } else if (uiState.isLoading || uiState.plates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = OrangePrimary)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍽️", fontSize = 56.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("¡Añade el primer plato!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Pulsa + para empezar", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    }
                }
            }
        } else {
            val pagerState = forYouPagerState

            // Cargar más platos cuando el usuario está a 3 del final
            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage >= uiState.plates.size - 3 && uiState.hasMorePlates) {
                    viewModel.loadMorePlates()
                }
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) { page ->
                if (page >= uiState.plates.size) {
                    if (uiState.isLoadingMore) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(color = OrangePrimary)
                                Text("Cargando más platos...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                            }
                        }
                        return@VerticalPager
                    }
                    EndOfFeedPage(
                        onRefresh = { viewModel.loadPlates() },
                        onExplore = onNavigateToExplore
                    )
                    return@VerticalPager
                }
                val plate = uiState.plates[page]
                val isLiked = currentUserId.isNotEmpty() && currentUserId in plate.likedByUsers
                var showReportDialog by remember { mutableStateOf(false) }
                // Desplazamiento de esta página respecto al centro del pager, entre -1 y 1.
                // 0 = página actual, ±1 = vecinos. Lo usamos para el parallax sutil.
                val pageOffset = ((pagerState.currentPage - page) +
                    pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                DiscoverPage(
                    plate = plate,
                    isLiked = isLiked,
                    isSaved = plate.id in uiState.savedPlateIds,
                    pageIndex = page + 1,
                    totalPages = uiState.plates.size,
                    pageOffset = pageOffset,
                    unreadNotifications = uiState.unreadNotificationCount,
                    onLike = { viewModel.toggleLike(plate.id) },
                    onSave = { viewModel.toggleSave(plate.id) },
                    onRate = { showRatingFor = plate.id },
                    onShare = { ShareManager.sharePlateText(context, plate) },
                    onDetail = { onNavigateToPlateDetail(plate.id) },
                    onReport = { showReportDialog = true },
                    onNotifications = onNavigateToNotifications,
                    onSeed = { viewModel.seedFromMealDB() }
                )
                if (showReportDialog) {
                    ReportDialog(
                        onDismiss = { showReportDialog = false },
                        onReport = { reason ->
                            viewModel.reportPlate(plate.id, reason)
                            showReportDialog = false
                        }
                    )
                }
            }
        }
        } // fin AnimatedContent feedMode
    }

    // Banner de reto semanal — aparece encima del feed cuando hay reto activo
    if (uiState.feedMode == com.app.foodranker.viewmodel.FeedMode.FOR_YOU &&
        uiState.plates.isNotEmpty() && uiState.seedingProgress == null) {
        com.app.foodranker.ui.screens.challenge.ChallengeBanner(
            onParticipate = { onNavigateToAddPlate() }
        )
    }

    showRatingFor?.let { plateId ->
        RatingBottomSheet(
            onDismiss = { showRatingFor = null },
            onSubmit = { flavor, presentation, value, comment ->
                viewModel.submitRating(plateId, flavor, presentation, value, comment)
                showRatingFor = null
            },
            isLoading = false
        )
    }
}

@Composable
private fun SeedDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🌍 Cargar platos del mundo v2") },
        text = { Text("Descargará ~112 platos reales con fotos de Pexels en alta resolución. Tarda 60-90 segundos. ¿Continuar?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Cargar", color = OrangePrimary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverPage(
    plate: Plate,
    isLiked: Boolean,
    pageIndex: Int,
    totalPages: Int,
    pageOffset: Float,
    unreadNotifications: Int,
    onLike: () -> Unit,
    onRate: () -> Unit,
    onShare: () -> Unit,
    onDetail: () -> Unit,
    onReport: () -> Unit = {},
    onSave: () -> Unit = {},
    isSaved: Boolean = false,
    onNotifications: () -> Unit = {},
    onSeed: () -> Unit = {}
) {
    // Estado de la animación de "heart burst" al hacer doble tap.
    // Lo asociamos al id del plato para resetearlo al cambiar de página.
    var burstTrigger by remember(plate.id) { mutableStateOf(0) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(plate.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isLiked) onLike()
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        burstTrigger++
                    },
                    onTap = { onDetail() }
                )
            }
    ) {

        // Imagen o placeholder con gradiente, con parallax sutil al hacer swipe.
        // Movemos la imagen ~50dp en sentido contrario al desplazamiento del pager
        // para crear el efecto de profundidad. graphicsLayer evita recomposiciones.
        val parallaxModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = pageOffset * 60.dp.toPx()
                // Escala mínima para que los bordes de la imagen no se vean al moverla.
                val scaleBoost = 1f + (pageOffset.absoluteValue * 0.05f)
                scaleX = scaleBoost
                scaleY = scaleBoost
            }
        if (plate.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = plate.imageUrl,
                contentDescription = plate.name,
                contentScale = ContentScale.Crop,
                modifier = parallaxModifier
            )
        } else {
            Box(
                modifier = parallaxModifier
                    .background(Brush.verticalGradient(plate.category.placeholderGradient())),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(plate.category.emoji, fontSize = 96.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(plate.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Gradiente superior sutil (para el header)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
        // Gradiente inferior fuerte (para el texto)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.38f to Color.Transparent,
                            0.65f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Black.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        // Botones de acción — lado derecho (estilo Reels minimalista)
        // Acciones primarias: Like, Valorar, Compartir.
        // Acción negativa (Reportar) escondida bajo un menú "más" para no
        // contaminar visualmente la barra principal.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 130.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LikeActionButton(likes = plate.likes, isLiked = isLiked, onLike = onLike)
            ActionButton(icon = Icons.Default.Star, tint = StarYellow, onClick = onRate, contentDesc = "Valorar")
            ActionButton(
                icon = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                tint = if (isSaved) OrangePrimary else Color.White,
                onClick = onSave,
                contentDesc = if (isSaved) "Guardado" else "Guardar"
            )
            ActionButton(icon = Icons.Default.Share, tint = Color.White, onClick = onShare, contentDesc = "Compartir")
            MoreActionButton(onReport = onReport)
        }

        // Info del plato. El gesto de un toque en cualquier parte de la página
        // ya navega al detalle (gestionado por el Box raíz), por lo que aquí
        // no añadimos clickable adicional para evitar doble disparo.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 90.dp, bottom = 28.dp)
        ) {
            // Píldora unificada con score y nº de votos.
            Surface(shape = RoundedCornerShape(20.dp), color = OrangePrimary) {
                Text(
                    "★ ${"%.1f".format(plate.averageScore)}  ·  ${plate.totalRatings.formatCompact()} votos",
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                plate.name,
                color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "📍 ${plate.city}, ${plate.country}  •  ${plate.category.emoji} ${plate.category.displayName}",
                color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        // Header top: logo + dots centrados, campana de notificaciones a la derecha.
        // Estilo Instagram Stories — limpio sobre el gradiente superior.
        var showSeedDialog by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "FOODRANKER",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (com.app.foodranker.BuildConfig.DEBUG) showSeedDialog = true
                        }
                    )
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(minOf(totalPages, 7)) { i ->
                        val isActive = i == (pageIndex - 1).coerceIn(0, 6)
                        Box(
                            modifier = Modifier
                                .size(if (isActive) 7.dp else 4.dp)
                                .background(
                                    if (isActive) Color.White else Color.White.copy(alpha = 0.30f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
            NotificationIconButton(
                unreadCount = unreadNotifications,
                onClick = onNotifications,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
            )
        }
        if (showSeedDialog) {
            SeedDialog(
                onDismiss = { showSeedDialog = false },
                onConfirm = {
                    showSeedDialog = false
                    onSeed()
                }
            )
        }

        // Animación de "heart burst" al hacer doble tap. Se renderiza encima
        // de toda la página pero por debajo del header. Se autodisuelve.
        HeartBurst(triggerKey = burstTrigger, modifier = Modifier.align(Alignment.Center))

        // Hint "desliza arriba" solo en el primer plato
        if (pageIndex == 1) {
            var hintVisible by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2500)
                hintVisible = false
            }
            val swipeAlpha by animateFloatAsState(
                targetValue = if (hintVisible) 1f else 0f,
                animationSpec = tween(600),
                label = "swipeHint"
            )
            val swipeOffset by animateFloatAsState(
                targetValue = if (hintVisible) 0f else -20f,
                animationSpec = tween(600),
                label = "swipeOffset"
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .graphicsLayer { alpha = swipeAlpha; translationY = swipeOffset },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
                Text("Desliza para descubrir", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LikeActionButton(likes: Int, isLiked: Boolean, onLike: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.35f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        finishedListener = { pressed = false },
        label = "likeScale"
    )
    val heartColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFE53935) else Color.White,
        animationSpec = tween(200),
        label = "heartColor"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            pressed = true
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onLike()
        }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Like",
                tint = heartColor,
                modifier = Modifier.size(24.dp).scale(scale)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(likes.formatCompact(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Animación tipo Instagram: aparece un corazón grande central y varios
// corazones pequeños que vuelan radialmente y se desvanecen. Se dispara
// cada vez que cambia [triggerKey]. Si triggerKey == 0 no muestra nada.
@Composable
private fun HeartBurst(triggerKey: Int, modifier: Modifier = Modifier) {
    if (triggerKey == 0) return

    // Animatables que reinician en cada nuevo trigger.
    val mainScale = remember(triggerKey) { Animatable(0.2f) }
    val mainAlpha = remember(triggerKey) { Animatable(1f) }
    // Partículas: cada una con su ángulo y delay aleatorios fijos por trigger.
    val particles = remember(triggerKey) {
        List(7) {
            ParticleSpec(
                angleDeg = Random.nextInt(0, 360).toFloat(),
                distance = Random.nextInt(70, 140).toFloat(),
                delayMs = Random.nextInt(0, 120),
                rotation = Random.nextInt(-40, 40).toFloat()
            )
        }
    }

    LaunchedEffect(triggerKey) {
        // El corazón central crece con un rebote suave y luego se desvanece.
        mainScale.animateTo(
            targetValue = 1.4f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
        mainAlpha.animateTo(0f, animationSpec = tween(durationMillis = 400))
    }

    Box(modifier = modifier.size(180.dp), contentAlignment = Alignment.Center) {
        // Corazón central
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = Color(0xFFE53935),
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    scaleX = mainScale.value
                    scaleY = mainScale.value
                    alpha = mainAlpha.value
                }
        )
        // Partículas
        particles.forEach { spec ->
            HeartParticle(spec = spec, triggerKey = triggerKey)
        }
    }
}

private data class ParticleSpec(
    val angleDeg: Float,
    val distance: Float,
    val delayMs: Int,
    val rotation: Float
)

@Composable
private fun HeartParticle(spec: ParticleSpec, triggerKey: Int) {
    val progress = remember(triggerKey) { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        kotlinx.coroutines.delay(spec.delayMs.toLong())
        progress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }
    val rad = Math.toRadians(spec.angleDeg.toDouble())
    val dx = (cos(rad) * spec.distance * progress.value).toFloat()
    val dy = (sin(rad) * spec.distance * progress.value).toFloat()
    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = null,
        tint = Color(0xFFE53935),
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                translationX = dx
                translationY = dy
                alpha = (1f - progress.value).coerceIn(0f, 1f)
                rotationZ = spec.rotation * progress.value
                val s = 0.5f + progress.value * 0.6f
                scaleX = s
                scaleY = s
            }
    )
}

// Botón compacto para el header (campana de notificaciones). Mantiene la
// estética glass-dark de los botones laterales para coherencia visual.
@Composable
private fun NotificationIconButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(containerColor = OrangePrimary) {
                        Text(
                            if (unreadCount > 9) "9+" else "$unreadCount",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notificaciones",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, tint: Color, onClick: () -> Unit, contentDesc: String) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDesc, tint = tint, modifier = Modifier.size(24.dp))
    }
}

// Menú "más" — icono de 3 puntos verticales que despliega acciones secundarias
// (de momento sólo Reportar). Mantiene la columna principal despejada.
@Composable
private fun MoreActionButton(onReport: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                .clip(CircleShape)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Más opciones",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Reportar plato") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color(0xFFC0392B)
                    )
                },
                onClick = {
                    expanded = false
                    onReport()
                }
            )
        }
    }
}

@Composable
private fun EndOfFeedPage(onRefresh: () -> Unit, onExplore: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF1A0A00), Color(0xFF3A1200), OrangePrimary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🎉", fontSize = 72.sp)
            Text(
                "¡Has visto todos los platos!",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "Vuelve más tarde para descubrir\nnuevos platos de todo el mundo",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 Volver a mezclar", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            OutlinedButton(
                onClick = onExplore,
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.5f))
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔍 Explorar por categoría", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit, onReport: (String) -> Unit) {
    val reasons = listOf(
        "not_food"      to "🚫 No es comida",
        "inappropriate" to "⚠️ Contenido inapropiado",
        "spam"          to "📢 Es spam",
        "wrong_info"    to "❌ Información incorrecta"
    )
    var selected by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar plato", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("¿Por qué quieres reportar este contenido?", fontSize = 14.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                reasons.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = key }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selected == key,
                            onClick = { selected = key },
                            colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                        )
                        Text(label, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let { onReport(it) } },
                enabled = selected != null
            ) {
                Text("Enviar reporte", color = OrangePrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun PlateCategory.placeholderGradient() = when (this) {
    PlateCategory.PASTA     -> listOf(Color(0xFFE8A838), Color(0xFF6B3A00))
    PlateCategory.SUSHI     -> listOf(Color(0xFF2D7AAF), Color(0xFF0A1628))
    PlateCategory.BURGER    -> listOf(Color(0xFFBF4828), Color(0xFF3A0A00))
    PlateCategory.PIZZA     -> listOf(Color(0xFFD44030), Color(0xFF3A0500))
    PlateCategory.TAPAS     -> listOf(Color(0xFFBF6840), Color(0xFF3A1800))
    PlateCategory.RAMEN     -> listOf(Color(0xFFD47828), Color(0xFF3A1200))
    PlateCategory.STEAK     -> listOf(Color(0xFF8B4040), Color(0xFF1A0505))
    PlateCategory.SEAFOOD   -> listOf(Color(0xFF2090B0), Color(0xFF001828))
    PlateCategory.DESSERT   -> listOf(Color(0xFFD46898), Color(0xFF3A0828))
    PlateCategory.BREAKFAST -> listOf(Color(0xFFD4A828), Color(0xFF3A2800))
    PlateCategory.SALAD     -> listOf(Color(0xFF4A9848), Color(0xFF081A05))
    PlateCategory.OTHER     -> listOf(Color(0xFF787888), Color(0xFF101018))
}
