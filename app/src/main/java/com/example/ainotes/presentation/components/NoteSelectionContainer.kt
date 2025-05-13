package com.example.ainotes.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.viewinterop.AndroidView

private const val MENU_ID_CREATE_NOTE = 1
private const val MENU_ID_COPY = 2
private const val MENU_ID_SELECT_ALL = 3

@Composable
fun NoteSelectionContainer(
    text: String,
    onCreateNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {

                val textColor = colorScheme.onSecondary.toArgb()
                setTextColor(textColor)
                setText(text, TextView.BufferType.SPANNABLE)
                setTextIsSelectable(true)
            }.also { tv ->
                tv.customSelectionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        menu.clear()
                        menu.add(0, MENU_ID_CREATE_NOTE, 0, "Создать заметку")
                        menu.add(0, MENU_ID_COPY, 1, "Копировать")
                        menu.add(0, MENU_ID_SELECT_ALL, 2, "Select all")
                        return true
                    }
                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                        menu.clear()
                        menu.add(0, MENU_ID_CREATE_NOTE, 0, "Создать заметку")
                        menu.add(0, MENU_ID_COPY, 1, "Копировать")
                        menu.add(0, MENU_ID_SELECT_ALL, 2, "Выбрать всё")
                        return true
                    }
                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        val selStart = tv.selectionStart.coerceAtLeast(0)
                        val selEnd = tv.selectionEnd.coerceAtLeast(0)
                        val selectedText = tv.text.substring(
                            selStart.coerceAtMost(selEnd),
                            selEnd.coerceAtLeast(selStart)
                        )
                        when (item.itemId) {
                            MENU_ID_CREATE_NOTE -> {
                                onCreateNote(selectedText)
                                mode.finish()
                                return true
                            }
                            MENU_ID_COPY -> {
                                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("text", selectedText))
                                mode.finish()
                                return true
                            }
                            MENU_ID_SELECT_ALL -> {
                                (tv.text as? Spannable)?.let { sp ->
                                    Selection.selectAll(sp)
                                }
                                mode.invalidate()
                                return true
                            }
                        }
                        return false
                    }
                    override fun onDestroyActionMode(mode: ActionMode) {}
                }

                tv.customInsertionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu) = false
                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = false
                    override fun onDestroyActionMode(mode: ActionMode) {}
                }
            }
        },
        update = { tv ->
            if (tv.text.toString() != text) {
                tv.text = text
            }
        }
    )
}