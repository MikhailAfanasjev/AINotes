package com.example.ainotes.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.ainotes.viewModels.NotesViewModel
import com.example.ainotes.utils.cleanResponse

@Composable
fun AddEditNoteScreen(
    navController: NavController,
    noteId: Long?,
    viewModel: NotesViewModel = hiltViewModel(),
    initialText: String = ""
) {
    val noteIdLong: Long? = noteId
    val isEditing = noteIdLong != null && noteIdLong != -1L

    var title by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf(cleanResponse(initialText).toString()) }
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()

    LaunchedEffect(isEditing, notes) {
        if (isEditing) {
            notes.find { it.id == noteIdLong }?.let { existing ->
                title = existing.title
                noteContent = cleanResponse(existing.note).toString()
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Заголовок заметки
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Заголовок") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = colorScheme.onSecondary,
                    focusedTextColor = colorScheme.onSecondary,
                    unfocusedTextColor = colorScheme.onSecondary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(Modifier.height(16.dp))

            // Поле заметки: занимает оставшееся пространство и скроллится
            TextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                placeholder = { Text("Заметка") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                singleLine = false,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = colorScheme.onSecondary,
                    focusedTextColor = colorScheme.onSecondary,
                    unfocusedTextColor = colorScheme.onSecondary
                ),
            )

            Spacer(Modifier.height(16.dp))

            // Кнопка Создать/Обновить
            Button(
                onClick = {
                    if (!isEditing) {
                        viewModel.addNote(title, noteContent)
                        Toast.makeText(context, "Заметка добавлена", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateNote(noteIdLong!!, title, noteContent)
                        Toast.makeText(context, "Заметка обновлена", Toast.LENGTH_SHORT).show()
                    }
                    navController.navigate("notes") {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary,
                    contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (!isEditing) "Создать заметку" else "Обновить заметку")
            }

            Spacer(Modifier.height(8.dp))

            // Кнопка Отменить
            Button(
                onClick = {
                    navController.navigate("notes") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error,
                    contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отменить")
            }
        }
    }
}
