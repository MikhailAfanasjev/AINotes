package com.example.ainotes.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Берёт Markdown-подобную строку и превращает её в AnnotatedString:
 * 1) **bold** → SpanStyle(fontWeight = Bold)
 * 2) *italic* → SpanStyle(fontStyle = Italic)
 * 3) линии, начинающиеся с "-" или "*" → "— "
 * 4) ### Заголовок → переведённый в UPPERCASE между пустыми строками
 */
fun cleanResponse(response: String): AnnotatedString {
    // 1. Обработка списков и заголовков
    val preprocessed = response
        .replace(Regex("(?m)^\\s*[-*]\\s+"), "— ")
        .replace(Regex("(?m)^###\\s*(.*)$")) { m ->
            m.groupValues[1]
                .replaceFirstChar { it.uppercaseChar() }
                .uppercase()
        }
    // 2. Шаблон для кодового блока: учитывает необязательную метку языка
    val codeBlockPattern = Regex("(?s)(?:```\\w+\\n)?```?\\n(.*?)```?")
    val parts = mutableListOf<Pair<String, SpanStyle?>>()
    var lastIndex = 0
    for (match in codeBlockPattern.findAll(preprocessed)) {
        val start = match.range.first
        val end = match.range.last + 1
        if (start > lastIndex) {
            parts += preprocessed.substring(lastIndex, start) to null
        }
        val rawCode = match.groupValues[1].trim('\n')
        val codeWithPadding = "\n$rawCode\n"
        val codeStyle = SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = Color(0xFFE0EEEE)
        )
        parts += codeWithPadding to codeStyle
        lastIndex = end
    }
    if (lastIndex < preprocessed.length) {
        parts += preprocessed.substring(lastIndex) to null
    }
    // 3. Собираем конечный AnnotatedString, применяя подчёркивания и полужирное
    val inlinePattern = Regex("\\*\\*(.*?)\\*\\*|\\*(.*?)\\*")
    return buildAnnotatedString {
        for ((text, style) in parts) {
            if (style != null) {
                withStyle(style) { append(text) }
            } else {
                var last = 0
                for (m in inlinePattern.findAll(text)) {
                    append(text.substring(last, m.range.first))
                    when {
                        m.groups[1] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(m.groups[1]!!.value)
                        }
                        m.groups[2] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(m.groups[2]!!.value)
                        }
                    }
                    last = m.range.last + 1
                }
                append(text.substring(last))
            }
        }
    }
}