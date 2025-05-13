package com.example.ainotes.data.local.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.UUID

open class ChatMessageEntity(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var role: String = "", // "user" или "assistant"
    var content: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var isComplete: Boolean = true
) : RealmObject()