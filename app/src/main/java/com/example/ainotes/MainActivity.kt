package com.example.ainotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ainotes.viewModels.ChatViewModel
import com.example.ainotes.viewModels.NotesViewModel
import com.example.ainotes.chatGPT.ApiKeyHelper
import com.example.ainotes.presentation.navigation.NavGraph
import com.example.ainotes.presentation.navigation.TopBar
import com.example.ainotes.presentation.ui.theme.AiNotesTheme
import com.example.ainotes.viewModels.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiKeyHelper.init(this)

        setContent {
            // Получаем ViewModel темы
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

            AiNotesTheme(
                // Используем состояние темы из ViewModel вместо системной настройки
                darkTheme = isDarkTheme,
            ) {
                val colors = MaterialTheme.colorScheme
                DisposableEffect(colors, isDarkTheme) {
                    window.statusBarColor = colors.background.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView)?.apply {
                        // Обновляем цвета системных элементов в соответствии с темой
                        isAppearanceLightStatusBars = !isDarkTheme
                        isAppearanceLightNavigationBars = !isDarkTheme
                    }
                    onDispose { }
                }

                val navController = rememberNavController()
                val chatViewModel: ChatViewModel = hiltViewModel()
                val chatMessages by chatViewModel.chatMessages.collectAsState()
                val notesViewModel: NotesViewModel = hiltViewModel()
                val notes by notesViewModel.notes.collectAsState()

                Scaffold(
                    topBar = {
                        val currentRoute = navController
                            .currentBackStackEntryAsState()
                            .value
                            ?.destination
                            ?.route ?: ""
                        if (!currentRoute.startsWith("detail")) {
                            TopBar(
                                navController = navController,
                                chatViewModel = chatViewModel,
                                chatMessages = chatMessages,
                                notesViewModel = notesViewModel,
                                themeViewModel = themeViewModel // Передаем ViewModel темы
                            )
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        chatViewModel = chatViewModel,
                        notesViewModel = notesViewModel
                    )
                }
            }
        }
    }
}