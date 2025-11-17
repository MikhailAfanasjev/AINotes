package com.example.ainotes.presentation.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ainotes.chatGPT.Message
import com.example.ainotes.viewModels.ChatViewModel
import com.example.linguareader.R

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ChatMessageItem(
    chatViewModel: ChatViewModel = hiltViewModel(),
    message: Message,
    onCreateNote: (String) -> Unit,
    onRetry: () -> Unit,
    showTyping: Boolean = false,
) {
    val isAssistant = message.role == "assistant"
    val bubbleShape = if (isAssistant) {
        RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
    }

    val colorScheme = MaterialTheme.colorScheme
    val bubbleColor = if (isAssistant) colorScheme.onPrimary else colorScheme.primary
    val maxBubbleWidth = LocalConfiguration.current.screenWidthDp.dp * 0.8f
    val context = LocalContext.current

    // Менеджер буфера обмена
    LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        contentAlignment = if (isAssistant) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Surface(
            color = bubbleColor,
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
            shape = bubbleShape,
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .wrapContentWidth()
        ) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = maxBubbleWidth)
                    .background(color = bubbleColor, shape = bubbleShape)
                    .padding(8.dp)
            ) {
                if (showTyping) TypingIndicator(bubbleColor = bubbleColor, contentColor = colorScheme.onSecondary)

                if (message.content.isNotBlank()) {
                    FormattedText(
                        text = message.content,
                        textColor = colorScheme.onSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        onCreateNote = onCreateNote
                    )
                }

                if (isAssistant && message.isComplete && message.content.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Создать заметку
                        IconButton(
                            onClick = {
                                onCreateNote(message.content)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notes),
                                contentDescription = "Создать заметку",
                                modifier = Modifier.size(16.dp),
                                tint = colorScheme.onSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Копирование с уведомлением
                        IconButton(
                            onClick = {
                                val clip = ClipData.newPlainText("simple text", message.content)
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                    clip
                                )
                                Toast
                                    .makeText(context, "Текст скопирован", Toast.LENGTH_SHORT)
                                    .show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = "Копировать ответ",
                                modifier = Modifier.size(16.dp),
                                tint = colorScheme.onSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Повторить ответ
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_reload),
                                contentDescription = "Повторить ответ",
                                modifier = Modifier.size(16.dp),
                                tint = colorScheme.onSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}