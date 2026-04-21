package com.sahil.peerlearn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahil.peerlearn.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 360.dp
        val glowWidth = maxWidth * 1.15f

        Scaffold(
            containerColor = SpaceBlack,
            topBar = {
                TopAppBar(
                    title = { Text("Knowledge Feed", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceSurface)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SpaceBlack)
            ) {
            // Radial Glow Effect
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(glowWidth)
                    .height(if (isCompact) 220.dp else 300.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PurpleGlow.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isCompact) 12.dp else 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.1f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(if (isCompact) 16.dp else 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Article,
                            contentDescription = "Feed",
                            modifier = Modifier.size(48.dp),
                            tint = PurpleAccent
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Coming Soon",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The personalized learning feed is under development.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            }
        }
    }
}
