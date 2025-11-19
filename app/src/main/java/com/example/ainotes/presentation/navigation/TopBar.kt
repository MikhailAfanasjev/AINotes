package com.example.ainotes.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ainotes.chatGPT.Message
import com.example.ainotes.viewModels.ChatViewModel
import com.example.ainotes.viewModels.NotesViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.example.linguareader.R
import androidx.compose.ui.graphics.Color
import com.example.ainotes.presentation.components.SettingsDrawer

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    navController: NavController,
    chatViewModel: ChatViewModel = hiltViewModel(),
    chatMessages: List<Message>,
    notesViewModel: NotesViewModel = hiltViewModel(),
    showSettingsDrawer: Boolean = false,
    onShowSettingsDrawer: (Boolean) -> Unit = {},
    expandModels: Boolean = false
) {
    val iconSize = 24.dp
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/") ?: ""
    val notes by notesViewModel.notes.collectAsState(emptyList())
    val selectedModel by chatViewModel.selectedModel.collectAsState()
    val isModelInitializing by chatViewModel.isModelInitializing.collectAsState()
    val modelInitialized by chatViewModel.modelInitialized.collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    val minSpacing = 8.dp
    val maxSpacing = (screenWidthDp.value * 0.15f).dp
    val dynamicSpacing = when {
        screenWidthDp < 360.dp -> minSpacing
        screenWidthDp < 480.dp -> (screenWidthDp.value * 0.08f).dp
        screenWidthDp < 720.dp -> (screenWidthDp.value * 0.12f).dp
        else -> maxSpacing
    }

    val isSmallScreen = screenWidthDp < 400.dp
    val adaptiveIconPadding = if (isSmallScreen) 4.dp else 8.dp

    val colorScheme = MaterialTheme.colorScheme

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Box {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = { /* пусто */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .drawWithContent {
                            drawContent()
                        },
                    navigationIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "Настройки",
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(end = adaptiveIconPadding)
                                    .clickable { onShowSettingsDrawer(true) }
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { navController.navigate("chat") }
                                    .padding(end = adaptiveIconPadding)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chat),
                                    contentDescription = "Чат",
                                    tint = if (currentRoute == "chat") colorScheme.onTertiary else colorScheme.tertiary,
                                    modifier = Modifier.size(iconSize)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Чат",
                                    color = if (currentRoute == "chat") colorScheme.onTertiary else colorScheme.tertiary,
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(dynamicSpacing))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { navController.navigate("notes") }
                                    .padding(end = adaptiveIconPadding)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_notes),
                                    contentDescription = "Заметки",
                                    tint = if (currentRoute == "notes") colorScheme.onTertiary else colorScheme.tertiary,
                                    modifier = Modifier.size(iconSize)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Заметки",
                                    color = if (currentRoute == "notes") colorScheme.onTertiary else colorScheme.tertiary,
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            when {
                                isModelInitializing -> {
                                    val infiniteTransition =
                                        rememberInfiniteTransition(label = "loading_rotation")
                                    val rotationAngle by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ), label = "rotation"
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_loading),
                                        contentDescription = "Загрузка модели",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .rotate(rotationAngle),
                                        tint = Color.Unspecified
                                    )
                                }

                                selectedModel.isEmpty() -> {
                                    // Модель не выбрана - не показываем иконку
                                }

                                !modelInitialized -> {
                                    // Модель выбрана, но не инициализирована (ошибка)
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_error),
                                        contentDescription = "Ошибка инициализации модели",
                                        modifier = Modifier
                                            .size(16.dp),
                                        tint = Color.Unspecified
                                    )
                                }

                                else -> {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_online),
                                        contentDescription = "Модель готова",
                                        modifier = Modifier
                                            .size(16.dp),
                                        tint = Color.Unspecified
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.background,
                        navigationIconContentColor = colorScheme.tertiary,
                        actionIconContentColor = colorScheme.tertiary
                    )
                )
            }

            SettingsDrawer(
                isVisible = showSettingsDrawer,
                onDismiss = { onShowSettingsDrawer(false) },
                chatViewModel = chatViewModel,
                notesViewModel = notesViewModel,
                chatMessages = chatMessages,
                currentRoute = currentRoute,
                expandModels = expandModels
            )
        }
    }
}