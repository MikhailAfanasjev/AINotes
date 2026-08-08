package com.example.ainotes.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MarkdownParser {

    /**
     * Разбирает входную строку с Markdown-разметкой на список сегментов.
     * Использует lazy sequence для эффективной обработки без промежуточных мутаций.
     */
    suspend fun parseSegments(input: String): List<MessageSegment> = withContext(Dispatchers.Default) {
        val processedInput = processThinkBlocks(input)

        // Вспомогательная функция для парсинга таблицы с доступом к processedInput
        fun parseTableFrom(startIndex: Int, pi: ProcessedInput): TableParseResult {
            val tableLines = mutableListOf<String>()
            var i = startIndex

            while (i < pi.lines.size && isTableRow(pi.lines[i].trim())) {
                tableLines.add(pi.lines[i].trim())
                i++
            }

            if (tableLines.size < 2) {
                return@parseTableFrom TableParseResult(emptyList(), emptyList(), emptyList(), startIndex)
            }

            val headers = parseTableRow(tableLines[0])
            val alignments = parseTableAlignment(tableLines[1], headers.size)

            val dataRows = tableLines.drop(2).mapNotNull { row ->
                val rowData = parseTableRow(row)
                if (rowData.size == headers.size) rowData else null
            }

            return@parseTableFrom TableParseResult(headers, dataRows, alignments, i)
        }

        // Рекурсивный парсер: возвращает последовательность (сегмент, следующий_индекс)
        fun parseFrom(index: Int): Sequence<Pair<MessageSegment, Int>> = sequence {
            if (index >= processedInput.lines.size) return@sequence

            val line = processedInput.lines[index]
            val trimmedLine = line.trim()

            when {
                // Блок think
                trimmedLine.startsWith("<<<THINK_BLOCK>>>") -> {
                    val thinkIndex = trimmedLine.substringAfter("<<<THINK_BLOCK>>>").toIntOrNull()
                    if (thinkIndex != null && thinkIndex < processedInput.thinkBlocks.size) {
                        yield(processedInput.thinkBlocks[thinkIndex] to index + 1)
                    } else {
                        yieldAll(parseFrom(index + 1))
                    }
                }

                // Блок кода ```...```
                trimmedLine.startsWith("```") -> {
                    val language = trimmedLine.substring(3).trim().ifEmpty { null }
                    var i = index + 1
                    val codeLines = mutableListOf<String>()

                    while (i < processedInput.lines.size && !processedInput.lines[i].trim().startsWith("```")) {
                        codeLines.add(processedInput.lines[i])
                        i++
                    }

                    if (codeLines.isNotEmpty() && i < processedInput.lines.size) {
                        yield(MessageSegment.Code(codeLines.joinToString("\n"), language) to i + 1)
                    } else {
                        yieldAll(parseFrom(i))
                    }
                }

                // Таблица
                isTableRow(trimmedLine) -> {
                    val tableResult = parseTableFrom(index, processedInput)
                    if (tableResult.rows.isNotEmpty()) {
                        yield(MessageSegment.Table(tableResult.headers, tableResult.rows, tableResult.alignments) to tableResult.endIndex)
                    } else {
                        yieldAll(parseFrom(index + 1))
                    }
                }

                // Горизонтальная линия ---
                trimmedLine.matches(Regex("^[-*_]{3,}\\s*$")) -> {
                    yield(MessageSegment.HorizontalRule to index + 1)
                }

                // Заголовок #...######
                Regex("^(#{1,6})\\s+(.+)$").find(trimmedLine) != null -> {
                    val headerMatch = Regex("^(#{1,6})\\s+(.+)$").find(trimmedLine)!!
                    yield(MessageSegment.Header(headerMatch.groupValues[1].length, headerMatch.groupValues[2].trim()) to index + 1)
                }

                // Цитата > ...
                trimmedLine.startsWith("> ") || trimmedLine == ">" -> {
                    var j = index
                    val quoteLines = mutableListOf<String>()

                    while (j < processedInput.lines.size && processedInput.lines[j].trim().let { it.startsWith("> ") || it == ">" }) {
                        quoteLines.add(processedInput.lines[j].trim().removePrefix(">").trim())
                        j++
                    }

                    if (quoteLines.isNotEmpty()) {
                        yield(MessageSegment.Quote(quoteLines.joinToString("\n")) to j)
                    } else {
                        yieldAll(parseFrom(index + 1))
                    }
                }

                // Нумерованный список 1. 2. 3.
                Regex("^\\d+\\.\\s+").find(trimmedLine) != null -> {
                    var j = index
                    val listItems = mutableListOf<Pair<Int, String>>()

                    while (j < processedInput.lines.size) {
                        val currentLine = processedInput.lines[j].trim()
                        val match = Regex("^(\\d+)\\.\\s+(.+)$").find(currentLine)
                        if (match != null) {
                            listItems.add(match.groupValues[1].toInt() to match.groupValues[2])
                            j++
                        } else break
                    }

                    // Генерируем сегменты для каждого элемента списка
                    yieldAll(listItems.mapIndexed { idx, (number, content) ->
                        MessageSegment.OrderedListItem(number, content) to index + idx + 1
                    })
                }

                // Маркированный список - или * или +
                Regex("^[-*+]\\s+").find(trimmedLine) != null -> {
                    var j = index
                    val listItems = mutableListOf<String>()

                    while (j < processedInput.lines.size) {
                        val currentLine = processedInput.lines[j].trim()
                        val match = Regex("^[-*+]\\s+(.+)$").find(currentLine)
                        if (match != null) {
                            listItems.add(match.groupValues[1])
                            j++
                        } else break
                    }

                    yieldAll(listItems.mapIndexed { idx, content ->
                        MessageSegment.UnorderedListItem(content) to index + idx + 1
                    })
                }

                // Обычный текст / пустая строка
                else -> {
                    var j = index
                    val textLines = mutableListOf<String>()

                    while (j < processedInput.lines.size) {
                        val currentLine = processedInput.lines[j]
                        val currentTrimmed = currentLine.trim()

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
                            yield(MessageSegment.Text(textContent) to j)
                        } else {
                            yieldAll(parseFrom(if (j > index) j else index + 1))
                        }
                    } else {
                        yieldAll(parseFrom(index + 1))
                    }
                }
            }
        }

        // Идиоматичное собрание результатов: извлекаем только сегменты из sequence пар (сегмент, индекс)
        return@withContext parseFrom(0).map { it.first }.toList()
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
        return (0 until expectedColumns).map { i ->
            val cell = if (i < alignmentCells.size) alignmentCells[i] else ""
            when {
                cell.startsWith(":") && cell.endsWith(":") -> MessageSegment.TableAlignment.CENTER
                cell.endsWith(":") -> MessageSegment.TableAlignment.RIGHT
                else -> MessageSegment.TableAlignment.LEFT
            }
        }
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
