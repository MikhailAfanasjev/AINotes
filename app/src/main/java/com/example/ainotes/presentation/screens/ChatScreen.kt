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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.ainotes.presentation.components.ChatMessageItem
import com.example.ainotes.presentation.components.FilterChip
import com.example.ainotes.presentation.ui.theme.White
import com.example.ainotes.utils.scrollToBottomWithOverflow
import com.example.ainotes.viewModels.ChatListViewModel
import com.example.ainotes.viewModels.ChatViewModel
import com.example.ainotes.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("SuspiciousIndentation", "UnrememberedMutableState")
@Composable
fun ChatScreen(
    navController: NavHostController,
    chatViewModel: ChatViewModel = hiltViewModel(),
    chatListViewModel: ChatListViewModel = hiltViewModel(),
    initialDarkTheme: Boolean,
    onOpenSettings: () -> Unit = {}
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
    val selectedModel by chatViewModel.selectedModel.collectAsState()
    val modelInitialized by chatViewModel.modelInitialized.collectAsState()
    val selectedPrompt by chatViewModel.selectedPrompt.collectAsState()

    // UI состояния - используют remember с ключом currentChatId для сброса при смене чата
    var userInput by rememberSaveable(currentChatId) { mutableStateOf("") }
    val listState = rememberSaveable(currentChatId, saver = LazyListState.Saver) { LazyListState() }
    val userInteracted = remember(currentChatId) { mutableStateOf(false) }

    // Флаг для предотвращения автоматической синхронизации при создании нового чата
    val isCreatingNewChatWithMessage = remember { mutableStateOf(false) }



    // Инициализация первого чата при запуске, если нет активного чата
    // Используем флаг для отслеживания, был ли уже выполнен начальный запуск
    val hasInitialized = remember { mutableStateOf(false) }

    LaunchedEffect(currentChatId, chatList.size, isChatsLoaded) {
        // Ждем, пока чаты загрузятся из БД
        if (!isChatsLoaded) {
            return@LaunchedEffect
        }

        // Выполняем инициализацию только один раз при первом запуске
        if (!hasInitialized.value) {
            if (currentChatId == null && chatList.isNotEmpty()) {
                // Если чаты есть, но нет выбранного - выбираем первый
                val firstChatId = chatList.first().id
                chatListViewModel.selectChat(firstChatId)
                // Синхронизация с ChatViewModel произойдет автоматически через LaunchedEffect(currentChatId)
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
            // Пропускаем автоматическую синхронизацию, если идет создание нового чата с сообщением
            if (isCreatingNewChatWithMessage.value) {
                return@LaunchedEffect
            }

            // Сбрасываем флаг взаимодействия при смене чата
            userInteracted.value = false

            // КРИТИЧЕСКИ ВАЖ��О: Всегда синхронизируем ChatViewModel с ChatListViewModel
            // даже если currentChatId = null. Это гарантирует очистку сообщений.
            // ВАЖНО: Эта синхронизация загружает сообщения из БД (skipLoad=false по умолчанию)
            chatViewModel.setCurrentChatId(currentChatId)
        }
    }

    // Обработка запроса на создание нового чата при отправке сообщения
    val requestNewChat by chatViewModel.requestNewChat.collectAsState()
    LaunchedEffect(requestNewChat) {
        requestNewChat?.let { messageText ->

            // Устанавливаем флаг, чтобы предотвратить автоматическую синхронизацию
            isCreatingNewChatWithMessage.value = true

            // Запоминаем старый chatId, чтобы дождаться изменения
            val oldChatId = currentChatId

            // Создаем новый чат
            chatListViewModel.createNewChat()

            // Ждем, пока currentChatId ИЗМЕНИТСЯ (станет другим, не null)
            chatListViewModel.currentChatId
                .first { it != null && it != oldChatId }
                .let { newChatId ->

                    // Синхронизируем ChatViewModel с новым чатом БЕЗ загрузки сообщений
                    // (так как чат только что создан и пуст)
                    chatViewModel.setCurrentChatId(newChatId, skipLoad = true)

                    // Сбрасываем запрос перед отправкой
                    chatViewModel.clearNewChatRequest()

                    // Отправляем сообщение в новый чат
                    chatViewModel.sendMessage(messageText)

                    // Сбрасываем флаг после отправки сообщения
                    isCreatingNewChatWithMessage.value = false
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
                            // Сбрасываем выбор - updateSelectedPrompt сам применит дефолтный промпт
                            chatViewModel.updateSelectedPrompt(null)
                        } else {
                            // Выбираем промпт - updateSelectedPrompt сам применит его
                            chatViewModel.updateSelectedPrompt(prompt)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                //вертикальный список сообщений чата
                // Проверяем, выбрана ли модель
                if (selectedModel.isEmpty()) {
                    // Модель не выбрана - показываем сообщение
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Spacer(modifier = Modifier.weight(1f))

                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = null,
                                tint = colorScheme.tertiary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = stringResource(R.string.activate_model),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = colorScheme.onSecondary
                                )
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = stringResource(R.string.activate_model_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSecondary.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Кнопка открытия настроек
                            val textMeasurer = rememberTextMeasurer()
                            val buttonText = stringResource(R.string.select_model_button)
                            val textStyle = MaterialTheme.typography.titleMedium

                            // Измеряем ширину текста
                            val textLayoutResult = textMeasurer.measure(
                                text = buttonText,
                                style = textStyle
                            )

                            // Вычисляем необходимую ширину: текст + иконка + отступы
                            val iconWidth = 20.dp
                            val spacerWidth = 8.dp
                            val buttonPadding = 32.dp // внутренние отступы кнопки
                            val textWidth =
                                with(LocalDensity.current) { textLayoutResult.size.width.toDp() }
                            val requiredWidth = textWidth + iconWidth + spacerWidth + buttonPadding
                            val availableWidth = LocalDensity.current.run {
                                (LocalConfiguration.current.screenWidthDp * 0.6f).dp
                            }

                            // Если текст не влезает в одну строку, увеличиваем высоту
                            val buttonHeight = if (requiredWidth > availableWidth) 80.dp else 64.dp

                            androidx.compose.material3.Button(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(buttonHeight),
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.onTertiary,
                                    contentColor = White
                                )
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_settings),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buttonText,
                                    style = textStyle,
                                    color = White,
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                } else if (currentChatId == null && chatMessages.isEmpty()) {
                    // Модель выбрана, но нет активного чата
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (!isChatsLoaded || isCreatingChat) {
                                Text(
                                    text = stringResource(R.string.chat_loading),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorScheme.onBackground
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.start_new_chat),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.start_new_chat_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
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
                                onEditMessage = if (message.role == "user") {
                                    { messageContent ->
                                        // Копируем текст в поле ввода
                                        userInput = messageContent
                                        // Удаляем сообщение из чата и БД
                                        chatViewModel.deleteMessage(messageContent, "user")
                                    }
                                } else null,
                                showTyping = showTyping,
                            )
                        }
                    }
                }
                // горизонтальное расположение текстового поля и кнопки отправки
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
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
                                text = if (selectedModel.isEmpty()) "Выберите модель в настройках" else stringResource(R.string.message),
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
                                        if (userInput.isNotBlank() && selectedModel.isNotEmpty()) {
                                            // Просто отправляем сообщение - ChatViewModel сам обработает
                                            // случай отсутствия активного чата через requestNewChat
                                            chatViewModel.sendMessage(userInput)
                                            userInput = ""
                                            keyboardController?.hide()
                                        }
                                    },
                                    enabled = userInput.isNotBlank() && selectedModel.isNotEmpty()
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_send_message),
                                        contentDescription = stringResource(R.string.send_message),
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
                                        contentDescription = stringResource(R.string.stop_generation),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        readOnly = selectedModel.isEmpty(),
                        singleLine = false,
                        maxLines = 10,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (userInput.isNotBlank() && !isWriting && selectedModel.isNotEmpty()) {
                                    // Просто отправляем сообщение - ChatViewModel сам обработает
                                    // случай отсутствия активного чата через requestNewChat
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
                        contentDescription = stringResource(R.string.scroll_down),
                        tint = colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}