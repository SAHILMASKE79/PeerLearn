package com.sahil.peerlearn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahil.peerlearn.ui.theme.*

@Composable
fun PeerCard(
    user: UserProfile,
    onClick: () -> Unit,
    onActionClick: () -> Unit,
    actionText: String = "Connect",
    isVertical: Boolean = true,
    matchedSkill: String? = null,
    onViewProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isAnonymous = user.name.isBlank() || user.name == "Unknown"
    val displayName = if (isAnonymous) "Anonymous User" else user.name
    val displayInitials = if (isAnonymous) "?" else user.name.firstOrNull()?.toString()?.uppercase() ?: "?"

    Card(
        modifier = modifier
            .then(if (isVertical) Modifier.fillMaxWidth() else Modifier.width(200.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SpaceSurface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PurpleGlow.copy(alpha = 0.2f))
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
                        .size(if (isVertical) 48.dp else 40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(if (isAnonymous) Color.Gray else PurpleAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayInitials,
                            color = if (isAnonymous) Color.White else PurpleAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isVertical) 18.sp else 16.sp
                        )
                    }
                    // Online dot (Hardcoded true)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .border(2.dp, SpaceSurface, CircleShape)
                            .align(Alignment.TopStart)
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isVertical) 15.sp else 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${user.college} ${if(user.year.isNotEmpty()) "• ${user.year}" else ""}",
                        color = Color(0xFF9E9E9E),
                        fontSize = if (isVertical) 12.sp else 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (isVertical) {
                Spacer(Modifier.height(12.dp))
                // Skills Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val maxSkills = 2
                    user.teachSkills.take(maxSkills).forEach { skill ->
                        Surface(
                            color = SpaceBlack.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, PurpleGlow.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "[$skill]",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (user.teachSkills.size > maxSkills) {
                        Text(
                            text = "+${user.teachSkills.size - maxSkills} more",
                            color = Color(0xFF9E9E9E),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewProfile ?: onClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PurpleAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent)
                    ) {
                        Text("View Profile", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onActionClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        Text("Chat →", fontSize = 12.sp, color = Color.White)
                    }
                }
            } else {
                // Horizontal (Recommended)
                if (matchedSkill != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Knows $matchedSkill 🎯",
                        color = PurpleAccent,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.height(32.dp).align(Alignment.End),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Connect", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}
