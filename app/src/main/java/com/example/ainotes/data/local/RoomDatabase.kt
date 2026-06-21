package com.example.ainotes.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.ainotes.data.local.dao.ChatDao
import com.example.ainotes.data.local.dao.ChatMessageDao
import com.example.ainotes.data.local.dao.NoteDao
import com.example.ainotes.data.local.entity.ChatEntity
import com.example.ainotes.data.local.entity.ChatMessageEntity
import com.example.ainotes.data.local.entity.Note

@Database(
    entities = [Note::class, ChatEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun chatDao(): ChatDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}