package com.sahil.peerlearn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sahil.peerlearn.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    userRepository: UserRepository = UserRepository()
) {
    val currentUid = Firebase.auth.currentUser?.uid ?: ""
    var allChatSummaries by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            userRepository.getChatSummaries(currentUid).collect {
                allChatSummaries = it
                isLoading = false
            }
        }
    }

    val filteredChats = if (searchQuery.isEmpty()) {
        allChatSummaries
    } else {
        allChatSummaries.filter { summary ->
            summary.peer.name.contains(searchQuery, ignoreCase = true)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val isCompact = screenWidth < 360.dp
        val horizontalPadding = if (isCompact) 12.dp else 16.dp
        val glowWidth = screenWidth * 1.15f

        Scaffold(
            containerColor = SpaceBlack,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpaceSurface.copy(alpha = 0.95f))
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = if (isCompact) 10.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PeerLearn",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { /* More options */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = 8.dp)
                            .background(SpaceBlack, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search", color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "All Chats",
                                color = PurpleAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.width(60.dp).height(2.dp).background(PurpleAccent))
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Standard Space Theme Glow
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(glowWidth)
                        .height(if (isCompact) 220.dp else 300.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    PurpleGlow.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PurpleAccent
                    )
                } else if (filteredChats.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = SpaceSurface
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isEmpty()) "No chats yet" else "No results found",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (searchQuery.isEmpty()) "Start messaging with peers" else "Try a different name",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredChats) { summary ->
                            ChatListItem(
                                peer = summary.peer,
                                lastMessage = summary.lastMessage,
                                lastMessageTime = summary.lastMessageTimestamp?.toDate() ?: Date(),
                                unreadCount = summary.unreadCount,
                                isPeerTyping = false,
                                onClick = {
                                    navController.navigate("chat/${summary.peer.uid}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    peer: UserProfile,
    lastMessage: String?,
    lastMessageTime: Date,
    unreadCount: Int,
    isPeerTyping: Boolean,
    onClick: () -> Unit
) {
    val avatarColor = remember(peer.name) { getAvatarColor(peer.name) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable { onClick() }
            .background(SpaceBlack)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(modifier = Modifier.size(52.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        peer.name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Online Dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF4DCA5D), CircleShape)
                        .border(2.dp, SpaceBlack, CircleShape)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        peer.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatChatTime(lastMessageTime),
                        color = if (unreadCount > 0) PurpleAccent else Color.Gray,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPeerTyping) {
                        Text(
                            "typing...",
                            color = PurpleAccent,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic
                        )
                    } else {
                        Text(
                            lastMessage ?: "",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(PurpleGlow, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                unreadCount.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .padding(start = 68.dp)
                .align(Alignment.End),
            color = Color.White.copy(alpha = 0.05f),
            thickness = 0.5.dp
        )
    }
}

fun getAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFFE53935),
        Color(0xFF8E24AA),
        Color(0xFF1E88E5),
        Color(0xFF00897B),
        Color(0xFFE91E63),
        Color(0xFF43A047),
        Color(0xFFFF7043),
        Color(0xFF5E35B1)
    )
    val index = abs(name.hashCode()) % colors.size
    return colors[index]
}

fun formatChatTime(date: Date): String {
    val now = Calendar.getInstance()
    val chatTime = Calendar.getInstance().apply { time = date }

    return when {
        now.get(Calendar.DATE) == chatTime.get(Calendar.DATE) &&
                now.get(Calendar.MONTH) == chatTime.get(Calendar.MONTH) &&
                now.get(Calendar.YEAR) == chatTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        now.get(Calendar.DAY_OF_YEAR) - chatTime.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    }
}
