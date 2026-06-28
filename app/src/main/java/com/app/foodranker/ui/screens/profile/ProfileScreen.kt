package com.app.foodranker.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.foodranker.ui.components.EmptyStateCentered
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.ui.theme.*
import com.app.foodranker.ui.theme.cardGradient
import com.app.foodranker.utils.RewardManager
import com.app.foodranker.utils.ShareManager
import com.app.foodranker.utils.formatCompact
import com.app.foodranker.viewmodel.AuthViewModel
import com.app.foodranker.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToPlate: (String) -> Unit = {},
    onNavigateToFollowList: (profileUserId: String, followers: Boolean) -> Unit = { _, _ -> },
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToReferral: () -> Unit = {},
    onAccountDeleted: () -> Unit = onSignOut,
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = authViewModel.currentUser
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showEditPlateId by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    // ON_RESUME carga el perfil tanto en la primera apertura como al volver.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadProfileIfStale(userId)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    if (uiState.isOwnProfile) {
                        IconButton(
                            onClick = {
                                val name = uiState.user?.name ?: authViewModel.currentUser?.displayName ?: "Yo"
                                ShareManager.shareProfile(context, userId, name)
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir perfil", tint = Color.White)
                        }
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar perfil", tint = Color.White)
                        }
                        IconButton(onClick = { authViewModel.signOut(); onSignOut() }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)
                        .background(com.app.foodranker.ui.components.shimmerBrush()))
                }
                item { Spacer(Modifier.height(16.dp)) }
                items(6) { rowIdx ->
                    if (rowIdx % 2 == 0) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.app.foodranker.ui.components.PlateCardSkeleton(modifier = Modifier.weight(1f).aspectRatio(0.85f))
                            com.app.foodranker.ui.components.PlateCardSkeleton(modifier = Modifier.weight(1f).aspectRatio(0.85f))
                        }
                    }
                }
            }
        } else {
            val listState = rememberLazyListState()
            // Volver al inicio al cambiar de tab para evitar posición obsoleta
            LaunchedEffect(uiState.activeTab) { listState.scrollToItem(0) }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // Hero banner con foto
                item {
                    ProfileHero(
                        name = uiState.user?.name ?: currentUser?.displayName ?: "Usuario",
                        photoUrl = uiState.user?.photoUrl ?: currentUser?.photoUrl?.toString() ?: "",
                        isPremium = uiState.user?.isPremium ?: false,
                        paddingTop = paddingValues.calculateTopPadding(),
                        bio = uiState.user?.bio ?: "",
                        city = uiState.user?.city ?: ""
                    )
                }

                // Red social propia (seguidores / siguiendo) "" mismo dato que en perfiles ajenos
                if (uiState.isOwnProfile) {
                    item {
                        ProfileSocialSummary(
                            followerCount = uiState.followerCount,
                            followingCount = uiState.followingCount,
                            onFollowersClick = { onNavigateToFollowList(userId, true) },
                            onFollowingClick = { onNavigateToFollowList(userId, false) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Botón seguir (solo perfil ajeno)
                if (!uiState.isOwnProfile) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.toggleFollow(userId) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isFollowing) DividerColor else OrangePrimary,
                                    contentColor = if (uiState.isFollowing) TextPrimary else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (uiState.isFollowing) "Siguiendo" else "Seguir",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { onNavigateToFollowList(userId, true) }
                                        .padding(8.dp)
                                ) {
                                    Text(uiState.followerCount.formatCompact(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OrangePrimary)
                                    Text("Seguidores", fontSize = 11.sp, color = TextSecondary)
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { onNavigateToFollowList(userId, false) }
                                        .padding(8.dp)
                                ) {
                                    Text(uiState.followingCount.formatCompact(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OrangePrimary)
                                    Text("Siguiendo", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                // Tarjeta de nivel y XP
                item {
                    val xp = uiState.user?.xp ?: 0
                    val badges = uiState.user?.badges ?: emptyList()
                    LevelCard(xp = xp, badges = badges)
                }

                // Rival card (own profile, rival found)
                uiState.nearbyRival?.takeIf { uiState.isOwnProfile }?.let { rival ->
                    item {
                        RivalCard(
                            rivalName = rival.name,
                            rivalXp = rival.xp,
                            gap = uiState.nearbyRivalGap
                        )
                    }
                }

                // Stats 2x2
                item {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(150); visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(500)) + fadeIn(tween(500))
                    ) {
                        val likesReceived = uiState.plates.sumOf { it.likes }
                        ProfileStats2x2(
                            totalPlates    = uiState.plates.size,
                            likesReceived  = likesReceived,
                            likesGiven     = uiState.likesGiven,
                            ratingsGiven   = uiState.ratingsGiven,
                            cityRank       = uiState.cityRank,
                            city           = uiState.user?.city ?: ""
                        )
                    }
                }

                // Tabs Mis platos / Guardados (solo perfil propio)
                item {
                    if (uiState.isOwnProfile) {
                        TabRow(
                            selectedTabIndex = when (uiState.activeTab) {
                                com.app.foodranker.viewmodel.ProfileTab.MY_PLATES -> 0
                                com.app.foodranker.viewmodel.ProfileTab.SAVED -> 1
                                com.app.foodranker.viewmodel.ProfileTab.COLLECTIONS -> 2
                            },
                            containerColor = SurfaceWhite,
                            contentColor = OrangePrimary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(
                                        tabPositions[when (uiState.activeTab) {
                                            com.app.foodranker.viewmodel.ProfileTab.MY_PLATES -> 0
                                            com.app.foodranker.viewmodel.ProfileTab.SAVED -> 1
                                            com.app.foodranker.viewmodel.ProfileTab.COLLECTIONS -> 2
                                        }]
                                    ),
                                    color = OrangePrimary
                                )
                            }
                        ) {
                            Tab(
                                selected = uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.MY_PLATES,
                                onClick = { viewModel.setTab(com.app.foodranker.viewmodel.ProfileTab.MY_PLATES) },
                                text = { Text("Mis platos (" + uiState.plates.size + ")", fontWeight = FontWeight.Medium,
                                    color = if (uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.MY_PLATES) OrangePrimary else TextSecondary) }
                            )
                            Tab(
                                selected = uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.SAVED,
                                onClick = { viewModel.setTab(com.app.foodranker.viewmodel.ProfileTab.SAVED) },
                                text = { Text("Guardados (" + uiState.savedPlates.size + ")", fontWeight = FontWeight.Medium,
                                    color = if (uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.SAVED) OrangePrimary else TextSecondary) }
                            )
                            Tab(
                                selected = uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.COLLECTIONS,
                                onClick = { viewModel.setTab(com.app.foodranker.viewmodel.ProfileTab.COLLECTIONS) },
                                text = { Text("Listas (" + uiState.collections.size + ")", fontWeight = FontWeight.Medium,
                                    color = if (uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.COLLECTIONS) OrangePrimary else TextSecondary) }
                            )
                        }
                    } else {
                        ProfileSectionTitle(
                            title = "Platos públicos",
                            subtitle = "Publicaciones · ${uiState.plates.size.formatCompact()}",
                            icon = Icons.Outlined.Collections,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                        )
                    }
                }

                // Contenido según tab activo
                // Tab Colecciones
                if (uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.COLLECTIONS && uiState.isOwnProfile) {
                    item {
                        CollectionsSection(
                            collections = uiState.collections,
                            collectionPlates = uiState.collectionPlates,
                            isLoadingCollectionPlates = uiState.isLoadingCollectionPlates,
                            onCreateCollection = { name, emoji -> viewModel.createCollection(name, emoji) },
                            onDeleteCollection = { id -> viewModel.deleteCollection(id) },
                            onCollectionTap = { plateIds -> viewModel.loadCollectionPlates(plateIds) },
                            onClearCollectionPlates = { viewModel.clearCollectionPlates() },
                            onNavigateToPlate = onNavigateToPlate
                        )
                    }
                }

                val displayedPlates = when {
                    uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.SAVED && uiState.isOwnProfile -> uiState.savedPlates
                    uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.COLLECTIONS -> emptyList()
                    else -> uiState.plates
                }

                if (uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.SAVED && uiState.isOwnProfile && uiState.savedPlates.isEmpty()) {
                    item {
                        EmptyStateCentered(
                            title = "Sin platos guardados",
                            message = "Pulsa el icono guardar en cualquier plato del feed",
                            icon = Icons.Default.Star,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp)
                        )
                    }
                }

                // Grid de platos o empty state
                if (displayedPlates.isEmpty() && !(uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.SAVED && uiState.isOwnProfile)) {
                    item {
                        EmptyStateCentered(
                            title = if (uiState.isOwnProfile) "Tu galería está vacía" else "Sin publicaciones",
                            message = if (uiState.isOwnProfile) {
                                "Publica desde Añadir en la barra inferior para mostrar tus platos."
                            } else {
                                "Cuando esta persona publique, verás aquí sus platos."
                            },
                            icon = Icons.Default.Collections,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                        )
                    }
                } else if (displayedPlates.isNotEmpty()) {
                    val plateRows = displayedPlates.chunked(2)
                    items(plateRows.size) { rowIndex ->
                        val rowPlates = plateRows[rowIndex]
                        val rowKey = rowPlates.joinToString { it.id }
                        var visible by remember(rowKey) { mutableStateOf(false) }
                        LaunchedEffect(rowKey) {
                            kotlinx.coroutines.delay(rowIndex * 60L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(250)) + slideInVertically(tween(280)) { 30 }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPlates.forEach { plate ->
                                    PlateGridItem(
                                        plate = plate,
                                        modifier = Modifier.weight(1f),
                                        showEditButton = uiState.isOwnProfile && uiState.activeTab == com.app.foodranker.viewmodel.ProfileTab.MY_PLATES,
                                        onEdit = { showEditPlateId = plate.id },
                                        onClick = { onNavigateToPlate(plate.id) }
                                    )
                                }
                                if (rowPlates.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Zona de cuenta al final (propio perfil): no interrumpe el grid de platos
                if (uiState.isOwnProfile) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        ProfileAccountDangerZone(
                            deleteError = deleteError,
                            isDeleting = uiState.isDeletingAccount,
                            onDeleteClick = { showDeleteDialog = true },
                            onPrivacy = onNavigateToPrivacy,
                            onTerms = onNavigateToTerms,
                            onReferral = onNavigateToReferral,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet editar plato
    showEditPlateId?.let { plateId ->
        val plate = uiState.plates.find { it.id == plateId }
        if (plate != null) {
            EditPlateSheet(
                plate = plate,
                onDismiss = { showEditPlateId = null },
                onSave = { desc ->
                    viewModel.updatePlateDescription(plateId, desc)
                    showEditPlateId = null
                }
            )
        }
    }

    // Bottom sheet editar perfil
    if (showEditSheet) {
        EditProfileSheet(
            currentBio = uiState.user?.bio ?: "",
            currentCity = uiState.user?.city ?: "",
            currentWebsite = uiState.user?.website ?: "",
            isSaving = uiState.isSavingProfile,
            onDismiss = { showEditSheet = false },
            onSave = { bio, city, website ->
                viewModel.updateProfile(bio, city, website) { showEditSheet = false }
            }
        )
    }

    // Diálogo confirmación eliminar cuenta
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar cuenta?", fontWeight = FontWeight.Bold, color = ErrorRed) },
            text = {
                Text(
                    "Esta acción es irreversible. Se borrarán permanentemente:\n\n• Tu perfil y foto\n• Todos tus platos publicados\n• Todas tus valoraciones\n• Tu historial de puntuación",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAccount(
                        onSuccess = {
                            authViewModel.signOut()
                            onAccountDeleted()
                        },
                        onError = { error -> deleteError = error }
                    )
                }) {
                    Text("Eliminar definitivamente", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ProfileSectionTitle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrangePrimary.copy(alpha = 0.92f),
                modifier = Modifier.size(24.dp)
            )
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
    }
}

@Composable
private fun ProfileSocialSummary(
    followerCount: Int,
    followingCount: Int,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.cardRaised)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileSocialChip(
                    value = followerCount.formatCompact(),
                    label = "Seguidores",
                    onClick = onFollowersClick
                )
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(DividerColor))
                ProfileSocialChip(
                    value = followingCount.formatCompact(),
                    label = "Siguiendo",
                    onClick = onFollowingClick
                )
            }
        }
    }
}

@Composable
private fun ProfileSocialChip(value: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = OrangePrimary)
        Text(label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileAccountDangerZone(
    deleteError: String?,
    isDeleting: Boolean,
    onDeleteClick: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onReferral: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.card)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Tu cuenta",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
            )

            HorizontalDivider(color = DividerColor.copy(alpha = 0.85f))

            Surface(
                onClick = onReferral,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFF3E0)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text("Invita amigos", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Comparte tu código y gana XP", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = OrangePrimary)
                }
            }

            Spacer(Modifier.height(6.dp))

            Surface(
                onClick = onPrivacy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = OrangePrimary.copy(alpha = 0.06f)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = OrangePrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                        Text("Política de privacidad", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                }
            }

            Surface(
                onClick = onTerms,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(10.dp),
                color = OrangePrimary.copy(alpha = 0.06f)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Article,
                            contentDescription = null,
                            tint = OrangePrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                        Text("Términos de uso", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DividerColor.copy(alpha = 0.85f))

            Surface(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Eliminación de cuenta",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            "Acción permanente. Se borrarán tu perfil y los datos asociados.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (deleteError != null) {
                        Text(deleteError, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    OutlinedButton(
                        onClick = onDeleteClick,
                        enabled = !isDeleting,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ErrorRed, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminar mi cuenta permanentemente")
                        }
                    }
                    Text(
                        "Tu perfil, platos y valoraciones se borrarán para siempre. No tiene vuelta atrás.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHero(
    name: String, photoUrl: String, isPremium: Boolean,
    paddingTop: androidx.compose.ui.unit.Dp,
    bio: String = "", city: String = ""
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp + paddingTop)
            .background(Brush.verticalGradient(listOf(Color(0xFF1A0A00), OrangePrimary, Color(0xFFFF8C42))))
    ) {
        // Brillo tipo "historias" detrás del avatar y scrim superior legible sobre la barra
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(100.dp + paddingTop)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )
        // Círculos decorativos
        Box(modifier = Modifier.size(200.dp).offset(x = 220.dp, y = (-40).dp).background(Color.White.copy(alpha = 0.05f), CircleShape))
        Box(modifier = Modifier.size(120.dp).offset(x = (-30).dp, y = 120.dp).background(Color.White.copy(alpha = 0.05f), CircleShape))

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Foto con anillo tipo "historia" suave sobre blanco
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(OrangePrimary, Color.White.copy(alpha = 0.95f), StarYellow.copy(alpha = 0.9f), OrangePrimary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .background(Color.White, CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(DividerColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(52.dp), tint = TextSecondary)
                    }
                }
                if (isPremium) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).size(28.dp),
                        shape = CircleShape,
                        color = StarYellow
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("⭐", fontSize = 14.sp) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White)
            if (bio.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(bio, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp))
            }
            if (city.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(city, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                }
            }
        }
    }
}

@Composable
private fun ProfileStats2x2(
    totalPlates: Int,
    likesReceived: Int,
    likesGiven: Int,
    ratingsGiven: Int,
    cityRank: Int = 0,
    city: String = ""
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.cardRaised)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(totalPlates.formatCompact(), "Platos\npublicados", modifier = Modifier.weight(1f))
                VerticalDivider()
                StatItem(likesReceived.formatCompact(), "Likes\nrecibidos", modifier = Modifier.weight(1f))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DividerColor)
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(likesGiven.formatCompact(), "Likes\ndados", modifier = Modifier.weight(1f))
                VerticalDivider()
                StatItem(ratingsGiven.formatCompact(), "Valoraciones\ndadas", modifier = Modifier.weight(1f))
            }
            if (cityRank > 0 && city.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DividerColor)
                StatItem(
                    value = "#$cityRank en $city",
                    label = "Ranking ciudad",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(DividerColor)
    )
}

@Composable
fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OrangePrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(
    currentBio: String,
    currentCity: String,
    currentWebsite: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var bio     by remember { mutableStateOf(currentBio) }
    var city    by remember { mutableStateOf(currentCity) }
    var website by remember { mutableStateOf(currentWebsite) }

    val websiteValid = website.isBlank() || isValidWebsite(website)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(26.dp))
                Text("Editar perfil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= com.app.foodranker.utils.InputLimits.BIO) bio = it },
                label = { Text("Bio") },
                placeholder = { Text("Cuéntanos algo sobre ti...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                shape = MaterialTheme.shapes.small,
                supportingText = { Text("${bio.length}/${com.app.foodranker.utils.InputLimits.BIO}", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    focusedLabelColor = OrangePrimary
                )
            )

            OutlinedTextField(
                value = city,
                onValueChange = { if (it.length <= com.app.foodranker.utils.InputLimits.CITY) city = it },
                label = { Text("Ciudad") },
                placeholder = { Text("¿Dónde eres foodie?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                leadingIcon = {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextSecondary)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    focusedLabelColor = OrangePrimary
                )
            )

            OutlinedTextField(
                value = website,
                onValueChange = { if (it.length <= com.app.foodranker.utils.InputLimits.WEBSITE) website = it },
                label = { Text("Web o red social (opcional)") },
                placeholder = { Text("instagram.com/tu_usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = !websiteValid,
                supportingText = if (!websiteValid) {
                    { Text("URL no válida", color = ErrorRed, fontSize = 11.sp) }
                } else null,
                shape = MaterialTheme.shapes.small,
                leadingIcon = {
                    Icon(Icons.Outlined.Link, contentDescription = null, tint = TextSecondary)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    focusedLabelColor = OrangePrimary
                )
            )

            Button(
                onClick = { onSave(bio, city, website) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isSaving && websiteValid,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                } else {
                    Text("Guardar cambios", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

private fun isValidWebsite(input: String): Boolean {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return true
    if (trimmed.contains(" ")) return false
    // Acepta dominio simple (algo.algo) o URL con esquema http(s)
    val pattern = Regex(
        pattern = "^(https?://)?([\\w-]+\\.)+[a-zA-Z]{2,}(/[\\w\\-./?%&=#@]*)?$"
    )
    return pattern.matches(trimmed)
}

@Composable
private fun LevelCard(xp: Int, badges: List<String>) {
    val level = RewardManager.getLevel(xp)
    val progress = RewardManager.getProgress(xp)
    val nextXP = RewardManager.getNextLevelXP(xp)
    var badgeDetail by remember { mutableStateOf<RewardManager.Badge?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.cardRaised)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(level.emoji, fontSize = 28.sp)
                    Column {
                        Text("Nivel y XP", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Text(level.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("$xp XP", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                if (level.number < 6) {
                    Text("🎯 $nextXP XP", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Medium)
                }
            }

            // Barra de progreso
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = OrangePrimary,
                trackColor = DividerColor
            )

            if (badges.isNotEmpty()) {
                Text("Logros · toca para más info", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    lazyItems(badges.distinct(), key = { it }) { badgeId ->
                        val badge = RewardManager.getBadge(badgeId)
                        if (badge != null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = OrangePrimary.copy(alpha = 0.12f),
                                tonalElevation = 0.dp,
                                modifier = Modifier.clickable { badgeDetail = badge }
                            ) {
                                Text(
                                    "${badge.emoji} ${badge.name}",
                                    fontSize = 12.sp,
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    badgeDetail?.let { b ->
        AlertDialog(
            onDismissRequest = { badgeDetail = null },
            title = { Text("${b.emoji} ${b.name}", fontWeight = FontWeight.Bold) },
            text = { Text(b.description, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { badgeDetail = null }) {
                    Text("Entendido", color = OrangePrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionsSection(
    collections: List<com.app.foodranker.data.model.PlateCollection>,
    collectionPlates: List<Plate>,
    isLoadingCollectionPlates: Boolean,
    onCreateCollection: (String, String) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onCollectionTap: (List<String>) -> Unit,
    onClearCollectionPlates: () -> Unit,
    onNavigateToPlate: (String) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf("★") }
    var openCollection by remember { mutableStateOf<com.app.foodranker.data.model.PlateCollection?>(null) }
    var collectionToDelete by remember { mutableStateOf<com.app.foodranker.data.model.PlateCollection?>(null) }

    val emojiOptions = listOf("★","♥","◆","♦","♠","♣","✿","♪","☺","✓","●","▲")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Mis listas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            TextButton(onClick = { showCreate = true }) {
                Text("+ Nueva lista", color = OrangePrimary, fontWeight = FontWeight.Medium)
            }
        }

        if (collections.isEmpty()) {
            EmptyStateCentered(
                title = "Sin listas todavia",
                message = "Crea listas para organizar tus platos favoritos",
                icon = Icons.Default.Star,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            collections.forEach { col ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        openCollection = col
                        onCollectionTap(col.plateIds)
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = MaterialTheme.shapes.small, color = OrangePrimary.copy(alpha = 0.1f)) {
                            Text(col.emoji, fontSize = 24.sp, modifier = Modifier.padding(8.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(col.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("${col.plateIds.size} platos", fontSize = 12.sp, color = TextSecondary)
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        IconButton(onClick = { collectionToDelete = col }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                                tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet: platos de la lista seleccionada
    openCollection?.let { col ->
        ModalBottomSheet(
            onDismissRequest = {
                openCollection = null
                onClearCollectionPlates()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(col.emoji, fontSize = 22.sp)
                    Text(col.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Spacer(Modifier.weight(1f))
                    Text("${col.plateIds.size} platos", fontSize = 13.sp, color = TextSecondary)
                }

                if (isLoadingCollectionPlates) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrangePrimary)
                    }
                } else if (collectionPlates.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (col.plateIds.isEmpty()) "Esta lista está vacía" else "No se pudieron cargar los platos",
                            color = TextSecondary, textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(collectionPlates) { plate ->
                            Card(
                                modifier = Modifier
                                    .aspectRatio(0.85f)
                                    .clickable {
                                        openCollection = null
                                        onClearCollectionPlates()
                                        onNavigateToPlate(plate.id)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box {
                                    if (plate.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = plate.imageUrl,
                                            contentDescription = plate.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(
                                                Brush.verticalGradient(listOf(OrangePrimary.copy(alpha = 0.3f), OrangePrimary.copy(alpha = 0.7f)))
                                            ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(plate.category.emoji, fontSize = 40.sp)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                                startY = 0.4f
                                            ))
                                    )
                                    Text(
                                        plate.name,
                                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmación antes de borrar una lista
    collectionToDelete?.let { col ->
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text("¿Eliminar lista?", fontWeight = FontWeight.Bold) },
            text = { Text("Se eliminará la lista \"${col.name}\". Los platos no se borrarán, solo dejarán de estar en esta lista.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCollection(col.id)
                    collectionToDelete = null
                }) {
                    Text("Eliminar", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (showCreate) {
        ModalBottomSheet(onDismissRequest = { showCreate = false; newName = "" }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Nueva lista", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojiOptions.forEach { emoji ->
                        Surface(modifier = Modifier.size(36.dp).clickable { newEmoji = emoji },
                            shape = MaterialTheme.shapes.small,
                            color = if (newEmoji == emoji) OrangePrimary.copy(alpha = 0.15f) else SurfaceMuted) {
                            Box(contentAlignment = Alignment.Center) { Text(emoji) }
                        }
                    }
                }
                OutlinedTextField(value = newName, onValueChange = { newName = it },
                    label = { Text("Nombre de la lista") }, placeholder = { Text("Ej: Mejor pizza en Madrid") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, focusedLabelColor = OrangePrimary))
                Button(onClick = { onCreateCollection(newName, newEmoji); showCreate = false; newName = "" },
                    enabled = newName.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)) {
                    Text("Crear lista", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPlateSheet(
    plate: Plate,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val maxLen = com.app.foodranker.utils.InputLimits.PLATE_DESCRIPTION
    var description by remember { mutableStateOf(plate.description) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = OrangePrimary)
                Text(
                    text = buildString {
                        append("Editar ")
                        append(plate.name)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= maxLen) description = it },
                label = { Text("Descripción") },
                placeholder = { Text("¿Qué lo hace especial?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false, maxLines = 4,
                shape = MaterialTheme.shapes.small,
                supportingText = { Text("${description.length}/$maxLen", color = TextSecondary, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, focusedLabelColor = OrangePrimary)
            )
            Button(
                onClick = { onSave(description) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) { Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White) }
        }
    }
}

@Composable
fun PlateGridItem(plate: Plate, modifier: Modifier = Modifier, showEditButton: Boolean = false, onEdit: () -> Unit = {}, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.card)
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                if (plate.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = plate.imageUrl,
                        contentDescription = plate.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(Brush.verticalGradient(plate.category.cardGradient())),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(plate.category.emoji, fontSize = 32.sp)
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = OrangePrimary
                ) {
                    Text(
                        "⭐ ${"%.1f".format(plate.averageScore)}",
                        color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                if (showEditButton) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                            .clickable { onEdit() },
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp).padding(4.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(plate.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, maxLines = 1)
                Text("${plate.city}, ${plate.country}", fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                if (plate.likes > 0) {
                    Text("❤️ ${plate.likes.formatCompact()}", fontSize = 11.sp, color = OrangePrimary)
                }
            }
        }
    }
}

@Composable
private fun RivalCard(rivalName: String, rivalXp: Int, gap: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        border = BorderStroke(1.dp, Color(0xFFFFCC80)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("⚔️", fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Rival cercano",
                    fontSize = 11.sp,
                    color = Color(0xFFA0522D),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    rivalName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF5D2E00)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "solo $gap XP más",
                    fontSize = 12.sp,
                    color = Color(0xFFA0522D),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$rivalXp XP",
                    fontSize = 11.sp,
                    color = Color(0xFFA0522D)
                )
            }
        }
    }
}

