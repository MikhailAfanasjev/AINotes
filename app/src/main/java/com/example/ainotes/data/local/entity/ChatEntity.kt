package com.example.ainotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var lastMessageAt: Long = System.currentTimeMillis(),
    var messageCount: Int = 0,
    var isTitleGenerated: Boolean = false,
    var selectedPrompt: String = ""
)