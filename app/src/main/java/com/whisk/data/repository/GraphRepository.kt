package com.whisk.data.repository

import com.whisk.data.local.NoteDao
import com.whisk.data.local.RelationshipDao
import com.whisk.domain.model.EdgeType
import com.whisk.domain.model.GraphEdge
import com.whisk.domain.model.GraphNode
import com.whisk.domain.model.KnowledgeGraph
import com.whisk.domain.model.NodeType
import com.whisk.domain.model.RelationshipType

class GraphRepository(
    private val noteDao: NoteDao,
    private val relationshipDao: RelationshipDao? = null,
) {

    suspend fun buildGlobalGraph(): KnowledgeGraph {
        val notes = noteDao.getAllNotesDirect()
        val nodesMap = mutableMapOf<String, GraphNode>()
        val edges = mutableListOf<GraphEdge>()
        val linkRegex = Regex("\\[\\[(.*?)]]")

        val titleToIdMap = notes.associateBy({ it.title }, { it.id })

        // 1. Create Object / Note Nodes
        notes.forEach { note ->
            val isOrphan = !linkRegex.containsMatchIn(note.body) &&
                notes.none { other -> (other.id != note.id) && other.body.contains("[[${note.title}]]") }
            val node = GraphNode(
                id = note.id,
                label = note.title,
                type = NodeType.NOTE,
                subtitle = "TYPE: ${note.typeId.uppercase()}",
                isOrphan = isOrphan,
                isArchived = note.isArchived,
                noteId = note.id,
                objectTypeId = note.typeId,
            )
            nodesMap[note.id] = node

            // Tags
            if (note.tags.isNotBlank()) {
                val tagsList = note.tags.split(",").asSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
                tagsList.forEach { tag ->
                    val tagNodeId = "tag_$tag"
                    if (!nodesMap.containsKey(tagNodeId)) {
                        nodesMap[tagNodeId] = GraphNode(
                            id = tagNodeId,
                            label = "#$tag",
                            type = NodeType.TAG,
                        )
                    }
                    edges.add(
                        GraphEdge(
                            sourceId = note.id,
                            targetId = tagNodeId,
                            type = EdgeType.TAG_MEMBERSHIP,
                        ),
                    )
                }
            }
        }

        // 2. Create Explicit Markdown Link Edges
        notes.forEach { note ->
            val matches = linkRegex.findAll(note.body)
            matches.forEach { match ->
                val targetTitle = match.groupValues[1].trim()
                val targetId = titleToIdMap[targetTitle]
                if (targetId != null && targetId != note.id) {
                    edges.add(
                        GraphEdge(
                            sourceId = note.id,
                            targetId = targetId,
                            type = EdgeType.EXPLICIT_LINK,
                            label = "links to",
                        ),
                    )
                }
            }
        }

        // 3. Create First-Class Object Relationships Edges
        relationshipDao?.let { dao ->
            val relationships = dao.getAllRelationshipsDirect()
            relationships.forEach { rel ->
                if (nodesMap.containsKey(rel.sourceObjectId) && nodesMap.containsKey(rel.targetObjectId)) {
                    val relType = RelationshipType.fromString(rel.relationshipType)
                    edges.add(
                        GraphEdge(
                            sourceId = rel.sourceObjectId,
                            targetId = rel.targetObjectId,
                            type = EdgeType.OBJECT_RELATIONSHIP,
                            label = relType.displayName.lowercase(),
                        ),
                    )
                }
            }
        }

        return KnowledgeGraph(nodes = nodesMap.values.toList(), edges = edges)
    }

    suspend fun buildLocalGraph(centerNoteId: String, depth: Int = 1): KnowledgeGraph {
        val globalGraph = buildGlobalGraph()
        val selectedIds = mutableSetOf(centerNoteId)

        var currentBoundary = setOf(centerNoteId)
        repeat(depth) {
            val nextBoundary = mutableSetOf<String>()
            globalGraph.edges.forEach { edge ->
                if (edge.sourceId in currentBoundary) {
                    nextBoundary.add(edge.targetId)
                }
                if (edge.targetId in currentBoundary) {
                    nextBoundary.add(edge.sourceId)
                }
            }
            selectedIds.addAll(nextBoundary)
            currentBoundary = nextBoundary
        }

        val filteredNodes = globalGraph.nodes.filter { it.id in selectedIds }
        val filteredEdges = globalGraph.edges.filter { it.sourceId in selectedIds && it.targetId in selectedIds }

        return KnowledgeGraph(nodes = filteredNodes, edges = filteredEdges)
    }
}
