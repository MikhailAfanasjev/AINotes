package com.example.ainotes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE   // при удалении чата удаляются все его сообщения
        )
    ],
    indices = [Index("chatId")]   // ускоряет запросы по chatId
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val role: String,                     // "user" или "assistant"
    val contentRaw: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isComplete: Boolean = true,
    val reasoningContent: String = "",
    val reasoningDurationSeconds: Float = 0f
)