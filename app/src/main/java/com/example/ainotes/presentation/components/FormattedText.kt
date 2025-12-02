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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.example.ainotes.utils.MessageSegment
import com.example.ainotes.utils.parseMarkdownText
import com.example.ainotes.presentation.ui.theme.Black
import com.example.ainotes.utils.MarkdownParser
import com.example.linguareader.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FormattedText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    onCreateNote: ((String) -> Unit)? = null
) {
    var segments by remember { mutableStateOf<List<MessageSegment>>(emptyList()) }

    LaunchedEffect(text) {
        segments = withContext(Dispatchers.IO) {
            parseMarkdownText(text)
        }
    }

    if (onCreateNote != null) {
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

                    is MessageSegment.Table -> {
                        TableView(
                            headers = segment.headers,
                            rows = segment.rows,
                            alignments = segment.alignments,
                            textColor = textColor,
                            onCreateNote = onCreateNote
                        )
                    }

                    is MessageSegment.HorizontalRule -> {
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

                        is MessageSegment.Table -> {
                            TableView(
                                headers = segment.headers,
                                rows = segment.rows,
                                alignments = segment.alignments,
                                textColor = textColor,
                                onCreateNote = null
                            )
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
private fun TableCellContent(
    text: String,
    textColor: Color,
    onCreateNote: ((String) -> Unit)?,
    textStyle: TextStyle
) {
    var segments by remember { mutableStateOf<List<MessageSegment>>(emptyList()) }

    LaunchedEffect(text) {
        segments = withContext(Dispatchers.Default) {
            MarkdownParser.parseSegments(text)
        }
    }

    Column {
        segments.forEach { segment ->
            when (segment) {
                is MessageSegment.Text -> {
                    if (onCreateNote != null) {
                        NoteSelectionContainer(
                            text = segment.content,
                            onCreateNote = onCreateNote,
                            textColor = textColor,
                            backgroundColor = Color.Transparent,
                            fontSize = textStyle.fontSize,
                            fontWeight = textStyle.fontWeight ?: FontWeight.Normal,
                            fontStyle = textStyle.fontStyle ?: FontStyle.Normal,
                            isCode = false
                        )
                    } else {
                        Text(
                            text = formatInlineMarkdown(segment.content),
                            style = textStyle,
                            color = textColor,
                            softWrap = true
                        )
                    }
                }

                is MessageSegment.Code -> {
                    Text(
                        text = segment.content,
                        style = textStyle.copy(
                            fontFamily = FontFamily.Monospace,
                            background = Black
                        ),
                        color = textColor
                    )
                }

                is MessageSegment.UnorderedListItem -> {
                    Text(
                        text = "• ${segment.content}",
                        style = textStyle,
                        color = textColor
                    )
                }

                is MessageSegment.OrderedListItem -> {
                    Text(
                        text = "${segment.number}. ${segment.content}",
                        style = textStyle,
                        color = textColor
                    )
                }

                else -> {
                    // table внутри table запрещаем
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    width: Dp,
    style: TextStyle,
    color: Color,
    padding: Dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = padding, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        TableCellContent(
            text = text,
            textColor = color.copy(alpha = 0.75f),
            onCreateNote = null,
            textStyle = style
        )
    }
}

@Composable
private fun BodyCell(
    text: String,
    width: Dp,
    textStyle: TextStyle,
    color: Color,
    padding: Dp,
    onCreateNote: ((String) -> Unit)?
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = padding, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        TableCellContent(
            text = text,
            textColor = color,
            onCreateNote = onCreateNote,
            textStyle = textStyle
        )
    }
}

@Composable
private fun VerticalDivider(width: Dp, color: Color) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(color)
    )
}

@Composable
private fun HorizontalTableDivider(
    height: Dp,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(color)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableView(
    headers: List<String>,
    rows: List<List<String>>,
    alignments: List<MessageSegment.TableAlignment>,
    textColor: Color,
    onCreateNote: ((String) -> Unit)?
) {
    val horizontalScroll = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val minColWidth = 72.dp
    val maxColWidth = 320.dp
    val padding = 12.dp
    val dividerWidth = 1.dp

    val columnsCount = remember(headers, rows) {
        maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
    }

    val cellTextStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 18.sp
    )

    val headerTextStyle = TextStyle(
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium
    )

    /* ---------- COLUMN WIDTHS ---------- */

    val columnWidths = remember(headers, rows) {
        (0 until columnsCount).map { col ->
            val texts = buildList {
                headers.getOrNull(col)?.let { add(it) }
                rows.forEach { it.getOrNull(col)?.let(::add) }
            }

            val maxWidthPx = texts.maxOfOrNull { text ->
                textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = cellTextStyle,
                    constraints = Constraints(maxWidth = Int.MAX_VALUE)
                ).size.width
            } ?: with(density) { minColWidth.toPx().toInt() }

            val padded = maxWidthPx +
                    with(density) { (padding * 2).toPx().toInt() }

            with(density) {
                padded
                    .coerceIn(
                        minColWidth.toPx().toInt(),
                        maxColWidth.toPx().toInt()
                    )
                    .toDp()
            }
        }
    }

    val totalTableWidth = remember(columnWidths, columnsCount) {
        columnWidths.fold(0.dp) { acc, w -> acc + w } +
                dividerWidth * (columnsCount - 1)
    }

    val dividerColor = Color.Gray.copy(alpha = 0.3f)

    /* ---------- TABLE ---------- */

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = Black
    ) {
        Box(
            modifier = Modifier.horizontalScroll(horizontalScroll)
        ) {
            Column(
                modifier = Modifier.width(totalTableWidth)
            ) {

                /* ----- HEADER ----- */

                Row(
                    modifier = Modifier
                        .background(Black.copy(alpha = 0.85f))
                        .height(IntrinsicSize.Min)
                ) {
                    repeat(columnsCount) { col ->
                        HeaderCell(
                            text = headers.getOrNull(col).orEmpty(),
                            width = columnWidths[col],
                            style = headerTextStyle,
                            color = textColor,
                            padding = padding
                        )
                        if (col != columnsCount - 1) {
                            VerticalDivider(dividerWidth, dividerColor)
                        }
                    }
                }

                HorizontalTableDivider(
                    height = 1.25.dp,
                    color = dividerColor
                )

                /* ----- BODY (БЕЗ LazyColumn ❗) ----- */

                Column {
                    rows.forEachIndexed { rowIndex, row ->

                        Row(
                            modifier = Modifier
                                .background(
                                    if (rowIndex % 2 == 0)
                                        Black.copy(alpha = 0.55f)
                                    else
                                        Black.copy(alpha = 0.45f)
                                )
                                .height(IntrinsicSize.Min)
                        ) {
                            repeat(columnsCount) { col ->
                                BodyCell(
                                    text = row.getOrNull(col).orEmpty(),
                                    width = columnWidths[col],
                                    textStyle = cellTextStyle,
                                    color = textColor,
                                    padding = padding,
                                    onCreateNote = onCreateNote
                                )

                                if (col != columnsCount - 1) {
                                    VerticalDivider(dividerWidth, dividerColor)
                                }
                            }
                        }

                        /* ---------- ГОРИЗОНТАЛЬНАЯ ЛИНИЯ МЕЖДУ СТРОКАМИ ---------- */

                        if (rowIndex != rows.lastIndex) {
                            HorizontalTableDivider(
                                height = 0.75.dp,
                                color = dividerColor.copy(alpha = 0.6f)
                            )
                        }
                        HorizontalTableDivider(
                            height = 1.dp,
                            color = dividerColor
                        )
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Black.copy(alpha = 0.8f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language?.uppercase() ?: "CODE",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )

            IconButton(
                onClick = {
                    val clip = ClipData.newPlainText("code", code)
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                    Toast.makeText(context, "Код скопирован", Toast.LENGTH_SHORT).show()
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

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.Gray.copy(alpha = 0.3f)
        )

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
    val thinkBackgroundColor = Color(0xFF1A1A2E)
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
                background = Black,
                fontSize = 12.sp
            )
        )

        val matches = mutableListOf<Triple<IntRange, String, SpanStyle>>()
        patterns.forEach { (regex, style) ->
            regex.findAll(text).forEach { match ->
                matches.add(Triple(match.range, match.groupValues[1], style))
            }
        }

        matches.sortBy { it.first.first }

        var lastEnd = 0
        matches.forEach { (range, content, style) ->
            if (range.first > lastEnd) {
                append(text.substring(lastEnd, range.first))
            }

            withStyle(style) {
                append(content)
            }

            lastEnd = range.last + 1
        }

        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
}