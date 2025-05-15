package com.example.ainotes.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.Selection
import android.text.Spannable
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun NoteSelectionContainer(
    text: AnnotatedString,
    onCreateNote: (String) -> Unit,
    textColor: Color,
    backgroundColor: Color,
    isCode: Boolean = false,
    modifier: Modifier = Modifier
) {

    AndroidView(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(if (isCode) 8.dp else 0.dp))
            .padding(if (isCode) 8.dp else 0.dp),
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColor.toArgb())
                setTextIsSelectable(true)
                if (isCode) typeface = Typeface.MONOSPACE
                customSelectionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        menu.clear()
                        menu.add(0, MENU_ID_CREATE_NOTE, 0, "Создать заметку")
                        menu.add(0, MENU_ID_COPY, 1, "Копировать")
                        menu.add(0, MENU_ID_SELECT_ALL, 2, "Выбрать всё")
                        return true
                    }
                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = true
                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        val selStart = selectionStart.coerceAtLeast(0)
                        val selEnd = selectionEnd.coerceAtLeast(0)
                        val selected = text.text.substring(
                            minOf(selStart, selEnd),
                            maxOf(selStart, selEnd)
                        )
                        return when (item.itemId) {
                            MENU_ID_CREATE_NOTE -> {
                                onCreateNote(selected)
                                mode.finish()
                                true
                            }
                            MENU_ID_COPY -> {
                                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("text", selected))
                                mode.finish()
                                true
                            }
                            MENU_ID_SELECT_ALL -> {
                                (text as? Spannable)?.let { Selection.selectAll(it) }
                                mode.invalidate()
                                true
                            }
                            else -> false
                        }
                    }
                    override fun onDestroyActionMode(mode: ActionMode) {}
                }
            }
        },
        update = { tv ->
            if (tv.text.toString() != text.text) tv.text = text.text
        }
    )
}

private const val MENU_ID_CREATE_NOTE = 1
private const val MENU_ID_COPY = 2
private const val MENU_ID_SELECT_ALL = 3