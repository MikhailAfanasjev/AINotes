package com.example.ainotes.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.chatGPT.ChatGPTApiService
import com.example.ainotes.chatGPT.ChatGPTRequest
import com.example.ainotes.chatGPT.Message
import com.example.ainotes.data.local.entity.ChatMessageEntity
import com.example.ainotes.data.repository.ChatMessageRepository
import com.example.ainotes.data.repository.ChatRepository
import com.example.ainotes.utils.NetworkUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.BufferedSource
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: ChatGPTApiService,
    private val chatRepo: ChatMessageRepository,
    private val chatEntityRepo: ChatRepository,
    private val baseUrlManager: com.example.ainotes.utils.BaseUrlManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        const val DEFAULT_SYSTEM_PROMPT_KEY = "default_system_prompt"
    }

    // Читаем системный промпт из ресурсов приложения
    private val defaultSystemPrompt: String by lazy {
        context.getString(com.example.ainotes.R.string.default_system_prompt)
    }

    private val _chatMessages = MutableStateFlow<List<Message>>(emptyList())
    val chatMessages: StateFlow<List<Message>> = _chatMessages

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    private val _selectedModel = MutableStateFlow("")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _systemPrompt = MutableStateFlow(defaultSystemPrompt)

    val defaultSystemPromptValue: String = defaultSystemPrompt

    private val _selectedPrompt = MutableStateFlow<String?>(null)
    val selectedPrompt: StateFlow<String?> = _selectedPrompt.asStateFlow()

    // 1) флаг, показывает, идёт ли сейчас вывод ассистента
    private val _isAssistantWriting = MutableStateFlow(false)
    val isAssistantWriting: StateFlow<Boolean> = _isAssistantWriting.asStateFlow()

    // 2) очередь пользовательских сообщений
    private val messageQueue = Channel<String>(Channel.UNLIMITED)
    private var currentSendJob: Job? = null

    // Список доступных моделей из API
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    // Флаг загрузки моделей
    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    // Статус инициализации модели
    private val _isModelInitializing = MutableStateFlow(false)
    val isModelInitializing: StateFlow<Boolean> = _isModelInitializing.asStateFlow()

    private val _modelInitialized = MutableStateFlow(false)
    val modelInitialized: StateFlow<Boolean> = _modelInitialized.asStateFlow()

    private val _isTitleGenerating = MutableStateFlow(false)
    val isTitleGenerating: StateFlow<Boolean> = _isTitleGenerating.asStateFlow()

    // Callback для запроса создания нового чата
    private val _requestNewChat = MutableStateFlow<String?>(null)
    val requestNewChat: StateFlow<String?> = _requestNewChat.asStateFlow()

    init {
        // Consumer for queued messages
        viewModelScope.launch {
            for (input in messageQueue) {
                // Wait for any ongoing generation
                while (_isAssistantWriting.value) delay(50)
                // Check connectivity

                if (!NetworkUtils.isConnected(context)) {

                    // Try refresh ngrok URL
                    val newUrl = baseUrlManager.refreshPublicUrl()

                }
                // Launch sending
                currentSendJob = viewModelScope.launch(Dispatchers.IO) { handleSend(input) }
                currentSendJob?.join()
            }
        }

        // загрузка из БД
        viewModelScope.launch {
            _currentChatId
                .filterNotNull()
                .flatMapLatest { chatId ->
                    chatRepo.getMessagesByChatId(chatId)
                        .map { entities ->
                            entities.filter { it.contentRaw.isNotBlank() }
                                .map { entity ->
                                    Message(
                                        role = entity.role,
                                        content = entity.contentRaw,
                                        isComplete = entity.isComplete,
                                        reasoningContent = entity.reasoningContent.takeIf { it.isNotBlank() },
                                        reasoningDurationSeconds = if (entity.reasoningDurationSeconds > 0f) entity.reasoningDurationSeconds else null
                                    )
                                }
                        }
                }
                .flowOn(Dispatchers.IO)
                .collect { messages ->
                    _chatMessages.value = messages
                }
        }

        // Загрузка списка моделей
        loadAvailableModels()
    }

    fun setCurrentChatId(chatId: String?, skipLoad: Boolean = false) {


        // Синхронно очищаем сообщения и сбрасываем выбор промпта при смене чата
        if (chatId != _currentChatId.value) {
            _chatMessages.value = emptyList()
            _selectedPrompt.value = null
            _systemPrompt.value = defaultSystemPrompt
        }

        _currentChatId.value = chatId

        if (chatId != null && !skipLoad) {
            // Flow автоматически загрузит сообщения – ничего не делаем
            // Загружаем сохранённый промпт из БД для этого чата
            viewModelScope.launch {
                val chat = chatEntityRepo.getChatById(chatId)
                val savedPrompt = chat?.selectedPrompt?.takeIf { it.isNotEmpty() }
                _selectedPrompt.value = savedPrompt
                _systemPrompt.value = savedPrompt ?: defaultSystemPrompt

            }
        } else if (chatId != null && skipLoad) {
            // При создании нового чата промпт уже сброшен синхронно выше.
            // Не сохраняем старый выбор — каждый чат имеет независимый выбор промпта.
        } else {
            // chatId == null - полная очистка
            _chatMessages.value = emptyList()
            _selectedPrompt.value = null
            _systemPrompt.value = defaultSystemPrompt
        }
    }

    fun setSystemPrompt(prompt: String) {
        _systemPrompt.value = prompt
    }

    fun updateSelectedPrompt(prompt: String?) {

        _selectedPrompt.value = prompt

        // ВАЖНО: Сразу применяем системный промпт
        _systemPrompt.value = prompt ?: defaultSystemPrompt



        // Сохраняем выбранный промпт в БД для текущего чата
        val currentChatId = _currentChatId.value
        if (currentChatId != null) {
            viewModelScope.launch {
                chatEntityRepo.updateChatSelectedPrompt(currentChatId, prompt ?: "")

            }
        } else {

        }
    }

    /**
     * Получить отображаемое название модели для UI
     */
    fun getModelDisplayName(modelKey: String): String {
        return modelKey
    }

    fun setModel(model: String) {
        val oldModel = _selectedModel.value
        _selectedModel.value = model

        // Всегда переинициализируем модель при смене
        if (oldModel != model) {

            initializeModel()
        }
    }

    private fun addMessage(message: Message) {
        val currentChatId = _currentChatId.value ?: return
        val newMessage = ChatMessageEntity(
            chatId = currentChatId,
            role = message.role,
            contentRaw = message.content,
            timestamp = System.currentTimeMillis(),
            isComplete = message.isComplete,
            reasoningContent = message.reasoningContent ?: "",
            reasoningDurationSeconds = message.reasoningDurationSeconds ?: 0f
        )
        viewModelScope.launch {
            chatRepo.addMessage(newMessage)
            chatEntityRepo.updateChatLastMessage(currentChatId)
        }
        // Обновляем UI немедленно
        _chatMessages.value += message
    }

    private fun updateLastAssistantMessage(
        content: String,
        isComplete: Boolean = false,
        tokenCount: Int = 0,
        tokensPerSecond: Float = 0f,
        generationTimeMs: Long = 0L,
        reasoningContent: String? = null,
        reasoningDurationSeconds: Float? = null
    ) {
        val messages = _chatMessages.value.toMutableList()
        val idx = messages.indexOfLast { it.role == "assistant" }
        if (idx != -1) {
            messages[idx] = messages[idx].copy(
                content = content,
                isComplete = isComplete,
                tokenCount = tokenCount,
                tokensPerSecond = tokensPerSecond,
                generationTimeMs = generationTimeMs,
                reasoningContent = reasoningContent,
                reasoningDurationSeconds = reasoningDurationSeconds
            )
            _chatMessages.value = messages
        }
    }

    fun sendMessage(inputText: String) {
        val currentChatId = _currentChatId.value

        if (currentChatId == null) {

            // Сохраняем сообщение для отправки после создания чата
            _requestNewChat.value = inputText
            return
        }


        addMessage(Message(role = "user", content = inputText))
        messageQueue.trySend(inputText)

        // Проверяем, нужно ли генерировать заголовок для чата
        viewModelScope.launch {
            val chat = chatEntityRepo.getChatById(currentChatId)
            if (chat != null && !chat.isTitleGenerated) {
                // Получаем количество пользовательских сообщений в чате
                val userMessagesCount = _chatMessages.value.count { it.role == "user" }

                // Генерируем заголовок после первого сообщения пользователя
                if (userMessagesCount == 1) {

                    generateChatTitle(currentChatId, inputText)
                }
            }
        }
    }

    /**
     * Сбрасывает запрос на создание нового чата после его обработки
     */
    fun clearNewChatRequest() {
        _requestNewChat.value = null
    }

    /**
     * Генерирует краткий заголовок для чата на основе первого сообщения пользователя
     */
    private fun generateChatTitle(chatId: String, firstUserMessage: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isTitleGenerating.value = true

            try {


                // Создаем специальный промпт для генерации заголовка
                val titlePrompt = """
                    Сгенерируй короткий, ёмкий заголовок для чата на основе следующего сообщения пользователя.
                    
                    Правила:
                    - Заголовок должен быть коротким (максимум 5-7 слов)
                    - Отражать основную тему или задачу из сообщения
                    - Не использовать лишние детали, эмоции или длинные фразы
                    - Избегать бесполезных заголовков вроде «Помоги», «Вопрос», «Привет»
                    - Если тема непонятна, выбрать наиболее сильную по смыслу часть
                    - Отвечать ТОЛЬКО заголовком, без дополнительных комментариев
                    
                    Сообщение пользователя: "$firstUserMessage"
                    
                    Заголовок:
                """.trimIndent()

                val titleMessages = listOf(
                    Message(
                        "system",
                        "Ты помощник, который генерирует краткие заголовки для чатов."
                    ),
                    Message("user", titlePrompt)
                )

                val titleRequest = ChatGPTRequest(
                    model = _selectedModel.value,
                    messages = titleMessages,
                    stream = true
                )

                val gson = Gson()
                val titleBuilder = StringBuilder()

                // Используем suspend функцию вместо callback
                val response = api.sendChatMessage(titleRequest)

                if (response.isSuccessful) {
                    response.body()?.source()?.let { source ->
                        try {
                            // Читаем стрим для получения заголовка
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
                                        titleBuilder.append(chunk)
                                    }
                                }
                            }

                            // Очищаем заголовок от лишних символов и обрезаем
                            val generatedTitle = titleBuilder.toString()
                                .trim()
                                .replace(Regex("[\"'«»]"), "") // Убираем кавычки
                                .replace(Regex("\\s+"), " ") // Нормализуем пробелы
                                .take(60) // Ограничиваем длину
                                .ifEmpty { "Новый чат" }



                            // Обновляем заголовок в базе данных
                            chatEntityRepo.updateChatTitleGenerated(
                                chatId,
                                generatedTitle
                            )

                            _isTitleGenerating.value = false

                        } catch (e: IOException) {

                            _isTitleGenerating.value = false
                        }
                    }
                } else {

                    _isTitleGenerating.value = false
                }

            } catch (e: Exception) {

                _isTitleGenerating.value = false
            }
        }
    }

    fun stopGeneration() {
        // отменяем сетевой вызов
        // В suspend-стиле отмена происходит через cancel корутины
        _isAssistantWriting.value = false
        val lastContent = _chatMessages.value.lastOrNull { it.role == "assistant" }?.content.orEmpty()
        updateLastAssistantMessage(content = lastContent, isComplete = true)
        // сохраняем текущее (возможно неполное) сообщение ассистента в БД
        viewModelScope.launch {
            val currentChatId = _currentChatId.value ?: return@launch
            chatRepo.addMessage(
                ChatMessageEntity(
                    chatId = currentChatId,
                    role = "assistant",
                    contentRaw = lastContent,
                    timestamp = System.currentTimeMillis(),
                    isComplete = true
                )
            )
            chatEntityRepo.updateChatLastMessage(currentChatId)
        }
    }

    private fun handleSend(input: String) {
        val currentChatId = _currentChatId.value ?: return

        _isAssistantWriting.value = true

        // Логируем текущий системный промпт для отладки


        val allMessages = listOf(Message("system", _systemPrompt.value)) + _chatMessages.value
        val req = ChatGPTRequest(model = _selectedModel.value, messages = allMessages, stream = true)

        // Используем suspend функцию вместо callback
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = api.sendChatMessage(req)

                if (response.isSuccessful) {
                    response.body()?.source()?.let { source ->
                        try {
                            streamResponse(source, currentChatId)
                        } catch (_: IOException) {
                            // соединение было отменено — просто выходим
                        } finally {
                            _isAssistantWriting.value = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        updateLastAssistantMessage("Ошибка: ${response.code()}", isComplete = true)
                        _isAssistantWriting.value = false
                    }
                }
            } catch (e: Exception) {
                _isAssistantWriting.value = false
            }
        }
    }

    // обновлена для работы с chatId
    private suspend fun streamResponse(
        source: BufferedSource,
        chatId: String
    ) {
        val builder = StringBuilder()
        val gson = Gson()

        // Переменные для отслеживания метрик токенов (только для content, БЕЗ reasoning)
        val startTime = System.currentTimeMillis()
        var tokenCount = 0
        var contentStartTime: Long? = null // Время начала генерации content (без reasoning)
        var currentTokensPerSecond = 0f

        // Переменные для отслеживания reasoning content
        val reasoningBuilder = StringBuilder()
        var reasoningStartTime: Long? = null
        var reasoningEndTime: Long? = null
        var isReasoningPhase = false

        // Читаем строку за строкой из source
        while (!source.exhausted()) {
            val line = source.readUtf8Line().orEmpty()
            if (line.trim() == "data: [DONE]") break

            if (line.startsWith("data:")) {
                val jsonLine = line.removePrefix("data:").trim()

                // Парсим delta объект
                val deltaObject = runCatching {
                    gson.fromJson(jsonLine, JsonObject::class.java)
                        .getAsJsonArray("choices")[0]
                        .asJsonObject["delta"].asJsonObject
                }.getOrNull()

                if (deltaObject != null) {
                    // Проверяем наличие reasoning_content
                    val reasoningChunk = deltaObject.get("reasoning_content")?.asString.orEmpty()
                    if (reasoningChunk.isNotEmpty()) {
                        if (!isReasoningPhase) {
                            isReasoningPhase = true
                            reasoningStartTime = System.currentTimeMillis()

                        }
                        reasoningBuilder.append(reasoningChunk)
                        // НЕ увеличиваем tokenCount для reasoning!
                    }

                    // Проверяем наличие обычного content
                    val contentChunk = deltaObject.get("content")?.asString.orEmpty()
                    if (contentChunk.isNotEmpty()) {
                        // Если была фаза размышления и она закончилась
                        if (isReasoningPhase && reasoningEndTime == null) {
                            reasoningEndTime = System.currentTimeMillis()
                        }

                        // Засекаем время начала генерации content (БЕЗ учета reasoning)
                        if (contentStartTime == null) {
                            contentStartTime = System.currentTimeMillis()

                        }

                        builder.append(contentChunk)
                        tokenCount++ // Увеличиваем счетчик ТОЛЬКО для content

                        // Рассчитываем скорость генерации ТОЛЬКО для content
                        val currentTime = System.currentTimeMillis()
                        val contentElapsedSeconds = (currentTime - contentStartTime!!) / 1000f
                        if (contentElapsedSeconds > 0) {
                            currentTokensPerSecond = tokenCount / contentElapsedSeconds
                        }

                        // Обновляем сообщение ассистента по мере поступления текста
                        withContext(Dispatchers.Main) {
                            val reasoningDurationSeconds =
                                if (reasoningEndTime != null && reasoningStartTime != null) {
                                    (reasoningEndTime!! - reasoningStartTime!!) / 1000f
                                } else null

                            updateLastAssistantMessage(
                                content = builder.toString(),
                                isComplete = false,
                                tokenCount = tokenCount,
                                tokensPerSecond = currentTokensPerSecond,
                                generationTimeMs = currentTime - contentStartTime!!,
                                reasoningContent = reasoningBuilder.toString()
                                    .takeIf { it.isNotBlank() },
                                reasoningDurationSeconds = reasoningDurationSeconds
                            )
                        }
                    }
                }
            }
        }

        // Если размышление не завершилось естественным образом (не было content после него)
        if (isReasoningPhase && reasoningEndTime == null) {
            reasoningEndTime = System.currentTimeMillis()
        }

        // Если не было content вообще, устанавливаем contentStartTime = startTime
        if (contentStartTime == null) {
            contentStartTime = startTime
        }

        // Финальное завершение с итоговыми метриками
        val finalRaw = builder.toString()
        val finalReasoningContent = reasoningBuilder.toString().takeIf { it.isNotBlank() }
        val finalReasoningDuration = if (reasoningEndTime != null && reasoningStartTime != null) {
            (reasoningEndTime!! - reasoningStartTime!!) / 1000f
        } else null

        // Вычисляем метрики ТОЛЬКО для content (без reasoning)
        val contentGenerationTime = System.currentTimeMillis() - contentStartTime
        val finalTokensPerSecond = if (contentGenerationTime > 0) {
            (tokenCount * 1000f) / contentGenerationTime
        } else {
            0f
        }

        withContext(Dispatchers.Main) {
            updateLastAssistantMessage(
                content = builder.toString(),
                isComplete = true,
                tokenCount = tokenCount,
                tokensPerSecond = finalTokensPerSecond,
                generationTimeMs = contentGenerationTime,
                reasoningContent = finalReasoningContent,
                reasoningDurationSeconds = finalReasoningDuration
            )
        }

        // Сохраняем готовый ответ в БД
        val finalEntity = ChatMessageEntity(
            chatId = chatId,
            role = "assistant",
            contentRaw = finalRaw,
            timestamp = System.currentTimeMillis(),
            isComplete = true,
            reasoningContent = finalReasoningContent ?: "",
            reasoningDurationSeconds = finalReasoningDuration ?: 0f
        )
        chatRepo.addMessage(finalEntity)
        chatEntityRepo.updateChatLastMessage(chatId)
    }


    fun clearChat() {
        val currentChatId = _currentChatId.value ?: return
        _chatMessages.value = emptyList()
        viewModelScope.launch {
            chatRepo.deleteMessagesByChatId(currentChatId)
            chatEntityRepo.updateChatLastMessage(currentChatId)
        }
    }

    /**
     * Удаляет последнее сообщение ассистента из чата и базы данных
     */
    fun removeLastAssistantMessage() {
        val currentChatId = _currentChatId.value ?: return
        val messages = _chatMessages.value.toMutableList()
        val lastAssistantIndex = messages.indexOfLast { it.role == "assistant" }
        if (lastAssistantIndex != -1) {
            messages.removeAt(lastAssistantIndex)
            _chatMessages.value = messages
            viewModelScope.launch {
                // Удаляем последнее сообщение ассистента из БД
                val all = chatRepo.getMessagesByChatId(currentChatId).firstOrNull() ?: emptyList()
                val lastAssistant = all.filter { it.role == "assistant" }.maxByOrNull { it.timestamp }
                lastAssistant?.let { chatRepo.deleteMessage(it) }
                chatEntityRepo.updateChatLastMessage(currentChatId)
            }
        }
    }

    /**
     * Удаляет конкретное сообщение из чата и базы данных по содержимому и роли
     * Если удаляется сообщение пользователя, также удаляется следующий ответ ассистента
     */
    fun deleteMessage(messageContent: String, role: String) {
        val currentChatId = _currentChatId.value ?: return
        val messages = _chatMessages.value.toMutableList()
        val messageIndex = messages.indexOfFirst { it.content == messageContent && it.role == role }
        if (messageIndex != -1) {
            val shouldDeleteAssistantResponse = role == "user" &&
                    messageIndex + 1 < messages.size &&
                    messages[messageIndex + 1].role == "assistant"

            if (shouldDeleteAssistantResponse) {
                messages.removeAt(messageIndex + 1) // сначала удаляем ответ ассистента
                messages.removeAt(messageIndex)     // потом сообщение пользователя
            } else {
                messages.removeAt(messageIndex)
            }
            _chatMessages.value = messages

            viewModelScope.launch {
                // Удаляем из БД
                val all = chatRepo.getMessagesByChatId(currentChatId).firstOrNull() ?: emptyList()
                val userMsg = all.find { it.contentRaw == messageContent && it.role == role }
                userMsg?.let { chatRepo.deleteMessage(it) }
                if (shouldDeleteAssistantResponse) {
                    val assistantMsg = all
                        .filter { it.role == "assistant" && it.timestamp > (userMsg?.timestamp ?: 0) }
                        .minByOrNull { it.timestamp }
                    assistantMsg?.let { chatRepo.deleteMessage(it) }
                }
                chatEntityRepo.updateChatLastMessage(currentChatId)
            }
        }
    }

    /**
     * Инициализация модели при запуске приложения
     */
    private fun initializeModel() {
        // Пропускаем инициализацию, если модель еще не выбрана
        if (_selectedModel.value.isEmpty()) {

            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isModelInitializing.value = true
            _modelInitialized.value = false // Сбрасываем статус перед новой инициализацией

            try {


                // Проверяем подключение к сети
                if (!NetworkUtils.isConnected(context)) {

                    baseUrlManager.refreshPublicUrl()
                }

                // Создаем простой запрос для "разогрева" модели с коротким сообщением
                val initMessage = Message("user", "Hi")
                val initRequest = ChatGPTRequest(
                    model = _selectedModel.value,
                    messages = listOf(initMessage),
                    stream = true // Используем stream = true, так как LM Studio всегда стримит
                )



                // Используем suspend функцию вместо callback
                val response = api.sendChatMessage(initRequest)

                if (response.isSuccessful) {
                    response.body()?.source()?.let { source ->
                        try {
                            // Читаем стрим для инициализации (не сохраняем содержимое)
                            var tokenCount = 0
                            val gson = Gson()

                            while (!source.exhausted()) {
                                val line = source.readUtf8Line().orEmpty()
                                if (line.trim() == "data: [DONE]") {
                                    break
                                }

                                if (line.startsWith("data:")) {
                                    val jsonLine = line.removePrefix("data:").trim()
                                    val chunk = runCatching {
                                        gson.fromJson(jsonLine, JsonObject::class.java)
                                            .getAsJsonArray("choices")[0]
                                            .asJsonObject["delta"].asJsonObject
                                            .get("content")?.asString.orEmpty()
                                    }.getOrNull().orEmpty()

                                    if (chunk.isNotEmpty()) {
                                        tokenCount++
                                        // Каждые 100 токенов логируем прогресс
                                        if (tokenCount % 100 == 0) {
                                            // Каждые 100 токенов логируем прогресс
                                        }
                                    }
                                }
                            }

                            withContext(Dispatchers.Main) {
                                _modelInitialized.value = true
                            }

                        } catch (e: IOException) {
                            withContext(Dispatchers.Main) {
                                _modelInitialized.value = false
                            }
                        }
                    } ?: run {

                        _modelInitialized.value = false
                    }
                } else {
                    _modelInitialized.value = false
                }

                _isModelInitializing.value = false
            } catch (e: Exception) {
                _modelInitialized.value = false
                _isModelInitializing.value = false

            }
        }
    }

    /**
     * Установить модель по отображаемому названию (для UI)
     */
    fun setModelByDisplayName(displayName: String) {
        val oldModel = _selectedModel.value
        _selectedModel.value = displayName

        // Всегда переинициализируем модель при смене (даже если предыдущая не была инициализирована)
        if (oldModel != displayName) {

            initializeModel()
        }
    }

    /**
     * Повторить инициализацию текущей модели (для UI)
     */
    fun retryModelInitialization() {

        initializeModel()
    }

    /**
     * Отменить инициализацию модели (для UI)
     */
    fun cancelModelInitialization() {

        _isModelInitializing.value = false
        _modelInitialized.value = false
    }

    /**
     * Повторно отправляет запрос без дублирования сообщения пользователя
     */
    fun retryLastMessage(userMessage: String) {
        // Добавляем в очередь для обработки, но не добавляем в список сообщений
        messageQueue.trySend(userMessage)
    }

    /**
     * Загружает список доступных моделей из OpenAI API
     */
    fun loadAvailableModels() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingModels.value = true

            try {


                // Проверяем подключение к сети
                if (!NetworkUtils.isConnected(context)) {

                    baseUrlManager.refreshPublicUrl()
                }

                val response = api.getModels()

                if (response.isSuccessful) {
                    val modelsResponse = response.body()
                    if (modelsResponse != null) {
                        val models = modelsResponse.data.map { it.id }

                        withContext(Dispatchers.Main) {
                            _availableModels.value = models


                            // Не выбираем модель автоматически - пользователь должен выбрать сам

                        }
                    } else {

                    }
                } else {

                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrBlank()) {

                    }
                }

            } catch (e: Exception) {


                // Дополнительная диагностика
                when (e) {
                    is java.net.SocketTimeoutException -> {

                    }

                    is java.net.ConnectException -> {

                    }

                    is java.net.UnknownHostException -> {

                    }
                }
            } finally {
                _isLoadingModels.value = false
            }
        }
    }
}
