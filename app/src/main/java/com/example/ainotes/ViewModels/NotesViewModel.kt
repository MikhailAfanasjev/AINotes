package com.example.ainotes.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ainotes.data.local.entity.Note
import com.example.ainotes.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    init {
        noteRepository.getAllNotes()
            .onEach { list ->
                Log.d("NotesViewModel", "Loaded notes: ${list.size}")
                _notes.value = list
            }
            .launchIn(viewModelScope)
    }

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
        viewModelScope.launch {
            val existing = _notes.value.find { it.id == noteId }
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