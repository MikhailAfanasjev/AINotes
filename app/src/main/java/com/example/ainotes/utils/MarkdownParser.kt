package com.example.ainotes.utils

object MarkdownParser {

    /**
     * Разбирает входную строку с Markdown-разметкой на список сегментов
     */
    fun parseSegments(input: String): List<MessageSegment> {
        // Сначала обрабатываем <think> блоки, так как они могут быть многострочными
        val processedInput = processThinkBlocks(input)

        val segments = mutableListOf<MessageSegment>()
        val lines = processedInput.lines
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmedLine = line.trim()

            // Проверяем на блок think (уже обработанный)
            if (trimmedLine.startsWith("<<<THINK_BLOCK>>>")) {
                val thinkIndex = trimmedLine.substringAfter("<<<THINK_BLOCK>>>").toIntOrNull()
                if (thinkIndex != null && thinkIndex < processedInput.thinkBlocks.size) {
                    segments.add(processedInput.thinkBlocks[thinkIndex])
                }
                i++
                continue
            }

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
                        currentTrimmed.startsWith("<<<THINK_BLOCK>>>") ||
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

    /**
     * Класс для хранения результата обработки think-блоков
     */
    private data class ProcessedInput(
        val lines: List<String>,
        val thinkBlocks: List<MessageSegment.Think>
    )

    /**
     * Обрабатывает блоки <think>...</think> и текстовый формат "Thought for X seconds"
     * и заменяет их на плейсхолдеры
     */
    private fun processThinkBlocks(input: String): ProcessedInput {
        val thinkBlocks = mutableListOf<MessageSegment.Think>()
        var processedText = input
        var blockIndex = 0

        // 1. Обрабатываем <think>...</think> теги
        val thinkRegex = Regex("<think>([\\s\\S]*?)</think>", RegexOption.IGNORE_CASE)
        thinkRegex.findAll(input).forEach { match ->
            val thinkContent = match.groupValues[1].trim()
            thinkBlocks.add(MessageSegment.Think(thinkContent, 0f))
            processedText = processedText.replaceFirst(
                match.value,
                "\n<<<THINK_BLOCK>>>$blockIndex\n"
            )
            blockIndex++
        }

        // 2. Обрабатываем текстовый формат "Thought for X seconds"
        // Паттерн: "Thought for X seconds" за которым следует содержимое до двойной пустой строки
        val thoughtPattern = Regex(
            "Thought for ([\\d.]+) seconds?\\s*\\n\\s*\\n([\\s\\S]*?)(?=\\n\\s*\\n|$)",
            RegexOption.IGNORE_CASE
        )

        // Собираем все совпадения
        val matches = thoughtPattern.findAll(processedText).toList()

        // Создаем временный список для блоков с правильными индексами
        val tempBlocks = mutableListOf<Pair<Int, MessageSegment.Think>>()

        // Обрабатываем с конца, чтобы не нарушить позиции при замене
        matches.reversed().forEach { match ->
            val durationStr = match.groupValues[1]
            val duration = durationStr.toFloatOrNull() ?: 0f
            val thinkContent = match.groupValues[2].trim()

            // Сохраняем индекс и блок
            val currentIndex = blockIndex
            tempBlocks.add(0, currentIndex to MessageSegment.Think(thinkContent, duration))

            // Заменяем на плейсхолдер
            val startPos = match.range.first
            val endPos = match.range.last + 1

            processedText = processedText.substring(0, startPos) +
                    "<<<THINK_BLOCK>>>$currentIndex" +
                    processedText.substring(minOf(endPos, processedText.length))

            blockIndex++
        }

        // Добавляем блоки в правильном порядке
        tempBlocks.forEach { (_, block) ->
            thinkBlocks.add(block)
        }

        return ProcessedInput(
            lines = processedText.split("\n"),
            thinkBlocks = thinkBlocks
        )
    }
}