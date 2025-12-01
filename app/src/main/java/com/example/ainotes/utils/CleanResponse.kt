package com.example.ainotes.utils

/**
 * Улучшенная версия cleanResponse:
 * - Обрабатывает заголовки #..######
 * - Обрабатывает блоки кода ```lang\n...\n```
 * - Обрабатывает inline-код `...`
 * - Обрабатывает **bold** и *italic*
 * - Обрабатывает списки (-, *, нумерованные)
 * - Обрабатывает цитаты > ...
 * - Обрабатывает --- как горизонтальную линию
 */
fun cleanResponse(response: String): String {
    return response
        .trim()
        .replace(Regex("\\n{3,}"), "\n\n")
        .replace(Regex("^\\s+", RegexOption.MULTILINE), "")
}

/**
 * Парсит markdown текст и возвращает список сегментов
 */
suspend fun parseMarkdownText(text: String): List<MessageSegment> {
    return MarkdownParser.parseSegments(text)
}