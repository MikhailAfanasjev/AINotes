package com.example.ainotes.data.local.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.UUID

open class ChatMessageEntity(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var chatId: String = "", // ID чата, к которому принадлежит сообщение
    var role: String = "", // "user" или "assistant"
    var contentRaw: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var isComplete: Boolean = true,
    var reasoningContent: String = "", // Содержимое блока размышлений (reasoning_content)
    var reasoningDurationSeconds: Float = 0f // Время генерации размышлений в секундах
) : RealmObject()