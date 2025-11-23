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
        .replace(Regex("\\n{3,}"), "\n\n") // Убираем лишние переносы строк
        .replace(Regex("^\\s+", RegexOption.MULTILINE), "") // Убираем лишние пробелы в начале строк
}

/**
 * Старая версия cleanResponse - убирает всю разметку (оставлена для совместимости)
 */
fun cleanResponseOld(response: String): String {
    // 1) Найдём все блоки кода и разобьём текст на части: текст / кодовый блок
    data class Part(val isCodeBlock: Boolean, val lang: String?, val content: String)

    val codeBlockRegex = Regex("(?s)```(?:([\\w#+-]+)\\n)?(.*?)```")
    val parts = mutableListOf<Part>()
    var lastIdx = 0
    for (m in codeBlockRegex.findAll(response)) {
        val start = m.range.first
        val end = m.range.last + 1
        if (start > lastIdx) {
            parts += Part(isCodeBlock = false, lang = null, content = response.substring(lastIdx, start))
        }
        val lang = m.groupValues[1].ifBlank { null }
        val codeContent = m.groupValues[2]
        parts += Part(isCodeBlock = true, lang = lang, content = codeContent)
        lastIdx = end
    }
    if (lastIdx < response.length) {
        parts += Part(isCodeBlock = false, lang = null, content = response.substring(lastIdx))
    }

    // Inline pattern: inline code | bold | italic
    val inlinePattern = Regex("`([^`]+)`|\\*\\*(.+?)\\*\\*|\\*(.+?)\\*")

    val result = StringBuilder()
    for ((index, part) in parts.withIndex()) {
        if (part.isCodeBlock) {
            result.append("\n\n")
            part.lang?.let { lang ->
                result.append(lang.uppercase())
                result.append("\n")
            }
            result.append(part.content.trimEnd())
            result.append("\n")
            result.append("\n\n")
        } else {
            val lines = part.content.split("\n")
            for ((i, rawLine) in lines.withIndex()) {
                var line = rawLine

                if (line.trim().matches(Regex("^-{3,}\\s*$"))) {
                    result.append("\n   ━━━━━━━━━━━━━━━━   \n\n")
                    continue
                }

                val headerMatch = Regex("^\\s*(#{1,6})\\s*(.*)$").find(line)
                if (headerMatch != null) {
                    val text = headerMatch.groupValues[2].trim()
                    result.append("\n")
                    result.append(text)
                    result.append("\n\n")
                    continue
                }

                val quoteMatch = Regex("^\\s*>\\s?(.*)$").find(line)
                if (quoteMatch != null) {
                    val qText = quoteMatch.groupValues[1]
                    result.append("▌ ")
                    var last = 0
                    for (m in inlinePattern.findAll(qText)) {
                        result.append(qText.substring(last, m.range.first))
                        when {
                            m.groups[1] != null -> result.append(m.groups[1]!!.value)
                            m.groups[2] != null -> result.append(m.groups[2]!!.value)
                            m.groups[3] != null -> result.append(m.groups[3]!!.value)
                        }
                        last = m.range.last + 1
                    }
                    result.append(qText.substring(last))
                    result.append("\n")
                    continue
                }

                val unorderedMatch = Regex("^\\s*[-*]\\s+(.*)$").find(line)
                if (unorderedMatch != null) {
                    val item = unorderedMatch.groupValues[1]
                    result.append("   • ")
                    var last = 0
                    for (m in inlinePattern.findAll(item)) {
                        result.append(item.substring(last, m.range.first))
                        when {
                            m.groups[1] != null -> result.append(m.groups[1]!!.value)
                            m.groups[2] != null -> result.append(m.groups[2]!!.value)
                            m.groups[3] != null -> result.append(m.groups[3]!!.value)
                        }
                        last = m.range.last + 1
                    }
                    result.append(item.substring(last))
                    result.append("\n")
                    continue
                }

                val orderedMatch = Regex("^\\s*(\\d+)\\.\\s+(.*)$").find(line)
                if (orderedMatch != null) {
                    val number = orderedMatch.groupValues[1]
                    val item = orderedMatch.groupValues[2]
                    result.append("   $number. ")
                    var last = 0
                    for (m in inlinePattern.findAll(item)) {
                        result.append(item.substring(last, m.range.first))
                        when {
                            m.groups[1] != null -> result.append(m.groups[1]!!.value)
                            m.groups[2] != null -> result.append(m.groups[2]!!.value)
                            m.groups[3] != null -> result.append(m.groups[3]!!.value)
                        }
                        last = m.range.last + 1
                    }
                    result.append(item.substring(last))
                    result.append("\n")
                    continue
                }

                var last = 0
                for (m in inlinePattern.findAll(line)) {
                    result.append(line.substring(last, m.range.first))
                    when {
                        m.groups[1] != null -> result.append(m.groups[1]!!.value)
                        m.groups[2] != null -> result.append(m.groups[2]!!.value)
                        m.groups[3] != null -> result.append(m.groups[3]!!.value)
                    }
                    last = m.range.last + 1
                }
                result.append(line.substring(last))
                if (i < lines.size - 1) result.append("\n")
            }
        }
        if (index < parts.size - 1) result.append("\n")
    }

    return result.toString()
}

/**
 * Форматирует только текст без обработки блоков кода ```
 * Используется для обработки отдельных текстовых сегментов
 */
fun cleanTextOnly(text: String): String {
    // Inline pattern: inline code | bold | italic
    val inlinePattern = Regex("`([^`]+)`|\\*\\*(.+?)\\*\\*|\\*(.+?)\\*")

    val result = StringBuilder()
    val lines = text.split("\n")
    for ((i, rawLine) in lines.withIndex()) {
        var line = rawLine

        if (line.trim().matches(Regex("^-{3,}\\s*$"))) {
            result.append("\n   ━━━━━━━━━━━━━━━━   \n\n")
            continue
        }

        val headerMatch = Regex("^\\s*(#{1,6})\\s*(.*)$").find(line)
        if (headerMatch != null) {
            val headerText = headerMatch.groupValues[2].trim()
            result.append("\n")
            result.append(headerText)
            result.append("\n\n")
            continue
        }

        val quoteMatch = Regex("^\\s*>\\s?(.*)$").find(line)
        if (quoteMatch != null) {
            val qText = quoteMatch.groupValues[1]
            result.append("▌ ")
            var last = 0
            for (m in inlinePattern.findAll(qText)) {
                result.append(qText.substring(last, m.range.first))
                when {
                    m.groups[1] != null -> result.append(m.groups[1]!!.value)
                    m.groups[2] != null -> result.append(m.groups[2]!!.value)
                    m.groups[3] != null -> result.append(m.groups[3]!!.value)
                }
                last = m.range.last + 1
            }
            result.append(qText.substring(last))
            result.append("\n")
            continue
        }

        val unorderedMatch = Regex("^\\s*[-*]\\s+(.*)$").find(line)
        if (unorderedMatch != null) {
            val item = unorderedMatch.groupValues[1]
            result.append("   • ")
            var last = 0
            for (m in inlinePattern.findAll(item)) {
                result.append(item.substring(last, m.range.first))
                when {
                    m.groups[1] != null -> result.append(m.groups[1]!!.value)
                    m.groups[2] != null -> result.append(m.groups[2]!!.value)
                    m.groups[3] != null -> result.append(m.groups[3]!!.value)
                }
                last = m.range.last + 1
            }
            result.append(item.substring(last))
            result.append("\n")
            continue
        }

        val orderedMatch = Regex("^\\s*(\\d+)\\.\\s+(.*)$").find(line)
        if (orderedMatch != null) {
            val number = orderedMatch.groupValues[1]
            val item = orderedMatch.groupValues[2]
            result.append("   $number. ")
            var last = 0
            for (m in inlinePattern.findAll(item)) {
                result.append(item.substring(last, m.range.first))
                when {
                    m.groups[1] != null -> result.append(m.groups[1]!!.value)
                    m.groups[2] != null -> result.append(m.groups[2]!!.value)
                    m.groups[3] != null -> result.append(m.groups[3]!!.value)
                }
                last = m.range.last + 1
            }
            result.append(item.substring(last))
            result.append("\n")
            continue
        }

        var last = 0
        for (m in inlinePattern.findAll(line)) {
            result.append(line.substring(last, m.range.first))
            when {
                m.groups[1] != null -> result.append(m.groups[1]!!.value)
                m.groups[2] != null -> result.append(m.groups[2]!!.value)
                m.groups[3] != null -> result.append(m.groups[3]!!.value)
            }
            last = m.range.last + 1
        }
        result.append(line.substring(last))
        if (i < lines.size - 1) result.append("\n")
    }

    return result.toString()
}

/**
 * Парсит markdown текст и возвращает список сегментов
 */
fun parseMarkdownText(text: String): List<MessageSegment> {
    return MarkdownParser.parseSegments(text)
}

/**
 * Тестовая функция для проверки парсинга markdown
 */
fun testMarkdownParsing(): String {
    val testText = """# Заголовок 1
## Заголовок 2

Обычный текст с **жирным** и *курсивом* и `кодом`.

> Это цитата

- Первый пункт списка
- Второй пункт списка

1. Нумерованный список
2. Второй пункт

---

```kotlin
fun hello() {
    println("Hello, World!")
}
```

Еще текст после кода."""

    val segments = parseMarkdownText(testText)
    val result = StringBuilder()
    result.append("Parsed ${segments.size} segments:\n")
    segments.forEachIndexed { index, segment ->
        result.append("$index: ${segment::class.simpleName}")
        when (segment) {
            is MessageSegment.Header -> result.append(" (level=${segment.level}, content='${segment.content}')")
            is MessageSegment.Text -> result.append(" (content='${segment.content.take(50)}...')")
            is MessageSegment.Think -> result.append(" (duration=${segment.durationSeconds}s, content='${segment.content.take(50)}...')")
            is MessageSegment.Code -> result.append(
                " (lang=${segment.language}, content='${
                    segment.content.take(
                        30
                    )
                }...')"
            )

            is MessageSegment.Quote -> result.append(" (content='${segment.content}')")
            is MessageSegment.UnorderedListItem -> result.append(" (content='${segment.content}')")
            is MessageSegment.OrderedListItem -> result.append(" (number=${segment.number}, content='${segment.content}')")
            is MessageSegment.HorizontalRule -> result.append(" (horizontal rule)")
        }
        result.append("\n")
    }
    return result.toString()
}

/**
 * Простой тест для проверки отображения markdown
 */
fun simpleMarkdownTest(): String {
    val testCases = listOf(
        "# Заголовок 1" to "Header(1)",
        "## Заголовок 2" to "Header(2)",
        "**жирный текст**" to "Text with bold",
        "*курсив*" to "Text with italic",
        "`код`" to "Text with code",
        "> цитата" to "Quote",
        "- пункт списка" to "UnorderedListItem",
        "1. нумерованный" to "OrderedListItem(1)",
        "---" to "HorizontalRule",
        "```kotlin\ncode\n```" to "Code(kotlin)"
    )

    val results = StringBuilder()
    testCases.forEach { (input, expected) ->
        val segments = parseMarkdownText(input)
        val actual = when (val segment = segments.firstOrNull()) {
            is MessageSegment.Header -> "Header(${segment.level})"
            is MessageSegment.Text -> "Text with ${
                if (input.contains("**")) "bold" else if (input.contains(
                        "*"
                    )
                ) "italic" else if (input.contains("`")) "code" else "plain"
            }"

            is MessageSegment.Quote -> "Quote"
            is MessageSegment.UnorderedListItem -> "UnorderedListItem"
            is MessageSegment.OrderedListItem -> "OrderedListItem(${segment.number})"
            is MessageSegment.HorizontalRule -> "HorizontalRule"
            is MessageSegment.Code -> "Code(${segment.language ?: "no-lang"})"
            is MessageSegment.Think -> "Think(${segment.durationSeconds}s)"
            null -> "null"
        }
        val status = if (actual.contains(expected.split("(")[0])) "✅" else "❌"
        results.append("$status '$input' -> $actual (expected: $expected)\n")
    }

    return results.toString()
}