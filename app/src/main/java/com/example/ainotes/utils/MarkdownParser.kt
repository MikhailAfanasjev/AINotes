package com.example.ainotes.utils

import android.util.Log

object MarkdownParser {
    private const val TAG = "MarkdownParser"

    /**
     * Разбирает входную строку с Markdown-разметкой на список сегментов:
     * - MessageSegment.Text для обычного текста
     * - MessageSegment.Code для фрагментов кода
     *
     * При каждом встретившемся ``` переключается режим: если код, то начинаем кодовый сегмент;
     * если текст, то возвращаемся в текстовый режим. Между сегментами код и текст обрабатываются отдельно.
     */
    fun parseSegments(input: String): List<MessageSegment> {
        // Логируем текст до парсинга
        Log.d(TAG, "Parsing input: \n$input")

        val segments = mutableListOf<MessageSegment>()
        val delimiter = "```"
        var index = 0
        var isCode = false
        var lastIndex = 0

        while (index < input.length) {
            val next = input.indexOf(delimiter, index)
            if (next == -1) break

            // Добавляем текст до блока ```
            if (next > index) {
                val part = input.substring(index, next)
                if (isCode) segments += MessageSegment.Code(part)
                else segments += MessageSegment.Text(part)
            }

            index = next + delimiter.length
            isCode = !isCode

            // Пропустить возможную метку языка при начале кода
            if (isCode) {
                val nl = input.indexOf('\n', index)
                if (nl != -1) {
                    val possibleLang = input.substring(index, nl).trim()
                    if (possibleLang.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                        index = nl + 1
                    }
                }
            }

            lastIndex = index
        }

        // Добавить оставшийся текст
        if (lastIndex < input.length) {
            val part = input.substring(lastIndex)
            if (isCode) segments += MessageSegment.Code(part)
            else segments += MessageSegment.Text(part)
        }

        return segments
    }
}
