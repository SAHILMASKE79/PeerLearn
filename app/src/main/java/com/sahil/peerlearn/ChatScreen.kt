package com.sahil.peerlearn

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sahil.peerlearn.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    var codeText by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }

    val messages by viewModel.messages.collectAsState()
    val isPeerTyping by viewModel.isPeerTyping.collectAsState()
    val peerProfile by viewModel.peerProfile.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var replyingTo by remember { mutableStateOf<Message?>(null) }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var showMessageOptions by remember { mutableStateOf(false) }

    val showScrollButton by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                lastVisibleItem.index < layoutInfo.totalItemsCount - 3
            }
        }
    }

    val keyboardState by rememberUpdatedState(WindowInsets.isImeVisible)

    LaunchedEffect(keyboardState) {
        if (keyboardState && messages.isNotEmpty()) {
            delay(100)
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(isPeerTyping) {
        if (isPeerTyping) {
            delay(100)
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
        viewModel.observeTyping(chatId, peerUid)
        viewModel.markMessagesAsRead(chatId, currentUid)
        viewModel.loadPeerProfile(peerUid)
    }

    val avatarColor = remember(peerProfile?.name) { getAvatarColorFromChat(peerProfile?.name ?: "") }

    Box(modifier = Modifier.fillMaxSize().background(SpaceBlack)) {
        // Standard Space Theme Glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(450.dp, 300.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            PurpleGlow.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        Canvas(Modifier.fillMaxSize()) {
            val patternColor = Color(0xFF252525).copy(alpha = 0.5f)
            for (x in 0..size.width.toInt() step 60) {
                for (y in 0..size.height.toInt() step 60) {
                    drawCircle(color = patternColor, radius = 1.5f, center = Offset(x.toFloat(), y.toFloat()))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceSurface.copy(alpha = 0.9f))
                    .statusBarsPadding()
                    .padding(4.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowBack, tint = Color.White, modifier = Modifier.size(22.dp), contentDescription = "Back")
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(avatarColor)
                        .clickable { navController.navigate("peer_profile/${peerUid}") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        peerProfile?.name?.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigate("peer_profile/${peerUid}") }
                ) {
                    Text(
                        peerProfile?.name ?: "Loading...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isPeerTyping) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "typing",
                                color = Color(0xFF4DCA5D),
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic
                            )
                            Row(
                                modifier = Modifier.padding(start = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "typing")
                                listOf(0, 200, 400).forEach { delay ->
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 0.2f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(600, delay),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "alpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4DCA5D).copy(alpha = alpha))
                                    )
                                }
                            }
                        }
                    } else {
                        Text("online", color = Color(0xFF4DCA5D), fontSize = 12.sp)
                    }
                }
                IconButton(onClick = {
                    Toast.makeText(context, "Voice call coming soon!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Call, tint = PurpleAccent, modifier = Modifier.size(22.dp), contentDescription = "Call")
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, tint = Color.White, modifier = Modifier.size(22.dp), contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(SpaceSurface)
                    ) {
                        listOf(
                            "View Profile" to { navController.navigate("peer_profile/$peerUid") },
                            "Share Code" to { showCodeDialog = true },
                            "Clear Chat" to { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() },
                            "Block User" to { Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show() }
                        ).forEach { (label, action) ->
                            DropdownMenuItem(
                                text = { Text(label, color = Color.White) },
                                onClick = {
                                    action()
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    messages.groupBy { formatDate(it.timestamp) }.forEach { (date, msgs) ->
                        item { DateSeparator(date) }
                        items(msgs) { msg ->
                            val isMe = msg.senderId == currentUid
                            MessageRow(
                                msg = msg,
                                isMe = isMe,
                                avatarColor = avatarColor,
                                peerName = peerProfile?.name ?: "",
                                screenWidth = screenWidth,
                                onLongPress = {
                                    selectedMessage = msg
                                    showMessageOptions = true
                                }
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                    item {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isPeerTyping,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            TypingIndicator(avatarColor, peerProfile?.name ?: "")
                        }
                    }
                }

                // Scroll-to-bottom Floating Action Button
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollButton,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                val totalItems = listState.layoutInfo.totalItemsCount
                                if (totalItems > 0) {
                                    listState.animateScrollToItem(totalItems - 1)
                                }
                            }
                        },
                        containerColor = PurpleAccent,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom",
                            tint = Color.White
                        )
                    }
                }
            }

            if (replyingTo != null) {
                ReplyPreview(replyingTo!!) { replyingTo = null }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceSurface)
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(onClick = {
                    Toast.makeText(context, "Attachments coming soon!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.AttachFile, tint = Color(0xFF9E9E9E), modifier = Modifier.size(22.dp), contentDescription = null)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SpaceBlack, RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = messageText,
                            onValueChange = { text ->
                                messageText = text
                                viewModel.onTextChanged(text, chatId, currentUid)
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message...", color = Color(0xFF9E9E9E)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 5
                        )
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.EmojiEmotions, tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp), contentDescription = null)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (messageText.isNotEmpty()) PurpleAccent else SpaceBlack,
                            CircleShape
                        )
                        .clickable {
                            if (messageText.isNotBlank()) {
                                viewModel.setTyping(chatId, currentUid, false)
                                viewModel.sendTextMessage(
                                    currentUid, peerUid, messageText,
                                    replyToId = replyingTo?.id ?: "",
                                    replyToText = replyingTo?.message ?: ""
                                )
                                messageText = ""
                                replyingTo = null
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (messageText.isNotEmpty()) Icons.Default.Send else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (messageText.isNotEmpty()) Color.White else Color(0xFF9E9E9E),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    if (showMessageOptions && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showMessageOptions = false },
            containerColor = SpaceSurface,
            text = {
                Column {
                    val options = listOf(
                        "Reply" to { replyingTo = selectedMessage; showMessageOptions = false },
                        "Copy" to { clipboardManager.setText(AnnotatedString(selectedMessage!!.message)); showMessageOptions = false },
                        "Forward" to { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show(); showMessageOptions = false },
                        "Delete" to { if (selectedMessage!!.senderId == currentUid) viewModel.deleteMessage(selectedMessage!!.id); showMessageOptions = false }
                    )
                    options.forEach { (label, action) ->
                        Text(
                            label,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth().clickable { action() }.padding(16.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showCodeDialog) {
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            containerColor = SpaceSurface,
            title = { Text("Share Code 💻", color = Color.White) },
            text = {
                Column {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedLanguage,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Language", color = Color.Gray) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PurpleAccent
                            )
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("Kotlin", "Python", "Java", "C++", "JavaScript", "Other").forEach { lang ->
                                DropdownMenuItem(text = { Text(lang) }, onClick = { selectedLanguage = lang; expanded = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { codeText = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        placeholder = { Text("Paste code here...", color = Color.Gray) },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color(0xFF00FF88)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleAccent
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (codeText.isNotBlank()) {
                            viewModel.sendCodeMessage(currentUid, peerUid, codeText, selectedLanguage)
                            codeText = ""; showCodeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) { Text("Send 🚀") }
            }
        )
    }
}

@Composable
fun MessageRow(msg: Message, isMe: Boolean, avatarColor: Color, peerName: String, screenWidth: androidx.compose.ui.unit.Dp, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(peerName.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 11.sp)
            }
            Spacer(Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = screenWidth * 0.75f)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isMe) 16.dp else 4.dp,
                        topEnd = if (isMe) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(if (isMe) PurpleGlow.copy(alpha = 0.8f) else SpaceSurface)
                .let { 
                    if (!isMe) it.border(1.dp, Color(0xFF2A2A3D), RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    else it
                }
                .pointerInput(msg.id) { detectTapGestures(onLongPress = { onLongPress() }) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column {
                if (msg.replyToText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isMe) SpaceBlack.copy(0.4f) else SpaceBlack, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(msg.replyToText.take(60), color = PurpleAccent, fontSize = 12.sp, maxLines = 2)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                
                if (msg.type == "code") {
                    Box(modifier = Modifier.background(Color.Black.copy(0.3f), RoundedCornerShape(4.dp)).padding(4.dp)) {
                        Text(msg.language.uppercase(), color = PurpleAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(msg.message, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                } else {
                    Text(msg.message, color = Color.White, fontSize = 15.sp, lineHeight = 20.sp)
                }

                Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(msg.timestamp), color = if (isMe) Color.White.copy(0.6f) else Color(0xFF9E9E9E), fontSize = 11.sp)
                    if (isMe) {
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = when {
                                msg.status == "sending" -> Icons.Default.Schedule
                                msg.isRead -> Icons.Default.DoneAll
                                else -> Icons.Default.Done
                            },
                            tint = if (msg.isRead) PurpleAccent else Color.White.copy(0.6f),
                            modifier = Modifier.size(14.dp),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateSeparator(date: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.background(Color.Black.copy(0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(date, color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun TypingDotsSmall() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(0, 150, 300).forEach { delay ->
            val alpha by infiniteTransition.animateFloat(
                0.2f, 1f,
                infiniteRepeatable(tween(400, delay), RepeatMode.Reverse),
                label = "dot"
            )
            Text(".", color = Color(0xFF4DCA5D).copy(alpha = alpha), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TypingIndicator(avatarColor: Color, peerName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(peerName.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 11.sp)
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier.background(
                SpaceSurface,
                RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            ).padding(14.dp, 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(0, 150, 300).forEach { delay ->
                    val scale by infiniteTransition.animateFloat(
                        0.6f, 1f,
                        infiniteRepeatable(tween(400, delay), RepeatMode.Reverse),
                        label = "dot"
                    )
                    Box(modifier = Modifier.size(8.dp).scale(scale).background(Color(0xFF9E9E9E), CircleShape))
                }
            }
        }
    }
}

@Composable
fun ReplyPreview(msg: Message, onCancel: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(SpaceSurface).padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(2.dp).height(36.dp).background(PurpleAccent))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Reply", color = PurpleAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(msg.message.take(50), color = Color(0xFF9E9E9E), fontSize = 12.sp, maxLines = 1)
        }
        IconButton(onClick = onCancel) { Icon(Icons.Default.Close, tint = Color(0xFF9E9E9E), modifier = Modifier.size(18.dp), contentDescription = null) }
    }
}

fun formatDate(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

fun formatTime(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

fun getAvatarColorFromChat(name: String): Color {
    if (name.isEmpty()) return Color(0xFF5B9BD5)
    val colors = listOf(
        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
        Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DD0E1),
        Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFFAED581), Color(0xFFFF8A65)
    )
    return colors[Math.abs(name.hashCode()) % colors.size]
}
