package com.example.ainotes.data.repository

import com.example.ainotes.data.local.dao.ChatMessageDao
import com.example.ainotes.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatMessageRepository @Inject constructor(
    private val messageDao: ChatMessageDao
) {
    fun getAllMessages(): Flow<List<ChatMessageEntity>> = messageDao.getAllMessages()

    fun getMessagesByChatId(chatId: String): Flow<List<ChatMessageEntity>> =
        messageDao.getMessagesByChatId(chatId)

    suspend fun addMessage(message: ChatMessageEntity) {
        messageDao.insertMessage(message)
    }

    suspend fun deleteAllMessages() {
        messageDao.deleteAllMessages()
    }

    suspend fun deleteMessagesByChatId(chatId: String) {
        messageDao.deleteMessagesByChatId(chatId)
    }

    suspend fun deleteMessage(message: ChatMessageEntity) {
        messageDao.deleteMessage(message.timestamp, message.role, message.chatId)
    }
}