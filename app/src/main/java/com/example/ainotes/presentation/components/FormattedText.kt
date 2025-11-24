package com.example.ainotes.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.example.linguareader.R

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
                        CodeBlockWithHeader(
                            code = segment.content.trim(),
                            language = segment.language,
                            textColor = textColor,
                            onCreateNote = onCreateNote
                        )
                    }

                    is MessageSegment.Think -> {
                        ThinkBlockWithHeader(
                            content = segment.content.trim(),
                            durationSeconds = segment.durationSeconds,
                            textColor = textColor,
                            onCreateNote = onCreateNote
                        )
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
                            CodeBlockWithHeader(
                                code = segment.content.trim(),
                                language = segment.language,
                                textColor = textColor,
                                onCreateNote = null
                            )
                        }

                        is MessageSegment.Think -> {
                            ThinkBlockWithHeader(
                                content = segment.content.trim(),
                                durationSeconds = segment.durationSeconds,
                                textColor = textColor,
                                onCreateNote = null
                            )
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

@Composable
private fun CodeBlockWithHeader(
    code: String,
    language: String?,
    textColor: Color,
    onCreateNote: ((String) -> Unit)?
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Black)
    ) {
        // Header with language name and copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Black.copy(alpha = 0.8f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language name
            Text(
                text = language?.uppercase() ?: "CODE",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )

            // Copy button
            IconButton(
                onClick = {
                    val clip = ClipData.newPlainText("code", code)
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                        clip
                    )
                    Toast
                        .makeText(context, "Код скопирован", Toast.LENGTH_SHORT)
                        .show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = "Копировать код",
                    modifier = Modifier.size(16.dp),
                    tint = textColor.copy(alpha = 0.7f)
                )
            }
        }

        // Horizontal divider line
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.Gray.copy(alpha = 0.3f)
        )

        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            if (onCreateNote != null) {
                NoteSelectionContainer(
                    text = code,
                    onCreateNote = onCreateNote,
                    textColor = textColor,
                    backgroundColor = Color.Transparent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Normal,
                    isCode = true
                )
            } else {
                SelectionContainer {
                    Text(
                        text = code,
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkBlockWithHeader(
    content: String,
    durationSeconds: Float,
    textColor: Color,
    onCreateNote: ((String) -> Unit)?
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // Цвет для think блока (слегка отличается от code блока)
    val thinkBackgroundColor = Color(0xFF1A1A2E) // Темно-синеватый оттенок

    // Состояние для сворачивания/разворачивания блока
    var isExpanded by remember { mutableStateOf(false) }

    // Анимация поворота стрелки
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "think_arrow_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(thinkBackgroundColor)
    ) {
        // Header with "Thought for X seconds" and expand/collapse button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(thinkBackgroundColor.copy(alpha = 0.8f))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Thought for X seconds" text
            Text(
                text = if (durationSeconds > 0) {
                    String.format("Thought for %.1f seconds", durationSeconds)
                } else {
                    "Thought"
                },
                color = textColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )

            // Expand/collapse button
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                modifier = Modifier
                    .size(16.dp)
                    .rotate(arrowRotation),
                tint = textColor.copy(alpha = 0.7f)
            )
        }

        // Анимированное содержимое блока
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 300)
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 300)
            )
        ) {
            Column {
                // Horizontal divider line
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )

                // Think content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    if (onCreateNote != null) {
                        NoteSelectionContainer(
                            text = content,
                            onCreateNote = onCreateNote,
                            textColor = textColor.copy(alpha = 0.9f),
                            backgroundColor = Color.Transparent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Italic,
                            isCode = false
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = content,
                                color = textColor.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 20.sp
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