package com.sahil.peerlearn

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.sahil.peerlearn.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    uid: String,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    
    val profile by viewModel.userProfile.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var nameState by remember { mutableStateOf("") }
    var bioState by remember { mutableStateOf("") }
    var collegeState by remember { mutableStateOf("") }
    var yearState by remember { mutableStateOf("") }
    var githubState by remember { mutableStateOf("") }
    var linkedinState by remember { mutableStateOf("") }
    val teachSkillsList = remember { mutableStateListOf<String>() }
    val learnSkillsList = remember { mutableStateListOf<String>() }

    var isDataLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        viewModel.fetchProfile(uid)
    }

    LaunchedEffect(profile) {
        profile?.let {
            nameState = it.name
            bioState = it.bio
            collegeState = it.college
            yearState = it.year
            githubState = it.githubLink
            linkedinState = it.linkedinLink
            teachSkillsList.clear()
            teachSkillsList.addAll(it.teachSkills)
            learnSkillsList.clear()
            learnSkillsList.addAll(it.learnSkills)
            isDataLoaded = true
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success) {
            snackbarHostState.showSnackbar((uiState as ProfileUiState.Success).message)
            if ((uiState as ProfileUiState.Success).message.contains("Profile updated")) {
                delay(1000)
                onBack()
            }
        } else if (uiState is ProfileUiState.Error) {
            snackbarHostState.showSnackbar((uiState as ProfileUiState.Error).message)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 360.dp
        val horizontalPadding = if (isCompact) 16.dp else 24.dp
        val glowWidth = maxWidth * 1.15f

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceBlack)
                )
            },
            containerColor = SpaceBlack
        ) { padding ->
        if (!isDataLoaded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PurpleAccent)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Standard Space Theme Glow
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(glowWidth)
                        .height(if (isCompact) 220.dp else 300.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PurpleGlow.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isCompact) 24.dp else 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri ->
                            uri?.let { viewModel.uploadProfileImage(currentUid, it) }
                        }

                        Box(
                            modifier = Modifier.clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCompact) 88.dp else 100.dp)
                                    .clip(CircleShape)
                                    .background(SpaceSurface)
                                    .border(2.dp, PurpleAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (profile?.profileImageUrl.isNullOrEmpty()) {
                                    Text(
                                        text = nameState.firstOrNull()?.toString()?.uppercase() ?: "?",
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurpleAccent
                                    )
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(profile?.profileImageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.size(if (isCompact) 28.dp else 32.dp),
                                shape = CircleShape,
                                color = PurpleAccent,
                                border = BorderStroke(2.dp, SpaceBlack)
                            ) {
                                Icon(
                                    Icons.Rounded.CameraAlt,
                                    null,
                                    modifier = Modifier.padding(6.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UnderlineField("Full Name", nameState, Icons.Rounded.Person) { nameState = it }
                        UnderlineField("Bio", bioState, Icons.Rounded.Info, hint = "Tell about yourself...") { bioState = it }
                        UnderlineField("College", collegeState, Icons.Rounded.School) { collegeState = it }
                        UnderlineField("Year of Birth", yearState, Icons.Rounded.CalendarMonth) { yearState = it }
                        UnderlineField("GitHub Link", githubState, Icons.Rounded.Link, hint = "github.com/username") { githubState = it }
                        UnderlineField("LinkedIn Link", linkedinState, Icons.Rounded.Link, hint = "linkedin.com/in/username") { linkedinState = it }
                    }

                    Spacer(Modifier.height(32.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                    Spacer(Modifier.height(24.dp))

                    Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                        SkillEditSection("Skills I Know", teachSkillsList, PurpleAccent, enabled = true)
                        Spacer(Modifier.height(32.dp))
                        SkillEditSection("Want to Learn", learnSkillsList, PurpleGlow, enabled = true)
                    }

                    Spacer(Modifier.height(48.dp))

                    Box(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                        Button(
                            onClick = {
                                viewModel.updateProfile(
                                    uid = currentUid,
                                    name = nameState,
                                    bio = bioState,
                                    college = collegeState,
                                    year = yearState,
                                    githubLink = githubState,
                                    linkedinLink = linkedinState,
                                    teachSkills = teachSkillsList.toList(),
                                    learnSkills = learnSkillsList.toList()
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            enabled = uiState !is ProfileUiState.Loading
                        ) {
                            if (uiState is ProfileUiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Save Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
        }
    }
}

@Composable
fun UnderlineField(
    label: String,
    value: String,
    icon: ImageVector,
    hint: String = "",
    enabled: Boolean = true,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(hint, color = Color.White.copy(alpha = 0.3f)) },
            enabled = enabled,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = PurpleAccent,
                unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f),
                disabledIndicatorColor = Color.White.copy(alpha = 0.05f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White.copy(0.5f),
                cursorColor = PurpleAccent
            )
        )
    }
}

@Composable
fun SkillEditSection(title: String, skills: MutableList<String>, color: Color, enabled: Boolean) {
    var textValue by remember { mutableStateOf("") }
    
    Column {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                placeholder = { Text("Add skill...", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.White.copy(0.5f)
                )
            )
            IconButton(
                onClick = {
                    if (textValue.isNotBlank() && !skills.contains(textValue.trim())) {
                        skills.add(textValue.trim())
                        textValue = ""
                    }
                },
                enabled = enabled,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(if(enabled) color else color.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Rounded.Add, null, tint = Color.White)
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
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
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(skill, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.Close,
                            null,
                            modifier = Modifier.size(14.dp).clickable(enabled = enabled) { skills.remove(skill) },
                            tint = color
                        )
                    }
                }
            }
        }
    }
}
