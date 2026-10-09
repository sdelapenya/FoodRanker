package com.app.foodranker.ui.screens.nearby

import androidx.compose.ui.res.stringResource
import com.app.foodranker.R
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.foodranker.ui.components.EmptyStateCentered
import com.app.foodranker.ui.components.PlateCardHorizontal
import com.app.foodranker.ui.theme.*
import com.app.foodranker.viewmodel.NearbyDishesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyDishesScreen(
    onNavigateBack: () -> Unit,
    onPlateClick: (String) -> Unit,
    /** Lleva a publicar el primer plato de ese local, que es lo que promete el texto. */
    onVenueClick: (String) -> Unit = {},
    viewModel: NearbyDishesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.load() }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.load() else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(uiState.error) {
        val msg = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nb_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.legal_back), tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }

                uiState.noLocationPermission -> EmptyStateCentered(
                    icon = Icons.Default.LocationOn,
                    title = stringResource(R.string.nrb_need_loc),
                    message = stringResource(R.string.nrb_need_loc_d),
                    actionLabel = stringResource(R.string.nrb_enable),
                    onAction = { locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                )

                uiState.results.isEmpty() -> Column {
                    EmptyStateCentered(
                        icon = Icons.Default.MyLocation,
                        title = stringResource(R.string.nrb_empty),
                        // Tres casos, no dos: antes, buscar y SÍ encontrar locales
                        // enseñaba "tampoco hemos encontrado locales cerca" con la
                        // lista de locales justo debajo.
                        message = when {
                            !uiState.searchedPlaces ->
                                stringResource(R.string.nrb_empty_hint)
                            uiState.placesSuggestions.isEmpty() ->
                                stringResource(R.string.nrb_empty_searched)
                            else ->
                                stringResource(R.string.nrb_empty_pick)
                        },
                        actionLabel = if (uiState.searchedPlaces) null else stringResource(R.string.nrb_search),
                        onAction = if (uiState.searchedPlaces) null else { { viewModel.searchNearbyViaPlaces() } }
                    )
                    if (uiState.isSearchingPlaces) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OrangePrimary)
                        }
                    }
                    if (uiState.placesSuggestions.isNotEmpty()) {
                        Text(
                            stringResource(R.string.nrb_venues),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        uiState.placesSuggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onVenueClick(suggestion.placeId) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(suggestion.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(suggestion.address, fontSize = 12.sp, color = TextSecondary)
                                }
                                // Sin una pista visual, la lista parecía decorativa: los
                                // testers informaron de que no se podía "hacer nada" con ella.
                                Icon(
                                    Icons.Default.AddCircleOutline,
                                    contentDescription = stringResource(R.string.nrb_post_here),
                                    tint = OrangePrimary
                                )
                            }
                            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }

                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(uiState.results) { nearbyVenue ->
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    nearbyVenue.venue.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    formatDistance(nearbyVenue.distanceMeters),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            nearbyVenue.dishes.take(3).forEachIndexed { index, plate ->
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
}

private fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m" else "%.1f km".format(meters / 1000)
