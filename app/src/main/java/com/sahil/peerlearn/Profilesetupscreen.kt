package com.sahil.peerlearn

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.*
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.launch
import com.sahil.peerlearn.ui.theme.*

// ─────────────────────────────────────────────
//  Data  (Skills & Year options)
// ─────────────────────────────────────────────
private val allSkills = listOf(
    "Kotlin", "Java", "Python", "C/C++", "DSA",
    "Web Dev", "UI/UX", "ML/AI", "Databases",
    "Git", "Android", "React", "Node.js", "DevOps"
)

private val yearOptions = listOf(
    "1st Year", "2nd Year", "3rd Year", "4th Year", "Alumni"
)

// ─────────────────────────────────────────────
//  ProfileSetupScreen
// ─────────────────────────────────────────────
@Composable
fun ProfileSetupScreen(
    authManager: AuthManager,
    onProfileComplete: () -> Unit = {}
) {
    val userRepository = remember { UserRepository() }
    val viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authManager, userRepository) as T
        }
    })
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var name          by remember { mutableStateOf("") }
    var collegeName   by remember { mutableStateOf("") }
    var selectedYear  by remember { mutableStateOf("") }
    var bio           by remember { mutableStateOf("") }
    val teachSkills   = remember { mutableStateListOf<String>() }
    val learnSkills   = remember { mutableStateListOf<String>() }
    var currentStep   by remember { mutableIntStateOf(0) }

    // Observe error state
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            android.widget.Toast.makeText(context, (uiState as AuthUiState.Error).message, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // PERFORMANCE OPTIMIZATION: Disabled animations for emulator stability
    /*
    val infiniteAnim = rememberInfiniteTransition(label = "blob")
    val blob1X by infiniteAnim.animateFloat(
        initialValue = 0f, targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(7000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "b1x"
    )
    val blob2Y by infiniteAnim.animateFloat(
        initialValue = 0f, targetValue = -50f,
        animationSpec = infiniteRepeatable(tween(9000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "b2y"
    )
    */

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        // Radial Glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(450.dp, 300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PurpleGlow.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "PEERLEARN",
                fontSize = 12.sp,
                fontWeight = FontWeight.W700,
                color = PurpleAccent,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Set Up Your Profile",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "Help peers find and connect with you",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(28.dp))

            StepIndicator(currentStep = currentStep, totalSteps = 3)

            Spacer(Modifier.height(8.dp))

            Text(
                text = when (currentStep) {
                    0    -> "Step 1 of 3 — Basic Info"
                    1    -> "Step 2 of 3 — What you can teach"
                    else -> "Step 3 of 3 — What you want to learn"
                },
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    else
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                },
                label = "stepContent"
            ) { step ->
                when (step) {
                    0 -> StepBasicInfo(
                        name = name, onNameChange = { name = it },
                        college = collegeName, onCollegeChange = { collegeName = it },
                        selectedYear = selectedYear, onYearSelect = { selectedYear = it },
                        bio = bio, onBioChange = { bio = it }
                    )
                    1 -> StepSkillPicker(
                        title = "🎓  Skills You Can Teach",
                        subtitle = "Topics you're confident explaining to others",
                        selected = teachSkills,
                        accentColor = PurpleAccent
                    )
                    2 -> StepSkillPicker(
                        title = "📚  Skills You Want to Learn",
                        subtitle = "What are you looking for help with?",
                        selected = learnSkills,
                        accentColor = PurpleGlow
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f).height(54.dp),
                        border = BorderStroke(1.dp, PurpleGlow.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back", fontSize = 15.sp, fontWeight = FontWeight.W500)
                    }
                }

                val scope = rememberCoroutineScope()
                Button(
                    onClick = { 
                        if (currentStep < 2) {
                            currentStep++ 
                        } else {
                            if (name.isBlank() || collegeName.isBlank() || selectedYear.isBlank()) {
                                android.widget.Toast.makeText(context, "Please fill all required fields", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            if (user != null) {
                                viewModel.updateProfile(
                                    uid = user.uid,
                                    name = name,
                                    college = collegeName,
                                    year = selectedYear,
                                    bio = bio,
                                    teachSkills = teachSkills.toList(),
                                    learnSkills = learnSkills.toList(),
                                    onComplete = onProfileComplete
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(54.dp),
                    enabled = uiState !is AuthUiState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(PurpleGlow, PurpleAccent)),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    if (currentStep < 2) "Continue" else "Finish Setup",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    if (currentStep < 2) Icons.AutoMirrored.Rounded.ArrowForwardIos else Icons.Rounded.Check,
                                    null, Modifier.size(15.dp), tint = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Step Indicator
// ─────────────────────────────────────────────
@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            val isDone   = index < currentStep
            val width by animateDpAsState(if (isActive) 36.dp else 10.dp, label = "dot")
            val color = when {
                isDone   -> PurpleAccent
                isActive -> PurpleGlow
                else     -> SpaceSurface
            }
            Box(Modifier.height(10.dp).width(width).clip(CircleShape).background(color))
        }
    }
}

// ─────────────────────────────────────────────
//  Step 1 — Basic Info
// ─────────────────────────────────────────────
@Composable
fun StepBasicInfo(
    name: String,         onNameChange: (String) -> Unit,
    college: String,      onCollegeChange: (String) -> Unit,
    selectedYear: String, onYearSelect: (String) -> Unit,
    bio: String,          onBioChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        GlassTextField(
            value = name, onValueChange = onNameChange,
            placeholder = "Full Name", icon = Icons.Rounded.Person
        )

        GlassTextField(
            value = college, onValueChange = onCollegeChange,
            placeholder = "College / Institution", icon = Icons.Rounded.School
        )

        GlassCard {
            Column(Modifier.padding(16.dp)) {
                Text("Year of Study", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.W500)
                Spacer(Modifier.height(12.dp))
                // CRASH FIX: Replaced FlowRow with scrollable Row to avoid library incompatibility
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    yearOptions.forEach { year ->
                        PeerChip(label = year, selected = selectedYear == year, accentColor = PurpleAccent) {
                            onYearSelect(year)
                        }
                    }
                }
            }
        }

        GlassTextField(
            value = bio, onValueChange = onBioChange,
            placeholder = "Short bio — e.g. CSE 2nd year, love Android dev 🚀",
            icon = Icons.Rounded.Edit,
            singleLine = false,
            minLines = 3
        )
    }
}

// ─────────────────────────────────────────────
//  Step 2 & 3 — Skill Picker
// ─────────────────────────────────────────────
@Composable
fun StepSkillPicker(
    title: String,
    subtitle: String,
    selected: MutableList<String>,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(subtitle, fontSize = 13.sp, color = TextSecondary)

        GlassCard {
            Column(Modifier.padding(16.dp)) {
                // CRASH FIX: Replaced FlowRow with scrollable Row to avoid library incompatibility
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allSkills.forEach { skill ->
                        PeerChip(
                            label = skill,
                            selected = skill in selected,
                            accentColor = accentColor
                        ) {
                            if (skill in selected) selected.remove(skill) else selected.add(skill)
                        }
                    }
                }
            }
        }

        if (selected.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = accentColor, modifier = Modifier.size(16.dp))
                Text(
                    "${selected.size} selected: ${selected.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = accentColor,
                    fontWeight = FontWeight.W500
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Reusable UI Components
// ─────────────────────────────────────────────

@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PurpleGlow.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .background(SpaceSurface.copy(alpha = 0.70f), RoundedCornerShape(16.dp))
    ) { content() }
}

@Composable
fun PeerChip(label: String, selected: Boolean, accentColor: Color, onClick: () -> Unit) {
    val bgColor     by animateColorAsState(if (selected) accentColor.copy(0.20f) else SpaceSurface, label = "bg")
    val borderColor by animateColorAsState(if (selected) accentColor else PurpleGlow.copy(alpha = 0.2f), label = "border")
    val textColor   by animateColorAsState(if (selected) accentColor else TextSecondary, label = "text")

    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 13.sp, color = textColor, fontWeight = FontWeight.W500)
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextSecondary, fontSize = 14.sp) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
        },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor   = SpaceSurface.copy(alpha = 0.8f),
            unfocusedContainerColor = SpaceSurface.copy(alpha = 0.6f),
            focusedBorderColor      = PurpleAccent.copy(alpha = 0.7f),
            unfocusedBorderColor    = PurpleGlow.copy(alpha = 0.3f),
            focusedTextColor        = Color.White,
            unfocusedTextColor      = Color.White,
            cursorColor             = PurpleAccent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// REMOVED: Problematic FlowRow wrapper that caused NoSuchMethodError
// @OptIn(ExperimentalLayoutApi::class)
// @Composable
// fun FlowRow(spacing: Dp, content: @Composable () -> Unit) { ... }
