package com.example.ainotes.data.local.entity

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.UUID

open class ChatEntity(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var lastMessageAt: Long = System.currentTimeMillis(),
    var messageCount: Int = 0
) : RealmObject()