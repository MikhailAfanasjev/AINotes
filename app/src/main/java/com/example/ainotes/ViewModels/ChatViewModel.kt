package com.example.ainotes.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.chatGPT.ChatGPTApiService
import com.example.ainotes.chatGPT.ChatGPTRequest
import com.example.ainotes.chatGPT.Message
import com.example.ainotes.data.local.entity.ChatMessageEntity
import com.example.ainotes.data.repository.ChatMessageRepository
import com.example.ainotes.utils.cleanResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okio.BufferedSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: ChatGPTApiService,
    private val chatRepo: ChatMessageRepository
) : ViewModel() {

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "Пиши ответы на русском языке"
    }

    private val _chatMessages = MutableStateFlow<List<Message>>(emptyList())
    val chatMessages: StateFlow<List<Message>> = _chatMessages

    private val _selectedModel = MutableStateFlow("grok-3-gemma3-12b-distilled")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _systemPrompt = MutableStateFlow(DEFAULT_SYSTEM_PROMPT)

    val defaultSystemPrompt: String = DEFAULT_SYSTEM_PROMPT
    private var currentCall: Call<ResponseBody>? = null

    // 1) флаг, показывает, идёт ли сейчас вывод ассистента
    private val _isAssistantWriting = MutableStateFlow(false)
    val isAssistantWriting: StateFlow<Boolean> = _isAssistantWriting.asStateFlow()

    // 2) очередь пользовательских сообщений
    private val messageQueue = Channel<String>(Channel.UNLIMITED)
    private var currentSendJob: Job? = null

    val availableModels = listOf(
        "gemma-3-1b-it (очень быстрая, базовая точность)",
        "grok-3-gemma3-4b (сбалансированная скорость и точность)",
        "grok-3-gemma3-12b (умеренная скорость, высокая точность)"
        //"gemma-3-27b-it"
    )

    init {
        // worker, который берёт из очереди и запускает handleSend в отдельном job
        viewModelScope.launch {
            for (userInput in messageQueue) {
                // ждём окончания предыдущего
                while (_isAssistantWriting.value) delay(50)
                // запускаем новую генерацию в своей Job
                currentSendJob = viewModelScope.launch(Dispatchers.IO) {
                    handleSend()
                }
                // ждём её окончания, чтобы не начать следующую
                currentSendJob?.join()
            }
        }

        // загрузка из БД
        viewModelScope.launch {
            val persisted = chatRepo.getAllMessages()
                // фильтруем пустые
                .filter { it.contentRaw.isNotBlank() }
                // конвертируем каждую строку в AnnotatedString
                .map { entity ->
                    Message(
                        role = entity.role,
                        content = cleanResponse(entity.contentRaw).text,
                        isComplete = entity.isComplete
                    )
                }
            _chatMessages.value = persisted
        }
    }

    fun setSystemPrompt(prompt: String) {
        _systemPrompt.value = prompt
    }

    fun setModel(model: String) {
        _selectedModel.value = model
    }

    private fun addMessage(message: Message) {
        _chatMessages.value += message
        viewModelScope.launch {
            chatRepo.addMessage(
                ChatMessageEntity(
                    role = message.role,
                    contentRaw = message.content,
                    timestamp = System.currentTimeMillis(),
                    isComplete = true
                )
            )
        }
    }

    private fun updateLastAssistantMessage(content: String, isComplete: Boolean = false) {
        val messages = _chatMessages.value.toMutableList()
        val idx = messages.indexOfLast { it.role == "assistant" }
        if (idx != -1) {
            messages[idx] = messages[idx].copy(
                content    = content,
                isComplete = isComplete
            )
            _chatMessages.value = messages
        }
    }

    fun sendMessage(inputText: String) {
        addMessage(Message(role = "user", content = inputText))
        messageQueue.trySend(inputText)
    }

    fun stopGeneration() {
        // отменяем сетевой вызов
        currentCall?.cancel()
        // сбрасываем флаг и помечаем последнее сообщение как завершённое
        _isAssistantWriting.value = false
        val lastContent = _chatMessages.value.lastOrNull { it.role == "assistant" }?.content.orEmpty()
        updateLastAssistantMessage(content = lastContent, isComplete = true)
        // сохраняем текущее (возможно неполное) сообщение ассистента в БД
        viewModelScope.launch {
            chatRepo.addMessage(
                ChatMessageEntity(
                    role = "assistant",
                    contentRaw = lastContent,
                    timestamp = System.currentTimeMillis(),
                    isComplete = true
                )
            )
        }
    }

    private fun handleSend() {
        _isAssistantWriting.value = true
        val allMessages = listOf(Message("system", _systemPrompt.value)) + _chatMessages.value
        val req = ChatGPTRequest(model = _selectedModel.value, messages = allMessages, stream = true)

        // получаем Call вместо suspend
        currentCall = api.sendChatMessageCall(req)

        // подготовили JSON‑парсер и StringBuilder для накопления чанков
        val gson = Gson()
        val builder = StringBuilder()

        // добавляем пустое сообщение ассистента, которое будем обновлять
        addMessage(Message(role = "assistant", content = "", isComplete = false))

        currentCall?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.source()?.let { source ->
                        // читаем стрим в корутине IO
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                streamResponse(source, gson, builder)
                            } catch (_: IOException) {
                                // соединение было отменено — просто выходим
                            } finally {
                                _isAssistantWriting.value = false
                            }
                        }
                    }
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
                        addMessage(Message("assistant", "Ошибка: ${response.code()}"))
                        _isAssistantWriting.value = false
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // сюда придёт при cancel()
                _isAssistantWriting.value = false
            }
        })
    }


    // 2) streamResponse — расширена до трёх параметров
    private suspend fun streamResponse(
        source: BufferedSource,
        gson: Gson,
        builder: StringBuilder
    ) {
        // Читаем строку за строкой из source
        while (!source.exhausted()) {
            val line = source.readUtf8Line().orEmpty()
            if (line.trim() == "data: [DONE]") break

            if (line.startsWith("data:")) {
                val jsonLine = line.removePrefix("data:").trim()
                val chunk = runCatching {
                    gson.fromJson(jsonLine, JsonObject::class.java)
                        .getAsJsonArray("choices")[0]
                        .asJsonObject["delta"].asJsonObject
                        .get("content")?.asString.orEmpty()
                }.getOrNull().orEmpty()

                if (chunk.isNotEmpty()) {
                    builder.append(chunk)
                    val annotated = cleanResponse(builder.toString())
                    withContext(Dispatchers.Main) {
                        // обновляем сообщение ассистента по мере поступления текста
                        updateLastAssistantMessage(annotated.text, isComplete = false)
                    }
                }
            }
        }

        // Финальное завершение
        val finalRaw = builder.toString()
        withContext(Dispatchers.Main) {
            updateLastAssistantMessage(cleanResponse(builder.toString()).text, isComplete = true)
        }

        // Сохраняем готовый ответ в БД
        chatRepo.addMessage(
            ChatMessageEntity(
                role = "assistant",
                contentRaw = finalRaw,
                timestamp = System.currentTimeMillis(),
                isComplete = true
            )
        )
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
        viewModelScope.launch {
            chatRepo.deleteAllMessages()
        }
    }
}