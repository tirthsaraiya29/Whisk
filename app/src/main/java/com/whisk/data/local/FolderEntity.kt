package com.whisk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.whisk.domain.model.Folder

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String?
) {
    fun toDomain(noteCount: Int = 0) = Folder(id = id, name = name, parentId = parentId, noteCount = noteCount)
    companion object {
        fun fromDomain(folder: Folder) = FolderEntity(id = folder.id, name = folder.name, parentId = folder.parentId)
    }
}
