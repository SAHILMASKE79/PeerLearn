package com.example.peerlearn


import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

// ─────────────────────────────────────────────
//  Dummy peer data
// ─────────────────────────────────────────────
private data class Peer(
    val name: String,
    val college: String,
    val teaches: List<String>,
    val learns: List<String>,
    val avatarColor: Color
)

private val dummyPeers = listOf(
    Peer("Rohit Sharma",  "VNIT Nagpur",      listOf("DSA","Java"),        listOf("ML/AI"),    Color(0xFF4F8EF7)),
    Peer("Priya Verma",   "RCOEM Nagpur",     listOf("UI/UX","Figma"),     listOf("Android"),  Color(0xFF9B59FF)),
    Peer("Aman Gupta",    "GCOE Amravati",    listOf("Python","ML/AI"),    listOf("DSA"),      Color(0xFF00D4FF)),
    Peer("Sneha Patil",   "Symbiosis Pune",   listOf("React","Node.js"),   listOf("Kotlin"),   Color(0xFFFF6B6B)),
    Peer("Vikram Das",    "IIT Bombay",       listOf("DevOps","Git"),      listOf("UI/UX"),    Color(0xFF2ECC71)),
)

// ─────────────────────────────────────────────
//  HomeScreen
// ─────────────────────────────────────────────
@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val infiniteAnim = rememberInfiniteTransition(label = "blob")
    val blob1X by infiniteAnim.animateFloat(
        initialValue = 0f, targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(8000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "b1x"
    )

    Box(Modifier.fillMaxSize().background(BgDeep)) {

        // Ambient blobs
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AccentPurple.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.8f + blob1X, size.height * 0.1f),
                    radius = size.width * 0.5f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AccentBlue.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.6f),
                    radius = size.width * 0.45f
                )
            )
        }

        Column(Modifier.fillMaxSize()) {

            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PEERLEARN", fontSize = 11.sp, fontWeight = FontWeight.W700,
                        color = AccentCyan, letterSpacing = 3.sp)
                    Text("Find your study partner 🎯", fontSize = 13.sp, color = TextSecondary)
                }
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            // ── Search Bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
                    .background(BgCard.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Text("Search peers, skills...", fontSize = 14.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Tab Row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Discover", "Matches", "Sessions").forEachIndexed { index, label ->
                    val isActive = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (isActive) Brush.linearGradient(listOf(AccentBlue, AccentPurple))
                                else Brush.linearGradient(listOf(BgCard, BgCard))
                            )
                            .border(
                                1.dp,
                                if (isActive) Color.Transparent else GlassStroke,
                                RoundedCornerShape(50.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.W600,
                            color = if (isActive) TextPrimary else TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Peer Cards ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                dummyPeers.forEach { peer ->
                    PeerCard(peer = peer)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Peer Card
// ─────────────────────────────────────────────
@Composable
private fun PeerCard(peer: Peer) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassStroke, RoundedCornerShape(20.dp))
            .background(BgCard.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(peer.avatarColor.copy(alpha = 0.25f))
                        .border(2.dp, peer.avatarColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        peer.name.first().toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = peer.avatarColor
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(peer.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.School, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Text(peer.college, fontSize = 12.sp, color = TextSecondary)
                    }
                }

                // Connect Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple)))
                        .clickable { /* send connect request */ }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }

            Divider(color = GlassStroke)

            // Skills row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SkillSection(
                    icon = Icons.Rounded.School,
                    label = "Teaches",
                    skills = peer.teaches,
                    color = AccentCyan,
                    modifier = Modifier.weight(1f)
                )
                SkillSection(
                    icon = Icons.Rounded.AutoAwesome,
                    label = "Learning",
                    skills = peer.learns,
                    color = AccentPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SkillSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    skills: List<String>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.W600)
        }
        skills.forEach { skill ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.12f))
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(skill, fontSize = 11.sp, color = color)
            }
        }
    }
}