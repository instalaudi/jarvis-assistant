package com.jarvis.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val role: String,
    val timestamp: Long,
    val isLoading: Boolean = false,
    val toolCallsJson: String? = null,
    val toolCallId: String? = null
)
