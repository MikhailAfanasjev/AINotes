package com.example.ainotes.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ainotes.data.local.entity.Note

@Composable
fun NoteDetailsDialog(
    note: Note,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.85f),
            color = colors.secondary // цвет фона диалога
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSecondary, // цвет заголовка
                    maxLines = Int.MAX_VALUE
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSecondary, // цвет текста заметки
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }
}