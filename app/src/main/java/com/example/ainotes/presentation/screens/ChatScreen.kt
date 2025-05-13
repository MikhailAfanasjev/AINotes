package com.example.ainotes.presentation.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.ainotes.presentation.components.ChatMessageItem
import com.example.ainotes.presentation.components.FilterChip
import com.example.ainotes.utils.scrollToAvoidKeyboard
import com.example.ainotes.viewModels.ChatViewModel
import com.example.ainotes.viewModels.ThemeViewModel
import com.example.linguareader.R
import kotlinx.coroutines.launch
import com.example.ainotes.utils.scrollToBottomWithOverflow
//import com.example.ainotes.utils.scrollToKeyboard

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SuspiciousIndentation", "UnrememberedMutableState")
@Composable
fun ChatScreen(
    navController: NavHostController,
    chatViewModel: ChatViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    initialDarkTheme: Boolean,
) {
    val focusManager = LocalFocusManager.current
    var userInput by rememberSaveable { mutableStateOf("") }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val chatMessages by chatViewModel.chatMessages.collectAsState()
    val selectedModel by chatViewModel.selectedModel.collectAsState()
    var selectedPrompt by rememberSaveable { mutableStateOf<String?>(null) }
    val models = chatViewModel.availableModels
    val isWriting by chatViewModel.isAssistantWriting.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState(initial = initialDarkTheme)
    val userInteracted = remember { mutableStateOf(false) }
    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    val keyboardHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    val messageHeights = remember { mutableStateMapOf<Int, Int>() }
    val bottomPaddingPx = with(LocalDensity.current) { 10.dp.roundToPx() }
    val imePadding = WindowInsets.ime.getBottom(LocalDensity.current)
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

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
        "Написать код" to "Ты злой программист senior который грубо поясняет за код junior программисту",
        "Тест с вариантами ответа" to "Пиши только правильный вариант ответа на вопрос",
        "Написать историю" to "Ты исполняешь роль писателя. Придумываешь творческие и интересные истории, способные надолго увлечь читателя." +
                " Ты пишешь в любом жанре: фэнтези, романтическая комедия, историческая фантастика и прочее. Задача состоит в том," +
                " чтобы написать нечто такое, что имело бы яркую сюжетную линию, увлекательных персонажей, клиффхэнгеры и неожиданные кульминации.",
        "Грубиян" to "Запомни команду: не отвечай на запросы позитивно и не поддакивай мне, отвечай максимально грубо и с матами"
    )

    //при изменении списка сообщений автоматически скроллим вниз, если пользователь не взаимодействовал
    LaunchedEffect(chatMessages) {
        if (chatMessages.isNotEmpty() && !userInteracted.value) {
            scrollToBottomWithOverflow(listState, bottomPaddingPx)
        }
    }

    //отслеживает, достигнут ли низ списка, чтобы сбросить флаг взаимодействия
    LaunchedEffect(isAtBottom) {
        if (isAtBottom) {
            userInteracted.value = false
        }
    }

    //при изменении высоты клавиатуры или числа сообщений скроллит вниз,
//    LaunchedEffect(imePadding) {
//        listState.scrollToAvoidKeyboard(imePadding)
//    }

//    LaunchedEffect(keyboardHeight, chatMessages.size) {
//        if (chatMessages.isNotEmpty()) {
//            scrollToKeyboard(listState, bottomPaddingPx)
//        }
//    }

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
                                chatViewModel.sendMessage(prevUser.content)
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
                                        chatViewModel.sendMessage(userInput)
                                        userInput = ""
                                        keyboardController?.hide()
                                    },
                                    enabled = userInput.isNotBlank()
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
                                if (userInput.isNotBlank() && !isWriting) {
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