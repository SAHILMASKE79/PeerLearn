package com.sahil.peerlearn

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sahil.peerlearn.ui.theme.PurpleGlow

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
    val isLoading = uiState is ProfileUiState.Loading || (profile == null && uiState !is ProfileUiState.Error)

    LaunchedEffect(uid) {
        viewModel.fetchProfile(uid)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 360.dp
        val horizontalPadding = if (isCompact) 12.dp else 16.dp
        val glowWidth = maxWidth * 1.15f

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(glowWidth)
                        .height(if (isCompact) 220.dp else 300.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(PurpleGlow.copy(alpha = 0.18f), Color.Transparent)
                            )
                        )
                )

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                uiState is ProfileUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (uiState as ProfileUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.fetchProfile(uid) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry")
                        }
                    }
                }

                else -> {
                    profile?.let { user ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 0.dp, bottom = if (isCompact) 20.dp else 32.dp),
                            verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
                        ) {
                            item {
                                ProfileHeader(
                                    user = user,
                                    peersCount = viewModel.peersCount,
                                    showBackArrow = showBackArrow,
                                    onBackClick = onBackClick,
                                    onImageSelected = { uid, uri -> viewModel.uploadProfileImage(uid, uri) },
                                    isCompact = isCompact,
                                    horizontalPadding = horizontalPadding
                                )
                            }

                            item {
                                SkillsSection(user = user)
                            }

                            item {
                                ProfileCard(title = "Achievements", icon = Icons.Rounded.EmojiEvents) {
                                    Text("No achievements yet", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                    Text(
                                        "Complete tasks to earn badges!",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            item {
                                ProfileCard(title = "Projects", icon = Icons.Rounded.Folder) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "No projects added yet",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                            fontSize = 14.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        OutlinedButton(
                                            onClick = { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Add Project", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }

                            item {
                                ProfileCard(title = "Social Links", icon = Icons.Rounded.Link) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SocialRow("GitHub", user.githubLink) {
                                            if (user.githubLink.isNotEmpty()) {
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(if (user.githubLink.startsWith("http")) user.githubLink else "https://${user.githubLink}")
                                                )
                                                context.startActivity(intent)
                                            }
                                        }
                                        SocialRow("LinkedIn", user.linkedinLink) {
                                            if (user.linkedinLink.isNotEmpty()) {
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(if (user.linkedinLink.startsWith("http")) user.linkedinLink else "https://${user.linkedinLink}")
                                                )
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                ProfileCard(title = "About", icon = Icons.Rounded.Description) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        InfoRow(Icons.Rounded.School, "College", user.college.ifBlank { "Not added" })
                                        InfoRow(Icons.Rounded.CalendarMonth, "Year", user.year.ifBlank { "Not added" })
                                        InfoRow(Icons.Rounded.Email, "Email", user.email)
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Bio",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = user.bio.ifBlank { "Tell about yourself..." },
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }

                            item {
                                OutlinedButton(
                                    onClick = onEditClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Edit Profile", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            item {
                                OutlinedButton(
                                    onClick = onLogoutClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: UserProfile,
    peersCount: Int,
    showBackArrow: Boolean,
    onBackClick: () -> Unit,
    onImageSelected: (String, Uri) -> Unit,
    isCompact: Boolean,
    horizontalPadding: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
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
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + if (isCompact) 20.dp else 28.dp,
                    start = horizontalPadding,
                    end = horizontalPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileImage(
                user = user,
                isCompact = isCompact,
                onImageSelected = { uri -> onImageSelected(user.uid, uri) }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${user.college} • ${user.year}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Have", user.teachSkills.size.toString())
                StatItem("Want", user.learnSkills.size.toString())
                StatItem("Peers", peersCount.toString())
            }
        }
    }
}

@Composable
private fun ProfileImage(user: UserProfile, isCompact: Boolean, onImageSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { onImageSelected(it) } }
    )

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Color(0xFF2d1f5e))
            .border(2.dp, Color(0xFF7C4DFF), CircleShape)
            .clickable {
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        contentAlignment = Alignment.Center
    ) {
        if (user.profileImageUrl.isNotEmpty()) {
            AsyncImage(
                model = user.profileImageUrl,
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = user.name.take(2).uppercase(),
                color = Color(0xFFa78bfa),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatItem(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
    }
}

@Composable
private fun ProfileCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SkillsSection(user: UserProfile) {
    ProfileCard(title = "Skills", icon = Icons.Rounded.Work) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SkillChipGroup(
                title = "Have",
                skills = user.teachSkills,
                chipColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            SkillChipGroup(
                title = "Want",
                skills = user.learnSkills,
                chipColor = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }
    }
}

@Composable
private fun SkillChipGroup(
    title: String,
    skills: List<String>,
    chipColor: Color,
    containerColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (skills.isEmpty()) {
            Text(
                text = "Add your skills",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(skills) { skill ->
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text(skill, color = chipColor, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = containerColor.copy(alpha = 0.5f),
                            selectedLabelColor = chipColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = true,
                            borderColor = chipColor.copy(alpha = 0.45f),
                            selectedBorderColor = chipColor.copy(alpha = 0.45f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialRow(platform: String, link: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(platform, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        Text(
            text = link.ifBlank { "Not added" },
            color = if (link.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text("$label: ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}
