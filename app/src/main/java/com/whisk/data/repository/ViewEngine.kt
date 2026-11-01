package com.whisk.data.repository

import com.whisk.domain.model.DatabaseView
import com.whisk.domain.model.FilterRule
import com.whisk.domain.model.Note
import com.whisk.domain.model.NotePropertyValue
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyOperator
import com.whisk.domain.model.SortDirection
import com.whisk.domain.model.SortRule

object ViewEngine {

    fun applyViewQuery(
        notes: List<Note>,
        propertyValues: Map<String, List<NotePropertyValue>>, // noteId -> propertyValues
        propertyDefinitions: Map<String, PropertyDefinition>, // defId -> PropertyDefinition
        view: DatabaseView,
    ): List<Note> {
        var result = notes

        // 1. Target Type Filter
        if (!view.targetTypeId.isNullOrBlank()) {
            result = result.filter { it.typeId == view.targetTypeId }
        }

        // 2. Custom Filter Rules
        if (view.filters.isNotEmpty()) {
            result = result.filter { note ->
                val values = propertyValues[note.id] ?: emptyList()
                view.filters.all { rule ->
                    evaluateFilterRule(note, values, propertyDefinitions, rule)
                }
            }
        }

        // 3. Multi-property Sorting
        if (view.sorts.isNotEmpty()) {
            val comparator = buildMultiComparator(view.sorts, propertyValues)
            result = result.sortedWith(comparator)
        } else {
            // Default sort: updatedAt DESC
            result = result.sortedByDescending { it.updatedAt }
        }

        return result
    }

    private fun evaluateFilterRule(
        note: Note,
        noteValues: List<NotePropertyValue>,
        propertyDefs: Map<String, PropertyDefinition>,
        rule: FilterRule,
    ): Boolean {
        val rawTarget = rule.value ?: ""
        val propId = rule.propertyId

        // Handle built-in properties
        val actualValueStr: String? = when (propId) {
            "title" -> note.title
            "status" -> note.status
            "typeId" -> note.typeId
            "folder" -> note.folder
            "tags" -> note.tags.joinToString(",")
            else -> {
                val propVal = noteValues.firstOrNull { it.propertyDefinitionId == propId }
                propVal?.textValue ?: propVal?.numberValue?.toString() ?: propVal?.booleanValue?.toString()
            }
        }

        val propVal = noteValues.firstOrNull { it.propertyDefinitionId == propId }

        return when (rule.operator) {
            PropertyOperator.EQUALS -> {
                if (actualValueStr != null) {
                    actualValueStr.equals(rawTarget, ignoreCase = true)
                } else false
            }
            PropertyOperator.NOT_EQUALS -> {
                if (actualValueStr != null) {
                    !actualValueStr.equals(rawTarget, ignoreCase = true)
                } else true
            }
            PropertyOperator.CONTAINS -> {
                if (propVal?.listValue?.isNotEmpty() == true) {
                    propVal.listValue.any { it.contains(rawTarget, ignoreCase = true) }
                } else {
                    actualValueStr?.contains(rawTarget, ignoreCase = true) == true
                }
            }
            PropertyOperator.GREATER_THAN -> {
                val numVal = propVal?.numberValue ?: actualValueStr?.toDoubleOrNull()
                val targetNum = rawTarget.toDoubleOrNull()
                if (numVal != null && targetNum != null) {
                    numVal > targetNum
                } else false
            }
            PropertyOperator.LESS_THAN -> {
                val numVal = propVal?.numberValue ?: actualValueStr?.toDoubleOrNull()
                val targetNum = rawTarget.toDoubleOrNull()
                if (numVal != null && targetNum != null) {
                    numVal < targetNum
                } else false
            }
            PropertyOperator.BEFORE -> {
                if (actualValueStr != null) actualValueStr < rawTarget else false
            }
            PropertyOperator.AFTER -> {
                if (actualValueStr != null) actualValueStr > rawTarget else false
            }
            PropertyOperator.IS_EMPTY -> {
                actualValueStr.isNullOrBlank() && (propVal?.listValue.isNullOrEmpty())
            }
            PropertyOperator.IS_NOT_EMPTY -> {
                !actualValueStr.isNullOrBlank() || (propVal?.listValue?.isNotEmpty() == true)
            }
        }
    }

    private fun buildMultiComparator(
        sorts: List<SortRule>,
        propertyValues: Map<String, List<NotePropertyValue>>,
    ): Comparator<Note> {
        return Comparator { a, b ->
            for (sort in sorts) {
                val propId = sort.propertyId
                val dir = sort.direction

                val valA = extractComparableValue(a, propertyValues[a.id], propId)
                val valB = extractComparableValue(b, propertyValues[b.id], propId)

                val cmp = compareValuesGeneric(valA, valB)
                if (cmp != 0) {
                    return@Comparator if (dir == SortDirection.ASCENDING) cmp else -cmp
                }
            }
            0
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun compareValuesGeneric(a: Any?, b: Any?): Int {
        if (a == null && b == null) return 0
        if (a == null) return 1
        if (b == null) return -1

        if (a is Comparable<*> && b is Comparable<*>) {
            return (a as Comparable<Any>).compareTo(b as Comparable<Any>)
        }
        return 0
    }

    private fun extractComparableValue(
        note: Note,
        noteValues: List<NotePropertyValue>?,
        propId: String,
    ): Any? {
        return when (propId) {
            "title" -> note.title.lowercase()
            "status" -> note.status?.lowercase()
            "typeId" -> note.typeId.lowercase()
            "createdAt" -> note.createdAt
            "updatedAt" -> note.updatedAt
            else -> {
                val pVal = noteValues?.firstOrNull { it.propertyDefinitionId == propId }
                when {
                    pVal?.numberValue != null -> pVal.numberValue
                    pVal?.booleanValue != null -> pVal.booleanValue
                    pVal != null && !pVal.textValue.isNullOrBlank() -> pVal.textValue.lowercase()
                    else -> null
                }
            }
        }
    }
}
