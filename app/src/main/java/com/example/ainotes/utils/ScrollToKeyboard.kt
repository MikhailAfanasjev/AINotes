package com.example.ainotes.utils

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState


suspend fun LazyListState.scrollToAvoidKeyboard(
    bottomPaddingPx: Int
) {
    val layout = this.layoutInfo

    // Если нет видимых элементов — ничего не делаем
    val lastVisible = layout.visibleItemsInfo.lastOrNull() ?: return

    // Координата нижнего края viewport без учёта отступа
    val viewportBottom = layout.viewportEndOffset - bottomPaddingPx

    // Координата нижнего края нашего элемента
    val itemBottom = lastVisible.offset + lastVisible.size

    // Если элемент «выходит» за effectiveViewportBottom — двигаем список вверх
    val overlap = itemBottom - viewportBottom
    if (overlap > 0) {
        this.scrollBy(overlap.toFloat())
    }
}


//suspend fun scrollToKeyboard(
//    state: LazyListState,
//    bottomPaddingPx: Int
//) {
//    val layout = state.layoutInfo
//    if (layout.totalItemsCount == 0) return
//
//    // Ищем первый видимый элемент, который частично/полностью может перекрываться клавиатурой
//    val overlappingItem = layout.visibleItemsInfo.lastOrNull {
//        it.offset + it.size > layout.viewportEndOffset - bottomPaddingPx
//    } ?: return
//
//    // Вычисляем на сколько пикселей этот элемент перекрывается с клавиатурой
//    val overflow = (overlappingItem.offset + overlappingItem.size) - (layout.viewportEndOffset - bottomPaddingPx)
//
//    if (overflow > 0) {
//        // Прокручиваем на высоту перекрытия, чтобы элемент оказался выше клавиатуры
//        state.scrollBy(overflow.toFloat())
//    }
//}