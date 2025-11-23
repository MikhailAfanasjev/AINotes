package com.example.ainotes.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ainotes.utils.MessageSegment
import com.example.ainotes.utils.parseMarkdownText
import com.example.ainotes.presentation.ui.theme.Black

@Composable
fun FormattedText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    onCreateNote: ((String) -> Unit)? = null
) {
    val segments = parseMarkdownText(text)

    if (onCreateNote != null) {
        // Если нужна функция создания заметок, используем NoteSelectionContainer для каждого сегмента
        Column(modifier = modifier) {
            segments.forEach { segment ->
                when (segment) {
                    is MessageSegment.Text -> {
                        NoteSelectionContainer(
                            text = segment.content,
                            onCreateNote = onCreateNote,
                            textColor = textColor,
                            backgroundColor = Color.Transparent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Normal,
                            isCode = false
                        )
                    }
                    is MessageSegment.Code -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Black)
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            NoteSelectionContainer(
                                text = segment.content.trim(),
                                onCreateNote = onCreateNote,
                                textColor = textColor,
                                backgroundColor = Color.Transparent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                fontStyle = FontStyle.Normal,
                                isCode = true
                            )
                        }
                    }
                    is MessageSegment.Header -> {
                        val (fontSize, fontWeight, topPadding) = when (segment.level) {
                            1 -> Triple(30.sp, FontWeight.Bold, 16.dp)
                            2 -> Triple(22.sp, FontWeight.Bold, 14.dp)
                            3 -> Triple(18.sp, FontWeight.Bold, 12.dp)
                            4 -> Triple(16.sp, FontWeight.Bold, 10.dp)
                            5 -> Triple(14.sp, FontWeight.Bold, 8.dp)
                            6 -> Triple(12.sp, FontWeight.Bold, 6.dp)
                            else -> Triple(14.sp, FontWeight.Bold, 8.dp)
                        }

                        NoteSelectionContainer(
                            text = segment.content,
                            onCreateNote = onCreateNote,
                            textColor = if (segment.level == 6) textColor.copy(alpha = 0.7f) else textColor,
                            backgroundColor = Color.Transparent,
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            fontStyle = FontStyle.Normal,
                            isCode = false
                        )
                    }

                    is MessageSegment.Quote -> {
                        NoteSelectionContainer(
                            text = segment.content,
                            onCreateNote = onCreateNote,
                            textColor = textColor.copy(alpha = 0.8f),
                            backgroundColor = Color.Gray.copy(alpha = 0.1f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Italic,
                            isCode = false
                        )
                    }

                    is MessageSegment.UnorderedListItem -> {
                        NoteSelectionContainer(
                            text = "• ${segment.content}",
                            onCreateNote = onCreateNote,
                            textColor = textColor,
                            backgroundColor = Color.Transparent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Normal,
                            isCode = false
                        )
                    }

                    is MessageSegment.OrderedListItem -> {
                        NoteSelectionContainer(
                            text = "${segment.number}. ${segment.content}",
                            onCreateNote = onCreateNote,
                            textColor = textColor,
                            backgroundColor = Color.Transparent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Normal,
                            isCode = false
                        )
                    }

                    is MessageSegment.HorizontalRule -> {
                        // Горизонтальная линия не нужна в выделении
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = textColor.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    } else {
        // Простое отображение без функции создания заметок
        SelectionContainer {
            Column(modifier = modifier) {
                segments.forEach { segment ->
                    when (segment) {
                        is MessageSegment.Text -> {
                            Text(
                                text = formatInlineMarkdown(segment.content),
                                color = textColor,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        is MessageSegment.Code -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Black)
                                    .horizontalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = segment.content.trim(),
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    softWrap = false
                                )
                            }
                        }

                        is MessageSegment.Header -> {
                            val (fontSize, fontWeight, topPadding) = when (segment.level) {
                                1 -> Triple(30.sp, FontWeight.Bold, 16.dp)
                                2 -> Triple(22.sp, FontWeight.Bold, 14.dp)
                                3 -> Triple(18.sp, FontWeight.Bold, 12.dp)
                                4 -> Triple(16.sp, FontWeight.Bold, 10.dp)
                                5 -> Triple(14.sp, FontWeight.Bold, 8.dp)
                                6 -> Triple(12.sp, FontWeight.Bold, 6.dp)
                                else -> Triple(14.sp, FontWeight.Bold, 8.dp)
                            }

                            Text(
                                text = formatInlineMarkdown(segment.content),
                                color = if (segment.level == 6) textColor.copy(alpha = 0.7f) else textColor,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                modifier = Modifier.padding(top = topPadding, bottom = 4.dp)
                            )
                        }

                        is MessageSegment.Quote -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color.Gray.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                // Вертикальная линия цитаты
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = textColor.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                        .width(4.dp)
                                        .height(20.dp)
                                )

                                Text(
                                    text = formatInlineMarkdown(segment.content),
                                    color = textColor.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }

                        is MessageSegment.UnorderedListItem -> {
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = "•",
                                    color = textColor,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = formatInlineMarkdown(segment.content),
                                    color = textColor,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        is MessageSegment.OrderedListItem -> {
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = "${segment.number}.",
                                    color = textColor,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = formatInlineMarkdown(segment.content),
                                    color = textColor,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        is MessageSegment.HorizontalRule -> {
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                thickness = 2.dp,
                                color = textColor.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val patterns = listOf(
            // Bold **text**
            Regex("\\*\\*(.+?)\\*\\*") to SpanStyle(fontWeight = FontWeight.Bold),
            // Italic *text*
            Regex("\\*([^*]+?)\\*") to SpanStyle(fontStyle = FontStyle.Italic),
            // Inline code `text`
            Regex("`([^`]+)`") to SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Black,
                fontSize = 12.sp
            )
        )

        // Найти все совпадения
        val matches = mutableListOf<Triple<IntRange, String, SpanStyle>>()
        patterns.forEach { (regex, style) ->
            regex.findAll(text).forEach { match ->
                matches.add(Triple(match.range, match.groupValues[1], style))
            }
        }

        // Сортировать по позиции
        matches.sortBy { it.first.first }

        // Обработать текст
        var lastEnd = 0
        matches.forEach { (range, content, style) ->
            // Добавить текст до совпадения
            if (range.first > lastEnd) {
                append(text.substring(lastEnd, range.first))
            }

            // Добавить стилизованный текст
            withStyle(style) {
                append(content)
            }

            lastEnd = range.last + 1
        }

        // Добавить оставшийся текст
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
}