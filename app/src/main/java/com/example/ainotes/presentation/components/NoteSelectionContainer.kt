package com.example.ainotes.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun NoteSelectionContainer(
    text: String,
    onCreateNote: (String) -> Unit,
    textColor: Color,
    backgroundColor: Color,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    isCode: Boolean = false,
    modifier: Modifier = Modifier
) {

    AndroidView(
        modifier = modifier
            .then(
                if (backgroundColor != Color.Transparent && !isCode) {
                    Modifier.background(
                        backgroundColor,
                        RoundedCornerShape(if (isCode) 8.dp else 0.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(if (isCode) 0.dp else 0.dp),
        factory = { ctx ->
            TextView(ctx).apply {
                setTextIsSelectable(true)
                // Для блоков кода отключаем перенос текста
                if (isCode) {
                    setSingleLine(false)
                    setHorizontallyScrolling(true)
                }
                customSelectionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        menu.clear()
                        menu.add(0, MENU_ID_CREATE_NOTE, 0, "Создать заметку")
                        menu.add(0, MENU_ID_COPY, 1, "Копировать")
                        menu.add(0, MENU_ID_SELECT_ALL, 2, "Выбрать всё")
                        return true
                    }
                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = true
                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        val selStart = selectionStart.coerceAtLeast(0)
                        val selEnd = selectionEnd.coerceAtLeast(0)
                        val selected = text.substring(
                            minOf(selStart, selEnd),
                            maxOf(selStart, selEnd)
                        )
                        return when (item.itemId) {
                            MENU_ID_CREATE_NOTE -> {
                                onCreateNote(selected)
                                mode.finish()
                                true
                            }
                            MENU_ID_COPY -> {
                                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("text", selected))
                                mode.finish()
                                true
                            }
                            MENU_ID_SELECT_ALL -> {
                                (this@apply.text as? Spannable)?.let { Selection.selectAll(it) }
                                mode.invalidate()
                                true
                            }
                            else -> false
                        }
                    }
                    override fun onDestroyActionMode(mode: ActionMode) {}
                }
            }
        },
        update = { tv ->
            // Переставляем цвет текста и фон на каждый релэйаут
            tv.setTextColor(textColor.toArgb())
            // Для блоков кода фон устанавливается на уровне Box, поэтому делаем TextView прозрачным
            tv.setBackgroundColor(
                if (isCode) android.graphics.Color.TRANSPARENT else backgroundColor.toArgb()
            )

            // Применяем размер шрифта
            tv.textSize = fontSize.value

            // Применяем стиль шрифта
            val androidTypeface = when {
                isCode -> Typeface.MONOSPACE
                fontWeight == FontWeight.Bold && fontStyle == FontStyle.Italic ->
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)

                fontWeight == FontWeight.Bold ->
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                fontStyle == FontStyle.Italic ->
                    Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)

                else -> Typeface.DEFAULT
            }
            tv.typeface = androidTypeface

            // Применяем inline markdown разметку
            val styledText = applyInlineMarkdown(text)
            if (tv.text.toString() != styledText.toString()) {
                tv.text = styledText
            }
        }
    )
}

private fun applyInlineMarkdown(text: String): SpannableString {
    var processedText = text
    val styleRanges = mutableListOf<Triple<Int, Int, Int>>() // start, end, style

    // Обрабатываем жирный текст **text**
    var regex = Regex("\\*\\*(.+?)\\*\\*")
    var matches = regex.findAll(processedText).toList()
        .reversed() // Обрабатываем с конца, чтобы не сбивать индексы

    matches.forEach { match ->
        val start = match.range.first
        val content = match.groupValues[1]

        // Добавляем стиль для будущего применения
        styleRanges.add(Triple(start, start + content.length, Typeface.BOLD))

        // Заменяем **text** на text
        processedText = processedText.replaceRange(start, match.range.last + 1, content)
    }

    // Обрабатываем курсив *text* (но не те, что были частью **)
    regex = Regex("\\*([^*]+?)\\*")
    matches = regex.findAll(processedText).toList().reversed()

    matches.forEach { match ->
        val start = match.range.first
        val content = match.groupValues[1]

        // Добавляем стиль для будущего применения
        styleRanges.add(Triple(start, start + content.length, Typeface.ITALIC))

        // Заменяем *text* на text
        processedText = processedText.replaceRange(start, match.range.last + 1, content)
    }

    // Обрабатываем inline код `text`
    regex = Regex("`([^`]+)`")
    matches = regex.findAll(processedText).toList().reversed()

    matches.forEach { match ->
        val start = match.range.first
        val content = match.groupValues[1]

        // Добавляем специальный стиль для монospace (используем константу)
        styleRanges.add(Triple(start, start + content.length, -1)) // -1 означает monospace

        // Заменяем `text` на text
        processedText = processedText.replaceRange(start, match.range.last + 1, content)
    }

    // Создаем SpannableString с обработанным текстом
    val spannable = SpannableString(processedText)

    // Применяем все собранные стили
    styleRanges.forEach { (start, end, style) ->
        if (start < spannable.length && end <= spannable.length) {
            when (style) {
                Typeface.BOLD -> spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                Typeface.ITALIC -> spannable.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                -1 -> spannable.setSpan( // monospace
                    TypefaceSpan("monospace"),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    return spannable
}

private const val MENU_ID_CREATE_NOTE = 1
private const val MENU_ID_COPY = 2
private const val MENU_ID_SELECT_ALL = 3