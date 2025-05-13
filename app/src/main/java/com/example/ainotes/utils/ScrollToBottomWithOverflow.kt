package com.example.ainotes.utils

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState

suspend fun scrollToBottomWithOverflow(
    state: LazyListState,
    bottomPaddingPx: Int
) {
    val lastIndex = state.layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return

    // 1) Ставим последний элемент в начало viewport
    state.scrollToItem(lastIndex)

    val layout = state.layoutInfo
    // Вычитаем паддинг
    val effectiveViewportEnd = layout.viewportEndOffset - bottomPaddingPx
    val item = layout.visibleItemsInfo.find { it.index == lastIndex } ?: return

    // 2) Считаем, насколько элемент выступает вниз за эффективный конец viewport
    val overflow = (item.offset + item.size) - effectiveViewportEnd

    if (overflow > 0) {
        // Докатываем список «вниз» на это число пикселей
        state.scrollBy(overflow.toFloat())
    }
}