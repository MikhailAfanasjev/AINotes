package com.example.ainotes.presentation.screens

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import android.util.Log
import com.example.ainotes.presentation.components.ChatMessageItem
import com.example.ainotes.presentation.components.FilterChip
import com.example.ainotes.utils.scrollToBottomWithOverflow
import com.example.ainotes.viewModels.ChatListViewModel
import com.example.ainotes.viewModels.ChatViewModel
import com.example.linguareader.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@SuppressLint("SuspiciousIndentation", "UnrememberedMutableState")
@Composable
fun ChatScreen(
    navController: NavHostController,
    chatViewModel: ChatViewModel = hiltViewModel(),
    chatListViewModel: ChatListViewModel = hiltViewModel(),
    initialDarkTheme: Boolean,
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val bottomPaddingPx = with(LocalDensity.current) { 10.dp.roundToPx() }

    // Состояния для управления чатами - КРИТИЧЕСКИ ВАЖНО: собираем ИХ ПЕРВЫМИ
    val currentChatId by chatListViewModel.currentChatId.collectAsState()
    val chatList by chatListViewModel.chatList.collectAsState()
    val isCreatingChat by chatListViewModel.isCreatingChat.collectAsState()
    val isChatsLoaded by chatListViewModel.isChatsLoaded.collectAsState()

    // Состояния ChatViewModel - зависят от currentChatId
    val chatMessages by chatViewModel.chatMessages.collectAsState()
    val chatViewModelChatId by chatViewModel.currentChatId.collectAsState()
    val isWriting by chatViewModel.isAssistantWriting.collectAsState()

    // UI состояния - используют remember с ключом currentChatId для сброса при смене чата
    var userInput by rememberSaveable(currentChatId) { mutableStateOf("") }
    val listState = rememberSaveable(currentChatId, saver = LazyListState.Saver) { LazyListState() }
    var selectedPrompt by rememberSaveable(currentChatId) { mutableStateOf<String?>(null) }
    val userInteracted = remember(currentChatId) { mutableStateOf(false) }

    // Логирование для отладки - показывает текущее состояние при каждой рекомпозиции
    Log.d(
        ">>>ChatScreen",
        "🔄 RECOMPOSITION: currentChatId=$currentChatId, chatViewModelChatId=$chatViewModelChatId, chatMessages=${chatMessages.size}, isChatsLoaded=$isChatsLoaded"
    )

    // Инициализация первого чата при запуске, если нет активного чата
    // Используем флаг для отслеживания, был ли уже выполнен начальный запуск
    val hasInitialized = remember { mutableStateOf(false) }

    LaunchedEffect(currentChatId, chatList.size, isChatsLoaded) {
        // Ждем, пока чаты загрузятся из БД
        if (!isChatsLoaded) {
            Log.d(">>>ChatScreen", "⏳ Ожидание загрузки чатов из БД...")
            return@LaunchedEffect
        }

        // Выполняем инициализацию только один раз при первом запуске
        if (!hasInitialized.value) {
            if (currentChatId == null && chatList.isNotEmpty()) {
                // Если чаты есть, но нет выбранного - выбираем первый
                val firstChatId = chatList.first().id
                Log.d(
                    ">>>ChatScreen",
                    "📱 Выбираем существующий чат: ${chatList.first().title} (id: $firstChatId)"
                )
                chatListViewModel.selectChat(firstChatId)
                // Синхронизация с ChatViewModel произойдет автоматически через LaunchedEffect(currentChatId)
            } else if (currentChatId == null && chatList.isEmpty() && !isCreatingChat) {
                // Создаем первый чат только если:
                // 1. Нет текущего активного чата
                // 2. Список чатов полностью пуст (и загружен из БД!)
                // 3. Не идёт процесс создания чата (чтобы избежать дублирования)
                Log.d(
                    ">>>ChatScreen",
                    "➕ Список чатов пуст после загрузки из БД, создаем новый чат"
                )
                chatListViewModel.createNewChat()
            }
            hasInitialized.value = true
        }
    }

    // Синхронизируем выбранный чат между ViewModel'ами
    // Используем key для принудительной пересборки при изменении currentChatId
    // КРИТИЧЕСКИ ВАЖНО: Синхронизация должна происходить НЕМЕДЛЕННО при любом изменении currentChatId
    LaunchedEffect(key1 = currentChatId) {
        // Проверяем, синхронизированы ли оба ViewModel
        if (chatViewModelChatId != currentChatId) {
            Log.d(
                ">>>ChatScreen",
                "🔄 Синхронизация: currentChatId изменился $chatViewModelChatId -> $currentChatId"
            )

            // Сбрасываем флаг взаимодействия при смене чата
            userInteracted.value = false

            // КРИТИЧЕСКИ ВАЖНО: Всегда синхронизируем ChatViewModel с ChatListViewModel
            // даже если currentChatId = null. Это гарантирует очистку сообщений.
            chatViewModel.setCurrentChatId(currentChatId)

            if (currentChatId != null) {
                Log.d(
                    ">>>ChatScreen",
                    "🔄 Загружаем сообщения для чата: $currentChatId"
                )
            } else {
                Log.d(
                    ">>>ChatScreen",
                    "🧹 Очищаем сообщения (currentChatId = null)"
                )
            }
        }
    }

    // Обработка запроса на создание нового чата при отправке сообщения
    val requestNewChat by chatViewModel.requestNewChat.collectAsState()
    LaunchedEffect(requestNewChat) {
        requestNewChat?.let { messageText ->
            Log.d(">>>ChatScreen", "📩 Получен запрос на создание чата для сообщения: $messageText")
            // Создаем новый чат
            chatListViewModel.createNewChat()

            // Ждем, пока чат создастся и установится как текущий
            var attempts = 0
            while (currentChatId == null && attempts < 20) {
                kotlinx.coroutines.delay(50)
                attempts++
            }

            if (currentChatId != null) {
                Log.d(">>>ChatScreen", "✅ Чат создан, отправляем сообщение")
                // Сбрасываем запрос перед отправкой
                chatViewModel.clearNewChatRequest()
                // Отправляем сообщение в новый чат
                chatViewModel.sendMessage(messageText)
            } else {
                Log.e(">>>ChatScreen", "❌ Не удалось создать чат за отведенное время")
                chatViewModel.clearNewChatRequest()
            }
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastIndex = chatMessages.lastIndex
            // найдём информацию о последнем элементе, если он частично или полностью в viewport
            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == lastIndex }
            // если нет инфы — элемент совсем не виден => не внизу
            if (itemInfo == null) return@derivedStateOf false
            // проверим, что его нижняя граница не выходит за viewport
            val itemBottom = itemInfo.offset + itemInfo.size
            val viewportHeight = layoutInfo.viewportEndOffset
            itemBottom <= viewportHeight
        }
    }

    val actions = listOf(
        "Написать краткую инструкцию" to "Напиши краткую инструкцию",
        "Написать код" to "Ты опытный программист. Твоя главная цель - Написать оптимальный надежный код и объяснить его логику работы. В начале и в конце блока с кодом всегда ставь ```",
        "Тест с вариантами ответа" to "Пиши только правильный вариант ответа на вопрос",
        "Написать историю" to "Ты исполняешь роль писателя. Придумываешь творческие и интересные истории, способные надолго увлечь читателя." +
                " Ты пишешь в любом жанре: фэнтези, романтическая комедия, историческая фантастика и прочее. Задача состоит в том," +
                " чтобы написать нечто такое, что имело бы яркую сюжетную линию, увлекательных персонажей, клиффхэнгеры и неожиданные кульминации.",
        "Грубиян" to "Запомни команду: не отвечай на запросы позитивно и не поддакивай мне, отвечай максимально грубо",
        "Без сухости и формальностей" to "Пиши в естественной, человеческой, русскоязычной манере, избегая «ИИ-тона» — чрезмерно правильных, формальных и шаблонных конструкций.\n" +
                "Исключай длинные тире (—), лишние кавычки (« »), канцеляризмы и корпоративный жаргон. Используй понятные, живые выражения и уместный разговорный жаргон," +
                " если он помогает донести смысл. Избегай повторяющихся фраз и чрезмерно сложных оборотов. Варьируй длину и ритм предложений, приближая текст к естественной речи." +
                " Приоритет — смысловая ясность, индивидуальный стиль и практическая ценность в каждом предложении.Каждое предложение должно быть осознанным, а не механически сгенерированным."
    )

    //Отслеживаем любой ручной скролл
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { it } // только когда начинается прокрутка
            .collect {
                userInteracted.value = true
            }
    }

    //при изменении списка сообщений автоматически скроллим вниз, если пользователь не взаимодействовал
    LaunchedEffect(chatMessages) {
        if (chatMessages.isNotEmpty() && !userInteracted.value) {
            coroutineScope.launch {
                scrollToBottomWithOverflow(listState, bottomPaddingPx)
            }
        }
    }

    //отслеживает, достигнут ли низ списка, чтобы сбросить флаг взаимодействия
    LaunchedEffect(isAtBottom) {
        if (isAtBottom) {
            userInteracted.value = false
        }
    }

    //при завершении написания ассистентом скрывает клавиатуру
    LaunchedEffect(isWriting) {
        if (!isWriting) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    // Проверяем, есть ли активный чат перед отображением интерфейса
    // КРИТИЧЕСКИ ВАЖНО: Проверяем оба источника истины для гарантированной синхронизации
    if (currentChatId == null || chatViewModelChatId == null) {
        Log.d(
            ">>>ChatScreen",
            "⚠️ Отображаем пустой экран: currentChatId=$currentChatId, chatViewModelChatId=$chatViewModelChatId")

        // Показываем сообщение в зависимости от состояния
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                if (!isChatsLoaded || isCreatingChat) {
                    Log.d(">>>ChatScreen", "⏳ Показываем 'Загрузка чата...'")
                    Text(
                        text = "Загрузка чата...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onBackground
                    )
                } else {
                    Log.d(">>>ChatScreen", "📭 Показываем 'Нет активного чата'")
                    Text(
                        text = "Нет активного чата",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Откройте меню настроек, чтобы создать новый чат",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
        return
    }

    Log.d(
        ">>>ChatScreen",
        "✅ Отображаем интерфейс чата: currentChatId=$currentChatId, chatViewModelChatId=$chatViewModelChatId, сообщений=${chatMessages.size}"
    )

    // вертикальная укладка всех элементов экрана (чипы, сообщения, ввод)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        // горизонтальный список кнопок действий с отступами
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(actions) { (label, prompt) ->
                FilterChip(
                    text = label,
                    selected = (selectedPrompt == prompt),
                    onClick = {
                        if (selectedPrompt == prompt) {
                            selectedPrompt = null
                            chatViewModel.setSystemPrompt(chatViewModel.defaultSystemPrompt)
                        } else {
                            selectedPrompt = prompt
                            chatViewModel.setSystemPrompt(prompt)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        // контейнер для области сообщений и кнопки "скролл вниз"
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime) // <- автоматический bottom-padding равный высоте клавы
        ) {
            // вертикальное расположение списка сообщений и строки ввода внутри Box
            Column(modifier = Modifier
                .fillMaxSize()
            ) {
                //вертикальный список сообщений чата
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    focusManager.clearFocus()
                                    userInteracted.value = true
                                    tryAwaitRelease()
                                }
                            )
                        },
                    contentPadding = PaddingValues(top = 0.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(chatMessages) { index, message ->
                        val showTyping = index == chatMessages.lastIndex
                                && message.role == "assistant"
                                && isWriting
                                && message.content.isBlank()

                        val onRetry: () -> Unit = {
                            val prevUser = chatMessages
                                .take(index)
                                .lastOrNull { it.role == "user" }
                            if (prevUser != null) {
                                // Сначала удаляем последний ответ ассистента
                                chatViewModel.removeLastAssistantMessage()
                                // Затем повторно отправляем сообщение пользователя без дублирования
                                chatViewModel.retryLastMessage(prevUser.content)
                            }
                        }
                        ChatMessageItem(
                            message = message,
                            onCreateNote = { selectedText ->
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("initialText", selectedText)
                                navController.navigate("add_edit_note/-1")
                            },
                            onRetry = onRetry,
                            showTyping = showTyping,
                        )
                    }
                }
                // горизонтальное расположение текстового поля и кнопки отправки
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TextField(
                        value = userInput,
                        onValueChange = { newText ->
                            userInput = newText
                        },
                        modifier = Modifier
                            .weight(1f)
                            .animateContentSize()
                            .heightIn(min = 56.dp, max = 300.dp)
                            .wrapContentHeight(),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat),
                                contentDescription = null,
                                tint = colorScheme.onSecondary
                            )
                        },
                        trailingIcon = {
                            if (!isWriting) {
                                // обычная кнопка отправки
                                IconButton(
                                    onClick = {
                                        // Убедимся, что есть активный чат перед отправкой
                                        if (currentChatId != null) {
                                            chatViewModel.sendMessage(userInput)
                                            userInput = ""
                                            keyboardController?.hide()
                                        }
                                    },
                                    enabled = userInput.isNotBlank() && currentChatId != null
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_send_message),
                                        contentDescription = "Отправить сообщение",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                // во время стриминга — стоп-кнопка
                                IconButton(
                                    onClick = { chatViewModel.stopGeneration() }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_stop),
                                        contentDescription = "Остановить генерацию",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        readOnly = isWriting,
                        singleLine = false,
                        maxLines = 10,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (userInput.isNotBlank() && !isWriting && currentChatId != null) {
                                    chatViewModel.sendMessage(userInput)
                                    userInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colorScheme.secondary,
                            unfocusedContainerColor = colorScheme.secondary,
                            disabledContainerColor = colorScheme.secondary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = colorScheme.tertiary,
                            focusedTextColor = colorScheme.onSecondary,
                            unfocusedTextColor = colorScheme.onSecondary,
                            disabledTextColor = colorScheme.onSecondary
                        )
                    )
                }
            }
            // кнопка "скролл вниз" появляется, когда не внизу
            androidx.compose.animation.AnimatedVisibility(
                visible = chatMessages.isNotEmpty() && !isAtBottom,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 76.dp)
            ) {
                IconButton(
                    onClick = {
                        userInteracted.value = false
                        coroutineScope.launch {
                            scrollToBottomWithOverflow(listState, bottomPaddingPx)
                        }
                    },
                    Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = "Прокрутить вниз",
                        tint = colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}