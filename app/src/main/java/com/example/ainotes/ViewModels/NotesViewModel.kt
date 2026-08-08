package com.example.ainotes.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.data.local.entity.Note
import com.example.ainotes.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    // Читаем заметки напрямую из DAO через репозиторий — без кэширования в StateFlow
    val notes: StateFlow<List<Note>> = noteRepository.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addNote(title: String, note: String) {
        val newNote = Note(
            title = title,
            note = note
        )
        viewModelScope.launch {
            noteRepository.addNote(newNote)
        }
    }

    fun updateNote(noteId: Long, title: String, note: String) {
        // Читаем актуальные данные из DAO через репозиторий (не из кэша ViewModel)
        viewModelScope.launch {
            val existing = noteRepository.getNoteById(noteId)
            existing?.let {
                val updated = it.copy(title = title, note = note)
                noteRepository.addNote(updated)
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }

    fun deleteAllNotes() {
        viewModelScope.launch {
            noteRepository.deleteAllNotes()
        }
    }
}
