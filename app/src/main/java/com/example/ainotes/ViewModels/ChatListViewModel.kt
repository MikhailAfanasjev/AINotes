package com.example.ainotes.viewModels

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

    private val _chatList = MutableStateFlow<List<ChatEntity>>(emptyList())
    val chatList: StateFlow<List<ChatEntity>> = _chatList.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    private val _isCreatingChat = MutableStateFlow(false)
    val isCreatingChat: StateFlow<Boolean> = _isCreatingChat.asStateFlow()

    init {
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            _chatList.value = chatRepository.getAllChats()
        }
    }

    fun createNewChat(title: String = "Новый чат") {
        _isCreatingChat.value = true
        viewModelScope.launch {
            try {
                val chat = chatRepository.createChat(generateChatTitle())
                _currentChatId.value = chat.id
                loadChats()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isCreatingChat.value = false
            }
        }
    }

    fun selectChat(chatId: String) {
        _currentChatId.value = chatId
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
            if (_currentChatId.value == chatId) {
                // Если удаляем текущий чат, выбираем первый доступный или null
                val remainingChats = chatRepository.getAllChats()
                _currentChatId.value = remainingChats.firstOrNull()?.id
            }
            loadChats()
        }
    }

    fun updateChatTitle(chatId: String, newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateChatTitle(chatId, newTitle)
            loadChats()
        }
    }

    fun updateChatLastMessage(chatId: String) {
        viewModelScope.launch {
            chatRepository.updateChatLastMessage(chatId)
            loadChats()
        }
    }

    private fun generateChatTitle(): String {
        // Находим все существующие номера чатов
        val existingNumbers = _chatList.value
            .mapNotNull { chat ->
                // Извлекаем номер из названия чата (например, "Чат 5" -> 5)
                val regex = Regex("Чат (\\d+)")
                regex.find(chat.title)?.groupValues?.get(1)?.toIntOrNull()
            }
            .toSet()

        // Находим первый свободный номер, начиная с 1
        var nextNumber = 1
        while (existingNumbers.contains(nextNumber)) {
            nextNumber++
        }

        return "Чат $nextNumber"
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