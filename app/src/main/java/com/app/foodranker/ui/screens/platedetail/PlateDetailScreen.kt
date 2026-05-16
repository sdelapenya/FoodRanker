package com.app.foodranker.ui.screens.platedetail

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.data.model.Comment
import com.app.foodranker.data.model.Rating
import com.app.foodranker.ui.components.PlateDetailSkeleton
import com.app.foodranker.ui.screens.addplate.FoodTextField
import com.app.foodranker.ui.screens.addplate.ScoreSlider
import com.app.foodranker.ui.theme.*
import com.app.foodranker.utils.ShareManager
import com.app.foodranker.utils.formatCompact
import com.app.foodranker.utils.AdManager
import com.app.foodranker.viewmodel.PlateDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateDetailScreen(
    plateId: String,
    onNavigateBack: () -> Unit,
    viewModel: PlateDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showRatingSheet by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showShareCard by remember { mutableStateOf(false) }
    var showCollectionSheet by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var showImageZoom by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadUserCollections() }

    LaunchedEffect(plateId) {
        viewModel.loadPlate(plateId)
        if (AdManager.recordPlateDetailView()) {
            val activity = context as? android.app.Activity
            if (activity != null) AdManager.showInterstitial(activity) {}
        }
    }
    LaunchedEffect(uiState.plate) { if (uiState.plate != null) contentVisible = true }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        val msg = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    // Recargar al volver a la pantalla
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadPlate(plateId)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.plate?.name ?: "Detalle",
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
                    // Botón guardar
                    IconButton(onClick = { viewModel.toggleSave(plateId) }) {
                        Icon(
                            imageVector = if (uiState.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (uiState.isSaved) "Guardado" else "Guardar",
                            tint = if (uiState.isSaved) OrangePrimary else TextPrimary
                        )
                    }
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir", tint = OrangePrimary)
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("📤 Compartir texto") },
                                onClick = {
                                    uiState.plate?.let { ShareManager.sharePlateText(context, it) }
                                    showShareMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📚 Añadir a lista") },
                                onClick = {
                                    showShareMenu = false
                                    showCollectionSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🖼️ Compartir como imagen") },
                                onClick = {
                                    showShareMenu = false
                                    showShareCard = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🍴 Buscar en TheFork") },
                                onClick = {
                                    uiState.plate?.let { plate ->
                                        val query = "${plate.restaurantName} ${plate.city}"
                                        val uri = Uri.parse("https://www.thefork.es/busqueda?q=${Uri.encode(query)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                    showShareMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📊 Ver ranking completo") },
                                onClick = {
                                    val url = "https://foodranker.app/plate/$plateId"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                    showShareMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !uiState.hasUserRated && uiState.plate != null,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { showRatingSheet = true },
                    containerColor = OrangePrimary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Valorar este plato",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            PlateDetailSkeleton(modifier = Modifier.padding(paddingValues))
        } else {
            uiState.plate?.let { plate ->
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = slideInVertically(
                        initialOffsetY = { 60 },
                        animationSpec = tween(400)
                    ) + fadeIn(tween(400))
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        // Imagen principal
                        item {
                            AnimatedVisibility(
                                visible = contentVisible,
                                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .background(DividerColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (plate.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = plate.imageUrl,
                                            contentDescription = plate.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { showImageZoom = true }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Brush.verticalGradient(plate.category.detailGradient())),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(plate.category.emoji, fontSize = 72.sp)
                                                Spacer(Modifier.height(8.dp))
                                                Text(plate.name, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        color = OrangePrimary
                                    ) {
                                        Text(
                                            "${plate.category.emoji} ${plate.category.displayName}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Info principal
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        plate.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${plate.restaurantName} · ${plate.city}, ${plate.country}",
                                            fontSize = 14.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    // Botón Ver en mapa
                                    if (plate.restaurantName.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(
                                            onClick = {
                                                val query = "${plate.restaurantName} ${plate.city}".trim()
                                                val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                                if (intent.resolveActivity(context.packageManager) != null) {
                                                    context.startActivity(intent)
                                                } else {
                                                    val webUri = Uri.parse("https://maps.google.com/?q=${Uri.encode(query)}")
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                                }
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Map, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Ver en mapa", color = OrangePrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    if (plate.description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(plate.description, fontSize = 14.sp, color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = OrangePrimary
                                        ) {
                                            Text(
                                                "★ ${"%.1f".format(plate.averageScore)}  ·  ${plate.totalRatings.formatCompact()} votos",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                        LikeBadge(
                                            likes = plate.likes,
                                            isLiked = uiState.isLiked,
                                            onLike = { viewModel.toggleLike(plateId) }
                                        )
                                    }
                                }
                            }
                        }

                        // Ya valorado
                        if (uiState.hasUserRated) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = SuccessGreen.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ya has valorado este plato", color = SuccessGreen, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Sección comentarios
                        item {
                            Text(
                                "💬 Comentarios (${uiState.comments.size})",
                                fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = commentText,
                                        onValueChange = { new ->
                                            if (new.length <= com.app.foodranker.utils.InputLimits.COMMENT_TEXT) {
                                                commentText = new
                                            }
                                        },
                                        placeholder = { Text("Escribe un comentario...", color = TextSecondary.copy(alpha = 0.5f)) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OrangePrimary,
                                            unfocusedBorderColor = DividerColor
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.addComment(plateId, commentText)
                                            commentText = ""
                                        },
                                        enabled = commentText.isNotBlank() && !uiState.isSubmittingComment
                                    ) {
                                        if (uiState.isSubmittingComment) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OrangePrimary)
                                        } else {
                                            Icon(Icons.Default.Send, contentDescription = "Enviar", tint = OrangePrimary)
                                        }
                                    }
                                }
                            }
                        }
                        if (uiState.comments.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("Sé el primero en comentar 💬", color = TextSecondary, fontSize = 14.sp)
                                }
                            }
                        } else {
                            itemsIndexed(uiState.comments, key = { _, c -> c.id }) { _, comment ->
                                val isOwn = comment.userId == viewModel.currentUserId
                                if (isOwn) {
                                    @OptIn(ExperimentalMaterial3Api::class)
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                                viewModel.deleteComment(comment.id); true
                                            } else false
                                        }
                                    )
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = false,
                                        backgroundContent = {
                                            Box(
                                                modifier = Modifier.fillMaxSize()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(ErrorRed),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Borrar",
                                                    tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                                            }
                                        }
                                    ) {
                                        CommentItem(comment = comment, isOwn = true, onDelete = { viewModel.deleteComment(comment.id) })
                                    }
                                } else {
                                    CommentItem(comment = comment, isOwn = false, onDelete = {})
                                }
                            }
                        }

                        // Sección valoraciones
                        item {
                            Text(
                                "⭐ Valoraciones (${uiState.ratings.size})",
                                fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        // Lista valoraciones con animación escalonada
                        if (uiState.ratings.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Sé el primero en valorar este plato 🌟",
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(uiState.ratings) { index, rating ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(rating.id) {
                                    kotlinx.coroutines.delay(index * 80L)
                                    visible = true
                                }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = slideInHorizontally(
                                        initialOffsetX = { 100 },
                                        animationSpec = tween(300)
                                    ) + fadeIn(tween(300))
                                ) {
                                    RatingItem(rating = rating)
                                }
                            }
                        }

                        // Rewarded ad: ganar XP extra
                        item {
                            OutlinedButton(
                                onClick = {
                                    val activity = context as? android.app.Activity
                                    if (activity != null) {
                                        AdManager.showRewarded(
                                            activity,
                                            onRewarded = { viewModel.awardXpFromAd() },
                                            onDismiss = {}
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Ver anuncio y ganar +50 XP", color = OrangePrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    // Preview de la tarjeta antes de compartir
    // Sheet para seleccionar colección
    if (showCollectionSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(onDismissRequest = { showCollectionSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Añadir a lista", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                if (uiState.collections.isEmpty()) {
                    Text("No tienes listas todavía. Crea una desde tu perfil.", fontSize = 14.sp, color = TextSecondary)
                } else {
                    uiState.collections.forEach { col ->
                        val alreadyAdded = plateId in col.plateIds
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable(enabled = !alreadyAdded) {
                                    viewModel.addToCollection(col.id, plateId)
                                    showCollectionSheet = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(col.emoji, fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(col.name, fontWeight = FontWeight.Medium, color = if (alreadyAdded) TextSecondary else TextPrimary)
                                Text("${col.plateIds.size} platos", fontSize = 12.sp, color = TextSecondary)
                            }
                            if (alreadyAdded) {
                                Text("Ya añadido", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                            } else {
                                Icon(Icons.Default.Add, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        HorizontalDivider(color = DividerColor)
                    }
                }
            }
        }
    }

    if (showShareCard && uiState.plate != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showShareCard = false }) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                com.app.foodranker.ui.components.ShareCard(plate = uiState.plate!!)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showShareCard = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.5f))
                        )
                    ) { Text("Cancelar") }
                    Button(
                        onClick = {
                            showShareCard = false
                            uiState.plate?.let { ShareManager.sharePlateWithImageUrl(context, it) }
                            com.app.foodranker.utils.AnalyticsManager.logPlateShared("image")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) { Text("Compartir 📤", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showImageZoom && uiState.plate?.imageUrl?.isNotEmpty() == true,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(200)),
        exit = scaleOut(tween(180)) + fadeOut(tween(180))
    ) {
        if (uiState.plate?.imageUrl?.isNotEmpty() == true) {
            ImageZoomViewer(imageUrl = uiState.plate!!.imageUrl, onDismiss = { showImageZoom = false })
        }
    }

    if (showRatingSheet) {
        RatingBottomSheet(
            onDismiss = { showRatingSheet = false },
            onSubmit = { flavor, presentation, value, comment ->
                viewModel.submitRating(plateId, flavor, presentation, value, comment)
                showRatingSheet = false
            },
            isLoading = uiState.isSubmittingRating
        )
    }
}

@Composable
private fun ImageZoomViewer(imageUrl: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
                .transformable(state = transformState),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            )
            // Hint
            if (scale == 1f) {
                Text(
                    "Pellizca para hacer zoom",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                )
            }
        }
    }
}

private fun PlateCategory.detailGradient() = when (this) {
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

@Composable
fun CommentItem(comment: Comment, isOwn: Boolean, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(DividerColor),
                contentAlignment = Alignment.Center
            ) {
                if (comment.userPhotoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = comment.userPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(comment.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(comment.text, fontSize = 14.sp, color = TextSecondary)
            }
            if (isOwn) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun LikeBadge(
    likes: Int,
    isLiked: Boolean,
    onLike: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    var showHeart by remember { mutableStateOf(false) }
    val heartAlpha by animateFloatAsState(
        targetValue = if (showHeart) 1f else 0f,
        animationSpec = tween(150),
        finishedListener = { if (showHeart) showHeart = false },
        label = "heartAlpha"
    )
    val heartScale by animateFloatAsState(
        targetValue = if (showHeart) 1.6f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heartScale"
    )
    val likeScale by animateFloatAsState(
        targetValue = if (pressed) 1.35f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        finishedListener = { pressed = false },
        label = "likeBadgeScale"
    )
    val heartColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFE53935) else TextSecondary,
        animationSpec = tween(200),
        label = "heartColor"
    )

    Box(contentAlignment = Alignment.Center) {
        // Heart burst overlay
        if (heartAlpha > 0f) {
            Text("❤️", fontSize = 48.sp,
                modifier = Modifier.graphicsLayer {
                    this.alpha = heartAlpha
                    this.scaleX = heartScale
                    this.scaleY = heartScale
                })
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable {
                pressed = true
                showHeart = true
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onLike()
            }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isLiked) "Quitar like" else "Dar like",
            tint = heartColor,
            modifier = Modifier
                .size(26.dp)
                .scale(likeScale)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = likes.formatCompact(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (isLiked) heartColor else TextPrimary
        )
    }
    } // cierre Box
}

@Composable
fun RatingItem(rating: Rating) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(DividerColor),
                contentAlignment = Alignment.Center
            ) {
                if (rating.userPhotoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = rating.userPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(rating.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("★ ${"%.1f".format(rating.averageScore)}", fontWeight = FontWeight.Bold, color = OrangePrimary, fontSize = 14.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniScore("😋", rating.flavorScore)
                    MiniScore("🎨", rating.presentationScore)
                    MiniScore("💰", rating.valueScore)
                }
                if (rating.comment.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(rating.comment, fontSize = 13.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun MiniScore(emoji: String, score: Float) {
    Text("$emoji ${"%.1f".format(score)}", fontSize = 11.sp, color = TextSecondary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (Float, Float, Float, String) -> Unit,
    isLoading: Boolean
) {
    var flavorScore by remember { mutableFloatStateOf(5f) }
    var presentationScore by remember { mutableFloatStateOf(5f) }
    var valueScore by remember { mutableFloatStateOf(5f) }
    var comment by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("⭐ Valorar este plato", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
            ScoreSlider("Sabor", "😋", flavorScore) { flavorScore = it }
            ScoreSlider("Presentación", "🎨", presentationScore) { presentationScore = it }
            ScoreSlider("Precio/Calidad", "💰", valueScore) { valueScore = it }
            FoodTextField(
                value = comment,
                onValueChange = { comment = it },
                label = "Comentario (opcional)",
                placeholder = "¿Qué te pareció?",
                singleLine = false,
                maxLines = 3,
                maxLength = com.app.foodranker.utils.InputLimits.RATING_COMMENT,
                showCounter = true
            )
            Button(
                onClick = { onSubmit(flavorScore, presentationScore, valueScore, comment) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Publicar valoración 🚀", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
