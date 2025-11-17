package com.example.ainotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ainotes.chatGPT.ApiKeyHelper
import com.example.ainotes.presentation.navigation.NavGraph
import com.example.ainotes.presentation.navigation.TopBar
import com.example.ainotes.presentation.ui.theme.AiNotesTheme
import com.example.ainotes.utils.dataStore
import com.example.ainotes.viewModels.ChatViewModel
import com.example.ainotes.viewModels.NotesViewModel
import com.example.linguareader.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        var isContentReady = false
        splashScreen.setKeepOnScreenCondition { !isContentReady }

        setTheme(R.style.Theme_AINotes)
        ApiKeyHelper.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            // Используем системную тему
            val isDarkTheme = isSystemInDarkTheme()

            AiNotesTheme(darkTheme = isDarkTheme) {
                val colors = MaterialTheme.colorScheme
                DisposableEffect(colors, isDarkTheme) {
                    window.statusBarColor = colors.background.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView)?.apply {
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
                                notesViewModel = notesViewModel
                            )
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController      = navController,
                        modifier           = Modifier.padding(innerPadding),
                        chatViewModel      = chatViewModel,
                        notesViewModel     = notesViewModel,
                        initialDarkTheme = isDarkTheme
                    )
                }

                LaunchedEffect(Unit) {
                    isContentReady = true
                }
            }
        }
    }
}