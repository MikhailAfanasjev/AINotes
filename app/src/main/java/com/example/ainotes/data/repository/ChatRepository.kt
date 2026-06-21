package com.example.ainotes.data.repository

import com.example.ainotes.data.local.dao.ChatDao
import com.example.ainotes.data.local.dao.ChatMessageDao
import com.example.ainotes.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: ChatMessageDao
) {
    fun getAllChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()

    suspend fun createChat(title: String): ChatEntity {
        val chat = ChatEntity(title = title)
        chatDao.insertChat(chat)
        return chat
    }

    suspend fun deleteChat(chatId: String) {
        // Благодаря onDelete = CASCADE сообщения удалятся автоматически
        chatDao.deleteChatById(chatId)
    }

    suspend fun updateChatTitle(chatId: String, newTitle: String) {
        chatDao.updateChatTitle(chatId, newTitle)
    }

    suspend fun updateChatLastMessage(chatId: String) {
        val timestamp = System.currentTimeMillis()
        val count = messageDao.getMessageCount(chatId)  // <-- используем suspend функцию
        chatDao.updateChatLastMessage(chatId, timestamp, count)
    }

    suspend fun getChatById(chatId: String): ChatEntity? {
        return chatDao.getChatById(chatId)
    }

    suspend fun updateChatTitleGenerated(chatId: String, title: String) {
        chatDao.updateChatTitleGenerated(chatId, title)
    }

    suspend fun updateChatSelectedPrompt(chatId: String, prompt: String) {
        chatDao.updateChatSelectedPrompt(chatId, prompt)
    }
}