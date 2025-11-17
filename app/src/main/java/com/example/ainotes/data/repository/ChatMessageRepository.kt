package com.example.ainotes.data.repository

import com.example.ainotes.data.local.entity.ChatMessageEntity
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatMessageRepository @Inject constructor() {

    suspend fun getAllMessages(): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                val results = realm.where(ChatMessageEntity::class.java)
                    .sort("timestamp")   // упорядочиваем по времени
                    .findAll()
                realm.copyFromRealm(results)
            } finally {
                realm.close()
            }
        }

    suspend fun getMessagesByChatId(chatId: String): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                val results = realm.where(ChatMessageEntity::class.java)
                    .equalTo("chatId", chatId)
                    .sort("timestamp")
                    .findAll()
                realm.copyFromRealm(results)
            } finally {
                realm.close()
            }
        }

    suspend fun addMessage(entity: ChatMessageEntity) =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { tx ->
                    tx.insertOrUpdate(entity)
                }
            } finally {
                realm.close()
            }
        }

    suspend fun deleteAllMessages() =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { tx ->
                    tx.where(ChatMessageEntity::class.java)
                        .findAll()
                        .deleteAllFromRealm()
                }
            } finally {
                realm.close()
            }
        }

    suspend fun deleteMessagesByChatId(chatId: String) =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { tx ->
                    tx.where(ChatMessageEntity::class.java)
                        .equalTo("chatId", chatId)
                        .findAll()
                        .deleteAllFromRealm()
                }
            } finally {
                realm.close()
            }
        }

    suspend fun deleteMessage(entity: ChatMessageEntity) =
        withContext(Dispatchers.IO) {
            val realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { tx ->
                    val messageToDelete = tx.where(ChatMessageEntity::class.java)
                        .equalTo("timestamp", entity.timestamp)
                        .equalTo("role", entity.role)
                        .equalTo("chatId", entity.chatId)
                        .findFirst()
                    messageToDelete?.deleteFromRealm()
                }
            } finally {
                realm.close()
            }
        }
}