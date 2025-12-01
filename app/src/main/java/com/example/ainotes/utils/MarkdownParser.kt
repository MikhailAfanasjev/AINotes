package com.example.ainotes.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MarkdownParser {

    /**
     * Разбирает входную строку с Markdown-разметкой на список сегментов
     */
    suspend fun parseSegments(input: String): List<MessageSegment> = withContext(Dispatchers.Default) {
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

            // Проверяем на таблицу
            if (isTableRow(trimmedLine)) {
                val tableData = parseTable(lines, i)
                if (tableData.rows.isNotEmpty()) {
                    segments.add(MessageSegment.Table(tableData.headers, tableData.rows, tableData.alignments))
                    i = tableData.endIndex
                    continue
                }
            }

            // Горизонтальная линия
            if (trimmedLine.matches(Regex("^[-*_]{3,}\\s*$"))) {
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
                val quoteLines = mutableListOf<String>()
                var j = i

                while (j < lines.size && lines[j].trim().let {
                        it.startsWith("> ") || it == ">"
                    }) {
                    quoteLines.add(lines[j].trim().removePrefix(">").trim())
                    j++
                }

                if (quoteLines.isNotEmpty()) {
                    segments.add(MessageSegment.Quote(quoteLines.joinToString("\n")))
                    i = j
                    continue
                }
            }

            // Нумерованные списки 1. 2. 3.
            val orderedListMatch = Regex("^(\\d+)\\.\\s+(.+)$").find(trimmedLine)
            if (orderedListMatch != null) {
                val listItems = mutableListOf<Pair<Int, String>>()
                var j = i
                var expectedNumber = 1

                while (j < lines.size) {
                    val currentLine = lines[j].trim()
                    val match = Regex("^(\\d+)\\.\\s+(.+)$").find(currentLine)

                    if (match != null) {
                        val number = match.groupValues[1].toInt()
                        val content = match.groupValues[2]
                        listItems.add(number to content)
                        expectedNumber = number + 1
                        j++
                    } else {
                        break
                    }
                }

                listItems.forEach { (number, content) ->
                    segments.add(MessageSegment.OrderedListItem(number, content))
                }
                i = j
                continue
            }

            // Маркированные списки - или *
            val unorderedListMatch = Regex("^[-*+]\\s+(.+)$").find(trimmedLine)
            if (unorderedListMatch != null) {
                val listItems = mutableListOf<String>()
                var j = i

                while (j < lines.size) {
                    val currentLine = lines[j].trim()
                    val match = Regex("^[-*+]\\s+(.+)$").find(currentLine)

                    if (match != null) {
                        listItems.add(match.groupValues[1])
                        j++
                    } else {
                        break
                    }
                }

                listItems.forEach { content ->
                    segments.add(MessageSegment.UnorderedListItem(content))
                }
                i = j
                continue
            }

            // Обычный текст (включая пустые строки)
            if (trimmedLine.isNotEmpty() || line.isEmpty()) {
                // Собираем последовательные строки обычного текста
                val textLines = mutableListOf<String>()
                var j = i

                while (j < lines.size) {
                    val currentLine = lines[j]
                    val currentTrimmed = currentLine.trim()

                    // Проверяем, не является ли строка специальным элементом
                    if (currentTrimmed.startsWith("```") ||
                        currentTrimmed.startsWith("<<<THINK_BLOCK>>>") ||
                        currentTrimmed.matches(Regex("^[-*_]{3,}\\s*$")) ||
                        Regex("^#{1,6}\\s+").find(currentTrimmed) != null ||
                        currentTrimmed.startsWith("> ") ||
                        currentTrimmed == ">" ||
                        Regex("^\\d+\\.\\s+").find(currentTrimmed) != null ||
                        Regex("^[-*+]\\s+").find(currentTrimmed) != null ||
                        isTableRow(currentTrimmed)
                    ) {
                        break
                    }

                    textLines.add(currentLine)
                    j++
                }

                if (textLines.isNotEmpty()) {
                    val textContent = textLines.joinToString("\n").trim()
                    if (textContent.isNotEmpty()) {
                        segments.add(MessageSegment.Text(textContent))
                    }
                }
                i = if (j > i) j else i + 1
                continue
            }

            i++
        }

        return@withContext segments
    }

    /**
     * Проверяет, является ли строка строкой таблицы
     */
    private fun isTableRow(line: String): Boolean {
        return line.contains('|') &&
                !line.trim().startsWith("```") &&
                line.trim().isNotEmpty()
    }

    /**
     * Парсит таблицу из Markdown
     */
    private fun parseTable(lines: List<String>, startIndex: Int): TableParseResult {
        val tableLines = mutableListOf<String>()
        var i = startIndex

        // Собираем все строки таблицы
        while (i < lines.size && isTableRow(lines[i].trim())) {
            tableLines.add(lines[i].trim())
            i++
        }

        if (tableLines.size < 2) {
            return TableParseResult(emptyList(), emptyList(), emptyList(), startIndex)
        }

        // Парсим заголовки
        val headerRow = tableLines[0]
        val headers = parseTableRow(headerRow)

        // Парсим выравнивание
        val alignmentRow = tableLines[1]
        val alignments = parseTableAlignment(alignmentRow, headers.size)

        // Парсим данные
        val dataRows = mutableListOf<List<String>>()
        for (j in 2 until tableLines.size) {
            val rowData = parseTableRow(tableLines[j])
            if (rowData.size == headers.size) {
                dataRows.add(rowData)
            }
        }

        return TableParseResult(headers, dataRows, alignments, i)
    }

    /**
     * Парсит строку таблицы
     */
    private fun parseTableRow(row: String): List<String> {
        return row.split('|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Парсит строку выравнивания таблицы
     */
    private fun parseTableAlignment(alignmentRow: String, expectedColumns: Int): List<MessageSegment.TableAlignment> {
        val alignmentCells = parseTableRow(alignmentRow)
        val alignments = mutableListOf<MessageSegment.TableAlignment>()

        for (i in 0 until expectedColumns) {
            val cell = if (i < alignmentCells.size) alignmentCells[i] else ""
            val alignment = when {
                cell.startsWith(":") && cell.endsWith(":") -> MessageSegment.TableAlignment.CENTER
                cell.endsWith(":") -> MessageSegment.TableAlignment.RIGHT
                else -> MessageSegment.TableAlignment.LEFT
            }
            alignments.add(alignment)
        }

        return alignments
    }

    /**
     * Класс для хранения результата обработки think-блоков
     */
    private data class ProcessedInput(
        val lines: List<String>,
        val thinkBlocks: List<MessageSegment.Think>
    )

    /**
     * Результат парсинга таблицы
     */
    private data class TableParseResult(
        val headers: List<String>,
        val rows: List<List<String>>,
        val alignments: List<MessageSegment.TableAlignment>,
        val endIndex: Int
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
        val thoughtPattern = Regex(
            "Thought for ([\\d.]+) seconds?\\s*\\n\\s*\\n([\\s\\S]*?)(?=\\n\\s*\\n|$)",
            RegexOption.IGNORE_CASE
        )

        val matches = thoughtPattern.findAll(processedText).toList()
        val tempBlocks = mutableListOf<Pair<Int, MessageSegment.Think>>()

        matches.reversed().forEach { match ->
            val durationStr = match.groupValues[1]
            val duration = durationStr.toFloatOrNull() ?: 0f
            val thinkContent = match.groupValues[2].trim()

            val currentIndex = blockIndex
            tempBlocks.add(0, currentIndex to MessageSegment.Think(thinkContent, duration))

            val startPos = match.range.first
            val endPos = match.range.last + 1

            processedText = processedText.substring(0, startPos) +
                    "<<<THINK_BLOCK>>>$currentIndex" +
                    processedText.substring(minOf(endPos, processedText.length))

            blockIndex++
        }

        tempBlocks.forEach { (_, block) ->
            thinkBlocks.add(block)
        }

        return ProcessedInput(
            lines = processedText.split("\n"),
            thinkBlocks = thinkBlocks
        )
    }
}