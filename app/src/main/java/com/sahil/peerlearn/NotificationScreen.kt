package com.sahil.peerlearn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.peerlearn.ui.theme.PurpleAccent
import com.sahil.peerlearn.ui.theme.PurpleGlow
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val currentUid = Firebase.auth.currentUser?.uid ?: ""
    val currentUserName = Firebase.auth.currentUser?.displayName ?: "Peer"
    val notifications by viewModel.notifications.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            viewModel.fetchNotifications(currentUid)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NotificationNavEvent.NavigateToChat -> {
                    onNavigateToChat(event.peerUid)
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1C))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                "Notifications",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when (val state = uiState) {
            is NotificationUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PurpleAccent)
                }
            }
            is NotificationUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = Color.Red, textAlign = TextAlign.Center)
                }
            }
            else -> {
                if (notifications.isEmpty()) {
                    // EMPTY STATE
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "🔔",
                            fontSize = 64.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No notifications yet!",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Connect with peers to get\n" +
                                    "notifications here",
                            color = Color(0xFF9E9E9E),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(notifications) { notif ->
                            NotificationCard(
                                notification = notif,
                                onAccept = {
                                    viewModel.acceptConnection(
                                        notif,
                                        currentUid,
                                        currentUserName
                                    )
                                },
                                onDecline = {
                                    viewModel.declineConnection(
                                        notif,
                                        currentUid
                                    )
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                0.5.dp,
                Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                ),
                RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(listOf(PurpleAccent, PurpleGlow)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        notification.fromName.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        notification.fromName,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        notification.message,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        formatTimeAgo(notification.createdAt),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (notification.type == "connection_request") {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Decline
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFFE53935).copy(alpha = 0.5f)
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            "Decline",
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Accept
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            "Accept",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatTimeAgo(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp.toDate().time

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}
