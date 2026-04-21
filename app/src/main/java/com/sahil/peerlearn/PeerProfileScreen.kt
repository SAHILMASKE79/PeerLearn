package com.sahil.peerlearn

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.sahil.peerlearn.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerProfileScreen(
    peerUid: String,
    navController: NavController,
    viewModel: PeerProfileViewModel = viewModel()
) {
    if (peerUid.isEmpty()) {
        navController.popBackStack()
        return
    }

    val peerProfile by viewModel.peerProfile.collectAsState()
    val connectionDetails by viewModel.connectionDetails.collectAsState()
    val context = LocalContext.current
    val currentUid = Firebase.auth.currentUser?.uid ?: ""
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    var showDisconnectSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is PeerProfileUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetUiState()
            }
            is PeerProfileUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetUiState()
            }
            else -> {}
        }
    }

    LaunchedEffect(peerUid) {
        viewModel.fetchPeerProfile(peerUid)
        viewModel.checkConnectionStatus(currentUid, peerUid)
    }

    val currentUserName = Firebase.auth.currentUser?.displayName ?: "Peer"
    val isLoaded = peerProfile != null

    if (showDisconnectSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDisconnectSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1535),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            DisconnectSheetContent(
                peerName = peerProfile?.name ?: "Peer",
                onCancel = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        showDisconnectSheet = false
                    }
                },
                onConfirm = {
                    viewModel.disconnectPeer(currentUid, peerUid) {
                        scope.launch {
                            sheetState.hide()
                            snackbarHostState.showSnackbar("Disconnected. Chat history is safe.")
                        }.invokeOnCompletion {
                            showDisconnectSheet = false
                        }
                    }
                }
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 360.dp
        val horizontalPadding = if (isCompact) 16.dp else 20.dp
        val glowWidth = maxWidth * 1.15f

        Scaffold(
            containerColor = SpaceBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            // Radial Glow Effect
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(glowWidth)
                    .height(if (isCompact) 220.dp else 300.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PurpleGlow.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                peerProfile?.let { peer: UserProfile ->
                    PeerProfileHeader(
                        peer = peer,
                        connectionDetails = connectionDetails,
                        currentUid = currentUid,
                        isCompact = isCompact,
                        onBackClick = { navController.popBackStack() },
                        onConnectClick = {
                            viewModel.sendConnectionRequest(currentUid, currentUserName, peerUid)
                        },
                        onMessageClick = {
                            navController.navigate("chat/$peerUid")
                        },
                        onAcceptClick = {
                            viewModel.acceptConnection(currentUid, currentUserName, peerUid)
                        },
                        onDeclineClick = {
                            viewModel.declineConnection(currentUid, peerUid)
                        },
                        onDisconnectClick = {
                            showDisconnectSheet = true
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Skills Card
                    AnimatedVisibility(
                        visible = isLoaded,
                        enter = fadeIn(tween(400)) + slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(400, delayMillis = 100)
                        )
                    ) {
                        PeerInfoCard(title = "Skills") {
                            SkillSection(
                                title = "Skills I Know",
                                skills = peerProfile?.teachSkills ?: emptyList(),
                                chipColor = PurpleAccent
                            )
                            Spacer(Modifier.height(16.dp))
                            SkillSection(
                                title = "Wants to Learn",
                                skills = peerProfile?.learnSkills ?: emptyList(),
                                chipColor = PurpleGlow
                            )
                        }
                    }

                    // About Card
                    AnimatedVisibility(
                        visible = isLoaded,
                        enter = fadeIn(tween(400)) + slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(400, delayMillis = 200)
                        )
                    ) {
                        PeerInfoCard(title = "About") {
                            Text(
                                peerProfile?.bio ?: "Not added yet",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(Modifier.height(20.dp))
                            IconInfoRow(
                                Icons.Rounded.School,
                                "College",
                                peerProfile?.college ?: "Not added"
                            )
                            Spacer(Modifier.height(12.dp))
                            IconInfoRow(
                                Icons.Rounded.CalendarToday,
                                "Year",
                                peerProfile?.year ?: "Not added"
                            )
                        }
                    }

                    // Projects Card
                    AnimatedVisibility(
                        visible = isLoaded,
                        enter = fadeIn(tween(400)) + slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(400, delayMillis = 300)
                        )
                    ) {
                        PeerInfoCard(title = "Projects") {
                            Text("No projects yet", color = TextSecondary, fontSize = 14.sp)
                        }
                    }

                    // Social Card
                    AnimatedVisibility(
                        visible = isLoaded,
                        enter = fadeIn(tween(400)) + slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(400, delayMillis = 400)
                        )
                    ) {
                        PeerInfoCard(title = "Social Profiles") {
                            SocialRow(
                                icon = Icons.Rounded.Link,
                                label = "GitHub",
                                link = peerProfile?.githubLink ?: "",
                                context = context
                            )
                            Spacer(Modifier.height(12.dp))
                            SocialRow(
                                icon = Icons.Rounded.Link,
                                label = "LinkedIn",
                                link = peerProfile?.linkedinLink ?: "",
                                context = context
                            )
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PeerProfileHeader(
    peer: UserProfile,
    connectionDetails: ConnectionDetails,
    currentUid: String,
    isCompact: Boolean,
    onBackClick: () -> Unit,
    onConnectClick: () -> Unit,
    onMessageClick: () -> Unit,
    onAcceptClick: () -> Unit,
    onDeclineClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    val avatarScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PurpleGlow.copy(alpha = 0.7f),
                        SpaceBlack.copy(alpha = 0f)
                    )
                )
            )
            .padding(bottom = 24.dp)
    ) {
        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding() + 8.dp,
                    start = 8.dp
                )
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding() + if (isCompact) 28.dp else 40.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(if (isCompact) 78.dp else 90.dp)
                    .scale(avatarScale)
                    .border(2.dp, PurpleAccent.copy(alpha = 0.5f), CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = peer.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = if (isCompact) 30.sp else 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Name
            Text(
                text = peer.name,
                color = Color.White,
                fontSize = if (isCompact) 20.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // College • Year
            Text(
                text = "${peer.college} • ${peer.year}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompact) 20.dp else 40.dp)
            ) {
                StatItem("Know", peer.teachSkills.size)
                StatItem("Learn", peer.learnSkills.size)
                StatItem("Peers", 0)
            }

            Spacer(Modifier.height(24.dp))

            // Connection button
            AnimatedContent(
                targetState = connectionDetails.status,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "button_transition"
            ) { status ->
                when (status) {
                    ConnectionStatus.NOT_CONNECTED, ConnectionStatus.DISCONNECTED -> {
                        Button(
                            onClick = onConnectClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = PurpleAccent
                            ),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, PurpleAccent),
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .fillMaxWidth()
                        ) {
                            Text("Connect", fontWeight = FontWeight.Bold)
                        }
                    }

                    ConnectionStatus.PENDING -> {
                        if (connectionDetails.requestedBy == currentUid) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .padding(horizontal = 32.dp)
                                    .fillMaxWidth()
                            ) {
                                Text("Request Sent ⏳", color = Color.White.copy(alpha = 0.7f))
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 32.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onAcceptClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onDeclineClick,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Decline", color = Color.White)
                                }
                            }
                        }
                    }

                    ConnectionStatus.CONNECTED -> {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDisconnectClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF4CAF50)
                                ),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Connected ✓", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onMessageClick,
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                shape = CircleShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Message 💬", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisconnectSheetContent(
    peerName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFE53935).copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.PersonRemove,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Disconnect from $peerName?",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Aap $peerName se disconnect ho jaoge.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Surface(
            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f))
        ) {
            Text(
                "Chat history will be safe",
                color = Color(0xFF4CAF50),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Text("Cancel", color = Color.White)
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("Disconnect", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun StatItem(label: String, count: Int) {
    var targetCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(count) { targetCount = count }
    val animatedCount by animateIntAsState(
        targetValue = targetCount,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "count"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            animatedCount.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun PeerInfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpaceSurface.copy(alpha = 0.7f))
            .border(1.dp, PurpleAccent.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        content()
    }
}

@Composable
fun SkillSection(title: String, skills: List<String>, chipColor: Color) {
    Column {
        Text(
            title,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        if (skills.isEmpty()) {
            Text("No skills added", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f))
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                skills.forEach { skill ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(chipColor.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                chipColor.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = skill,
                            fontSize = 12.sp,
                            color = chipColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconInfoRow(icon: ImageVector, label: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PurpleAccent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
            Text(text, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SocialRow(
    icon: ImageVector,
    label: String,
    link: String,
    context: android.content.Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = link.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    context.startActivity(intent)
                } catch (e: Exception) {
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            if (link.isEmpty()) "$label: Not added" else label,
            fontSize = 14.sp,
            color = if (link.isEmpty()) Color.White.copy(alpha = 0.3f) else Color.White
        )
        if (link.isNotEmpty()) {
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.OpenInNew, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}
