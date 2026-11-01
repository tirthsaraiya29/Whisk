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
        PropertyDefinitionEntity::class,
        NotePropertyValueEntity::class,
        ObjectTypeDefinitionEntity::class,
        ObjectRelationshipEntity::class,
        DatabaseViewEntity::class,
        DailyNoteMetadataEntity::class,
        ActivityEventEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class WhiskDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun canvasDao(): CanvasDao
    abstract fun taskDao(): TaskDao
    abstract fun chatDao(): ChatDao
    abstract fun propertyDao(): PropertyDao
    abstract fun objectTypeDao(): ObjectTypeDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun databaseViewDao(): DatabaseViewDao
    abstract fun dailyNoteDao(): DailyNoteDao
    abstract fun activityEventDao(): ActivityEventDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `property_definitions` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `description` TEXT,
                        `optionsJson` TEXT,
                        `position` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `note_property_values` (
                        `id` TEXT NOT NULL,
                        `noteId` TEXT NOT NULL,
                        `propertyDefinitionId` TEXT NOT NULL,
                        `textValue` TEXT,
                        `numberValue` REAL,
                        `booleanValue` INTEGER,
                        `listValueJson` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`propertyDefinitionId`) REFERENCES `property_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_property_values_noteId` ON `note_property_values` (`noteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_property_values_propertyDefinitionId` ON `note_property_values` (`propertyDefinitionId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_note_property_values_noteId_propertyDefinitionId` ON `note_property_values` (`noteId`, `propertyDefinitionId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN typeId TEXT NOT NULL DEFAULT 'note'")
                db.execSQL("ALTER TABLE notes ADD COLUMN description TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE notes ADD COLUMN icon TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE notes ADD COLUMN coverImage TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_typeId` ON `notes` (`typeId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `object_type_definitions` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `icon` TEXT,
                        `isBuiltIn` INTEGER NOT NULL,
                        `configurationJson` TEXT,
                        `position` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `object_relationships` (
                        `id` TEXT NOT NULL,
                        `sourceObjectId` TEXT NOT NULL,
                        `targetObjectId` TEXT NOT NULL,
                        `relationshipType` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`sourceObjectId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`targetObjectId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_object_relationships_sourceObjectId` ON `object_relationships` (`sourceObjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_object_relationships_targetObjectId` ON `object_relationships` (`targetObjectId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_object_relationships_source_target_type` ON `object_relationships` (`sourceObjectId`, `targetObjectId`, `relationshipType`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `database_views` (
                        `id` TEXT NOT NULL,
                        `targetTypeId` TEXT,
                        `name` TEXT NOT NULL,
                        `viewType` TEXT NOT NULL,
                        `visiblePropertiesJson` TEXT,
                        `filterConfigJson` TEXT,
                        `sortConfigJson` TEXT,
                        `groupPropertyId` TEXT,
                        `datePropertyId` TEXT,
                        `cardSize` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_note_metadata` (
                        `noteId` TEXT NOT NULL,
                        `dateKey` TEXT NOT NULL,
                        `timezone` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`noteId`),
                        FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_note_metadata_dateKey` ON `daily_note_metadata` (`dateKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_note_metadata_noteId` ON `daily_note_metadata` (`noteId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `activity_events` (
                        `id` TEXT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `dateKey` TEXT NOT NULL,
                        `relatedObjectId` TEXT,
                        `relatedObjectTitle` TEXT,
                        `metadata` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_dateKey` ON `activity_events` (`dateKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_timestamp` ON `activity_events` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_relatedObjectId` ON `activity_events` (`relatedObjectId`)")
            }
        }

        fun getDatabase(context: Context): WhiskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhiskDatabase::class.java,
                    "whisk_database",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
