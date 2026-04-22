package com.sahil.peerlearn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.Flow

interface ConnectionViewModel {
    fun sendConnectionRequest(currentUid: String, peerUid: String)
    fun getConnectionStatus(currentUid: String, peerUid: String): Flow<String>
}

// Updated Theme Colors based on requirements
val PremiumDark = Color(0xFF0D0D1A)
val PurpleAccent = Color(0xFF7C4DFF)
val BlueAccent = Color(0xFF5B9BD5)
val OnlineGreen = Color(0xFF4ADE80)
val OfflineGray = Color(0xFF555555)

@Composable
fun PeerCard(
    user: UserProfile,
    onViewProfile: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier,
    matchPercentage: Int? = null,
    isOnline: Boolean = false,
    currentUid: String,
    viewModel: ConnectionViewModel
) {
    val connectionStatus by viewModel
        .getConnectionStatus(currentUid, user.uid)
        .collectAsState(initial = "none")

    val initials = user.name.split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = PremiumDark),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, PurpleAccent.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar (Left)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF2D1F5E), Color(0xFF1A1A2E))
                            )
                        )
                        .border(1.5.dp, PurpleAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.profileImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initials,
                            color = Color(0xFFA78BFA),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            color = Color(0xFFF0EEFF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) OnlineGreen else OfflineGray)
                        )
                    }

                    Text(
                        text = user.college.ifEmpty { "Learning Peer" },
                        color = Color(0xFF888AAA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Knows Skills
                    SkillRowPremium(
                        label = "knows",
                        skills = user.teachSkills,
                        chipBg = PurpleAccent.copy(alpha = 0.15f),
                        chipText = Color(0xFFA78BFA),
                        chipBorder = PurpleAccent.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Wants Skills
                    SkillRowPremium(
                        label = "wants",
                        skills = user.learnSkills,
                        chipBg = BlueAccent.copy(alpha = 0.12f),
                        chipText = Color(0xFF7BB8E8),
                        chipBorder = BlueAccent.copy(alpha = 0.25f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Actions
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(90.dp)
                ) {
                    // Connection Logic
                    when (connectionStatus) {
                        "none" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(PurpleAccent, Color(0xFF5B2FD4))
                                        )
                                    )
                                    .clickable {
                                        viewModel.sendConnectionRequest(currentUid, user.uid)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Connect",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                        "pending" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF444444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Pending...",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                        "connected" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1a5c38))
                                    .clickable { onConnectClick() }, // onConnectClick should navigate to chat
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Message",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Profile Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x337C4DFF),
                                        Color(0x335B9BD5)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0x597C4DFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onViewProfile() }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Profile",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFC4A8FF)
                        )
                    }
                }
            }
        }

        // Match Badge (Top Right)
        if (matchPercentage != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 0.dp, end = 12.dp),
                color = PurpleAccent,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "$matchPercentage% match",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SkillRowPremium(
    label: String,
    skills: List<String>,
    chipBg: Color,
    chipText: Color,
    chipBorder: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 9.sp,
            modifier = Modifier.width(36.dp)
        )
        
        if (skills.isEmpty()) {
            PremiumSkillChip(
                text = "Not specified",
                bg = Color(0xFF444466).copy(alpha = 0.2f),
                textColor = Color(0xFF888AAA),
                border = Color(0xFF444466).copy(alpha = 0.4f)
            )
        } else {
            val displaySkills = skills.take(3)
            val extraCount = if (skills.size > 3) skills.size - 3 else 0

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                displaySkills.forEach { skill ->
                    PremiumSkillChip(text = skill, bg = chipBg, textColor = chipText, border = chipBorder)
                }
                if (extraCount > 0) {
                    PremiumSkillChip(text = "+$extraCount", bg = chipBg, textColor = chipText, border = chipBorder)
                }
            }
        }
    }
}

@Composable
fun PremiumSkillChip(
    text: String,
    bg: Color,
    textColor: Color,
    border: Color
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
        )
    }
}
