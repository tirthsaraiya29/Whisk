package com.whisk.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NoteEntity::class,
        FtsNoteEntity::class,
        FolderEntity::class,
        CanvasEntity::class,
        TaskEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class WhiskDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun canvasDao(): CanvasDao
    abstract fun taskDao(): TaskDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: WhiskDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN aliases TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notes ADD COLUMN status TEXT DEFAULT 'DRAFT'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING fts4(`id`, `title`, `body`, `tags`, content=`notes`) ")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS notes_ai AFTER INSERT ON notes BEGIN INSERT INTO notes_fts(rowid, id, title, body, tags) VALUES (new.rowid, new.id, new.title, new.body, new.tags); END;")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS notes_ad AFTER DELETE ON notes BEGIN INSERT INTO notes_fts(notes_fts, rowid, id, title, body, tags) VALUES('delete', old.rowid, old.id, old.title, old.body, old.tags); END;")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS notes_au AFTER UPDATE ON notes BEGIN INSERT INTO notes_fts(notes_fts, rowid, id, title, body, tags) VALUES('delete', old.rowid, old.id, old.title, old.body, old.tags); INSERT INTO notes_fts(rowid, id, title, body, tags) VALUES (new.rowid, new.id, new.title, new.body, new.tags); END;")
                db.execSQL("INSERT INTO `notes_fts`(`notes_fts`) VALUES('rebuild')")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_sessions` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `provider` TEXT,
                        `model` TEXT,
                        `contextRef` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_sessions_timestamp` ON `chat_sessions` (`timestamp`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` TEXT NOT NULL,
                        `sessionId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `contextPreview` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`sessionId`) REFERENCES `chat_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId` ON `chat_messages` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_timestamp` ON `chat_messages` (`timestamp`)")
            }
        }

        fun getDatabase(context: Context): WhiskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhiskDatabase::class.java,
                    "whisk_database",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
