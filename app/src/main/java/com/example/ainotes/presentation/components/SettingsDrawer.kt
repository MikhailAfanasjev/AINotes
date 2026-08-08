package com.example.ainotes.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ainotes.utils.ConnectionSettingsManager
import com.example.ainotes.viewModels.ChatListViewModel
import com.example.ainotes.viewModels.ChatViewModel
import com.example.ainotes.viewModels.NotesViewModel
import com.example.ainotes.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsDrawer(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    chatListViewModel: ChatListViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel(),
    chatMessages: List<com.example.ainotes.chatGPT.Message> = emptyList(),
    currentRoute: String = "",
    expandModels: Boolean = false
) {
    val selectedModel by chatViewModel.selectedModel.collectAsState()
    val models by chatViewModel.availableModels.collectAsState()
    val isLoadingModels by chatViewModel.isLoadingModels.collectAsState()
    val isModelInitializing by chatViewModel.isModelInitializing.collectAsState()
    val modelInitialized by chatViewModel.modelInitialized.collectAsState()
    val notes by notesViewModel.notes.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    // Получаем ширину экрана и вычисляем 75%, но не более 350dp
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val calculatedWidth = screenWidthDp * 0.75f
    val drawerWidth = if (calculatedWidth > 350.dp) 350.dp else calculatedWidth

    // Connection settings manager
    val context = LocalContext.current
    val connectionSettingsManager = remember { ConnectionSettingsManager(context) }

    // Chat list states
    val chatList by chatListViewModel.chatList.collectAsState()
    val currentChatId by chatListViewModel.currentChatId.collectAsState()
    val chatViewModelChatId by chatViewModel.currentChatId.collectAsState()
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
            chatListViewModel.refreshChats()
        }
    }

    // Состояние для сворачивания/разворачивания списка моделей
    var isModelListExpanded by remember { mutableStateOf(false) }

    // Состояние для сворачивания/разворачивания настроек подключения
    var isConnectionSettingsExpanded by remember { mutableStateOf(false) }

    // Состояние для выбора типа подключения (true = LM Studio, false = API ключ)
    var isLMStudioMode by remember { mutableStateOf(connectionSettingsManager.isLMStudioMode()) }

    // Состояние для выбора локальной сети или NGROK (true = локальная сеть, false = NGROK)
    var isLocalNetworkMode by remember { mutableStateOf(connectionSettingsManager.isLocalNetworkMode()) }

    // Состояния для текстовых полей - загружаем из защищенного хранилища
    var localNetworkUrl by remember { mutableStateOf(connectionSettingsManager.getLocalNetworkUrl()) }
    var ngrokLocalUrl by remember { mutableStateOf(connectionSettingsManager.getNgrokLocalUrl()) }
    var ngrokApiUrl by remember { mutableStateOf(connectionSettingsManager.getNgrokApiUrl()) }
    var ngrokApiKey by remember { mutableStateOf(connectionSettingsManager.getNgrokApiKey()) }

    // Автоматически раскрываем список моделей при expandModels = true
    androidx.compose.runtime.LaunchedEffect(isVisible, expandModels) {
        if (isVisible && expandModels) {
            isModelListExpanded = true
        }
    }

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

    // Анимация поворота стрелки для моделей
    val arrowRotation by animateFloatAsState(
        targetValue = if (isModelListExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrow_rotation"
    )

    // Анимация поворота стрелки для настроек подключения
    val connectionArrowRotation by animateFloatAsState(
        targetValue = if (isConnectionSettingsExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "connection_arrow_rotation"
    )

    // Состояние скролла для контента
    val scrollState = rememberScrollState()

    // Обработка нажатия кнопки "назад"
    BackHandler(enabled = isVisible) {
        onDismiss()
    }

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
                    .width(drawerWidth)
                    .align(Alignment.CenterStart)
                    .offset(x = (-drawerWidth * (1f - animationProgress)))
                    .shadow(8.dp)
                    .clickable { },
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
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

                                selectedModel.isEmpty() -> {
                                    // Модель не выбрана
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_error),
                                        contentDescription = "Модель не загружена",
                                        modifier = Modifier.size(20.dp),
                                        tint = colorScheme.error
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Модель не загружена",
                                            color = colorScheme.onBackground,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Выберите модель для загрузки",
                                            color = colorScheme.onBackground.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                !modelInitialized -> {
                                    // Модель выбрана, но не инициализирована (ошибка)
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
                            painter = painterResource(id = R.drawable.ic_more),
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

                            when {
                                isLoadingModels -> {
                                    // Показываем индикатор загрузки
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_loading),
                                            contentDescription = "Загрузка моделей",
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.Unspecified
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Загрузка моделей...",
                                            color = colorScheme.onSurface,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                models.isEmpty() -> {
                                    // Показываем сообщение об ошибке с кнопкой повтора
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Не удалось загрузить модели",
                                            color = colorScheme.error,
                                            fontSize = 16.sp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Нажмите для повтора",
                                            color = colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontSize = 14.sp,
                                            modifier = Modifier.clickable {
                                                chatViewModel.loadAvailableModels()
                                            }
                                        )
                                    }
                                }

                                else -> {
                                    // Показываем список моделей
                                    models.forEach { model ->
                                        val isCurrentModel = selectedModel == model
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
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Настройки подключения
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isConnectionSettingsExpanded = !isConnectionSettingsExpanded
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Настройки подключения",
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = if (isConnectionSettingsExpanded) "Свернуть" else "Развернуть",
                            tint = colorScheme.onSurface,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(connectionArrowRotation)
                        )
                    }

                    // Анимированные настройки подключения
                    AnimatedVisibility(
                        visible = isConnectionSettingsExpanded,
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

                            // Подключение к LM Studio
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isLMStudioMode = true
                                        connectionSettingsManager.setConnectionMode(
                                            ConnectionSettingsManager.CONNECTION_MODE_LM_STUDIO
                                        )
                                        // Перезагружаем модели при смене режима
                                        chatViewModel.loadAvailableModels()
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = isLMStudioMode,
                                    onClick = {
                                        isLMStudioMode = true
                                        connectionSettingsManager.setConnectionMode(
                                            ConnectionSettingsManager.CONNECTION_MODE_LM_STUDIO
                                        )
                                        // Перезагружаем модели при смене режима
                                        chatViewModel.loadAvailableModels()
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colorScheme.primary,
                                        unselectedColor = colorScheme.tertiary
                                    )
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Подключение к LM Studio",
                                    color = colorScheme.onSurface,
                                    fontSize = 16.sp
                                )
                            }

                            // Подменю для LM Studio
                            AnimatedVisibility(
                                visible = isLMStudioMode,
                                enter = expandVertically(
                                    animationSpec = tween(durationMillis = 300)
                                ),
                                exit = shrinkVertically(
                                    animationSpec = tween(durationMillis = 300)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 36.dp, top = 8.dp)
                                ) {
                                    // Подключение в локальной сети
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                isLocalNetworkMode = true
                                                connectionSettingsManager.setLMStudioMode(
                                                    ConnectionSettingsManager.LM_STUDIO_MODE_LOCAL
                                                )
                                                // Перезагружаем модели при смене режима
                                                chatViewModel.loadAvailableModels()
                                            }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        RadioButton(
                                            selected = isLocalNetworkMode,
                                            onClick = {
                                                isLocalNetworkMode = true
                                                connectionSettingsManager.setLMStudioMode(
                                                    ConnectionSettingsManager.LM_STUDIO_MODE_LOCAL
                                                )
                                                // Перезагружаем модели при смене режима
                                                chatViewModel.loadAvailableModels()
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = colorScheme.primary,
                                                unselectedColor = colorScheme.tertiary
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Подключение в локальной сети",
                                            color = colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }

                                    // Поля для локальной сети
                                    AnimatedVisibility(
                                        visible = isLocalNetworkMode,
                                        enter = expandVertically(
                                            animationSpec = tween(durationMillis = 200)
                                        ),
                                        exit = shrinkVertically(
                                            animationSpec = tween(durationMillis = 200)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = localNetworkUrl,
                                                onValueChange = {
                                                    localNetworkUrl = it
                                                    connectionSettingsManager.setLocalNetworkUrl(it)
                                                    // Перезагружаем модели при изменении URL
                                                    if (it.isNotEmpty()) {
                                                        chatViewModel.loadAvailableModels()
                                                    }
                                                },
                                                label = { Text("URL", fontSize = 12.sp) },
                                                placeholder = {
                                                    Text(
                                                        "http://192.168.1.83:1234",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                singleLine = true,
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = colorScheme.surface,
                                                    unfocusedContainerColor = colorScheme.surface,
                                                    focusedIndicatorColor = colorScheme.primary,
                                                    unfocusedIndicatorColor = colorScheme.onSurface.copy(
                                                        alpha = 0.3f
                                                    )
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // Подключение с помощью NGROK
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                isLocalNetworkMode = false
                                                connectionSettingsManager.setLMStudioMode(
                                                    ConnectionSettingsManager.LM_STUDIO_MODE_NGROK
                                                )
                                                // Перезагружаем модели при смене режима
                                                chatViewModel.loadAvailableModels()
                                            }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        RadioButton(
                                            selected = !isLocalNetworkMode,
                                            onClick = {
                                                isLocalNetworkMode = false
                                                connectionSettingsManager.setLMStudioMode(
                                                    ConnectionSettingsManager.LM_STUDIO_MODE_NGROK
                                                )
                                                // Перезагружаем модели при смене режима
                                                chatViewModel.loadAvailableModels()
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = colorScheme.primary,
                                                unselectedColor = colorScheme.tertiary
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Подключение с помощью NGROK",
                                            color = colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }

                                    // Поля для NGROK
                                    AnimatedVisibility(
                                        visible = !isLocalNetworkMode,
                                        enter = expandVertically(
                                            animationSpec = tween(durationMillis = 200)
                                        ),
                                        exit = shrinkVertically(
                                            animationSpec = tween(durationMillis = 200)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = ngrokLocalUrl,
                                                onValueChange = {
                                                    ngrokLocalUrl = it
                                                    connectionSettingsManager.setNgrokLocalUrl(it)
                                                },
                                                label = { Text("Локальный URL", fontSize = 12.sp) },
                                                placeholder = {
                                                    Text(
                                                        "http://192.168.1.83:1234",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                singleLine = true,
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = colorScheme.surface,
                                                    unfocusedContainerColor = colorScheme.surface,
                                                    focusedIndicatorColor = colorScheme.primary,
                                                    unfocusedIndicatorColor = colorScheme.onSurface.copy(
                                                        alpha = 0.3f
                                                    )
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            OutlinedTextField(
                                                value = ngrokApiUrl,
                                                onValueChange = {
                                                    ngrokApiUrl = it
                                                    connectionSettingsManager.setNgrokApiUrl(it)
                                                },
                                                label = { Text("NGROK API URL", fontSize = 12.sp) },
                                                placeholder = {
                                                    Text(
                                                        "https://api.ngrok.com/tunnels",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                singleLine = true,
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = colorScheme.surface,
                                                    unfocusedContainerColor = colorScheme.surface,
                                                    focusedIndicatorColor = colorScheme.primary,
                                                    unfocusedIndicatorColor = colorScheme.onSurface.copy(
                                                        alpha = 0.3f
                                                    )
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            OutlinedTextField(
                                                value = ngrokApiKey,
                                                onValueChange = {
                                                    ngrokApiKey = it
                                                    connectionSettingsManager.setNgrokApiKey(it)
                                                },
                                                label = { Text("API KEY", fontSize = 12.sp) },
                                                placeholder = {
                                                    Text(
                                                        "Введите API ключ NGROK",
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                singleLine = true,
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = colorScheme.surface,
                                                    unfocusedContainerColor = colorScheme.surface,
                                                    focusedIndicatorColor = colorScheme.primary,
                                                    unfocusedIndicatorColor = colorScheme.onSurface.copy(
                                                        alpha = 0.3f
                                                    )
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Подключение через API ключ
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isLMStudioMode = false
                                        connectionSettingsManager.setConnectionMode(
                                            ConnectionSettingsManager.CONNECTION_MODE_API_KEY
                                        )
                                        // Перезагружаем модели при смене режима
                                        chatViewModel.loadAvailableModels()
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = !isLMStudioMode,
                                    onClick = {
                                        isLMStudioMode = false
                                        connectionSettingsManager.setConnectionMode(
                                            ConnectionSettingsManager.CONNECTION_MODE_API_KEY
                                        )
                                        // Перезагружаем модели при смене режима
                                        chatViewModel.loadAvailableModels()
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colorScheme.primary,
                                        unselectedColor = colorScheme.tertiary
                                    )
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Подключение через API ключ",
                                    color = colorScheme.onSurface,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                        Spacer(Modifier.height(24.dp))

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

                        // Список чатов - заголовок
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

                                    // Ждем создания чата и синхронизируем ChatViewModel
                                    CoroutineScope(Dispatchers.Main).launch {
                                        // Ждем, пока чат будет создан (currentChatId изменится)
                                        chatListViewModel.currentChatId
                                            .first { it != null }
                                            .let { newChatId ->
                                                // Синхронизируем ChatViewModel с новым чатом БЕЗ загрузки сообщений
                                                chatViewModel.setCurrentChatId(
                                                    newChatId,
                                                    skipLoad = true
                                                )
                                                // Закрываем drawer только после синхронизации
                                                onDismiss()
                                            }
                                    }
                                },
                                enabled = !isCreatingChat
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add_chat),
                                    contentDescription = "Создать новый чат",
                                    tint = colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Список чатов (показываем только для экрана чата)
                        if (currentRoute == "chat") {
                            // Фильтруем только чаты с сообщениями
                            val nonEmptyChats = chatList.filter { it.messageCount > 0 }

                            // Список чатов
                            if (nonEmptyChats.isEmpty()) {
                                Text(
                                    text = "Нет чатов",
                                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                // Отображаем чаты в прокручиваемой колонке
                                nonEmptyChats.forEach { chat ->
                                    // Чат считается выбранным, если он соответствует ЛЮБОМУ из источников истины
                                    val isSelected =
                                        chat.id == currentChatId || chat.id == chatViewModelChatId

                                    ChatListItem(
                                        chat = chat,
                                        isSelected = isSelected,
                                        onChatClick = { chatId ->
                                            chatListViewModel.selectChat(chatId)
                                            chatViewModel.setCurrentChatId(chatId)
                                            onDismiss()
                                        },
                                        onDeleteClick = { chatId ->
                                            // КРИТИЧЕСКИ ВАЖНО: Удаляем чат из ChatListViewModel
                                            chatListViewModel.deleteChat(chatId)

                                            // КРИТИЧЕСКИ ВАЖНО: Немедленно синхронизируем ChatViewModel
                                            // Проверяем ОБА возможных источника истины
                                            if (chatId == currentChatId || chatId == chatViewModelChatId) {
                                                chatViewModel.setCurrentChatId(null)
                                            }
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