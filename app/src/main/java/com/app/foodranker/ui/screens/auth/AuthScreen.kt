package com.app.foodranker.ui.screens.auth

import androidx.compose.ui.res.stringResource
import com.app.foodranker.R
import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.foodranker.ui.components.FoodRankerLogo
import com.app.foodranker.ui.theme.*
import com.app.foodranker.viewmodel.AuthState
import com.app.foodranker.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    // El degradado de la pantalla de login es oscuro arriba — ver LightStatusBarIcons.
    LightStatusBarIcons()

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        viewModel.checkPendingGoogleSignIn()
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mostrarHojaCorreo by remember { mutableStateOf(false) }
    val resetEnviado by viewModel.passwordResetSent.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> onNavigateToHome()
            is AuthState.Error -> errorMessage = (authState as AuthState.Error).message
            else -> {}
        }
    }

    // Se resuelve aquí, en el ámbito @Composable: dentro de la función de abajo no se puede
    // leer un recurso, porque no es componible.
    val mensajeErrorLogin = stringResource(R.string.auth_error_google)

    fun launchGoogleSignIn() {
        val activity = context as? Activity
        if (activity == null) {
            errorMessage = mensajeErrorLogin
            return
        }
        viewModel.signInWithGoogle(activity)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0A00), Color(0xFFBF3D10), OrangePrimary)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Círculos decorativos de fondo
        Box(
            modifier = Modifier
                .size(380.dp)
                .offset(x = 120.dp, y = (-180).dp)
                .background(Color.White.copy(alpha = 0.04f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-100).dp, y = 220.dp)
                .background(Color.White.copy(alpha = 0.04f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = 130.dp, y = 260.dp)
                .background(Color.White.copy(alpha = 0.03f), CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Marca principal
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
            ) + fadeIn(tween(600))
            ) {
                FoodRankerLogo(
                    markSize = 132.dp,
                    onDark = true,
                    showTagline = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Descripción
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 400))
            ) {
                Text(
                    text = stringResource(R.string.auth_tagline),
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.80f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pills de features
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 550))
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeaturePill(icon = Icons.Outlined.Public, label = stringResource(R.string.auth_feature_global), modifier = Modifier.weight(1f))
                    FeaturePill(icon = Icons.Outlined.StarOutline, label = stringResource(R.string.auth_feature_rate), modifier = Modifier.weight(1f))
                    FeaturePill(icon = Icons.Outlined.Share, label = stringResource(R.string.auth_feature_share), modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Botón Google
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(600, delayMillis = 650)
                ) + fadeIn(tween(600, delayMillis = 650))
            ) {
                val isLoading = authState is AuthState.Loading
                val btnScale by animateFloatAsState(
                    targetValue = if (isLoading) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "buttonScale"
                )
                Button(
                    onClick = { launchGoogleSignIn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        // Altura MÍNIMA, no fija. Con `.height(56.dp)` el botón no podía
                        // crecer: con el texto del sistema al 130% "Continuar con Google"
                        // pasa a dos líneas y la segunda quedaba cortada por fuera del
                        // botón — el botón principal del login, ilegible. Visto en el
                        // emulador a 720x1640 con `font_scale 1.3` (2026-11-22). Con
                        // `heightIn` el aspecto al 100% es idéntico y crece solo cuando
                        // hace falta.
                        .heightIn(min = 56.dp)
                        .scale(btnScale),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        // Fijos los dos, y tienen que ir juntos: esta pantalla no sigue el
                        // tema (su fondo es el degradado naranja, igual en claro y en
                        // oscuro) y el boton es blanco siempre. Poner aqui el color de
                        // texto del tema dejaba el "Continue with Google" en gris claro
                        // sobre blanco, ilegible — visto en el emulador en oscuro al
                        // convertir la app a FoodTheme.colors (2026-11-17).
                        containerColor = Color.White,
                        contentColor = TextPrimary // tema-fijo: boton blanco sobre degradado
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OrangePrimary, strokeWidth = 2.dp)
                    } else {
                        Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.auth_continue_google), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Con la hoja abierta el error se pinta DENTRO de ella: aquí quedaría
            // tapado y el usuario no vería por qué no ha entrado.
            errorMessage?.takeIf { !mostrarHojaCorreo }?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = Color(0xFFFFCDD2),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 750))
            ) {
                TextButton(
                    onClick = {
                        errorMessage = null
                        viewModel.clearPasswordResetNotice()
                        mostrarHojaCorreo = true
                    },
                    enabled = authState !is AuthState.Loading
                ) {
                    Text(
                        stringResource(R.string.auth_email_option),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 800))
            ) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onNavigateToTerms, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(stringResource(R.string.auth_terms), fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
                    }
                    Text("·", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.CenterVertically))
                    TextButton(onClick = onNavigateToPrivacy, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(stringResource(R.string.auth_privacy), fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
                    }
                }
            }
        }

        if (mostrarHojaCorreo) {
            EmailAuthSheet(
                cargando = authState is AuthState.Loading,
                mensajeError = errorMessage,
                avisoResetEnviado = resetEnviado,
                onEntrar = { correo, clave ->
                    errorMessage = null
                    viewModel.clearPasswordResetNotice()
                    viewModel.signInWithEmail(correo, clave)
                },
                onRegistrar = { nombre, correo, clave ->
                    errorMessage = null
                    viewModel.clearPasswordResetNotice()
                    viewModel.signUpWithEmail(nombre, correo, clave)
                },
                onRestablecer = { correo ->
                    errorMessage = null
                    viewModel.sendPasswordReset(correo)
                },
                onDismiss = {
                    mostrarHojaCorreo = false
                    errorMessage = null
                    viewModel.clearPasswordResetNotice()
                }
            )
        }
    }
}

@Composable
private fun FeaturePill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        // ⚠️ EL ICONO VA ENCIMA DEL TEXTO, NO AL LADO. No es una preferencia estética:
        // con los tres en fila y `weight(1f)`, cada pastilla mide un tercio del ancho
        // (91,5dp a 360dp de pantalla) y el icono, el separador y el relleno se comen
        // 44dp. Al texto le quedaban 47,5dp y "Compartir" pide unos 57, así que salía
        // recortado como "Compa". En inglés ("Share") no se notaba.
        //
        // Apretar los márgenes daba justo los 12dp que faltaban, y por eso se descartó:
        // volvería a romperse con el texto del sistema en grande, o en cuanto se añada
        // un idioma con una palabra más larga. Apilando, el texto dispone de los 79dp
        // completos de la pastilla y aguanta las dos cosas.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                // Red de seguridad: si algún idioma futuro no cupiera ni así, que corte
                // con puntos suspensivos y no a mitad de palabra. El defecto de Compose
                // es `Clip`, que es justo lo que producía "Compa".
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
