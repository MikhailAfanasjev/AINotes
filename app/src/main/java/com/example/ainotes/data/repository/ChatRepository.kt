package com.example.ainotes.data.repository

import com.example.ainotes.data.local.entity.ChatEntity
import com.example.ainotes.data.local.entity.ChatMessageEntity
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor() {

    suspend fun getAllChats(): List<ChatEntity> =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                val results = realm.where(ChatEntity::class.java)
                    .sort("lastMessageAt", io.realm.Sort.DESCENDING)
                    .findAll()
                realm.copyFromRealm(results)
            } finally {
                realm.close()
            }
        }

    suspend fun createChat(title: String): ChatEntity =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                val chatEntity = ChatEntity().apply {
                    this.title = title
                }
                realm.executeTransaction { tx ->
                    tx.insertOrUpdate(chatEntity)
                }
                chatEntity
            } finally {
                realm.close()
            }
        }

    suspend fun deleteChat(chatId: String) =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { tx ->
                    // Удаляем все сообщения чата
                    tx.where(ChatMessageEntity::class.java)
                        .equalTo("chatId", chatId)
                        .findAll()
                        .deleteAllFromRealm()

                    // Удаляем сам чат
                    tx.where(ChatEntity::class.java)
                        .equalTo("id", chatId)
                        .findFirst()
                        ?.deleteFromRealm()
                }
            } finally {
                realm.close()
            }
        }

    suspend fun updateChatTitle(chatId: String, newTitle: String) =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { tx ->
                    val chat = tx.where(ChatEntity::class.java)
                        .equalTo("id", chatId)
                        .findFirst()
                    chat?.title = newTitle
                }
            } finally {
                realm.close()
            }
        }

    suspend fun updateChatLastMessage(chatId: String) =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { tx ->
                    val chat = tx.where(ChatEntity::class.java)
                        .equalTo("id", chatId)
                        .findFirst()

                    if (chat != null) {
                        chat.lastMessageAt = System.currentTimeMillis()

                        // Подсчитываем количество сообщений в чате
                        val messageCount = tx.where(ChatMessageEntity::class.java)
                            .equalTo("chatId", chatId)
                            .count()
                        chat.messageCount = messageCount.toInt()
                    }
                }
            } finally {
                realm.close()
            }
        }

    suspend fun getChatById(chatId: String): ChatEntity? =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                val chat = realm.where(ChatEntity::class.java)
                    .equalTo("id", chatId)
                    .findFirst()
                chat?.let { realm.copyFromRealm(it) }
            } finally {
                realm.close()
            }
        }

    suspend fun updateChatTitleGenerated(chatId: String, title: String) =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { tx ->
                    val chat = tx.where(ChatEntity::class.java)
                        .equalTo("id", chatId)
                        .findFirst()
                    if (chat != null) {
                        chat.title = title
                        chat.isTitleGenerated = true
                    }
                }
            } finally {
                realm.close()
            }
        }

    suspend fun updateChatSelectedPrompt(chatId: String, selectedPrompt: String) =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { tx ->
                    val chat = tx.where(ChatEntity::class.java)
                        .equalTo("id", chatId)
                        .findFirst()
                    chat?.selectedPrompt = selectedPrompt
                }
            } finally {
                realm.close()
            }
        }
}