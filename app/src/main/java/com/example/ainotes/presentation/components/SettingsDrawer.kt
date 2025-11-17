package com.example.ainotes.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ainotes.viewModels.ChatListViewModel
import com.example.ainotes.viewModels.ChatViewModel
import com.example.ainotes.viewModels.NotesViewModel
import com.example.linguareader.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SettingsDrawer(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    chatListViewModel: ChatListViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel(),
    chatMessages: List<com.example.ainotes.chatGPT.Message> = emptyList(),
    currentRoute: String = ""
) {
    val selectedModel by chatViewModel.selectedModel.collectAsState()
    val models = chatViewModel.availableModels
    val isModelInitializing by chatViewModel.isModelInitializing.collectAsState()
    val modelInitialized by chatViewModel.modelInitialized.collectAsState()
    val notes by notesViewModel.notes.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    // Chat list states
    val chatList by chatListViewModel.chatList.collectAsState()
    val currentChatId by chatListViewModel.currentChatId.collectAsState()
    val isCreatingChat by chatListViewModel.isCreatingChat.collectAsState()
    val isTitleGenerating by chatViewModel.isTitleGenerating.collectAsState()

    // Обновляем список чатов после генерации заголовка
    androidx.compose.runtime.LaunchedEffect(isTitleGenerating) {
        if (!isTitleGenerating) {
            chatListViewModel.refreshChats()
        }
    }

    // Обновляем список чатов при открытии drawer
    androidx.compose.runtime.LaunchedEffect(isVisible) {
        if (isVisible) {
            android.util.Log.d(
                ">>>SettingsDrawer",
                "📂 Открыт drawer. CurrentChatId: $currentChatId"
            )
            android.util.Log.d(">>>SettingsDrawer", "📋 Список чатов: ${chatList.size} шт.")
            chatList.forEach { chat ->
                android.util.Log.d(
                    ">>>SettingsDrawer",
                    "  - ${chat.title} (id: ${chat.id}) ${if (chat.id == currentChatId) "✓ ВЫБРАН" else ""}"
                )
            }
            chatListViewModel.refreshChats()
        }
    }

    // Состояние для сворачивания/разворачивания списка моделей
    var isModelListExpanded by remember { mutableStateOf(false) }

    // Анимация для плавного появления/исчезновения
    val animationProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "drawer_animation"
    )

    // Анимация прозрачности фона
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.3f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "background_alpha"
    )

    // Анимация поворота стрелки
    val arrowRotation by animateFloatAsState(
        targetValue = if (isModelListExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrow_rotation"
    )

    if (isVisible || animationProgress > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
                .background(colorScheme.background.copy(alpha = backgroundAlpha))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(350.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-350.dp * (1f - animationProgress)))
                    .shadow(8.dp)
                    .clickable { },
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(16.dp)
                        .graphicsLayer(alpha = animationProgress)
                ) {
                    // Заголовок
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Настройки",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Статус модели
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.background),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            when {
                                isModelInitializing -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_loading),
                                        contentDescription = "Загрузка модели",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Unspecified
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Инициализация модели...",
                                        color = colorScheme.onBackground,
                                        fontSize = 16.sp
                                    )
                                }

                                !modelInitialized -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_error),
                                        contentDescription = "Ошибка инициализации модели",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Unspecified
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Ошибка инициализации",
                                            color = colorScheme.error,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Нажмите для повтора",
                                            color = colorScheme.onBackground.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            modifier = Modifier.clickable {
                                                chatViewModel.retryModelInitialization()
                                            }
                                        )
                                    }
                                }

                                else -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_online),
                                        contentDescription = "Модель готова",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Unspecified
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Модель готова",
                                        color = colorScheme.onBackground,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Заголовок выбора модели с возможностью сворачивания
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isModelListExpanded = !isModelListExpanded
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Модели ИИ",
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow),
                            contentDescription = if (isModelListExpanded) "Свернуть" else "Развернуть",
                            tint = colorScheme.onSurface,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(arrowRotation)
                        )
                    }

                    // Анимированный список моделей
                    AnimatedVisibility(
                        visible = isModelListExpanded,
                        enter = expandVertically(
                            animationSpec = tween(durationMillis = 300)
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(durationMillis = 300)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Spacer(Modifier.height(8.dp))

                            models.forEach { model ->
                                val isCurrentModel =
                                    chatViewModel.getModelDisplayName(selectedModel) == model
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            chatViewModel.setModelByDisplayName(model)
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    RadioButton(
                                        selected = isCurrentModel,
                                        onClick = {
                                            chatViewModel.setModelByDisplayName(model)
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = colorScheme.primary,
                                            unselectedColor = colorScheme.tertiary
                                        )
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = model,
                                        color = colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Очистить текущий чат
                    if (currentRoute == "chat" && chatMessages.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    chatViewModel.clearChat()
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_remove),
                                contentDescription = "Очистить чат",
                                tint = colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = "Очистить текущий чат",
                                color = colorScheme.error,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Удалить заметки
                    if (currentRoute == "notes" && notes.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    notesViewModel.deleteAllNotes()
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_remove),
                                contentDescription = "Удалить заметки",
                                tint = colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = "Удалить все заметки",
                                color = colorScheme.error,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Список чатов (показываем только для экрана чата)
                    if (currentRoute == "chat") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Чаты",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            // Кнопка создания нового чата
                            IconButton(
                                onClick = {
                                    chatListViewModel.createNewChat()
                                    onDismiss()
                                },
                                enabled = !isCreatingChat
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add_chat),
                                    contentDescription = "Создать новый чат",
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Фильтруем только чаты с сообщениями
                        val nonEmptyChats = chatList.filter { it.messageCount > 0 }

                        // Список чатов до самого низа экрана
                        if (nonEmptyChats.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Нет чатов",
                                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(nonEmptyChats) { chat ->
                                    ChatListItem(
                                        chat = chat,
                                        isSelected = chat.id == currentChatId,
                                        onChatClick = { chatId ->
                                            chatListViewModel.selectChat(chatId)
                                            chatViewModel.setCurrentChatId(chatId)
                                            onDismiss()
                                        },
                                        onDeleteClick = { chatId ->
                                            chatListViewModel.deleteChat(chatId)
                                        },
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}