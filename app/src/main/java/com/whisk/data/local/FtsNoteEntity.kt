package com.whisk.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "notes_fts")
data class FtsNoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int,
    val id: String,
    val title: String,
    val body: String,
    val tags: String,
)
