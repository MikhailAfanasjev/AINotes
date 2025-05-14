package com.example.ainotes.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ainotes.chatGPT.Message
import com.example.ainotes.utils.MarkdownParser
import com.example.ainotes.utils.MessageSegment
import com.example.ainotes.viewModels.ChatViewModel
import com.example.linguareader.R

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

    val isWriting by chatViewModel.isAssistantWriting.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val bubbleColor = if (isAssistant) colorScheme.onPrimary else colorScheme.primary
    val contentColor = colorScheme.onSecondary
    val maxBubbleWidth = LocalConfiguration.current.screenWidthDp.dp * 0.8f
    val segments = remember(message.content) {
        MarkdownParser.parseSegments(message.content)
    }

    // Именно AnnotatedString
    val displayContent = AnnotatedString(message.content)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        contentAlignment = if (isAssistant) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Surface(
            color = bubbleColor,
            contentColor = contentColor,
            shape = bubbleShape,
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
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
                if (showTyping) {
                    TypingIndicator(bubbleColor = bubbleColor, contentColor = contentColor)
                }

                // Возвращена проверка на пустой контент
                if (message.content.isNotBlank()) {
                    // Рендерим каждый сегмент
                    segments.forEach { segment ->
                        when (segment) {
                            is MessageSegment.Text -> {
                                Text(
                                    text = segment.content,
                                    color = contentColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            is MessageSegment.Code -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(
                                            color = Color(0xFF000000),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = segment.content,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }


                if (isAssistant && message.isComplete && displayContent.text.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onRetry, modifier = Modifier.size(24.dp)) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_reload),
                                contentDescription = "Повторить ответ",
                                modifier = Modifier.size(16.dp),
                                tint = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}