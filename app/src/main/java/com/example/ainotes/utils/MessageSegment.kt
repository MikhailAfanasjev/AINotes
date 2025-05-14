package com.example.ainotes.utils

sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class Code(val content: String) : MessageSegment()
}