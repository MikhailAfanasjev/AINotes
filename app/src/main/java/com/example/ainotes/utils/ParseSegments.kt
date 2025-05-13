package com.example.ainotes.utils

sealed class Segment {
    data class Text(val content: String) : Segment()
    data class Code(val content: String) : Segment()
}

/**
 * Разбивает строку rawMarkdown на список Segment.Text и Segment.Code
 * по тройным ``` ``` блокам.
 */
fun parseSegments(raw: String): List<Segment> {
    val regex = Regex("```(?:\\w+)?\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
    val segments = mutableListOf<Segment>()
    var lastIndex = 0

    for (m in regex.findAll(raw)) {
        // обычный текст до кода
        if (m.range.first > lastIndex) {
            segments += Segment.Text(raw.substring(lastIndex, m.range.first))
        }
        // контент блока кода
        val code = m.groupValues[1].trim('\n')
        segments += Segment.Code(code)
        lastIndex = m.range.last + 1
    }
    // остаток после последнего блока
    if (lastIndex < raw.length) {
        segments += Segment.Text(raw.substring(lastIndex))
    }
    return segments
}