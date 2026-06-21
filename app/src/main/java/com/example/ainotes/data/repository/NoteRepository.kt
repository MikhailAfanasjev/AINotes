package com.example.ainotes.data.repository

import com.example.ainotes.data.local.dao.NoteDao
import com.example.ainotes.data.local.entity.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun addNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNoteById(note.id)
    }

    suspend fun deleteAllNotes() {
        noteDao.deleteAllNotes()
    }
}