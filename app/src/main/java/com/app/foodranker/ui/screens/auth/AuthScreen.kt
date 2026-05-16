package com.app.foodranker.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.foodranker.R
import com.app.foodranker.ui.components.FoodRankerLogo
import com.app.foodranker.ui.theme.*
import com.app.foodranker.viewmodel.AuthState
import com.app.foodranker.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onNavigateToHome()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.signInWithGoogle(it) }
            } catch (e: ApiException) { }
        }
    }

    fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
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
                    text = "Descubre los mejores platos del mundo,\nvalorados por foodies como tú",
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
                    FeaturePill(icon = Icons.Outlined.Public, label = "Global", modifier = Modifier.weight(1f))
                    FeaturePill(icon = Icons.Outlined.StarOutline, label = "Valorar", modifier = Modifier.weight(1f))
                    FeaturePill(icon = Icons.Outlined.Share, label = "Compartir", modifier = Modifier.weight(1f))
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
                        .height(56.dp)
                        .scale(btnScale),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TextPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OrangePrimary, strokeWidth = 2.dp)
                    } else {
                        Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Continuar con Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 800))
            ) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onNavigateToTerms, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("Términos", fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
                    }
                    Text("·", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.align(Alignment.CenterVertically))
                    TextButton(onClick = onNavigateToPrivacy, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("Privacidad", fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
                    }
                }
            }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
