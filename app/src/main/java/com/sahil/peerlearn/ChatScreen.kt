package com.sahil.peerlearn

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val chatId = remember(currentUid, peerUid) {
        listOf(currentUid, peerUid).sorted().joinToString("_")
    }

    var messageText by remember { mutableStateOf("") }
    var showCodeDialog by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showSessionDialog by remember { mutableStateOf(false) }
    var sessionTopic by remember { mutableStateOf("") }

    val messages by viewModel.messages.collectAsState()
    val isPeerTyping by viewModel.isPeerTyping.collectAsState()
    val peerProfile by viewModel.peerProfile.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(chatId, currentUid, peerUid, it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { viewModel.uploadImage(chatId, currentUid, peerUid, it) }
        }
    }

    fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.cacheDir
        val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
        viewModel.observeTyping(chatId, peerUid)
        viewModel.markMessagesAsRead(chatId, currentUid)
        viewModel.loadPeerProfile(peerUid)
        viewModel.observeSession(chatId)
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                text = peerProfile?.name ?: "Chat",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isPeerTyping) {
                                Text(
                                    "typing...",
                                    color = Color(0xFF4ade80),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSessionDialog = true }) {
                        Icon(
                            Icons.Default.Timer,
                            "Study Session",
                            tint = Color(0xFF7C4DFF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )

            activeSession?.let { session ->
                StudySessionBanner(
                    session = session,
                    timerSeconds = timerSeconds,
                    onEnd = { viewModel.endSession(session.sessionId) },
                    onTopicChange = { viewModel.updateSessionTopic(session.sessionId, it) },
                    onTogglePause = { viewModel.toggleSessionStatus(session.sessionId) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        MessageBubble(
                            message = msg,
                            isMe = msg.senderId == currentUid,
                            onCopyCode = {
                                clipboardManager.setText(AnnotatedString(msg.codeSnippet))
                                Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                            },
                            onImageClick = {
                                selectedImageUrl = it
                            }
                        )
                    }
                }
                
                if (uploadProgress != null) {
                    LinearProgressIndicator(
                        progress = { uploadProgress!! },
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                        color = PurpleAccent,
                        trackColor = Color.White.copy(0.1f)
                    )
                }
            }

            ChatInputBar(
                messageText = messageText,
                onValueChange = { 
                    messageText = it
                    viewModel.onTextChanged(it, chatId, currentUid)
                },
                onSend = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendTextMessage(currentUid, peerUid, messageText)
                        messageText = ""
                    }
                },
                onAttachClick = { showAttachmentSheet = true },
                onCodeClick = { showCodeDialog = true }
            )
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = SpaceSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ListItem(
                    headlineContent = { Text("Camera", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.CameraAlt, null, tint = PurpleAccent) },
                    modifier = Modifier.clickable {
                        showAttachmentSheet = false
                        val uri = createImageUri()
                        cameraImageUri = uri
                        cameraLauncher.launch(uri)
                    }
                )
                ListItem(
                    headlineContent = { Text("Gallery", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null, tint = PurpleAccent) },
                    modifier = Modifier.clickable {
                        showAttachmentSheet = false
                        galleryLauncher.launch("image/*")
                    }
                )
            }
        }
    }

    if (showSessionDialog) {
        AlertDialog(
            onDismissRequest = { showSessionDialog = false },
            containerColor = SpaceSurface,
            title = { Text("Start Study Session", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = sessionTopic,
                    onValueChange = { sessionTopic = it },
                    label = { Text("Topic", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.startSession(chatId, sessionTopic)
                    showSessionDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)) {
                    Text("Start")
                }
            }
        )
    }

    if (showCodeDialog) {
        CodeSnippetDialog(
            onDismiss = { showCodeDialog = false },
            onSend = { code, lang ->
                viewModel.sendCodeSnippet(currentUid, peerUid, code, lang)
                showCodeDialog = false
            }
        )
    }

    if (selectedImageUrl != null) {
        FullScreenImageViewer(
            imageUrl = selectedImageUrl!!,
            onDismiss = { selectedImageUrl = null }
        )
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
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun StudySessionBanner(
    session: StudySession,
    timerSeconds: Long,
    onEnd: () -> Unit,
    onTopicChange: (String) -> Unit,
    onTogglePause: () -> Unit
) {
    var isEditingTopic by remember { mutableStateOf(false) }
    var topicText by remember { mutableStateOf(session.topic) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Timer, null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            formatTimer(timerSeconds),
            color = if (session.status == "paused") Color.Gray else Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(Modifier.width(8.dp))
        
        IconButton(onClick = onTogglePause, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = if (session.status == "active") Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (session.status == "active") "Pause" else "Resume",
                tint = PurpleAccent,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.width(8.dp))
        
        if (isEditingTopic) {
            BasicTextField(
                value = topicText,
                onValueChange = { topicText = it },
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { 
                onTopicChange(topicText)
                isEditingTopic = false 
            }) {
                Icon(Icons.Default.Check, null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
            }
        } else {
            Text(
                session.topic, 
                color = Color.White, 
                fontSize = 14.sp,
                modifier = Modifier.weight(1f).clickable { isEditingTopic = true }
            )
        }

        IconButton(onClick = onEnd) {
            Icon(Icons.Default.Stop, null, tint = Color.Red, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    onCopyCode: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val bubbleColor = if (isMe) Color(0x337C4DFF) else Color(0xFF1A1A2E)
    val borderColor = if (isMe) Color(0x597C4DFF) else Color(0x33FFFFFF)

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .wrapContentWidth(if (isMe) Alignment.End else Alignment.Start)
                .clip(bubbleShape)
                .background(bubbleColor)
                .border(1.dp, borderColor, bubbleShape)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            when (message.type.lowercase()) {
                "text" -> Text(
                    text = message.text,
                    color = if (isMe) Color(0xFFFFFFFF) else Color(0xFFE0E0E0),
                    fontSize = 14.sp
                )
                "image" -> AsyncImage(
                    model = message.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(message.imageUrl) },
                    contentScale = ContentScale.Crop
                )
                "code" -> CodeMessageBubble(message, onCopyCode)
                else -> Text(
                    text = message.text,
                    color = if (isMe) Color(0xFFFFFFFF) else Color(0xFFE0E0E0),
                    fontSize = 14.sp
                )
            }
        }
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp.toDate()),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
fun CodeMessageBubble(message: Message, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D1A), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message.codeLanguage.uppercase(), color = PurpleAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(14.dp))
            }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
        ) {
            Text(
                message.codeSnippet,
                color = Color(0xFFE0E0E0),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onCodeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceSurface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttachClick) { Icon(Icons.Default.AttachFile, null, tint = PurpleAccent) }
        IconButton(onClick = onCodeClick) { Icon(Icons.Default.Code, null, tint = PurpleAccent) }
        
        TextField(
            value = messageText,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message...", color = Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SpaceBlack,
                unfocusedContainerColor = SpaceBlack,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp)
        )

        IconButton(onClick = onSend) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = PurpleAccent)
        }
    }
}

@Composable
fun CodeSnippetDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("kotlin") }
    val languages = listOf("kotlin", "java", "python", "javascript", "sql", "other")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceSurface,
        title = { Text("Share Code", color = Color.White) },
        text = {
            Column {
                Text("Language:", color = Color.Gray, fontSize = 12.sp)
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(100.dp)) {
                    items(languages) { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { language = lang },
                            label = { Text(lang) },
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(code, language) }, colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)) {
                Text("Send Code")
            }
        }
    )
}

fun formatTimer(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
