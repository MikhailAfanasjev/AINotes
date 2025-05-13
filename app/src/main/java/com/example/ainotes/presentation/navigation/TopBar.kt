package com.example.ainotes.presentation.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ainotes.viewModels.ChatViewModel
import com.example.ainotes.viewModels.NotesViewModel
import com.example.ainotes.chatGPT.Message
import com.example.ainotes.viewModels.ThemeViewModel
import com.example.linguareader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    navController: NavController,
    chatViewModel: ChatViewModel = hiltViewModel(),
    chatMessages: List<Message>,
    notesViewModel: NotesViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val iconSize = 24.dp
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/") ?: ""
    val notes by notesViewModel.notes.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    val selectedModel by chatViewModel.selectedModel.collectAsState()
    val models = chatViewModel.availableModels

    var menuBounds by remember { mutableStateOf<Rect?>(null) }
    var modelItemBounds by remember { mutableStateOf<Rect?>(null) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val dynamicSpacing = screenWidthDp * 0.2f

    val colorScheme = MaterialTheme.colorScheme

    CompositionLocalProvider(LocalRippleConfiguration provides null) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(end = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { navController.navigate("chat") }
                                .padding(end = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chat),
                                contentDescription = "Чат",
                                tint = if (currentRoute == "chat") colorScheme.onTertiary else colorScheme.tertiary, // активный/неактивный
                                modifier = Modifier.size(iconSize)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Чат",
                                color = if (currentRoute == "chat") colorScheme.onTertiary else colorScheme.tertiary, // активный/неактивный
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(dynamicSpacing))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { navController.navigate("notes") }
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notes),
                                contentDescription = "Заметки",
                                tint = if (currentRoute == "notes") colorScheme.onTertiary else colorScheme.tertiary, // активный/неактивный
                                modifier = Modifier.size(iconSize)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Заметки",
                                color = if (currentRoute == "notes") colorScheme.onTertiary else colorScheme.tertiary, // активный/неактивный
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(dynamicSpacing))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Больше",
                                modifier = Modifier.size(iconSize),
                                tint = colorScheme.tertiary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = {
                                showMenu = false
                                showModelMenu = false
                            },
                            modifier = Modifier
                                .onGloballyPositioned { coords -> menuBounds = coords.boundsInWindow() }
                                .width(200.dp),
                            shape = RoundedCornerShape(16.dp),
                            // ← Задаём фон меню secondary
                            containerColor = colorScheme.background
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier
                                    .onGloballyPositioned { coords -> modelItemBounds = coords.boundsInWindow() }
                                    .background(if (showModelMenu) colorScheme.secondary else colorScheme.background),
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_model_selection),
                                        contentDescription = null,
                                        tint = colorScheme.onSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                text = { Text("Выбор модели", color = colorScheme.onSecondary) },
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_more),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .rotate(if (showModelMenu) 270f else 0f)
                                            .size(16.dp)
                                    )
                                },
                                onClick = { showModelMenu = true },
                            )

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_light_dark),
                                        contentDescription = "Переключение темы",
                                        tint = colorScheme.onSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                text = { Text("Тема", color = colorScheme.onSecondary) },
                                onClick = {
                                    themeViewModel.toggleTheme()
                                    showMenu = false
                                    showModelMenu = false
                                }
                            )

                            if (currentRoute == "chat" && chatMessages.isNotEmpty()) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_remove),
                                            contentDescription = "Очистить чат",
                                            tint = colorScheme.onSurface, // цвет иконки удаления
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    text = { Text("Очистить чат", color = colorScheme.onSurface) }, // цвет текста очистки
                                    onClick = {
                                        showMenu = false
                                        showModelMenu = false
                                        chatViewModel.clearChat()
                                    }
                                )
                            }
                            if (currentRoute == "notes" && notes.isNotEmpty()) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_remove),
                                            contentDescription = "Удалить заметки",
                                            tint = colorScheme.onSurface, // цвет иконки удаления
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    text = { Text("Удалить заметки", color = colorScheme.onSurface) }, // цвет текста очистки
                                    onClick = {
                                        showMenu = false
                                        showModelMenu = false
                                        notesViewModel.deleteAllNotes()
                                    }
                                )
                            }
                        }

                        if (showModelMenu && menuBounds != null && modelItemBounds != null) {
                            val offsetDp = with(LocalDensity.current) {
                                DpOffset(
                                    x = menuBounds!!.width.toDp(),
                                    y = (modelItemBounds!!.bottom - menuBounds!!.top).toDp()
                                )
                            }
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { showModelMenu = false },
                                offset = offsetDp,
                                modifier = Modifier
                                    .width(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.background) // фон меню моделей
                            ) {
                                models.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = model == selectedModel,
                                                    onClick = {
                                                        chatViewModel.setModel(model)
                                                        showModelMenu = false
                                                    },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = colorScheme.primary, // цвет выбранного радио
                                                        unselectedColor = colorScheme.tertiary
                                                    )
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(model, color = colorScheme.onSecondary)
                                            }
                                        },
                                        onClick = {
                                            chatViewModel.setModel(model)
                                            showModelMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background, // цвет фона TopBar
                    navigationIconContentColor = colorScheme.tertiary,
                    actionIconContentColor = colorScheme.tertiary
                )
            )
        }
    }
}