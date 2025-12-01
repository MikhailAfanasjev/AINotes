package com.example.ainotes.utils

sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class Code(val content: String, val language: String? = null) : MessageSegment()
    data class Think(val content: String, val durationSeconds: Float = 0f) : MessageSegment()
    data class Header(val level: Int, val content: String) : MessageSegment()
    data class Quote(val content: String) : MessageSegment()
    data class UnorderedListItem(val content: String) : MessageSegment()
    data class OrderedListItem(val number: Int, val content: String) : MessageSegment()
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val alignments: List<TableAlignment>
    ) : MessageSegment()

    object HorizontalRule : MessageSegment()

    enum class TableAlignment {
        LEFT, CENTER, RIGHT
    }
}