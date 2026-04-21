package com.sahil.peerlearn

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.google.firebase.messaging.FirebaseMessaging
import com.sahil.peerlearn.ui.theme.PeerlearnTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        // Fetch FCM token and save to Firestore after login
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != null) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("FCM_TOKEN", "Token: $token")
                        
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .update("fcm_token", token)
                            .addOnSuccessListener {
                                Log.d("FCM", "Token updated successfully")
                            }
                            .addOnFailureListener { e ->
                                // If document doesn't exist yet, it might fail; 
                                // typically handled in profile setup, but logging for now
                                Log.e("FCM", "Error updating token", e)
                            }
                    }
                }
                
                // OneSignal login (existing)
                OneSignal.login(uid)
            }
        }

        setContent {
            PeerlearnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val auth = Firebase.auth
                    val context = LocalContext.current
                    val authManager = remember { AuthManager(context) }
                    val scope = rememberCoroutineScope()

                    // Handle notification navigation
                    LaunchedEffect(intent) {
                        val navigateTo = intent.getStringExtra("navigate_to")
                        if (navigateTo == "notifications") {
                            navController.navigate("notifications")
                        }
                    }

                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen(navController = navController)
                        }
                        composable("login") {
                            LoginScreen(
                                authManager = authManager,
                                onLoginSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("skill_setup") {
                            val user = auth.currentUser
                            if (user != null) {
                                SkillSetupScreen(
                                    user = user,
                                    authManager = authManager,
                                    onComplete = {
                                        navController.navigate("main") {
                                            popUpTo("skill_setup") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                        composable("main") {
                            AppNavigation(navController)
                        }
                        composable("email_verification") {
                            val user = auth.currentUser
                            if (user != null) {
                                EmailVerificationScreen(
                                    user = user,
                                    onResendEmail = {
                                        scope.launch { authManager.resendVerificationEmail() }
                                    },
                                    onRefresh = {
                                        if (user.isEmailVerified) {
                                            navController.navigate("main") {
                                                popUpTo("email_verification") { inclusive = true }
                                            }
                                        }
                                    },
                                    onLogout = {
                                        scope.launch {
                                            authManager.signOut()
                                            navController.navigate("login") {
                                                popUpTo("email_verification") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        composable("profile_setup") {
                            ProfileSetupScreen(
                                authManager = authManager,
                                onProfileComplete = {
                                    navController.navigate("main") {
                                        popUpTo("profile_setup") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavController) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val authManager = remember { AuthManager(context) }
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    
    var firebaseUser by remember { mutableStateOf(auth.currentUser) }
    var isCheckingStatus by remember { mutableStateOf(true) }
    var isProfileComplete by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
            firebaseUser = it.currentUser
            if (it.currentUser == null) {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(firebaseUser, refreshTrigger) {
        isCheckingStatus = true
        val user = firebaseUser
        if (user != null) {
            try {
                user.reload().await()
                val profile = userRepository.getUserProfile(user.uid).firstOrNull()
                isProfileComplete = profile?.teachSkills?.isNotEmpty() == true && profile.learnSkills.isNotEmpty()
            } catch (e: Exception) {
                Log.e("AppNavigation", "Status check failed", e)
            }
        }
        isCheckingStatus = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isCheckingStatus) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = com.sahil.peerlearn.ui.theme.PurpleAccent)
            }
        } else {
            val user = firebaseUser
            if (user != null) {
                when {
                    !user.isEmailVerified -> {
                        EmailVerificationScreen(
                            user = user,
                            onResendEmail = {
                                scope.launch { authManager.resendVerificationEmail() }
                            },
                            onRefresh = {
                                refreshTrigger++
                            },
                            onLogout = {
                                scope.launch {
                                    authManager.signOut()
                                }
                            }
                        )
                    }
                    !isProfileComplete -> {
                        SkillSetupScreen(
                            user = user,
                            authManager = authManager,
                            onComplete = {
                                refreshTrigger++
                            }
                        )
                    }
                    else -> {
                        MainAppContent(user = user, authManager = authManager)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(user: FirebaseUser, authManager: AuthManager) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Hide BottomBar on ChatScreen and other detail screens
    val showBottomBar = currentRoute in listOf("home", "feed", "chat_list", "profile")

    Scaffold(
        containerColor = com.sahil.peerlearn.ui.theme.SpaceBlack,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues(0.dp))
        ) {
            composable("home") {
                HomeScreen(
                    user = user,
                    onLogout = {
                        scope.launch {
                            authManager.signOut()
                        }
                    },
                    onProfileClick = { navController.navigate("profile") },
                    onSearchClick = { navController.navigate("search") },
                    onChatClick = { uid -> navController.navigate("chat/$uid") },
                    onPeerClick = { uid -> navController.navigate("peer_profile/$uid") },
                    onNotificationsClick = { navController.navigate("notifications") }
                )
            }
            composable("notifications") {
                NotificationScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToChat = { peerUid ->
                        navController.navigate("chat/$peerUid") {
                            // Clear notification screen from backstack if you want
                            popUpTo("notifications") { inclusive = true }
                        }
                    }
                )
            }
            composable("search") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Search Screen")
                }
            }
            composable("chat/{peerUid}") { backStackEntry ->
                val peerUid = backStackEntry.arguments?.getString("peerUid") ?: ""
                ChatScreen(
                    peerUid = peerUid,
                    navController = navController
                )
            }
            composable("peer_profile/{peerUid}") { backStackEntry ->
                val peerUid = backStackEntry.arguments?.getString("peerUid") ?: ""
                PeerProfileScreen(
                    peerUid = peerUid,
                    navController = navController
                )
            }
            composable("feed") {
                FeedScreen()
            }
            composable("chat_list") {
                ChatListScreen(navController = navController)
            }
            composable("profile") {
                ProfileScreen(
                    uid = user.uid,
                    onEditClick = { 
                        navController.navigate("edit_profile/${user.uid}")
                    },
                    onLogoutClick = {
                        scope.launch {
                            authManager.signOut()
                        }
                    },
                    showBackArrow = navController.previousBackStackEntry != null,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("edit_profile/{uid}") { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid") ?: user.uid
                EditProfileScreen(
                    uid = uid,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        NavigationItem("home", "Home", Icons.Default.Home),
        NavigationItem("feed", "Feed", Icons.Default.Article),
        NavigationItem("chat_list", "Chat", Icons.Default.Chat),
        NavigationItem("profile", "Profile", Icons.Default.Person)
    )

    NavigationBar(
        containerColor = com.sahil.peerlearn.ui.theme.SpaceSurface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { 
                    BadgedBox(
                        badge = {
                            if (item.route == "chat_list") {
                                // You can implement real unread count here
                                // Badge { Text("3") }
                            }
                        }
                    ) {
                        Icon(item.icon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = com.sahil.peerlearn.ui.theme.PurpleAccent,
                    selectedTextColor = com.sahil.peerlearn.ui.theme.PurpleAccent,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = com.sahil.peerlearn.ui.theme.TextSecondary,
                    unselectedTextColor = com.sahil.peerlearn.ui.theme.TextSecondary
                )
            )
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: ImageVector)
