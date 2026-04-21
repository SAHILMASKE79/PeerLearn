package com.sahil.peerlearn

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillSetupScreen(
    user: FirebaseUser,
    authManager: AuthManager,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }

    var teachSkills by remember { mutableStateOf(setOf<String>()) }
    var learnSkills by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }

    val allSkills = listOf(
        "Kotlin", "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Swift",
        "Flutter", "React Native", "Jetpack Compose", "XML Android",
        "Firebase", "MongoDB", "MySQL", "PostgreSQL",
        "REST API", "GraphQL", "Git & GitHub",
        "DSA", "System Design", "Machine Learning", "Data Science",
        "HTML/CSS", "React", "Node.js", "Docker", "Linux",
        "Cybersecurity", "DevOps", "Cloud (AWS/GCP)"
    )

    val canContinue = teachSkills.isNotEmpty() && learnSkills.isNotEmpty()

    val spaceBlack = Color(0xFF0D0D1A)
    val spaceSurface = Color(0xFF1A1A2E)
    val purpleAccent = Color(0xFF7C4DFF)

    Scaffold(
        containerColor = spaceBlack,
        bottomBar = {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val result = userRepository.updateProfileFields(
                            uid = user.uid,
                            name = user.displayName ?: "Peer",
                            college = "", // Placeholder, can be filled in Profile screen
                            year = "",    // Placeholder
                            bio = "",
                            teachSkills = teachSkills.toList(),
                            learnSkills = learnSkills.toList()
                        )
                        isLoading = false
                        if (result.isSuccess) {
                            onComplete()
                        } else {
                            Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                enabled = canContinue && !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = purpleAccent,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Skill Setup 🚀",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Let's personalize your experience. Choose at least one skill for each section.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // TEACH SECTION
            SkillSection(
                title = "What can you TEACH? 🎓",
                skills = allSkills,
                selectedSkills = teachSkills,
                onToggleSkill = { skill ->
                    teachSkills = if (teachSkills.contains(skill)) teachSkills - skill else teachSkills + skill
                },
                surfaceColor = spaceSurface,
                accentColor = purpleAccent
            )

            Spacer(Modifier.height(32.dp))

            // LEARN SECTION
            SkillSection(
                title = "What do you want to LEARN? 📚",
                skills = allSkills,
                selectedSkills = learnSkills,
                onToggleSkill = { skill ->
                    learnSkills = if (learnSkills.contains(skill)) learnSkills - skill else learnSkills + skill
                },
                surfaceColor = spaceSurface,
                accentColor = purpleAccent
            )
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillSection(
    title: String,
    skills: List<String>,
    selectedSkills: Set<String>,
    onToggleSkill: (String) -> Unit,
    surfaceColor: Color,
    accentColor: Color
) {
    Column {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // CRASH FIX: Replaced FlowRow with scrollable Row to avoid library incompatibility (NoSuchMethodError)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            skills.forEach { skill ->
                val isSelected = selectedSkills.contains(skill)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleSkill(skill) },
                    label = { 
                        Text(
                            text = skill,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
                        ) 
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        selectedContainerColor = accentColor,
                        labelColor = Color.White,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.White.copy(alpha = 0.2f),
                        selectedBorderColor = accentColor,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp
                    )
                )
            }
        }
    }
}
