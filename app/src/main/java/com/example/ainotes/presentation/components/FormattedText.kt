package com.example.ainotes.presentation.components

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DividerDefaults.color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ainotes.R
import com.example.ainotes.utils.MarkdownParser
import com.example.ainotes.utils.MessageSegment

@Composable
fun FormattedText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    fontSize: Float? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    isCode: Boolean = false,
    onCreateNote: ((String) -> Unit)? = null
) {
    var segments by remember { mutableStateOf<List<MessageSegment>>(emptyList()) }

    LaunchedEffect(text) {
        segments = MarkdownParser.parseSegments(text)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEachIndexed { index, segment ->
            when (segment) {
                is MessageSegment.Text -> {
                    Text(
                        text = segment.content,
                        color = textColor,
                        fontSize = ((fontSize ?: 14f)).sp,
                        fontWeight = fontWeight,
                        fontStyle = fontStyle,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is MessageSegment.Header -> {
                    val headerFontSize = when (segment.level) {
                        1 -> 24.sp
                        2 -> 20.sp
                        3 -> 18.sp
                        else -> 16.sp
                    }

                    Text(
                        text = segment.content,
                        color = textColor,
                        fontSize = headerFontSize,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is MessageSegment.Code -> {
                    CodeBlockWithCopyButton(
                        code = segment.content,
                        language = segment.language,
                        textColor = textColor,
                        onCreateNote = onCreateNote
                    )
                }

                is MessageSegment.Table -> {
                    MarkdownTable(
                        headers = segment.headers,
                        rows = segment.rows,
                        alignments = segment.alignments,
                        textColor = textColor
                    )
                }

                is MessageSegment.Quote -> {
                    QuoteBlock(
                        content = segment.content,
                        textColor = textColor
                    )
                }

                is MessageSegment.HorizontalRule -> {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                }

                is MessageSegment.OrderedListItem -> {
                    ListItem(
                        content = segment.content,
                        number = segment.number,
                        textColor = textColor,
                        isOrdered = true
                    )
                }

                is MessageSegment.UnorderedListItem -> {
                    ListItem(
                        content = segment.content,
                        textColor = textColor,
                        isOrdered = false
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
            }
        }
    }
}

@Composable
private fun CodeBlockWithCopyButton(
    code: String,
    language: String?,
    textColor: Color,
    onCreateNote: ((String) -> Unit)?
) {
    val clipboardManager = LocalClipboardManager.current
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language ?: "code",
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more),
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray.copy(alpha = 0.7f)
                )

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_copy),
                        contentDescription = "Копировать",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray.copy(alpha = 0.7f)
                    )
                }

                if (onCreateNote != null) {
                    IconButton(
                        onClick = { onCreateNote(code) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notes),
                            contentDescription = "Создать заметку",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
}

@Composable
private fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    alignments: List<MessageSegment.TableAlignment>,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        // Заголовки таблицы
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            headers.forEachIndexed { index, header ->
                val alignment = alignments.getOrNull(index) ?: MessageSegment.TableAlignment.LEFT
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = when (alignment) {
                        MessageSegment.TableAlignment.CENTER -> Alignment.Center
                        MessageSegment.TableAlignment.RIGHT -> Alignment.CenterEnd
                        else -> Alignment.CenterStart
                    }
                ) {
                    Text(
                        text = header,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Разделитель заголовков
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.Gray.copy(alpha = 0.3f)
        )

        // Строки таблицы
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEachIndexed { index, cell ->
                    val alignment = alignments.getOrNull(index) ?: MessageSegment.TableAlignment.LEFT
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = when (alignment) {
                            MessageSegment.TableAlignment.CENTER -> Alignment.Center
                            MessageSegment.TableAlignment.RIGHT -> Alignment.CenterEnd
                            else -> Alignment.CenterStart
                        }
                    ) {
                        Text(
                            text = cell,
                            color = textColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteBlock(
    content: String,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
    ) {
        // Вертикальная линия слева
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(24.dp)
                .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(1.5.dp))
        )

        Text(
            text = content,
            color = textColor.copy(alpha = 0.8f),
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ListItem(
    content: String,
    textColor: Color,
    number: Int? = null,
    isOrdered: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (isOrdered && number != null) {
            Text(
                text = "$number.",
                color = textColor,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Bullet point for unordered list
            }
        }

        Text(
            text = content,
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ThinkBlockWithHeader(
    content: String,
    durationSeconds: Float,
    textColor: Color,
    onCreateNote: ((String) -> Unit)?
) {
    val thinkBackgroundColor = MaterialTheme.colorScheme.onBackground
    var isExpanded by remember { mutableStateOf(false) }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(thinkBackgroundColor.copy(alpha = 0.8f))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                modifier = Modifier
                    .size(16.dp)
                    .rotate(arrowRotation),
                tint = textColor.copy(alpha = 0.7f)
            )
        }

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
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )

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
            Regex("\\*\\*(.+?)\\*\\*") to SpanStyle(fontWeight = FontWeight.Bold),
            Regex("\\*([^*]+?)\\*") to SpanStyle(fontStyle = FontStyle.Italic),
            Regex("`([^`]+)`") to SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color.Black,
                fontSize = 12.sp
            )
        )

        var remainingText = text
        val spans = mutableListOf<TextSpan>()

        for ((pattern, spanStyle) in patterns) {
            val matches = pattern.findAll(remainingText).toList()
            if (matches.isEmpty()) continue

            var lastEnd = 0
            for (match in matches) {
                if (match.range.first > lastEnd) {
                    spans.add(TextSpan(remainingText.substring(lastEnd, match.range.first), SpanStyle()))
                }

                val innerText = match.groupValues[1]
                spans.add(TextSpan(innerText, spanStyle))
                lastEnd = match.range.last + 1
            }

            if (lastEnd < remainingText.length) {
                spans.add(TextSpan(remainingText.substring(lastEnd), SpanStyle()))
            }

            remainingText = matches.joinToString("") { it.groupValues[1] }
        }

        if (spans.isEmpty()) {
            append(text)
        } else {
            for (span in spans) {
                withStyle(span.style) {
                    append(span.text)
                }
            }
        }
    }
}

private data class TextSpan(
    val text: String,
    val style: SpanStyle
)
