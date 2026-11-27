package com.app.foodranker.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.foodranker.R
import com.app.foodranker.ui.theme.*
import com.app.foodranker.viewmodel.BloqueadosViewModel
import kotlinx.coroutines.launch

/**
 * A quién has bloqueado, con un botón para deshacerlo.
 *
 * La lista se quita sola al desbloquear, sin recargar: el ViewModel escucha la
 * subcolección, así que basta con tocar el documento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloqueadosScreen(
    onNavigateBack: () -> Unit,
    viewModel: BloqueadosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val textoHecho = stringResource(R.string.block_undone)
    val textoFallo = stringResource(R.string.block_failed)

    Scaffold(
        containerColor = FoodTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.block_list_title),
                        fontWeight = FontWeight.Bold,
                        color = FoodTheme.colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = FoodTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FoodTheme.colors.surface)
            )
        }
    ) { padding ->
        when {
            uiState.cargando -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = OrangePrimary) }

            uiState.usuarios.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = FoodTheme.colors.textSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.block_list_empty),
                    color = FoodTheme.colors.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    // Se dice que no se avisa a nadie: es la duda que tiene todo el que
                    // bloquea por primera vez, y no decirlo frena a quien debería hacerlo.
                    Text(
                        stringResource(R.string.block_list_note),
                        color = FoodTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(uiState.usuarios, key = { it.id }) { usuario ->
                    val nombre = usuario.name.ifBlank { stringResource(R.string.user_fallback_name) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (usuario.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = usuario.photoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(FoodTheme.colors.surfaceMuted),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = FoodTheme.colors.textSecondary
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            nombre,
                            color = FoodTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            viewModel.desbloquear(usuario.id) { ok ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(if (ok) textoHecho else textoFallo)
                                }
                            }
                        }) {
                            Text(stringResource(R.string.block_unblock), color = OrangePrimary)
                        }
                    }
                }
            }
        }
    }
}
