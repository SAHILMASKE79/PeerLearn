package com.sahil.peerlearn

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

data class MatrixColumn(
    val x: Float,
    val speed: Float,
    val chars: List<Char>,
    var offset: Float
)

@Composable
fun SplashScreen(navController: NavController) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    
    // MATRIX RAIN EFFECT
    val matrixColumns = remember {
        List(25) { i ->
            MatrixColumn(
                x = i / 25f,
                speed = Random.nextFloat() * 0.003f + 0.001f,
                chars = List(20) { ('A'..'Z').random() },
                offset = Random.nextFloat()
            )
        }
    }

    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { ms ->
                time = ms / 1000f
            }
        }
    }

    // TERMINAL ANIMATION STATES
    var displayText by remember { mutableStateOf("") }
    var showStatus1 by remember { mutableStateOf(false) }
    var showStatus2 by remember { mutableStateOf(false) }
    var showStatus3 by remember { mutableStateOf(false) }
    
    val fullText = "> Initializing PeerLearn..."

    LaunchedEffect(Unit) {
        // Typing effect
        fullText.forEachIndexed { i, _ ->
            delay(50)
            displayText = fullText.substring(0, i + 1)
        }
        
        delay(500)
        showStatus1 = true
        delay(400)
        showStatus2 = true
        delay(400)
        showStatus3 = true
        
        // Total wait 3.5 seconds to see animations
        delay(1500)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                // Ensure we have the latest verification status
                currentUser.reload().await()
                
                if (!currentUser.isEmailVerified) {
                    navController.navigate("main") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    val isComplete = UserRepository().isProfileComplete(currentUser.uid)
                    if (!isComplete) {
                        navController.navigate("main") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("main") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            } catch (e: Exception) {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // SCAN LINE ANIMATION
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ),
        label = "scanLine"
    )

    // CURSOR ANIMATION
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val isCompact = maxWidth < 360.dp
        // 1. Matrix Rain Background
        val density = LocalDensity.current
        Canvas(modifier = Modifier.fillMaxSize()) {
            matrixColumns.forEach { col ->
                col.chars.forEachIndexed { idx, char ->
                    val yProgress = (col.offset + time * col.speed + idx * 0.04f) % 1.2f
                    val y = (yProgress - 0.2f) * size.height
                    
                    if (y in -50f..size.height + 50f) {
                        val alpha = when {
                            idx == 0 -> 1f
                            idx < 4 -> 0.7f
                            idx < 8 -> 0.4f
                            else -> 0.1f
                        }
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            char.toString(),
                            col.x * size.width,
                            y,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.argb((alpha * 255).toInt(), 0, 255, 70)
                                textSize = with(density) { 14.sp.toPx() }
                                typeface = android.graphics.Typeface.MONOSPACE
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }

        // 2. Scan Line
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to Color(0xFF00FF46).copy(alpha = 0.3f),
                    1f to Color.Transparent
                ),
                start = Offset(0f, scanY * size.height - 10.dp.toPx()),
                end = Offset(size.width, scanY * size.height + 10.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }

        // 3. Corner Decorations
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerSize = 30.dp.toPx()
            val strokeW = 2.dp.toPx()
            val color = Color(0xFF00FF46).copy(alpha = 0.5f)
            val pad = 20.dp.toPx()
            
            // Top left
            drawLine(color, Offset(pad, pad), Offset(pad + cornerSize, pad), strokeW)
            drawLine(color, Offset(pad, pad), Offset(pad, pad + cornerSize), strokeW)
            
            // Top right
            drawLine(color, Offset(size.width - pad, pad), Offset(size.width - pad - cornerSize, pad), strokeW)
            drawLine(color, Offset(size.width - pad, pad), Offset(size.width - pad, pad + cornerSize), strokeW)
            
            // Bottom left
            drawLine(color, Offset(pad, size.height - pad), Offset(pad + cornerSize, size.height - pad), strokeW)
            drawLine(color, Offset(pad, size.height - pad), Offset(pad, size.height - pad - cornerSize), strokeW)
            
            // Bottom right
            drawLine(color, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - cornerSize, size.height - pad), strokeW)
            drawLine(color, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - cornerSize), strokeW)
        }

        // 4. Center Terminal Box
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .widthIn(max = 300.dp)
                .heightIn(min = if (isCompact) 190.dp else 220.dp)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF00FF46), RoundedCornerShape(8.dp))
                .padding(if (isCompact) 12.dp else 16.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "root@peerlearn:~$",
                        color = Color(0xFF00FF46),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5F57), CircleShape))
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFBD2E), CircleShape))
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF28C840), CircleShape))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Typing Content
                Text(
                    displayText,
                    color = Color(0xFF00FF46),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(8.dp))
                
                if (showStatus1) {
                    Text("> Loading modules... [OK]", color = Color(0xFF00FF46), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                if (showStatus2) {
                    Text("> Connecting Firebase... [OK]", color = Color(0xFF00FF46), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                
                Spacer(Modifier.height(12.dp))
                
                if (showStatus3) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "> Welcome to PeerLearn",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "_",
                            color = Color(0xFF00FF46).copy(alpha = cursorAlpha),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 5. Bottom Hacker Info
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isCompact) 32.dp else 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "PEER_LEARN v1.0.0",
                color = Color(0xFF00FF46).copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "[ STUDENT TO STUDENT NETWORK ]",
                color = Color(0xFF00FF46).copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }
    }
}
