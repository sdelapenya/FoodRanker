package com.app.foodranker.ui.screens.addplate

import android.app.Activity
import android.net.Uri
import com.app.foodranker.utils.FoodImageValidator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.foodranker.data.model.PlateCategory
import com.app.foodranker.ui.theme.*
import com.app.foodranker.utils.AdManager
import com.app.foodranker.utils.InputLimits
import com.app.foodranker.viewmodel.AddPlateState
import com.app.foodranker.viewmodel.AddPlateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlateScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AddPlateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var plateName        by remember { mutableStateOf("") }
    var description      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PlateCategory.OTHER) }
    var restaurantName   by remember { mutableStateOf("") }
    var city             by remember { mutableStateOf("") }
    var country          by remember { mutableStateOf("") }
    var flavorScore      by remember { mutableFloatStateOf(7f) }
    var presentationScore by remember { mutableFloatStateOf(7f) }
    var valueScore       by remember { mutableFloatStateOf(7f) }
    var comment          by remember { mutableStateOf("") }
    var imageUri              by remember { mutableStateOf<Uri?>(null) }
    var imageValidating       by remember { mutableStateOf(false) }
    var imageValidationError  by remember { mutableStateOf<String?>(null) }
    var currentStep           by remember { mutableIntStateOf(1) }
    var formVisible           by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { kotlinx.coroutines.delay(100); formVisible = true }

    // Validar imagen con TFLite al seleccionarla
    LaunchedEffect(imageUri) {
        val uri = imageUri ?: return@LaunchedEffect
        imageValidating = true
        imageValidationError = null
        val result = FoodImageValidator.validate(context, uri)
        imageValidating = false
        if (result.first.not()) { imageValidationError = result.second; imageUri = null }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    LaunchedEffect(state) {
        if (state is AddPlateState.Success) {
            AdManager.showInterstitial(
                activity = context as Activity,
                onDismiss = { onSuccess(); viewModel.resetState() }
            )
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Publicar plato", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        Text("Paso $currentStep de 2", fontSize = 12.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (currentStep > 1) currentStep-- else onNavigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = formVisible,
            enter = slideInVertically(initialOffsetY = { 80 }, animationSpec = tween(500)) + fadeIn(tween(500))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Barra de progreso 2 pasos
            LinearProgressIndicator(
                progress = { if (currentStep == 1) 0.5f else 1f },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = OrangePrimary,
                trackColor = DividerColor
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(300)) { if (forward) it else -it } +
                        fadeIn(tween(250))) togetherWith
                    (slideOutHorizontally(tween(300)) { if (forward) -it else it } +
                        fadeOut(tween(200)))
                },
                modifier = Modifier.fillMaxSize(),
                label = "addplate_step"
            ) { step ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {

                // ── PASO 1: FOTO + NOMBRE + CATEGORÍA ─────────────────
                if (step == 1) {
                item {
                    PhotoPickerSection(
                        imageUri = imageUri,
                        imageValidating = imageValidating,
                        imageValidationError = imageValidationError,
                        onPickImage = { imageLauncher.launch("image/*") }
                    )
                }

                // ── DATOS DEL PLATO ───────────────────────────────────
                item {
                    SectionCard(title = "🍽️ El plato") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FoodTextField(
                                value = plateName,
                                onValueChange = { plateName = it },
                                label = "Nombre del plato *",
                                placeholder = "Ej: Croquetas de jamón ibérico",
                                maxLength = InputLimits.PLATE_NAME
                            )
                            FoodTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = "Descripción (opcional)",
                                placeholder = "¿Qué lo hace especial?",
                                singleLine = false,
                                maxLines = 3,
                                maxLength = InputLimits.PLATE_DESCRIPTION,
                                showCounter = true
                            )
                            // Chips de categoría (más rápido que dropdown)
                            Text("Categoría", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlateCategory.entries.forEach { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { selectedCategory = category },
                                        label = { Text("${category.emoji} ${category.displayName}", fontSize = 13.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = OrangePrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Botón "Siguiente" del paso 1
                item {
                    val step1Valid = plateName.isNotBlank() && imageUri != null && !imageValidating
                    Button(
                        onClick = { currentStep = 2 },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = step1Valid,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Text("Siguiente →", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    if (!step1Valid && imageUri == null) {
                        Spacer(Modifier.height(4.dp))
                        Text("Añade una foto y el nombre del plato para continuar",
                            fontSize = 12.sp, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                    }
                }
                } // fin if (step == 1)

                // ── PASO 2: UBICACIÓN + PUNTUACIÓN ────────────────────
                if (step == 2) {
                item {
                    SectionCard(title = "📍 ¿Dónde lo probaste?") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FoodTextField(
                                value = restaurantName,
                                onValueChange = { restaurantName = it },
                                label = "Restaurante / Bar *",
                                placeholder = "Nombre del local",
                                maxLength = InputLimits.RESTAURANT_NAME
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FoodTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = "Ciudad *",
                                    placeholder = "Madrid",
                                    modifier = Modifier.weight(1f),
                                    maxLength = InputLimits.CITY
                                )
                                FoodTextField(
                                    value = country,
                                    onValueChange = { country = it },
                                    label = "País *",
                                    placeholder = "España",
                                    modifier = Modifier.weight(1f),
                                    maxLength = InputLimits.COUNTRY
                                )
                            }
                        }
                    }
                }

                // ── PUNTUACIÓN ────────────────────────────────────────
                item {
                    SectionCard(title = "⭐ Tu puntuación") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ScoreSlider("Sabor", "😋", flavorScore) { flavorScore = it }
                            ScoreSlider("Presentación", "🎨", presentationScore) { presentationScore = it }
                            ScoreSlider("Precio/Calidad", "💰", valueScore) { valueScore = it }

                            val avg = (flavorScore + presentationScore + valueScore) / 3f
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = OrangePrimary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Puntuación media", fontWeight = FontWeight.Medium, color = TextPrimary)
                                    Text(
                                        "★ ${"%.1f".format(avg)}/10",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp,
                                        color = OrangePrimary
                                    )
                                }
                            }

                            FoodTextField(
                                value = comment,
                                onValueChange = { comment = it },
                                label = "Comentario (opcional)",
                                placeholder = "¿Lo recomendarías? ¿Qué lo hace especial?",
                                singleLine = false,
                                maxLines = 4,
                                maxLength = InputLimits.RATING_COMMENT,
                                showCounter = true
                            )
                        }
                    }
                }

                // ── BOTÓN PUBLICAR (paso 2) ───────────────────────────
                item {
                    val isValid = plateName.isNotBlank() && restaurantName.isNotBlank()
                            && city.isNotBlank() && country.isNotBlank()
                            && imageUri != null
                    val btnScale by animateFloatAsState(
                        targetValue = if (state is AddPlateState.Loading) 0.97f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "btnScale"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.submitPlate(
                                    context = context,
                                    name = plateName,
                                    description = description,
                                    category = selectedCategory,
                                    restaurantName = restaurantName,
                                    restaurantAddress = "",
                                    city = city,
                                    country = country,
                                    flavorScore = flavorScore,
                                    presentationScore = presentationScore,
                                    valueScore = valueScore,
                                    comment = comment,
                                    imageUri = imageUri
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp).scale(btnScale),
                            enabled = isValid && state !is AddPlateState.Loading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            if (state is AddPlateState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Publicar plato 🚀", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (state is AddPlateState.Error) {
                            Text(
                                text = (state as AddPlateState.Error).message,
                                color = ErrorRed,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                } // fin if (step == 2)
            }
            } // fin AnimatedContent
            } // fin Column
        }
    }
}

@Composable
private fun PhotoPickerSection(
    imageUri: Uri?,
    imageValidating: Boolean = false,
    imageValidationError: String? = null,
    onPickImage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onPickImage() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Foto del plato",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .clickable { onPickImage() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("📷 Cambiar foto", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFFFF8C42), OrangePrimary, Color(0xFFBF3D10)))),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📷", fontSize = 48.sp)
                    Text("Foto obligatoria *", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Toca aquí para añadir una foto del plato",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    // Error de validación de imagen con entrada animada
    AnimatedVisibility(
        visible = imageValidationError != null,
        enter = slideInVertically(tween(280)) { -20 } + fadeIn(tween(280)),
        exit = slideOutVertically(tween(200)) { -20 } + fadeOut(tween(200))
    ) {
        if (imageValidationError != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFFEDED))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Text(
                        imageValidationError,
                        fontSize = 13.sp,
                        color = androidx.compose.ui.graphics.Color(0xFFB00020),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    } // fin Column
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun FoodTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    maxLength: Int? = null,
    showCounter: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (maxLength == null || new.length <= maxLength) onValueChange(new)
            else onValueChange(new.take(maxLength))
        },
        label = { Text(label) },
        placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.5f)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp),
        supportingText = if (showCounter && maxLength != null) {
            { Text("${value.length}/$maxLength", color = TextSecondary, fontSize = 11.sp) }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangePrimary,
            focusedLabelColor = OrangePrimary
        )
    )
}

@Composable
fun ScoreSlider(label: String, emoji: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$emoji $label", fontWeight = FontWeight.Medium, color = TextPrimary)
            Text("${"%.1f".format(value)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OrangePrimary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 1f..10f,
            steps = 17,
            colors = SliderDefaults.colors(
                thumbColor = OrangePrimary,
                activeTrackColor = OrangePrimary,
                inactiveTrackColor = DividerColor
            )
        )
    }
}
