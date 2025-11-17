package com.example.ainotes.utils

object MarkdownParser {

    /**
     * Разбирает входную строку с Markdown-разметкой на список сегментов
     */
    fun parseSegments(input: String): List<MessageSegment> {
        val segments = mutableListOf<MessageSegment>()
        val lines = input.split("\n")
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmedLine = line.trim()

            // Проверяем на блок кода
            if (trimmedLine.startsWith("```")) {
                val language = trimmedLine.substring(3).trim().ifEmpty { null }
                val codeLines = mutableListOf<String>()
                i++ // пропускаем строку с ```

                // Собираем содержимое блока кода
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }

                if (codeLines.isNotEmpty()) {
                    segments.add(MessageSegment.Code(codeLines.joinToString("\n"), language))
                }
                i++ // пропускаем закрывающую ```
                continue
            }

            // Горизонтальная линия
            if (trimmedLine.matches(Regex("^-{3,}\\s*$"))) {
                segments.add(MessageSegment.HorizontalRule)
                i++
                continue
            }

            // Заголовки # ## ### #### ##### ######
            val headerMatch = Regex("^(#{1,6})\\s+(.+)$").find(trimmedLine)
            if (headerMatch != null) {
                val level = headerMatch.groupValues[1].length
                val content = headerMatch.groupValues[2].trim()
                segments.add(MessageSegment.Header(level, content))
                i++
                continue
            }

            // Цитаты > текст
            if (trimmedLine.startsWith("> ")) {
                val content = trimmedLine.substring(2)
                segments.add(MessageSegment.Quote(content))
                i++
                continue
            }

            // Нумерованные списки 1. 2. 3.
            val orderedListMatch = Regex("^(\\d+)\\.\\s+(.+)$").find(trimmedLine)
            if (orderedListMatch != null) {
                val number = orderedListMatch.groupValues[1].toInt()
                val content = orderedListMatch.groupValues[2]
                segments.add(MessageSegment.OrderedListItem(number, content))
                i++
                continue
            }

            // Маркированные списки - или *
            val unorderedListMatch = Regex("^[-*]\\s+(.+)$").find(trimmedLine)
            if (unorderedListMatch != null) {
                val content = unorderedListMatch.groupValues[1]
                segments.add(MessageSegment.UnorderedListItem(content))
                i++
                continue
            }

            // Обычный текст (включая пустые строки)
            if (trimmedLine.isNotEmpty() || line.isEmpty()) {
                // Собираем последовательные строки обычного текста
                val textLines = mutableListOf<String>()
                while (i < lines.size) {
                    val currentLine = lines[i]
                    val currentTrimmed = currentLine.trim()

                    // Проверяем, не является ли строка специальным элементом
                    if (currentTrimmed.startsWith("```") ||
                        currentTrimmed.matches(Regex("^-{3,}\\s*$")) ||
                        Regex("^#{1,6}\\s+").find(currentTrimmed) != null ||
                        currentTrimmed.startsWith("> ") ||
                        Regex("^\\d+\\.\\s+").find(currentTrimmed) != null ||
                        Regex("^[-*]\\s+").find(currentTrimmed) != null
                    ) {
                        break
                    }

                    textLines.add(currentLine)
                    i++
                }

                if (textLines.isNotEmpty()) {
                    val textContent = textLines.joinToString("\n").trim()
                    if (textContent.isNotEmpty()) {
                        segments.add(MessageSegment.Text(textContent))
                    }
                }
                continue
            }

            i++
        }

        return segments
    }
}