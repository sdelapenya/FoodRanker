package com.app.foodranker.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.app.foodranker.R
import com.app.foodranker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tos_screen_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.legal_back), tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.tos_updated),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            items(
                listOf(
                    R.string.tos_1_t to R.string.tos_1_b,
                    R.string.tos_2_t to R.string.tos_2_b,
                    R.string.tos_3_t to R.string.tos_3_b,
                    R.string.tos_4_t to R.string.tos_4_b,
                    R.string.tos_5_t to R.string.tos_5_b,
                    R.string.tos_6_t to R.string.tos_6_b,
                    R.string.tos_7_t to R.string.tos_7_b,
                    R.string.tos_8_t to R.string.tos_8_b,
                    R.string.tos_9_t to R.string.tos_9_b,
                    R.string.tos_10_t to R.string.tos_10_b,
                    R.string.tos_11_t to R.string.tos_11_b,
                    R.string.tos_12_t to R.string.tos_12_b,
                    R.string.tos_13_t to R.string.tos_13_b,
                    R.string.tos_14_t to R.string.tos_14_b,
                )
            ) { (titulo, cuerpo) ->
                PrivacySection(stringResource(titulo), stringResource(cuerpo))
            }
        }
    }
}
