package com.app.foodranker.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.ui.components.EmptyStateCentered
import com.app.foodranker.ui.components.PlateCardHorizontal
import com.app.foodranker.ui.theme.AppSpacing
import com.app.foodranker.ui.theme.*
import com.app.foodranker.utils.formatCompact
import com.app.foodranker.viewmodel.ExploreViewModel
import com.app.foodranker.viewmodel.SortOption
import androidx.compose.animation.*
import androidx.compose.animation.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateBack: () -> Unit,
    onPlateClick: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var isGridView  by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            Column(
                modifier = Modifier
                    .background(SurfaceWhite)
                    .padding(bottom = 8.dp)
            ) {
                TopAppBar(
                    title = {
                        Text("Explorar", fontWeight = FontWeight.Bold, color = TextPrimary)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Cambiar vista",
                                tint = TextPrimary
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = "Ordenar",
                                    tint = TextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                option.label,
                                                color = if (uiState.sortBy == option) OrangePrimary else TextPrimary,
                                                fontWeight = if (uiState.sortBy == option) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.onSortChange(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
                )

                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text("Buscar plato, restaurante o ciudad...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenHorizontal, vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = DividerColor
                    )
                )

                // Toggle Platos / Usuarios
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.searchMode == com.app.foodranker.viewmodel.SearchMode.PLATES,
                        onClick = { viewModel.setSearchMode(com.app.foodranker.viewmodel.SearchMode.PLATES) },
                        label = { Text("🍽️ Platos") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = SurfaceWhite)
                    )
                    FilterChip(
                        selected = uiState.searchMode == com.app.foodranker.viewmodel.SearchMode.USERS,
                        onClick = { viewModel.setSearchMode(com.app.foodranker.viewmodel.SearchMode.USERS) },
                        label = { Text("👤 Personas") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangePrimary, selectedLabelColor = SurfaceWhite)
                    )
                }

                if (uiState.searchMode == com.app.foodranker.viewmodel.SearchMode.PLATES) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = AppSpacing.screenHorizontal, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.onCategoryChange(null) },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor = SurfaceWhite
                        )
                    )
                    PlateCategory.entries.forEach { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.onCategoryChange(category) },
                            label = { Text("${category.emoji} ${category.displayName}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary,
                                selectedLabelColor = SurfaceWhite
                            )
                        )
                    }
                }
                } // fin if PLATES mode (categorías)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Vista de usuarios
            if (uiState.searchMode == com.app.foodranker.viewmodel.SearchMode.USERS) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrangePrimary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AppSpacing.screenHorizontal),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.userResults.isEmpty()) {
                            item {
                                EmptyStateCentered(title = "Sin resultados", message = "Prueba a buscar por nombre de usuario.",
                                    modifier = Modifier.padding(vertical = 24.dp), icon = Icons.Outlined.SearchOff)
                            }
                        }
                        items(uiState.userResults, key = { it.id }) { user ->
                            UserResultCard(user = user, onClick = { onPlateClick(user.id) })
                        }
                    }
                }
                return@Column
            }

            // Barra de estado de búsqueda
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screenHorizontal, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isLoading) "Buscando..."
                    else "${uiState.results.size.formatCompact()} platos encontrados",
                    fontSize = 14.sp, color = TextSecondary
                )
                Text(
                    text = uiState.sortBy.label,
                    fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Medium
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else {
            AnimatedContent(
                targetState = isGridView,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "explore_view_toggle"
            ) { gridView ->
            if (gridView) {
                // Vista cuadrícula
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AppSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = uiState.results,
                        key = { _, plate -> plate.id }
                    ) { index, plate ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(plate.id) {
                            kotlinx.coroutines.delay(index * 40L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.92f)
                        ) {
                            ExploreGridCard(plate = plate, onClick = { onPlateClick(plate.id) })
                        }
                    }
                }
            } else {
                // Vista lista (gridView == false)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AppSpacing.screenHorizontal),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = uiState.results,
                        key = { _, plate -> plate.id }
                    ) { index, plate ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(plate.id) {
                            kotlinx.coroutines.delay(index * 50L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInVertically(initialOffsetY = { 80 }, animationSpec = tween(300)) + fadeIn(tween(300))
                        ) {
                            PlateCardHorizontal(
                                plate = plate,
                                position = index + 1,
                                onClick = { onPlateClick(plate.id) }
                            )
                        }
                    }

                    if (uiState.results.isEmpty()) {
                        item {
                            EmptyStateCentered(
                                title = if (uiState.query.isNotEmpty()) "Sin resultados" else "Nada que mostrar aquí",
                                message = if (uiState.query.isNotEmpty())
                                    "Prueba con otro término o revisa la ortografía de «${uiState.query}»."
                                else "Cambia de categoría o usa la búsqueda para descubrir platos.",
                                modifier = Modifier.padding(vertical = 24.dp),
                                icon = if (uiState.query.isNotEmpty()) Icons.Outlined.SearchOff else Icons.Outlined.Search
                            )
                        }
                    }
                }
            }
            } // fin AnimatedContent gridView
            } // fin else (no loading)

            if (!uiState.isLoading && uiState.results.isEmpty() && isGridView) {
                EmptyStateCentered(
                    title = "Nada que mostrar aquí",
                    message = "Cambia de categoría o usa la búsqueda.",
                    modifier = Modifier.padding(vertical = 24.dp),
                    icon = Icons.Outlined.Search
                )
            }
        }
    }
}

@Composable
private fun ExploreGridCard(plate: com.app.foodranker.data.model.Plate, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
        shape = MaterialTheme.shapes.medium,
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (plate.imageUrl.isNotEmpty()) {
                coil.compose.AsyncImage(
                    model = plate.imageUrl,
                    contentDescription = plate.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    plate.category.gridColor(),
                                    plate.category.gridColor().copy(alpha = 0.6f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(plate.category.emoji, fontSize = 40.sp)
                }
            }
            // Gradiente + info
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f)
                            ), startY = 100f
                        )
                    )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = OrangePrimary
                ) {
                    Text(
                        "★ ${"%.1f".format(plate.averageScore)}",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    plate.name,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    "${plate.city}, ${plate.country}",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun UserResultCard(user: com.app.foodranker.viewmodel.UserResult, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(DividerColor), contentAlignment = Alignment.Center) {
                if (user.photoUrl.isNotEmpty()) {
                    coil.compose.AsyncImage(model = user.photoUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                } else {
                    Text(user.name.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold,
                        color = TextSecondary, fontSize = 18.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                if (user.bio.isNotEmpty()) Text(user.bio, fontSize = 13.sp, color = TextSecondary, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            if (user.xp > 0) {
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    color = OrangePrimary.copy(alpha = 0.1f)) {
                    Text("${user.xp} XP", fontSize = 11.sp, color = OrangePrimary,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

private fun com.app.foodranker.data.model.PlateCategory.gridColor() = when (this) {
    com.app.foodranker.data.model.PlateCategory.PASTA     -> androidx.compose.ui.graphics.Color(0xFFE8A838)
    com.app.foodranker.data.model.PlateCategory.SUSHI     -> androidx.compose.ui.graphics.Color(0xFF2D7AAF)
    com.app.foodranker.data.model.PlateCategory.BURGER    -> androidx.compose.ui.graphics.Color(0xFFBF4828)
    com.app.foodranker.data.model.PlateCategory.PIZZA     -> androidx.compose.ui.graphics.Color(0xFFD44030)
    com.app.foodranker.data.model.PlateCategory.TAPAS     -> androidx.compose.ui.graphics.Color(0xFFBF6840)
    com.app.foodranker.data.model.PlateCategory.RAMEN     -> androidx.compose.ui.graphics.Color(0xFFD47828)
    com.app.foodranker.data.model.PlateCategory.STEAK     -> androidx.compose.ui.graphics.Color(0xFF8B4040)
    com.app.foodranker.data.model.PlateCategory.SEAFOOD   -> androidx.compose.ui.graphics.Color(0xFF2090B0)
    com.app.foodranker.data.model.PlateCategory.DESSERT   -> androidx.compose.ui.graphics.Color(0xFFD46898)
    com.app.foodranker.data.model.PlateCategory.BREAKFAST -> androidx.compose.ui.graphics.Color(0xFFD4A828)
    com.app.foodranker.data.model.PlateCategory.SALAD     -> androidx.compose.ui.graphics.Color(0xFF4A9848)
    com.app.foodranker.data.model.PlateCategory.OTHER     -> androidx.compose.ui.graphics.Color(0xFF787888)
}