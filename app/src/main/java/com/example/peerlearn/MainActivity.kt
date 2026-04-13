package com.sahil.peerlearn

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
import com.sahil.peerlearn.ui.theme.PeerlearnTheme
import kotlinx.coroutines.delay
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

        // OneSignal Initialization
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, "74ed12e3-94a3-48bc-b54e-41651cc735cc")

        // Notification permission maango
        lifecycleScope.launch {
            OneSignal.Notifications.requestPermission(true)
        }

        // User login hone ke baad
        // OneSignal ID save karo
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid ?: return@addAuthStateListener

            // OneSignal me uid set karo
            OneSignal.login(uid)

            // 3 second baad ID save karo
            lifecycleScope.launch {
                delay(3000)
                val osId = OneSignal.User.pushSubscription.id ?: return@launch

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("oneSignalId", osId)
                    .addOnSuccessListener {
                        Log.d("OneSignal", "ID saved: $osId")
                    }
            }
        }

        setContent {
            PeerlearnTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val auth = Firebase.auth
    val authManager = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()
    
    var firebaseUser by remember { mutableStateOf(auth.currentUser) }
    var isCheckingProfile by remember { mutableStateOf(true) }
    var isProfileComplete by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
            firebaseUser = it.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(firebaseUser, refreshTrigger) {
        isCheckingProfile = true
        val user = firebaseUser
        if (user != null) {
            try {
                user.reload().await()
            } catch (e: Exception) { }
            
            if (!user.isEmailVerified) {
                isProfileComplete = false 
            } else {
                val userRepository = UserRepository()
                isProfileComplete = userRepository.isProfileComplete(user.uid)
            }
        } else {
            isProfileComplete = false
        }
        isCheckingProfile = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isCheckingProfile) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val user = firebaseUser
            when {
                user == null -> {
                    LoginScreen(
                        authManager = authManager,
                        onLoginSuccess = { }
                    )
                }
                !user.isEmailVerified -> {
                    EmailVerificationScreen(
                        user = user,
                        onResendEmail = {
                            scope.launch {
                                authManager.resendVerificationEmail()
                            }
                        },
                        onRefresh = { refreshTrigger++ },
                        onLogout = {
                            scope.launch {
                                authManager.signOut()
                            }
                        }
                    )
                }
                !isProfileComplete -> {
                    ProfileSetupScreen(
                        authManager = authManager,
                        onProfileComplete = { 
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

@Composable
fun MainAppContent(user: FirebaseUser, authManager: AuthManager) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Hide BottomBar on ChatScreen and other detail screens
    val showBottomBar = currentRoute in listOf("home", "feed", "chat_list", "profile")

    Scaffold(
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
                    onBackClick = { navController.popBackStack() }
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
        containerColor = Color(0xFF1C1C1C),
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
                    selectedIconColor = Color(0xFF5B9BD5),
                    selectedTextColor = Color(0xFF5B9BD5),
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E)
                )
            )
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: ImageVector)
