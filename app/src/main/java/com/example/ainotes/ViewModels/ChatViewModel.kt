package com.example.ainotes.viewModels

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
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.text.StringBuilder

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: ChatGPTApiService,
    private val chatRepo: ChatMessageRepository,
    private val chatEntityRepo: ChatRepository,
    private val baseUrlManager: com.example.ainotes.utils.BaseUrlManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "Пиши ответы на русском языке"
        private const val TAG = ">>>ChatViewModel"
    }

    private val _chatMessages = MutableStateFlow<List<Message>>(emptyList())
    val chatMessages: StateFlow<List<Message>> = _chatMessages

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    private val _selectedModel = MutableStateFlow("openai/gpt-oss-20b")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _systemPrompt = MutableStateFlow(DEFAULT_SYSTEM_PROMPT)

    val defaultSystemPrompt: String = DEFAULT_SYSTEM_PROMPT
    private var currentCall: Call<ResponseBody>? = null
    private var initializationCall: Call<ResponseBody>? = null

    // 1) флаг, показывает, идёт ли сейчас вывод ассистента
    private val _isAssistantWriting = MutableStateFlow(false)
    val isAssistantWriting: StateFlow<Boolean> = _isAssistantWriting.asStateFlow()

    // 2) очередь пользовательских сообщений
    private val messageQueue = Channel<String>(Channel.UNLIMITED)
    private var currentSendJob: Job? = null

    // Карта моделей: краткое название -> полное с описанием
    private val modelDisplayNames = mapOf(
        "openai/gpt-oss-20b" to "openai/gpt-oss-20b (сбалансированная скорость и точность)",
        "unsloth/gpt-oss-120b" to "openai/gpt-oss-120b (низкая скорость, очень высокая точность)",
        "grok-3-gemma3-4b-distilled" to "grok-3-gemma3-4b-distilled (высокая скорость, низкая точность)",
        "grok-3-gemma3-12b-distilled" to "grok-3-gemma3-12b-distilled (сбалансированная скорость и точность)"
    )

    val availableModels = modelDisplayNames.values.toList()

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
                    Log.w(TAG, "⚠️ Нет сети – пробуем обновить Ngrok URL")
                    // Try refresh ngrok URL
                    val newUrl = baseUrlManager.refreshPublicUrl()
                    Log.d(TAG, "🔄 refreshPublicUrl() вернул $newUrl; текущий baseUrl: ${baseUrlManager.getBaseUrl()}")
                }
                // Launch sending
                currentSendJob = viewModelScope.launch(Dispatchers.IO) { handleSend(input) }
                currentSendJob?.join()
            }
        }

        // загрузка из БД
        viewModelScope.launch {
            val persisted = chatRepo.getAllMessages()
                // фильтруем пустые
                .filter { it.contentRaw.isNotBlank() }
                // конвертируем каждую строку в String
                .map { entity ->
                    Message(
                        role = entity.role,
                        content = entity.contentRaw,
                        isComplete = entity.isComplete
                    )
                }
            _chatMessages.value = persisted
        }

        // Инициализация модели при запуске
        initializeModel()
    }

    fun setCurrentChatId(chatId: String?) {
        Log.d(TAG, "📝 setCurrentChatId вызван: $chatId (текущий: ${_currentChatId.value})")

        // КРИТИЧЕСКИ ВАЖНО: Последовательность операций строго синхронизирована
        // Шаг 1: Очищаем сообщения СИНХРОННО (если чат меняется)
        if (chatId != _currentChatId.value) {
            Log.d(TAG, "🧹 Шаг 1: Немедленно (синхронно) очищаем сообщения")
            _chatMessages.value = emptyList()
        }

        // Шаг 2: Обновляем currentChatId СИНХРОННО
        Log.d(TAG, "📝 Шаг 2: Обновляем currentChatId: ${_currentChatId.value} -> $chatId")
        _currentChatId.value = chatId

        // Шаг 3: Загружаем сообщения АСИНХРОННО (только если чат выбран)
        if (chatId != null) {
            Log.d(TAG, "📂 Шаг 3: Запускаем асинхронную загрузку сообщений для чата: $chatId")
            loadMessagesForChat(chatId)
        } else {
            Log.d(TAG, "✅ Шаг 3: Чат не выбран (null), сообщения уже очищены")
            // Дополнительная гарантия: еще раз убеждаемся, что сообщения точно пусты
            _chatMessages.value = emptyList()
        }

        Log.d(
            TAG,
            "✅ setCurrentChatId завершен: currentChatId=${_currentChatId.value}, messages=${_chatMessages.value.size}"
        )
    }

    private fun loadMessagesForChat(chatId: String) {
        viewModelScope.launch {
            val persisted = chatRepo.getMessagesByChatId(chatId)
                .filter { it.contentRaw.isNotBlank() }
                .map { entity ->
                    Message(
                        role = entity.role,
                        content = entity.contentRaw,
                        isComplete = entity.isComplete
                    )
                }
            _chatMessages.value = persisted
        }
    }

    fun setSystemPrompt(prompt: String) {
        _systemPrompt.value = prompt
    }

    /**
     * Получить отображаемое название модели для UI
     */
    fun getModelDisplayName(modelKey: String): String {
        return modelDisplayNames[modelKey] ?: modelKey
    }

    /**
     * Получить краткое название модели из отображаемого названия
     */
    private fun getModelKeyFromDisplayName(displayName: String): String {
        return modelDisplayNames.entries.find { it.value == displayName }?.key ?: displayName
    }

    fun setModel(model: String) {
        val modelKey = getModelKeyFromDisplayName(model)
        val oldModel = _selectedModel.value
        _selectedModel.value = modelKey

        // Всегда переинициализируем модель при смене
        if (oldModel != modelKey) {
            Log.d(TAG, "🔄 Смена модели через setModel: $oldModel -> $modelKey")
            initializeModel()
        }
    }

    private fun addMessage(message: Message) {
        val currentChatId = _currentChatId.value ?: return

        _chatMessages.value += message
        viewModelScope.launch {
            chatRepo.addMessage(
                ChatMessageEntity(
                    chatId = currentChatId,
                    role = message.role,
                    contentRaw = message.content,
                    timestamp = System.currentTimeMillis(),
                    isComplete = true
                )
            )

            // Обновляем информацию о чате
            chatEntityRepo.updateChatLastMessage(currentChatId)
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
        val currentChatId = _currentChatId.value

        if (currentChatId == null) {
            Log.w(TAG, "⚠️ Попытка отправить сообщение без активного чата - запрашиваем создание нового")
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
                    Log.d(TAG, "🎯 Первое сообщение пользователя - запускаем генерацию заголовка")
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
                Log.d(TAG, "🎯 Начинаем генерацию заголовка для чата: $chatId")

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

                val titleCall = api.sendChatMessageCall(titleRequest)
                val gson = Gson()
                val titleBuilder = StringBuilder()

                titleCall.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.source()?.let { source ->
                                viewModelScope.launch(Dispatchers.IO) {
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

                                        Log.d(TAG, "✅ Сгенерирован заголовок: $generatedTitle")

                                        // Обновляем заголовок в базе данных
                                        chatEntityRepo.updateChatTitleGenerated(
                                            chatId,
                                            generatedTitle
                                        )

                                        _isTitleGenerating.value = false

                                    } catch (e: IOException) {
                                        Log.e(TAG, "❌ Ошибка при чтении стрима заголовка", e)
                                        _isTitleGenerating.value = false
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "⚠️ Ошибка генерации заголовка: ${response.code()}")
                            _isTitleGenerating.value = false
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e(TAG, "❌ Исключение при генерации заголовка", t)
                        _isTitleGenerating.value = false
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "❌ Общее исключение при генерации заголовка", e)
                _isTitleGenerating.value = false
            }
        }
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
                                streamResponse(source, gson, builder, currentChatId)
                            } catch (_: IOException) {
                                // соединение было отменено — просто выходим
                            } finally {
                                _isAssistantWriting.value = false
                            }
                        }
                    }
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
                        updateLastAssistantMessage("Ошибка: ${response.code()}", isComplete = true)
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

    // обновлена для работы с chatId
    private suspend fun streamResponse(
        source: BufferedSource,
        gson: Gson,
        builder: StringBuilder,
        chatId: String
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
                    // убираем cleanResponse - передаем исходный markdown
                    withContext(Dispatchers.Main) {
                        // обновляем сообщение ассистента по мере поступления текста
                        updateLastAssistantMessage(builder.toString(), isComplete = false)
                    }
                }
            }
        }

        // Финальное завершение
        val finalRaw = builder.toString()
        withContext(Dispatchers.Main) {
            updateLastAssistantMessage(builder.toString(), isComplete = true)
        }

        // Сохраняем готовый ответ в БД
        chatRepo.addMessage(
            ChatMessageEntity(
                chatId = chatId,
                role = "assistant",
                contentRaw = finalRaw,
                timestamp = System.currentTimeMillis(),
                isComplete = true
            )
        )

        // Обновляем информацию о чате
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

            // Также удаляем из базы данных
            viewModelScope.launch {
                // Получаем все сообщения из БД и удаляем последнее сообщение ассистента
                val allMessages = chatRepo.getMessagesByChatId(currentChatId)
                val lastAssistantMessage = allMessages
                    .filter { it.role == "assistant" }
                    .maxByOrNull { it.timestamp }

                lastAssistantMessage?.let { message ->
                    chatRepo.deleteMessage(message)
                }

                chatEntityRepo.updateChatLastMessage(currentChatId)
            }
        }
    }

    /**
     * Инициализация модели при запуске приложения
     */
    private fun initializeModel() {
        viewModelScope.launch(Dispatchers.IO) {
            _isModelInitializing.value = true
            _modelInitialized.value = false // Сбрасываем статус перед новой инициализацией

            try {
                Log.d(TAG, "🚀 Инициализация модели: ${_selectedModel.value}")

                // Проверяем подключение к сети
                if (!NetworkUtils.isConnected(context)) {
                    Log.w(TAG, "⚠️ Нет подключения к интернету при инициализации модели")
                    baseUrlManager.refreshPublicUrl()
                }

                // Создаем простой запрос для "разогрева" модели с коротким сообщением
                val initMessage = Message("user", "Hi")
                val initRequest = ChatGPTRequest(
                    model = _selectedModel.value,
                    messages = listOf(initMessage),
                    stream = true // Используем stream = true, так как LM Studio всегда стримит
                )

                Log.d(TAG, "📡 Отправляем запрос инициализации для ${_selectedModel.value}")

                // Используем асинхронный вызов для обработки стрима
                val call = api.sendChatMessageCall(initRequest)
                initializationCall = call

                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        viewModelScope.launch(Dispatchers.IO) {
                            Log.d(TAG, "📶 Ответ сервера: код ${response.code()}")

                            if (response.isSuccessful) {
                                response.body()?.source()?.let { source ->
                                    try {
                                        // Читаем стрим для инициализации (не сохраняем содержимое)
                                        var tokenCount = 0
                                        val gson = Gson()

                                        while (!source.exhausted()) {
                                            val line = source.readUtf8Line().orEmpty()
                                            if (line.trim() == "data: [DONE]") {
                                                Log.d(
                                                    TAG,
                                                    "✅ Стрим инициализации завершен. Получено токенов: $tokenCount"
                                                )
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
                                                        Log.d(
                                                            TAG,
                                                            "🔄 Инициализация: получено $tokenCount токенов..."
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        withContext(Dispatchers.Main) {
                                            _modelInitialized.value = true
                                            Log.d(
                                                TAG,
                                                "✅ Модель ${_selectedModel.value} успешно инициализирована"
                                            )
                                        }

                                    } catch (e: IOException) {
                                        Log.e(TAG, "❌ Ошибка чтения стрима инициализации", e)
                                        withContext(Dispatchers.Main) {
                                            _modelInitialized.value = false
                                        }
                                    }
                                } ?: run {
                                    Log.w(TAG, "⚠️ Пустое тело ответа при инициализации")
                                    _modelInitialized.value = false
                                }
                            } else {
                                Log.w(
                                    TAG,
                                    "⚠️ Ошибка инициализации модели ${_selectedModel.value}: код ${response.code()}"
                                )

                                // Попробуем получить тело ошибки для диагностики
                                val errorBody = response.errorBody()?.string()
                                if (!errorBody.isNullOrBlank()) {
                                    Log.w(TAG, "📄 Тело ошибки: $errorBody")
                                }
                                _modelInitialized.value = false
                            }

                            _isModelInitializing.value = false
                            Log.d(
                                TAG,
                                "🏁 Завершена инициализация модели ${_selectedModel.value}. Успех: ${_modelInitialized.value}"
                            )
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e(
                            TAG,
                            "❌ Исключение при инициализации модели ${_selectedModel.value}",
                            t
                        )

                        // Дополнительная диагностика
                        when (t) {
                            is java.net.SocketTimeoutException -> {
                                Log.e(
                                    TAG,
                                    "⏱️ Таймаут при инициализации модели - возможно модель требует больше времени на загрузку"
                                )
                            }

                            is java.net.ConnectException -> {
                                Log.e(TAG, "🔌 Ошибка подключения к серверу")
                            }

                            is java.net.UnknownHostException -> {
                                Log.e(TAG, "🌐 Неизвестный хост - проверьте URL сервера")
                            }
                        }

                        _modelInitialized.value = false
                        _isModelInitializing.value = false
                        Log.d(
                            TAG,
                            "🏁 Завершена инициализация с ошибкой для модели ${_selectedModel.value}"
                        )
                    }
                })

            } catch (e: Exception) {
                _modelInitialized.value = false
                _isModelInitializing.value = false
                Log.e(TAG, "❌ Общее исключение при инициализации модели ${_selectedModel.value}", e)
            }
        }
    }

    /**
     * Установить модель по отображаемому названию (для UI)
     */
    fun setModelByDisplayName(displayName: String) {
        val modelKey = getModelKeyFromDisplayName(displayName)
        val oldModel = _selectedModel.value
        _selectedModel.value = modelKey

        // Всегда переинициализируем модель при смене (даже если предыдущая не была инициализирована)
        if (oldModel != modelKey) {
            Log.d(TAG, "🔄 Смена модели: $oldModel -> $modelKey")
            initializeModel()
        }
    }

    /**
     * Повторить инициализацию текущей модели (для UI)
     */
    fun retryModelInitialization() {
        Log.d(TAG, "🔄 Повторная инициализация модели по запросу пользователя")
        initializeModel()
    }

    /**
     * Отменить инициализацию модели (для UI)
     */
    fun cancelModelInitialization() {
        Log.d(TAG, "❌ Отмена инициализации модели по запросу пользователя")
        initializationCall?.cancel()
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
}