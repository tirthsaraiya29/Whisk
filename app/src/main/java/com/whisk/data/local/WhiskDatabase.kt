package com.whisk.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        CanvasEntity::class,
        TaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WhiskDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun canvasDao(): CanvasDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: WhiskDatabase? = null

        fun getDatabase(context: Context): WhiskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhiskDatabase::class.java,
                    "whisk_database"
                )
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
