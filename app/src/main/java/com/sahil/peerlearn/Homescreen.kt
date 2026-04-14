package com.sahil.peerlearn

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.peerlearn.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar

@Composable
fun HomeScreen(
    user: FirebaseUser,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    onChatClick: (String) -> Unit,
    onPeerClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(HomeRepository(), UserRepository()) as T
        }
    })
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val recommendedPeers by viewModel.recommendedPeers.collectAsState()
    val allPeers by viewModel.allPeers.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(user.uid) {
        viewModel.initHome(user.uid)
    }

    Scaffold(
        containerColor = SpaceBlack,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(
                unreadCount = unreadCount,
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationsClick
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBlack)
        ) {
            // Purple glow at top center
            Box(
                modifier = Modifier
                    .size(450.dp, 300.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PurpleGlow.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
            // Section 2: Greeting Card
            item {
                GreetingCard(
                    user = currentUser ?: UserProfile(name = user.displayName ?: "Peer"),
                    onProfileClick = onProfileClick
                )
            }

            // Section 3: Search Bar
            item {
                HomeSearchBar(onClick = onSearchClick)
            }

            // Section 4: Recommended Peers
            item {
                SectionHeader(
                    title = "Peers For You 🎯",
                    onSeeAllClick = { /* TODO */ }
                )
            }
            
            item {
                if (uiState is HomeUiState.Loading) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(3) { ShimmerPeerCard(isVertical = false) }
                    }
                } else if (currentUser != null && currentUser!!.learnSkills.isEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        RecommendedEmptyState(onUpdateProfile = onProfileClick)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(recommendedPeers) { peer ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.98f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "card_click"
                            )

                            val matchedSkill = peer.teachSkills.firstOrNull { skill ->
                                currentUser?.learnSkills?.contains(skill) == true
                            } ?: peer.teachSkills.firstOrNull() ?: "Skills"

                            PeerCard(
                                user = peer,
                                onClick = { onPeerClick(peer.uid) },
                                onActionClick = { onPeerClick(peer.uid) },
                                actionText = "Connect",
                                isVertical = false,
                                matchedSkill = matchedSkill,
                                modifier = Modifier
                                    .scale(scale)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { onPeerClick(peer.uid) }
                            )
                        }
                    }
                }
            }

            // Section 5: All Peers
            item {
                SectionHeader(
                    title = "All Peers 👥",
                    onSeeAllClick = { /* TODO */ }
                )
            }

            if (uiState is HomeUiState.Loading) {
                items(5) {
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        ShimmerPeerCard(isVertical = true)
                    }
                }
            } else if (allPeers.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "Be the first peer! 🚀",
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                items(allPeers) { peer ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.98f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "card_click"
                    )

                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        PeerCard(
                            user = peer,
                            onClick = { onPeerClick(peer.uid) },
                            onActionClick = { onChatClick(peer.uid) },
                            actionText = "Chat",
                            isVertical = true,
                            onViewProfile = { onPeerClick(peer.uid) },
                            modifier = Modifier
                                .scale(scale)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { onPeerClick(peer.uid) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun HomeTopBar(unreadCount: Int, onSearchClick: () -> Unit, onNotificationClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PeerLearn",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        
        Row {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color.White)
            }
            IconButton(onClick = onNotificationClick) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = Color.Red,
                                modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                            ) {
                                Text(unreadCount.toString(), color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun GreetingCard(user: UserProfile, onProfileClick: () -> Unit) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val emoji = when {
        hour < 12 -> "☀️"
        hour < 17 -> "👋"
        else -> "🌙"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(PurpleGlow, PurpleAccent)
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$greeting ${user.name}! $emoji",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Find your perfect learning peer",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🔥 7 day streak",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun HomeSearchBar(onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(24.dp),
            color = SpaceSurface,
            border = BorderStroke(1.dp, Color(0xFF2A2A3D))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Search by skill...",
                    color = Color(0xFF9E9E9E),
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        IconButton(
            onClick = { Toast.makeText(context, "Filters coming soon!", Toast.LENGTH_SHORT).show() },
            modifier = Modifier
                .size(48.dp)
                .background(PurpleAccent, CircleShape)
        ) {
            Icon(
                Icons.Rounded.FilterList,
                contentDescription = "Filter",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "See all →",
            color = PurpleAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onSeeAllClick() }
        )
    }
}

@Composable
fun RecommendedEmptyState(onUpdateProfile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯 Add skills to get matched!",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onUpdateProfile,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update Profile", color = Color.White)
            }
        }
    }
}


@Composable
fun EmptyStateCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2A2A3D))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = Color(0xFF9E9E9E),
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ShimmerPeerCard(isVertical: Boolean) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1200, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        SpaceBlack,
        SpaceSurface,
        SpaceBlack
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Card(
        modifier = Modifier
            .then(if (isVertical) Modifier.fillMaxWidth() else Modifier.width(260.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFF2A2A3D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(brush))
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.width(120.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(60.dp).height(20.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                Box(modifier = Modifier.width(60.dp).height(20.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            }
            if (!isVertical) {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            }
        }
    }
}
