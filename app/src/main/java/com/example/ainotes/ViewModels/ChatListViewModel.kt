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
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            val chats = chatRepository.getAllChats()
            Log.d(TAG, "📋 Загружено чатов: ${chats.size}")
            chats.forEach { chat ->
                Log.d(TAG, "  - ${chat.title} (id: ${chat.id})")
            }
            _chatList.value = chats
            _isChatsLoaded.value = true
        }
    }

    fun createNewChat() {
        Log.d(TAG, "➕ Начинаем создание нового чата")
        _isCreatingChat.value = true
        viewModelScope.launch {
            try {
                // Все чаты создаются с временным названием "Новый чат"
                // которое будет заменено после первого сообщения пользователя
                val chat = chatRepository.createChat("Новый чат")
                Log.d(TAG, "✅ Чат создан: ${chat.title} (id: ${chat.id})")
                _currentChatId.value = chat.id
                // Немедленно обновляем список чатов
                val updatedChats = chatRepository.getAllChats()
                Log.d(TAG, "🔄 Обновлен список чатов, теперь: ${updatedChats.size}")
                _chatList.value = updatedChats
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
        Log.d(TAG, "📊 Текущий currentChatId ДО удаления: ${_currentChatId.value}")
        Log.d(TAG, "📊 Текущий список чатов ДО удаления: ${_chatList.value.size} шт.")

        // КРИТИЧЕСКИ ВАЖНО: Немедленно сбрасываем currentChatId СИНХРОННО
        // ДО запуска корутины. Это гарантирует мгновенную перерисовку UI.
        val wasCurrentChat = _currentChatId.value == chatId
        if (wasCurrentChat) {
            Log.d(TAG, "🧹 НЕМЕДЛЕННО (синхронно) сбрасываем currentChatId для текущего чата")
            _currentChatId.value = null
            Log.d(TAG, "✅ currentChatId установлен в null (синхронно)")
        } else {
            Log.d(TAG, "ℹ️ Удаляемый чат НЕ является текущим, currentChatId не меняем")
        }

        Log.d(TAG, "📊 currentChatId ПОСЛЕ синхронного сброса: ${_currentChatId.value}")

        // Асинхронно удаляем чат из БД и обновляем список
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗄️ Запускаем асинхронное удаление из БД...")
                // Удаляем чат из БД
                chatRepository.deleteChat(chatId)
                Log.d(TAG, "✅ Чат удален из БД: $chatId")

                // Обновляем список чатов
                val remainingChats = chatRepository.getAllChats()
                _chatList.value = remainingChats
                Log.d(TAG, "📋 Список чатов обновлен, осталось: ${remainingChats.size}")

                // Если был удален текущий чат и есть другие чаты, 
                // можно автоматически выбрать первый (опционально)
                if (wasCurrentChat && remainingChats.isNotEmpty()) {
                    Log.d(
                        TAG,
                        "💡 Есть другие чаты (${remainingChats.size} шт.), можно выбрать первый"
                    )
                    // Раскомментируйте следующие строки, если хотите автоматически 
                    // выбирать первый чат после удаления текущего:
                    // val firstChatId = remainingChats.first().id
                    // _currentChatId.value = firstChatId
                    // Log.d(TAG, "✅ Автоматически выбран чат: $firstChatId")
                } else if (wasCurrentChat && remainingChats.isEmpty()) {
                    Log.d(TAG, "📭 Больше нет чатов, currentChatId остается null")
                }

                Log.d(TAG, "🗑️ ========== УДАЛЕНИЕ ЗАВЕРШЕНО ==========")
            } catch (e: Exception) {
                Log.e(TAG, "❌ ОШИБКА при удалении чата: $chatId", e)
                Log.d(TAG, "🗑️ ========== УДАЛЕНИЕ ПРЕРВАНО С ОШИБКОЙ ==========")
            }
        }
    }

    fun updateChatTitle(chatId: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateChatTitle(chatId, newTitle)
            // Немедленно обновляем список чатов
            _chatList.value = chatRepository.getAllChats()
        }
    }

    fun updateChatLastMessage(chatId: String) {
        viewModelScope.launch {
            chatRepository.updateChatLastMessage(chatId)
            // Немедленно обновляем список чатов
            _chatList.value = chatRepository.getAllChats()
        }
    }

    fun refreshChats() {
        Log.d(TAG, "🔄 Запрос на обновление списка чатов")
        viewModelScope.launch {
            val chats = chatRepository.getAllChats()
            Log.d(TAG, "📋 Обновлено чатов: ${chats.size}")
            _chatList.value = chats
            _isChatsLoaded.value = true
        }
    }

    fun getCurrentChat(): ChatEntity? {
        return _currentChatId.value?.let { chatId ->
            _chatList.value.find { it.id == chatId }
        }
    }

    fun ensureCurrentChat(): String? {
        return _currentChatId.value ?: run {
            // Если нет текущего чата, создаем новый асинхронно
            createNewChat()
            null // возвращаем null, так как создание асинхронное
        }
    }
}