package com.sahil.peerlearn

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.peerlearn.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authManager: AuthManager,
    onLoginSuccess: () -> Unit = {}
) {
    val userRepository = remember { UserRepository() }
    val viewModel: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authManager, userRepository) as T
        }
    })
    val uiState by viewModel.uiState.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoginMode by remember { mutableStateOf(true) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe UI State for success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        } else if (uiState is AuthUiState.Error) {
            Toast.makeText(context, (uiState as AuthUiState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(PurpleGlow, PurpleAccent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.People, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text("PeerLearn", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Learn together. Grow together.", color = Color.White.copy(alpha = 0.7f))

            Spacer(Modifier.height(40.dp))
            
            // Auth Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PurpleAccent.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .background(SpaceSurface.copy(0.8f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        if (isLoginMode) "Welcome Back 👋" else "Create Account ✨",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    if (!isLoginMode) {
                        GlassTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Full Name",
                            icon = Icons.Rounded.Person
                        )
                    }

                    GlassTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email address",
                        icon = Icons.Rounded.Email,
                        keyboardType = KeyboardType.Email
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Password", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, contentDescription = null,
                                tint = PurpleAccent, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = SpaceSurface,
                            unfocusedContainerColor = SpaceSurface.copy(alpha = 0.5f),
                            focusedBorderColor      = PurpleAccent,
                            unfocusedBorderColor    = SpaceSurface,
                            focusedTextColor        = Color.White,
                            unfocusedTextColor      = Color.White,
                            cursorColor             = PurpleAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isLoginMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                "Forgot Password?",
                                color = PurpleAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    val trimmedEmail = email.trim()
                                    if (trimmedEmail.isEmpty()) {
                                        Toast.makeText(context, "Please enter your email first", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    scope.launch {
                                        authManager.sendPasswordResetEmail(trimmedEmail).fold(
                                            onSuccess = {
                                                Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_LONG).show()
                                            },
                                            onFailure = { e ->
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isLoginMode) {
                                viewModel.login(email.trim(), password.trim())
                            } else {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.signup(email.trim(), password.trim(), name.trim())
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
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
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    if (isLoginMode) "Log In" else "Create Account",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = SpaceSurface)
                        Text("  or  ", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                        HorizontalDivider(Modifier.weight(1f), color = SpaceSurface)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.signInWithGoogle()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = uiState !is AuthUiState.Loading,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SpaceSurface.copy(alpha = 0.5f), contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.Public, null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Continue with Google", fontSize = 14.sp, fontWeight = FontWeight.W500)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isLoginMode) "New here? " else "Joined already? ",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Text(
                            if (isLoginMode) "Sign Up" else "Log In",
                            color = PurpleAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { isLoginMode = !isLoginMode }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor   = SpaceSurface,
            unfocusedContainerColor = SpaceSurface.copy(alpha = 0.5f),
            focusedBorderColor      = PurpleAccent,
            unfocusedBorderColor    = SpaceSurface,
            focusedTextColor        = Color.White,
            unfocusedTextColor      = Color.White,
            cursorColor             = PurpleAccent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

