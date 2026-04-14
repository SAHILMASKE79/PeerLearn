package com.sahil.peerlearn

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.sahil.peerlearn.ui.theme.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uid: String,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    showBackArrow: Boolean = false,
    onBackClick: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val profile by viewModel.userProfile.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uid) {
        viewModel.fetchProfile(uid)
    }

    Scaffold(
        containerColor = SpaceBlack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Radial Glow Effect
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(450.dp, 300.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PurpleGlow.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
            )

            when (uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PurpleAccent)
                }
                is ProfileUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text((uiState as ProfileUiState.Error).message, color = Color.Red)
                        Button(onClick = { viewModel.fetchProfile(uid) }, colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)) { Text("Retry") }
                    }
                }
                else -> {
                    profile?.let { user ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // SECTION 1 — HEADER
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
                                if (showBackArrow) {
                                    IconButton(
                                        onClick = onBackClick,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(
                                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                                                start = 8.dp
                                            )
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White)
                                    }
                                }

                                IconButton(
                                    onClick = onEditClick,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(
                                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                                            end = 8.dp
                                        )
                                ) {
                                    Icon(Icons.Rounded.Edit, "Edit", tint = Color.White)
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .border(2.dp, PurpleAccent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(user.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${user.college} • ${user.year}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))

                                    Spacer(Modifier.height(20.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        StatItem("Know", user.teachSkills.size.toString())
                                        StatItem("Learn", user.learnSkills.size.toString())
                                        StatItem("Peers", viewModel.peersCount.toString())
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                
                                // SECTION 2 — SKILLS CARD
                                ProfileCard(title = "Skills", icon = Icons.Rounded.Work) {
                                    Column {
                                        Text("I Can Teach:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(Modifier.height(8.dp))
                                        if (user.teachSkills.isEmpty()) {
                                            Text("Add your skills", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                        } else {
                                            SkillFlowRow(user.teachSkills, PurpleAccent)
                                        }
                                        
                                        Spacer(Modifier.height(16.dp))
                                        
                                        Text("Want to Learn:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(Modifier.height(8.dp))
                                        if (user.learnSkills.isEmpty()) {
                                            Text("Add your skills", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                        } else {
                                            SkillFlowRow(user.learnSkills, PurpleGlow)
                                        }
                                    }
                                }

                                // SECTION 3 — ACHIEVEMENTS CARD
                                ProfileCard(title = "Achievements", icon = Icons.Rounded.EmojiEvents) {
                                    Column {
                                        Text("No achievements yet", color = Color.White, fontSize = 14.sp)
                                        Text("Complete tasks to earn badges!", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                    }
                                }

                                // SECTION 4 — PROJECTS CARD
                                ProfileCard(title = "Projects", icon = Icons.Rounded.Folder) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("No projects added yet", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
                                        Spacer(Modifier.height(12.dp))
                                        OutlinedButton(
                                            onClick = { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, PurpleAccent)
                                        ) {
                                            Icon(Icons.Rounded.Add, null, tint = PurpleAccent)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Add Project", color = PurpleAccent)
                                        }
                                    }
                                }

                                // SECTION 5 — SOCIAL CARD
                                ProfileCard(title = "Social Links", icon = Icons.Rounded.Link) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SocialRow("GitHub", user.githubLink) {
                                            if (user.githubLink.isNotEmpty()) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if (user.githubLink.startsWith("http")) user.githubLink else "https://${user.githubLink}"))
                                                context.startActivity(intent)
                                            }
                                        }
                                        SocialRow("LinkedIn", user.linkedinLink) {
                                            if (user.linkedinLink.isNotEmpty()) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if (user.linkedinLink.startsWith("http")) user.linkedinLink else "https://${user.linkedinLink}"))
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                }

                                // SECTION 6 — ABOUT CARD
                                ProfileCard(title = "About", icon = Icons.Rounded.Description) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        InfoRow(Icons.Rounded.School, "College", user.college.ifBlank { "Not added" })
                                        InfoRow(Icons.Rounded.CalendarMonth, "Year", user.year.ifBlank { "Not added" })
                                        InfoRow(Icons.Rounded.Email, "Email", user.email)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Bio:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(user.bio.ifBlank { "Tell about yourself..." }, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, lineHeight = 20.sp)
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                                
                                TextButton(
                                    onClick = onLogoutClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Logout", fontWeight = FontWeight.Bold)
                                }
                                
                                Spacer(Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun ProfileCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SkillFlowRow(skills: List<String>, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        skills.forEach { skill ->
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
            ) {
                Text(
                    text = skill,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SocialRow(platform: String, link: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(platform, color = Color.White, fontSize = 14.sp)
        Text(
            text = link.ifBlank { "Not added" },
            color = if (link.isBlank()) Color.White.copy(alpha = 0.5f) else PurpleAccent,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text("$label: ", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp)
    }
}
