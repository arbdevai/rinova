package com.rinova.app.core.model

import java.util.UUID

enum class MessageSender {
    USER,
    AI
}

data class AiChatAction(
    val actionId: String = UUID.randomUUID().toString(),
    val title: String,
    val reason: String,
    val shellCommand: String,
    val isSafe: Boolean = true,
    var isExecuted: Boolean = false
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val actions: List<AiChatAction> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
