package com.sahil.peerlearn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CoursesScreen() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        val isCompact = maxWidth < 360.dp
        Column(
            modifier = Modifier.padding(if (isCompact) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(if (isCompact) 64.dp else 80.dp),
                tint = Color(0xFF7C4DFF)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Courses Coming Soon! 🚀",
                color = Color.White,
                fontSize = if (isCompact) 18.sp else 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
