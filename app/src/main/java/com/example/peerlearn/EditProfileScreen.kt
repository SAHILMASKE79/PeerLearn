package com.sahil.peerlearn

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    uid: String,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    // BUG 1 FIX: Firebase Auth se uid lo and check for null
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    
    val profile by viewModel.userProfile.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // State for all fields
    var nameState by remember { mutableStateOf("") }
    var bioState by remember { mutableStateOf("") }
    var collegeState by remember { mutableStateOf("") }
    var yearState by remember { mutableStateOf("") }
    var githubState by remember { mutableStateOf("") }
    var linkedinState by remember { mutableStateOf("") }
    val teachSkillsList = remember { mutableStateListOf<String>() }
    val learnSkillsList = remember { mutableStateListOf<String>() }

    var isDataLoaded by remember { mutableStateOf(false) }

    // PRE-FILL EXISTING DATA
    LaunchedEffect(uid) {
        viewModel.fetchProfile(uid)
    }

    LaunchedEffect(profile) {
        // BUG 1 FIX: UserProfile data load hone se pehle fields access na ho
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
            delay(1000)
            onBack()
        } else if (uiState is ProfileUiState.Error) {
            snackbarHostState.showSnackbar((uiState as ProfileUiState.Error).message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        // BUG 1 FIX: Show loading state jab tak data load nahi hota
        if (!isDataLoaded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF7C4DFF))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 40.dp)
            ) {
                // 1. Top Section (Avatar with Camera Overlay)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.clickable { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C4DFF).copy(alpha = 0.2f))
                                .border(2.dp, Color(0xFF7C4DFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = nameState.firstOrNull()?.toString()?.uppercase() ?: "?",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C4DFF)
                            )
                        }
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = Color(0xFF7C4DFF),
                            border = BorderStroke(2.dp, Color(0xFF121212))
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

                // 2. WhatsApp Style Underline Fields
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
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
                HorizontalDivider(color = Color(0xFF2A2A3D), thickness = 0.5.dp)
                Spacer(Modifier.height(24.dp))

                // 3. Skills Section
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    SkillEditSection("Skills I Know", teachSkillsList, Color(0xFF7C4DFF), enabled = true)
                    Spacer(Modifier.height(32.dp))
                    SkillEditSection("Want to Learn", learnSkillsList, Color(0xFF2196F3), enabled = true)
                }

                Spacer(Modifier.height(48.dp))

                // 4. Save Button
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
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
            Icon(icon, null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 12.sp, color = Color(0xFF9E9E9E))
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(hint, color = Color(0xFF9E9E9E).copy(0.5f)) },
            enabled = enabled,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color(0xFF7C4DFF),
                unfocusedIndicatorColor = Color(0xFF2A2A3D),
                disabledIndicatorColor = Color(0xFF2A2A3D).copy(0.5f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White.copy(0.5f),
                cursorColor = Color(0xFF7C4DFF)
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
                placeholder = { Text("Add skill...", fontSize = 14.sp, color = Color(0xFF9E9E9E)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    unfocusedBorderColor = Color(0xFF2A2A3D),
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
