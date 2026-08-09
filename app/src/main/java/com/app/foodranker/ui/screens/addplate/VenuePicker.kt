package com.app.foodranker.ui.screens.addplate

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.Venue
import com.app.foodranker.data.repository.VenueSuggestion
import com.app.foodranker.ui.theme.*

/**
 * Selector del local, primer paso de la identidad canónica (ver docs/VENUES.md).
 *
 * Camino principal: "usar mi ubicación", porque quien fotografía un plato está
 * físicamente en el restaurante — el local es uno de los más cercanos y basta con
 * tocarlo. La búsqueda por texto queda como alternativa para quien no da permiso.
 *
 * Una vez elegido el local se muestran los platos que ya tiene registrados: tocar
 * uno lleva a valorarlo en vez de crear un duplicado. Ese es el mecanismo que de
 * verdad evita los casi-duplicados; el slug solo caza los exactos.
 */
@Composable
fun VenuePicker(
    venue: Venue?,
    suggestions: List<VenueSuggestion>,
    isLoading: Boolean,
    error: String?,
    dishes: List<Plate>,
    isLoadingDishes: Boolean,
    onUseLocation: () -> Unit,
    onSearch: (String) -> Unit,
    onSelect: (VenueSuggestion) -> Unit,
    onClear: () -> Unit,
    onExistingDishClick: (String) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onUseLocation() }

    if (venue != null) {
        SelectedVenue(venue = venue, onClear = onClear)

        Spacer(Modifier.height(12.dp))
        when {
            isLoadingDishes -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = OrangePrimary)
                Spacer(Modifier.width(8.dp))
                Text("Buscando platos de este sitio…", fontSize = 12.sp, color = TextSecondary)
            }

            dishes.isNotEmpty() -> {
                Text(
                    "Ya valorados aquí · toca uno si es el tuyo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dishes.size) { i ->
                        ExistingDishChip(dishes[i]) { onExistingDishClick(dishes[i].id) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Si tu plato no está, sigue rellenando abajo y lo creas.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            else -> Text(
                "Nadie ha valorado platos aquí todavía. Vas a ser el primero.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        return
    }

    // ── Sin local elegido ──────────────────────────────────────────────────
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) onUseLocation()
                else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Buscar sitios cerca de mí", fontWeight = FontWeight.Bold)
        }

        Text("o búscalo por nombre", fontSize = 11.sp, color = TextSecondary)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Nombre del restaurante") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { onSearch(query) }, enabled = query.isNotBlank() && !isLoading) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                }
            }
        )

        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = OrangePrimary)
                Spacer(Modifier.width(8.dp))
                Text("Buscando…", fontSize = 12.sp, color = TextSecondary)
            }
        }

        error?.let {
            Text(it, color = ErrorRed, fontSize = 12.sp)
        }

        suggestions.forEach { s ->
            Surface(
                onClick = { onSelect(s) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = OrangePrimary.copy(alpha = 0.06f)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (s.address.isNotBlank()) {
                            Text(s.address, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedVenue(venue: Venue, onClear: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = SuccessGreen.copy(alpha = 0.10f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Place, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(venue.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val place = listOf(venue.address.ifBlank { venue.city }, venue.country)
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (place.isNotBlank()) {
                    Text(place, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Cambiar de sitio", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun ExistingDishChip(plate: Plate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = SurfaceWhite,
        modifier = Modifier.width(120.dp)
    ) {
        Column {
            AsyncImage(
                model = plate.imageUrl,
                contentDescription = plate.name,
                modifier = Modifier.fillMaxWidth().height(72.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(plate.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "★ %.1f".format(plate.averageScore),
                    fontSize = 10.sp,
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
