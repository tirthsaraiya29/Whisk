package com.whisk

import com.whisk.data.local.NoteEntity
import com.whisk.data.repository.NoteDto
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportImportSchemaReflectionTest {

    @Test
    fun testEveryNoteEntityPropertyIsCoveredByNoteDto() {
        val entityFields = NoteEntity::class.java.declaredFields.map { it.name }.toSet()
        val dtoFields = NoteDto::class.java.declaredFields.map { it.name }.toSet()

        entityFields.forEach { fieldName ->
            assertTrue(
                "NoteDto is missing field '$fieldName' present in NoteEntity. Schema drift detected!",
                dtoFields.contains(fieldName)
            )
        }
    }
}
