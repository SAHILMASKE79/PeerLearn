package com.sahil.peerlearn

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.sahil.peerlearn.ui.theme.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerUid: String,
    navController: NavController,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val peerProfile by viewModel.peerProfile.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUid = currentUser?.uid ?: ""
    val chatId = remember(peerUid, currentUid) { listOf(currentUid, peerUid).sorted().joinToString("_") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var messageText by remember { mutableStateOf("") }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCodeDialog by remember { mutableStateOf(false) }
    
    val activeSession by viewModel.activeStudySession.collectAsState()
    val timerSeconds by viewModel.sessionTimeLeft.collectAsState()

    fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(null)
        val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            capturedImageUri?.let { viewModel.sendImageMessage(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendImageMessage(it) }
    }

    LaunchedEffect(peerUid) {
        viewModel.setPeerUid(peerUid)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    
    Box(modifier = Modifier.fillMaxSize().background(SpaceBlack)) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.clickable {
                            navController.navigate("peer_profile/$peerUid")
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2d1f5e)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (peerProfile?.profileImageUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = peerProfile?.profileImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = peerProfile?.name?.take(2)?.uppercase() ?: "??",
                                    color = Color(0xFFa78bfa),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Column {
                            Text(
                                text = peerProfile?.name ?: "Loading...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active now",
                                color = Color(0xFF10b981),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1535),
                    titleContentColor = Color.White
                )
            )

            // Chat Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2d1f5e)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (peerProfile?.profileImageUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = peerProfile?.profileImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = peerProfile?.name?.take(2)?.uppercase() ?: "??",
                                    color = Color(0xFFa78bfa),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            peerProfile?.name ?: "Loading...",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "You are connected on PeerLearn",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                items(messages) { message ->
                    val isMe = message.senderId == currentUid
                    MessageBubble(
                        message = message,
                        isMe = isMe,
                        onImageClick = { selectedImageUrl = message.imageUrl },
                        onCodeClick = { /* Handle code */ }
                    )
                }
            }
            
            activeSession?.let { session ->
                StudySessionBanner(
                    session = session,
                    timeLeft = timerSeconds * 1000,
                    onEnd = { viewModel.endStudySession() },
                    onExtend = { viewModel.extendStudySession() },
                    onComplete = { viewModel.completeStudySession() }
                )
            }

            ChatInputBar(
                messageText = messageText,
                onMessageChange = { 
                    messageText = it
                    viewModel.onTextChanged(it, chatId, currentUid)
                },
                onSend = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                onImageClick = { showImageSourceDialog = true },
                onCodeClick = { showCodeDialog = true }
            )
        }

        if (showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = { showImageSourceDialog = false },
                title = { Text("Send Image") },
                text = { Text("Choose a source") },
                confirmButton = {
                    TextButton(onClick = {
                        val uri = createImageUri()
                        capturedImageUri = uri
                        cameraLauncher.launch(uri)
                        showImageSourceDialog = false
                    }) { Text("Camera") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        galleryLauncher.launch("image/*")
                        showImageSourceDialog = false
                    }) { Text("Gallery") }
                }
            )
        }

        if (showCodeDialog) {
            CodeSnippetDialog(
                onDismiss = { showCodeDialog = false },
                onSend = { code, lang ->
                    viewModel.sendCodeMessage(code, lang)
                    showCodeDialog = false
                }
            )
        }

        selectedImageUrl?.let { url ->
            FullScreenImageViewer(imageUrl = url, onDismiss = { selectedImageUrl = null })
        }
    }
}

@Composable
fun FullScreenImageViewer(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun StudySessionBanner(
    session: StudySession,
    timeLeft: Long,
    onEnd: () -> Unit,
    onExtend: () -> Unit,
    onComplete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF2d1f5e),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Active Study Session", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Time Left: ${formatTimer(timeLeft)}", color = Color(0xFFa78bfa), fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEnd) { Text("End", color = Color.White) }
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFa78bfa))
                ) {
                    Text("Complete", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    onImageClick: () -> Unit,
    onCodeClick: (String) -> Unit
) {
    val bubbleColor = if (isMe) Color(0xFF6d28d9) else Color(0xFF2d1f5e)
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMe) 16.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 16.dp
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        when (message.type) {
            "code" -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(bubbleShape)
                            .background(if (isMe) Color(0x337C4DFF) else Color(0xFF1A1A2E))
                            .border(1.dp, if (isMe) Color(0x597C4DFF) else Color(0x33FFFFFF), bubbleShape)
                    ) {
                        CodeMessageBubble(message, onCodeClick = { onCodeClick(message.codeSnippet) })
                    }
                }
            }
            else -> {
                Surface(
                    color = bubbleColor,
                    shape = bubbleShape,
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    when (message.type) {
                        "text" -> Text(
                            text = message.text,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = Color.White
                        )
                        "image" -> AsyncImage(
                            model = message.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clickable { onImageClick() },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp?.toDate() ?: Date()),
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun CodeMessageBubble(message: Message, onCodeClick: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1e1e1e))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(message.codeLanguage.uppercase(), color = Color(0xFFa78bfa), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(message.codeSnippet)) }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
            }
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF000000).copy(alpha = 0.3f))
                .padding(12.dp)
                .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = message.codeSnippet,
                    color = Color(0xFFd4d4d4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onImageClick: () -> Unit,
    onCodeClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1535),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onImageClick) {
                Icon(Icons.Default.Add, contentDescription = "Attach", tint = Color(0xFFa78bfa))
            }
            IconButton(onClick = onCodeClick) {
                Icon(Icons.Default.Code, contentDescription = "Code", tint = Color(0xFFa78bfa))
            }
            
            Surface(
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                color = Color(0xFF2d1f5e),
                shape = RoundedCornerShape(24.dp)
            ) {
                BasicTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFa78bfa))
                )
            }

            IconButton(
                onClick = onSend,
                enabled = messageText.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (messageText.isNotBlank()) Color(0xFFa78bfa) else Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CodeSnippetDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("kotlin") }
    val languages = listOf("kotlin", "java", "python", "javascript", "cpp", "c")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Code", color = Color.White) },
        text = {
            Column {
                Text("Language:", color = Color.Gray, fontSize = 12.sp)
                // CRASH FIX: Replaced FlowRow with scrollable Row to avoid library incompatibility (NoSuchMethodError)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { language = lang },
                            label = { Text(lang) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White.copy(alpha = 0.7f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("Paste your code here...") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(code, language) }) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color(0xFF1A1535)
    )
}

fun formatTimer(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
