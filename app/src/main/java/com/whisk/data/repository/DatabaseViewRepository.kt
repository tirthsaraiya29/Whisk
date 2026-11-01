package com.whisk.data.repository

import com.whisk.data.local.DatabaseViewDao
import com.whisk.data.local.DatabaseViewEntity
import com.whisk.domain.model.DatabaseView
import com.whisk.domain.model.ViewType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class DatabaseViewRepository(
    private val databaseViewDao: DatabaseViewDao,
) {

    val allViews: Flow<List<DatabaseView>> = databaseViewDao.getAllViews().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getViewsForTarget(targetTypeId: String?): Flow<List<DatabaseView>> {
        return databaseViewDao.getViewsForTarget(targetTypeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getViewById(id: String): DatabaseView? {
        return databaseViewDao.getViewById(id)?.toDomain()
    }

    suspend fun saveView(view: DatabaseView) {
        databaseViewDao.insertView(DatabaseViewEntity.fromDomain(view.copy(updatedAt = System.currentTimeMillis())))
    }

    suspend fun deleteView(id: String) {
        databaseViewDao.deleteViewById(id)
    }

    suspend fun duplicateView(viewId: String): DatabaseView? {
        val original = getViewById(viewId) ?: return null
        val duplicate = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        saveView(duplicate)
        return duplicate
    }

    suspend fun seedDefaultViewsIfEmpty() {
        val existing = databaseViewDao.getAllViewsDirect()
        if (existing.isEmpty()) {
            val defaults = listOf(
                DatabaseView(
                    id = "view_default_table",
                    name = "All Notes Table",
                    viewType = ViewType.TABLE,
                    position = 0,
                ),
                DatabaseView(
                    id = "view_project_board",
                    name = "Project Board",
                    viewType = ViewType.BOARD,
                    targetTypeId = "project",
                    groupPropertyId = "status",
                    position = 1,
                ),
                DatabaseView(
                    id = "view_default_calendar",
                    name = "Upcoming Calendar",
                    viewType = ViewType.CALENDAR,
                    datePropertyId = "createdAt",
                    position = 2,
                ),
                DatabaseView(
                    id = "view_default_gallery",
                    name = "Gallery",
                    viewType = ViewType.GALLERY,
                    position = 3,
                ),
                DatabaseView(
                    id = "view_default_list",
                    name = "Notes List",
                    viewType = ViewType.LIST,
                    position = 4,
                ),
            )
            defaults.forEach { saveView(it) }
        }
    }
}
