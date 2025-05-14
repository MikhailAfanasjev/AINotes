package com.example.ainotes.utils

object MarkdownParser {
    /**
     * Разбирает входную строку с Markdown-разметкой на список сегментов:
     * - MessageSegment.Text для обычного текста
     * - MessageSegment.Code для фрагментов кода
     *
     * Код после первого ``` сразу считается блоком кода, даже без закрывающих ```. После следующего ``` режим меняется.
     */
    fun parseSegments(input: String): List<MessageSegment> {
        val segments = mutableListOf<MessageSegment>()
        val delimiter = "```"
        var remaining = input
        var isCodeMode = false

        while (true) {
            val idx = remaining.indexOf(delimiter)
            if (idx < 0) break

            // Часть до ```
            val part = remaining.substring(0, idx)
            if (part.isNotEmpty()) {
                if (isCodeMode) {
                    segments += MessageSegment.Code(part)
                } else {
                    segments += MessageSegment.Text(part)
                }
            }

            // Откусываем маркер
            remaining = remaining.substring(idx + delimiter.length)
            isCodeMode = !isCodeMode

            // Если открыт блок кода с меткой языка, пропускаем первую строку
            if (isCodeMode) {
                // Проверяем, есть ли язык перед переносом строки
                val nlIndex = remaining.indexOf('\n')
                if (nlIndex >= 0) {
                    val lang = remaining.substring(0, nlIndex)
                    if (lang.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                        // Пропускаем метку языка и перевод строки
                        remaining = remaining.substring(nlIndex + 1)
                    }
                }
            }
        }

        // Остаток текста или кода
        if (remaining.isNotEmpty()) {
            if (isCodeMode) {
                segments += MessageSegment.Code(remaining)
            } else {
                segments += MessageSegment.Text(remaining)
            }
        }

        return segments
    }
}