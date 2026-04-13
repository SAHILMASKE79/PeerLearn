package com.sahil.peerlearn

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

val BgDeep        = Color(0xFF0A0A0F)
val BgCard        = Color(0xFF13131C)
val GlassStroke   = Color(0xFF2A2A3D)
val AccentBlue    = Color(0xFF4F8EF7)
val AccentPurple  = Color(0xFF9B59FF)
val AccentCyan    = Color(0xFF00D4FF)
val TextPrimary   = Color(0xFFF0F0FF)
val TextSecondary = Color(0xFF8888AA)

@Composable
fun LoginScreen(
    authManager: AuthManager,
    onLoginSuccess: () -> Unit = {}
) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoginMode     by remember { mutableStateOf(true) }
    var isLoading       by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf("") }

    val scope   = rememberCoroutineScope()
    val context = LocalContext.current

    // Google Sign-In launcher
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                scope.launch {
                    isLoading = true
                    val res = authManager.firebaseAuthWithGoogle(account.idToken!!)
                    isLoading = false
                    if (res.isSuccess) onLoginSuccess()
                    else errorMsg = res.exceptionOrNull()?.message ?: "Google login failed"
                }
            } catch (e: ApiException) {
                errorMsg = "Google sign-in failed: ${e.message}"
            }
        }
    }

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

    Box(Modifier.fillMaxSize().background(BgDeep)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AccentPurple.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.15f + blob1X, size.height * 0.25f),
                    radius = size.width * 0.55f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AccentBlue.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.65f + blob2Y),
                    radius = size.width * 0.5f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(AccentBlue, AccentPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.People, null, tint = TextPrimary, modifier = Modifier.size(36.dp))
            }

            Spacer(Modifier.height(20.dp))
            Text("PeerLearn", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Learn together. Grow together.", fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassStroke, RoundedCornerShape(24.dp))
                    .background(BgCard.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    Text(
                        if (isLoginMode) "Welcome Back 👋" else "Create Account ✨",
                        fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                    )

                    if (errorMsg.isNotEmpty()) {
                        Text(
                            errorMsg,
                            fontSize = 12.sp,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFF6B6B).copy(alpha = 0.1f))
                                .padding(10.dp)
                        )
                    }

                    GlassTextField(
                        value = email, onValueChange = { email = it; errorMsg = "" },
                        placeholder = "Email address", icon = Icons.Rounded.Email,
                        keyboardType = KeyboardType.Email
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = "" },
                        placeholder = { Text("Password", color = TextSecondary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    null, tint = TextSecondary, modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor   = BgCard.copy(alpha = 0.8f),
                            unfocusedContainerColor = BgCard.copy(alpha = 0.6f),
                            focusedBorderColor      = AccentBlue.copy(alpha = 0.7f),
                            unfocusedBorderColor    = GlassStroke,
                            focusedTextColor        = TextPrimary,
                            unfocusedTextColor      = TextPrimary,
                            cursorColor             = AccentCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMsg = "Email aur password dono bharo!"
                                return@Button
                            }
                            scope.launch {
                                isLoading = true
                                errorMsg = ""
                                val result = if (isLoginMode)
                                    authManager.loginWithEmail(email, password)
                                else
                                    authManager.signupWithEmail(email, password)
                                isLoading = false
                                if (result.isSuccess) onLoginSuccess()
                                else errorMsg = result.exceptionOrNull()?.message ?: "Kuch gadbad hui!"
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (!isLoading)
                                        Brush.linearGradient(listOf(AccentBlue, AccentPurple))
                                    else
                                        Brush.linearGradient(listOf(GlassStroke, GlassStroke)),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading)
                                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            else
                                Text(
                                    if (isLoginMode) "Log In" else "Sign Up",
                                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                                )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Divider(Modifier.weight(1f), color = GlassStroke)
                        Text("  or  ", fontSize = 12.sp, color = TextSecondary)
                        Divider(Modifier.weight(1f), color = GlassStroke)
                    }

                    OutlinedButton(
                        onClick = {
                            googleLauncher.launch(authManager.getGoogleSignInIntent())
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GlassStroke),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = BgCard.copy(alpha = 0.5f), contentColor = TextPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.Public, null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Continue with Google", fontSize = 14.sp, fontWeight = FontWeight.W500)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isLoginMode) "New to PeerLearn? " else "Already have an account? ",
                    fontSize = 14.sp, color = TextSecondary
                )
                Text(
                    if (isLoginMode) "Sign Up" else "Log In",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AccentCyan,
                    modifier = Modifier.clickable { isLoginMode = !isLoginMode; errorMsg = "" }
                )
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
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextSecondary, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(20.dp)) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor   = BgCard.copy(alpha = 0.8f),
            unfocusedContainerColor = BgCard.copy(alpha = 0.6f),
            focusedBorderColor      = AccentBlue.copy(alpha = 0.7f),
            unfocusedBorderColor    = GlassStroke,
            focusedTextColor        = TextPrimary,
            unfocusedTextColor      = TextPrimary,
            cursorColor             = AccentCyan
        ),
        modifier = Modifier.fillMaxWidth()
    )
}