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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
            return HomeViewModel(HomeRepository(UserRepository())) as T
        }
    })
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val recommendedPeers by viewModel.recommendedPeersWithMatch.collectAsState()
    val allPeers by viewModel.allPeers.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var skillSearchQuery by remember { mutableStateOf("") }

    val filteredRecommendedPeers by remember(recommendedPeers, skillSearchQuery) {
        derivedStateOf {
            val query = skillSearchQuery.trim()
            if (query.isEmpty()) {
                recommendedPeers
            } else {
                recommendedPeers.filter { (peer, _) ->
                    peer.teachSkills.any { it.contains(query, ignoreCase = true) } ||
                            peer.learnSkills.any { it.contains(query, ignoreCase = true) } ||
                            currentUser?.teachSkills?.any { it.contains(query, ignoreCase = true) && peer.learnSkills.contains(it) } == true ||
                            currentUser?.learnSkills?.any { it.contains(query, ignoreCase = true) && peer.teachSkills.contains(it) } == true
                }
            }
        }
    }

    LaunchedEffect(user.uid) {
        viewModel.initHome(user.uid)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = this.maxWidth < 360.dp
        val horizontalPadding = if (isCompact) 16.dp else 24.dp
        val topGlowWidth = this.maxWidth * 1.15f
        val topGlowHeight = if (isCompact) 220.dp else 300.dp


        Scaffold(
            containerColor = SpaceBlack,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                HomeTopBar(
                    unreadCount = unreadCount,
                    onSearchClick = onSearchClick,
                    onNotificationClick = onNotificationsClick,
                    horizontalPadding = horizontalPadding,
                    isCompact = isCompact
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SpaceBlack)
            ) {
                Box(
                    modifier = Modifier
                        .width(topGlowWidth)
                        .height(topGlowHeight)
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
                    contentPadding = PaddingValues(bottom = if (isCompact) 16.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Section 2: Greeting Card
                    item(key = "greeting") {
                        GreetingCard(
                            user = currentUser ?: UserProfile(name = user.displayName ?: "Peer"),
                            onProfileClick = onProfileClick,
                            horizontalPadding = horizontalPadding,
                            isCompact = isCompact
                        )
                    }

                    // Section 3: Search Bar
                    item(key = "search_bar") {
                        HomeSearchBar(
                            onClick = onSearchClick,
                            horizontalPadding = horizontalPadding,
                            isCompact = isCompact,
                            skillSearchQuery = skillSearchQuery,
                            onSkillSearchQueryChange = { skillSearchQuery = it }
                        )
                    }

                    // Section 4: Recommended Peers
                    if (filteredRecommendedPeers.isNotEmpty()) {
                        item(key = "header_recommended") {
                            SectionHeader(
                                title = "Recommended For You",
                                onSeeAllClick = { /* TODO */ },
                                horizontalPadding = horizontalPadding,
                                isCompact = isCompact
                            )
                        }

                        // Recommended Peers Section - Vertical Layout
                        items(
                            items = filteredRecommendedPeers,
                            key = { "rec_${it.first.uid}" }
                        ) { (peer, match) ->
                            Box(modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp)) {
                                PeerCard(
                                    user = peer,
                                    onViewProfile = { onPeerClick(peer.uid) },
                                    onConnectClick = { onChatClick(peer.uid) },
                                    matchPercentage = match,
                                    isOnline = true,
                                    currentUid = user.uid,
                                    viewModel = viewModel
                                )
                            }
                        }
                        
                        item(key = "spacer_recommended") { Spacer(Modifier.height(16.dp)) }
                    } else if (uiState is HomeUiState.Loading) {
                        item(key = "header_loading_recommended") {
                            SectionHeader(
                                title = "Finding Peers... 🎯",
                                onSeeAllClick = { },
                                horizontalPadding = horizontalPadding,
                                isCompact = isCompact
                            )
                        }
                        items(3, key = { "shimmer_recommended_$it" }) {
                            Box(modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp)) {
                                ShimmerPeerCard(isVertical = true, compact = isCompact)
                            }
                        }
                        item(key = "spacer_loading_recommended") { Spacer(Modifier.height(16.dp)) }
                    }

                    // Section 5: All Peers
                    item(key = "header_all") {
                        SectionHeader(
                            title = "All Peers 👥",
                            onSeeAllClick = { /* TODO */ },
                            horizontalPadding = horizontalPadding,
                            isCompact = isCompact
                        )
                    }

                    if (uiState is HomeUiState.Loading) {
                        items(5, key = { "shimmer_all_$it" }) {
                            Box(modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp)) {
                                ShimmerPeerCard(isVertical = true, compact = isCompact)
                            }
                        }
                    } else if (allPeers.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "Be the first peer! 🚀",
                                modifier = Modifier.padding(horizontal = horizontalPadding)
                            )
                        }
                    } else {
                        items(allPeers, key = { it.uid }) { peer ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by remember {
                                derivedStateOf { if (isPressed) 0.98f else 1f }
                            }
                            val animatedScale by animateFloatAsState(
                                targetValue = scale,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "card_click"
                            )

                            Box(modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp)) {
                                PeerCard(
                                    user = peer,
                                    onViewProfile = { onPeerClick(peer.uid) },
                                    onConnectClick = { onChatClick(peer.uid) },
                                    isOnline = true,
                                    currentUid = user.uid,
                                    viewModel = viewModel,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = animatedScale
                                            scaleY = animatedScale
                                        }
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
}

@Composable
fun HomeTopBar(
    unreadCount: Int,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    horizontalPadding: Dp = 24.dp,
    isCompact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .offset(y = (-26).dp)
            .padding(start = horizontalPadding, end = horizontalPadding, top = 0.dp, bottom = if (isCompact) 4.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PeerLearn",
            fontSize = if (isCompact) 22.sp else 26.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
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
fun GreetingCard(
    user: UserProfile,
    onProfileClick: () -> Unit,
    horizontalPadding: Dp = 24.dp,
    isCompact: Boolean = false
) {
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
            .padding(horizontal = horizontalPadding, vertical = if (isCompact) 12.dp else 16.dp),
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
                .padding(if (isCompact) 16.dp else 24.dp)
        ) {
            if (isCompact) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GreetingTextBlock(greeting = greeting, name = user.name, emoji = emoji, isCompact = true)
                    GreetingAvatar(user = user, onProfileClick = onProfileClick, isCompact = true)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GreetingTextBlock(greeting = greeting, name = user.name, emoji = emoji, isCompact = false, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    GreetingAvatar(user = user, onProfileClick = onProfileClick, isCompact = false)
                }
            }
        }
    }
}

@Composable
private fun GreetingTextBlock(
    greeting: String,
    name: String,
    emoji: String,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "$greeting $name! $emoji",
            fontSize = if (isCompact) 18.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = if (isCompact) 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Find your perfect learning peer",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GreetingAvatar(user: UserProfile, onProfileClick: () -> Unit, isCompact: Boolean) {
    Column(horizontalAlignment = if (isCompact) Alignment.Start else Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 52.dp else 56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (user.profileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text = user.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompact) 20.sp else 22.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "🔥 7 day streak",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HomeSearchBar(
    onClick: () -> Unit,
    horizontalPadding: Dp = 24.dp,
    isCompact: Boolean = false,
    skillSearchQuery: String,
    onSkillSearchQueryChange: (String) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(if (isCompact) 44.dp else 48.dp),
            shape = RoundedCornerShape(24.dp),
            color = SpaceSurface,
            border = BorderStroke(1.dp, Color(0xFF2A2A3D))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isCompact) 16.dp else 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = skillSearchQuery,
                    onValueChange = onSkillSearchQueryChange,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (skillSearchQuery.isEmpty()) {
                                Text(
                                    text = "Search by skill...",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        IconButton(
            onClick = { Toast.makeText(context, "Filters coming soon!", Toast.LENGTH_SHORT).show() },
            modifier = Modifier
                .size(if (isCompact) 44.dp else 48.dp)
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
fun SectionHeader(
    title: String,
    onSeeAllClick: () -> Unit,
    horizontalPadding: Dp = 24.dp,
    isCompact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = if (isCompact) 12.dp else 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = if (isCompact) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(12.dp))
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
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ShimmerPeerCard(isVertical: Boolean, compact: Boolean = false) {
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
            .then(if (isVertical) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().widthIn(max = if (compact) 320.dp else 360.dp)),
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
