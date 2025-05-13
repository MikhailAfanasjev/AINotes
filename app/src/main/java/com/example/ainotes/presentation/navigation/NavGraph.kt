package com.example.ainotes.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ainotes.viewModels.NotesViewModel
import com.example.ainotes.viewModels.ChatViewModel
import com.example.ainotes.presentation.screens.AddEditNoteScreen
import com.example.ainotes.presentation.screens.ChatScreen
import com.example.ainotes.presentation.screens.NoteScreen
import com.example.ainotes.utils.LocalNavigationController
import com.example.ainotes.viewModels.ThemeViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    initialDarkTheme: Boolean
) {
    CompositionLocalProvider(LocalNavigationController provides navController) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = "chat",
                modifier = modifier
            ) {
                composable("chat") {
                    ChatScreen(
                        navController = navController,
                        chatViewModel = chatViewModel,
                        themeViewModel = themeViewModel,
                        initialDarkTheme = initialDarkTheme
                    )
                }

                composable("notes") {
                    NoteScreen(
                        navController = navController,
                        viewModel = notesViewModel
                    )
                }

                composable(
                    route = "add_edit_note/{noteId}",
                    arguments = listOf(navArgument("noteId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    })
                ) { backStackEntry ->
                    val rawId = backStackEntry.arguments!!.getLong("noteId")
                    val noteId: Long? = rawId.takeIf { it >= 0L }

                    val initialText = navController
                        .previousBackStackEntry
                        ?.savedStateHandle
                        ?.get<String>("initialText")
                        .orEmpty()

                    AddEditNoteScreen(
                        navController = navController,
                        viewModel = notesViewModel,
                        noteId = noteId,
                        initialText = initialText
                    )
                }
            }
        }
    }
}