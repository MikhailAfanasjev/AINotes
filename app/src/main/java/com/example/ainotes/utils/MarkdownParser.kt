package com.example.ainotes.utils

object MarkdownParser {
    private val codeBlockPattern = Regex("(?s)```(?:\\w+)?\\n(.*?)```")

    /**
     * Разбирает входную строку с Markdown-разметкой на список сегментов:
     * - MessageSegment.Text for regular text
     * - MessageSegment.Code for code blocks
     */
    fun parseSegments(input: String): List<MessageSegment> {
        val segments = mutableListOf<MessageSegment>()
        var lastIndex = 0

        for (match in codeBlockPattern.findAll(input)) {
            val start = match.range.first
            val end = match.range.last + 1

            // Добавляем обычный текст до блока кода
            if (start > lastIndex) {
                val textPart = input.substring(lastIndex, start)
                segments += MessageSegment.Text(textPart)
            }

            // Содержимое кода без ``` и возможной метки языка
            val codeContent = match.groupValues[1].trim('\n')
            segments += MessageSegment.Code(codeContent)

            lastIndex = end
        }

        // Добавляем остаток текста после последнего блока кода
        if (lastIndex < input.length) {
            val remaining = input.substring(lastIndex)
            segments += MessageSegment.Text(remaining)
        }

        return segments
    }
}