package com.example.ainotes.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.data.local.entity.ChatEntity
import com.example.ainotes.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    companion object {
        private const val TAG = ">>>ChatListViewModel"
    }

    private val _chatList = MutableStateFlow<List<ChatEntity>>(emptyList())
    val chatList: StateFlow<List<ChatEntity>> = _chatList.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    private val _isCreatingChat = MutableStateFlow(false)
    val isCreatingChat: StateFlow<Boolean> = _isCreatingChat.asStateFlow()

    private val _isChatsLoaded = MutableStateFlow(false)
    val isChatsLoaded: StateFlow<Boolean> = _isChatsLoaded.asStateFlow()

    init {
        Log.d(TAG, "🚀 ChatListViewModel инициализирован")
        // Подписываемся на Flow списка чатов
        chatRepository.getAllChats()
            .onEach { chats ->
                Log.d(TAG, "📋 Загружено чатов: ${chats.size}")
                _chatList.value = chats
                _isChatsLoaded.value = true
            }
            .launchIn(viewModelScope)
    }

    fun createNewChat() {
        Log.d(TAG, "➕ Начинаем создание нового чата")
        _isCreatingChat.value = true
        viewModelScope.launch {
            try {
                val chat = chatRepository.createChat("Новый чат")
                Log.d(TAG, "✅ Чат создан: ${chat.title} (id: ${chat.id})")
                _currentChatId.value = chat.id
                // Обновление списка произойдёт автоматически через Flow
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка создания чата", e)
            } finally {
                _isCreatingChat.value = false
            }
        }
    }

    fun selectChat(chatId: String) {
        Log.d(TAG, "🎯 Выбран чат с ID: $chatId")
        _currentChatId.value = chatId
    }

    fun deleteChat(chatId: String) {
        Log.d(TAG, "🗑️ ========== НАЧАЛО УДАЛЕНИЯ ЧАТА ==========")
        Log.d(TAG, "🗑️ Удаляемый chatId: $chatId")

        val wasCurrentChat = _currentChatId.value == chatId
        if (wasCurrentChat) {
            Log.d(TAG, "🧹 НЕМЕДЛЕННО сбрасываем currentChatId")
            _currentChatId.value = null
        }

        viewModelScope.launch {
            try {
                chatRepository.deleteChat(chatId)
                Log.d(TAG, "✅ Чат удален из БД: $chatId")
                // Flow сам обновит список
                Log.d(TAG, "🗑️ ========== УДАЛЕНИЕ ЗАВЕРШЕНО ==========")
            } catch (e: Exception) {
                Log.e(TAG, "❌ ОШИБКА при удалении чата: $chatId", e)
            }
        }
    }

    fun updateChatTitle(chatId: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateChatTitle(chatId, newTitle)
        }
    }

    fun updateChatLastMessage(chatId: String) {
        viewModelScope.launch {
            chatRepository.updateChatLastMessage(chatId)
        }
    }

    fun refreshChats() {
        // Flow уже обновляет, но можем принудительно запросить (если нужно)
        // Можно оставить пустым, т.к. Flow реактивный.
    }

    fun getCurrentChat(): ChatEntity? {
        return _currentChatId.value?.let { chatId ->
            _chatList.value.find { it.id == chatId }
        }
    }

    fun ensureCurrentChat(): String? {
        return _currentChatId.value ?: run {
            createNewChat()
            null
        }
    }
}