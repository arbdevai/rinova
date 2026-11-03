package com.rinova.app.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rinova.app.core.model.AiChatAction
import com.rinova.app.core.model.ChatMessage
import com.rinova.app.core.model.MessageSender
import com.rinova.app.data.repository.AiDiagnosticRepository
import com.rinova.app.data.repository.SettingsRepository
import com.rinova.app.ui.components.RinovaSettingsDialog
import com.rinova.app.ui.components.RinovaSurfaceCard
import com.rinova.app.ui.theme.OutlineVariant
import com.rinova.app.ui.theme.Primary
import com.rinova.app.ui.theme.PrimaryContainer
import com.rinova.app.ui.theme.StatusSafe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDiagnosticScreen(
    aiRepository: AiDiagnosticRepository,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val chatMessages by aiRepository.chatMessages.collectAsStateWithLifecycle()
    val isSendingChat by aiRepository.isSendingChat.collectAsStateWithLifecycle()
    val isProMode by settingsRepository.isProMode.collectAsStateWithLifecycle(false)
    val currentModel by settingsRepository.geminiModel.collectAsStateWithLifecycle("gemini-2.0-flash")

    var inputPrompt by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var actionNotification by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Inisialisasi percakapan sambutan ramah dari Dr. Rinova
    LaunchedEffect(Unit) {
        aiRepository.initChatSessionIfNeeded(null)
    }

    // Auto-scroll ke bawah saat ada pesan baru
    LaunchedEffect(chatMessages.size, isSendingChat) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    if (showSettingsDialog) {
        RinovaSettingsDialog(
            settingsRepository = settingsRepository,
            onDismiss = { showSettingsDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101216))
            .imePadding()
    ) {
        // AI Co-Pilot Header Card
        RinovaSurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shapeRadius = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Dr. Rinova (Dokter HP AI)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showSettingsDialog = true }
                                .padding(vertical = 1.dp)
                        ) {
                            Text(
                                text = "Model: $currentModel",
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Ganti Model",
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan BYOK",
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Action Notification Banner
        AnimatedVisibility(
            visible = actionNotification != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StatusSafe.copy(alpha = 0.15f))
                    .border(0.8.dp, StatusSafe.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = actionNotification.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusSafe,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Chat Conversation List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    isProMode = isProMode,
                    onExecuteAction = { action ->
                        coroutineScope.launch {
                            val res = aiRepository.executeChatAction(action)
                            res.fold(
                                onSuccess = { outText ->
                                    actionNotification = "Tindakan '${action.title}' berhasil diterapkan: $outText"
                                },
                                onFailure = { err ->
                                    actionNotification = "Gagal menerapkan: ${err.localizedMessage}"
                                }
                            )
                        }
                    }
                )
            }

            if (isSendingChat) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Primary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Dr. Rinova sedang menganalisis...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF381B1B))
                                .border(1.dp, Color(0xFF8B2525), RoundedCornerShape(8.dp))
                                .clickable {
                                    aiRepository.cancelChat()
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Hentikan",
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick Suggestion Prompt Chips
        val quickPrompts = listOf(
            "Kenapa HP ini lambat?",
            "Aplikasi apa yang memakan banyak memori?",
            "Riset keamanan paket aplikasi di web",
            "Aman matikan RAM Plus?",
            "Pangkas cache galeri & sistem",
            "Hapus adware tersembunyi"
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E222B))
                        .border(0.8.dp, OutlineVariant, RoundedCornerShape(14.dp))
                        .clickable {
                            inputPrompt = prompt
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD1D5DB),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Chat Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Tanyakan keluhan ponsel Anda...",
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OutlineVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 3
            )

            IconButton(
                onClick = {
                    val promptText = inputPrompt.trim()
                    if (promptText.isNotEmpty() && !isSendingChat) {
                        inputPrompt = ""
                        coroutineScope.launch {
                            val key = settingsRepository.geminiApiKey.first()
                            val model = settingsRepository.geminiModel.first()
                            if (key.isEmpty()) {
                                showSettingsDialog = true
                            } else {
                                aiRepository.sendChatMessage(
                                    apiKey = key,
                                    userText = promptText,
                                    modelName = model
                                )
                            }
                        }
                    }
                },
                enabled = inputPrompt.isNotBlank() && !isSendingChat,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (inputPrompt.isNotBlank() && !isSendingChat) Primary else Color(0xFF1E222B))
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Kirim",
                    tint = if (inputPrompt.isNotBlank() && !isSendingChat) Color.Black else Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    isProMode: Boolean,
    onExecuteAction: (AiChatAction) -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = if (isUser) 320.dp else 460.dp)
                .fillMaxWidth(if (isUser) 0.82f else 0.95f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) Primary else Color(0xFF161922))
                .border(
                    0.8.dp,
                    if (isUser) Primary else OutlineVariant,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isUser) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                } else {
                    FormattedAiMessageContent(
                        text = message.text,
                        onCopyCode = { code ->
                            clipboardManager.setText(AnnotatedString(code))
                        }
                    )
                }

                // Interactive Action Cards inside bubble
                if (message.actions.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        message.actions.forEach { action ->
                            AiChatActionCard(
                                action = action,
                                isProMode = isProMode,
                                onExecute = { onExecuteAction(action) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormattedAiMessageContent(
    text: String,
    onCopyCode: (String) -> Unit
) {
    // Parser untuk mendeteksi blok kode ```language ... ``` atau heredoc
    val codeBlockRegex = Regex("""```([a-zA-Z0-9_-]*)\n([\s\S]*?)```""")
    val matches = codeBlockRegex.findAll(text).toList()

    if (matches.isEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            lineHeight = 21.sp
        )
    } else {
        var lastIdx = 0
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (match in matches) {
                val start = match.range.first
                val end = match.range.last + 1
                if (start > lastIdx) {
                    val normalText = text.substring(lastIdx, start).trim()
                    if (normalText.isNotEmpty()) {
                        Text(
                            text = normalText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            lineHeight = 21.sp
                        )
                    }
                }

                val language = match.groupValues[1].ifEmpty { "HEREDOC / CODE" }.uppercase()
                val codeContent = match.groupValues[2].trimEnd()

                HeredocCodeBlockCard(
                    language = language,
                    code = codeContent,
                    onCopy = { onCopyCode(codeContent) }
                )

                lastIdx = end
            }

            if (lastIdx < text.length) {
                val remainingText = text.substring(lastIdx).trim()
                if (remainingText.isNotEmpty()) {
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HeredocCodeBlockCard(
    language: String,
    code: String,
    onCopy: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF090B0E),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF262C38), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141720))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = language,
                    color = Primary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1F2430))
                        .clickable {
                            onCopy()
                            copied = true
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Salin",
                        tint = if (copied) StatusSafe else Color(0xFFD1D5DB),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (copied) "Disalin" else "Salin Kode",
                        color = if (copied) StatusSafe else Color(0xFFD1D5DB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Scrollable Code Content (Heredoc / HTML / Scripts)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Text(
                    text = code,
                    color = Color(0xFFA7F3D0),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun AiChatActionCard(
    action: AiChatAction,
    isProMode: Boolean,
    onExecute: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF12141A))
            .border(0.8.dp, OutlineVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Text(
            text = action.reason,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF),
            fontSize = 11.sp
        )

        if (isProMode) {
            Text(
                text = "> ${action.shellCommand}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFD8B3),
                fontSize = 10.sp
            )
        }

        if (action.isExecuted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StatusSafe.copy(alpha = 0.15f))
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusSafe,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sudah Dijalankan",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusSafe,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        } else {
            Button(
                onClick = onExecute,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text(
                    text = "Jalankan Solusi Ini",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
