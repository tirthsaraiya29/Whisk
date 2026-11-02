package com.whisk.data.repository

import com.whisk.data.local.NoteDao
import com.whisk.data.local.RelationshipDao
import com.whisk.domain.model.EdgeType
import com.whisk.domain.model.GraphEdge
import com.whisk.domain.model.GraphNode
import com.whisk.domain.model.KnowledgeGraph
import com.whisk.domain.model.NodeType
import com.whisk.domain.model.RelationshipType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class GraphRepository(
    private val noteDao: NoteDao,
    private val relationshipDao: RelationshipDao? = null,
) {

    suspend fun buildGlobalGraph(): KnowledgeGraph = withContext(Dispatchers.IO) {
        val notes = noteDao.getAllNotesDirect()
        val relationships = relationshipDao?.getAllRelationshipsDirect() ?: emptyList()

        withContext(Dispatchers.Default) {
            coroutineContext.ensureActive()

            val nodesMap = mutableMapOf<String, GraphNode>()
            val edges = mutableListOf<GraphEdge>()
            val linkRegex = Regex("\\[\\[(.*?)]]")

            val titleToIdMap = notes.associateBy({ it.title.lowercase() }, { it.id })

            val outgoingLinksCountMap = mutableMapOf<String, Int>()
            val incomingLinksCountMap = mutableMapOf<String, Int>()
            val explicitEdges = mutableListOf<GraphEdge>()

            // 1. Single pass: parse outgoing wikilinks for each note
            for (note in notes) {
                coroutineContext.ensureActive()
                val matches = linkRegex.findAll(note.body)
                for (match in matches) {
                    val targetTitle = match.groupValues[1].trim().lowercase()
                    val targetId = titleToIdMap[targetTitle]
                    if (targetId != null && targetId != note.id) {
                        outgoingLinksCountMap[note.id] = (outgoingLinksCountMap[note.id] ?: 0) + 1
                        incomingLinksCountMap[targetId] = (incomingLinksCountMap[targetId] ?: 0) + 1
                        explicitEdges.add(
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

            // 2. Process first-class object relationships
            val hasRelationshipSet = mutableSetOf<String>()
            val relationshipEdges = mutableListOf<GraphEdge>()

            for (rel in relationships) {
                coroutineContext.ensureActive()
                hasRelationshipSet.add(rel.sourceObjectId)
                hasRelationshipSet.add(rel.targetObjectId)
                val relType = RelationshipType.fromString(rel.relationshipType)
                relationshipEdges.add(
                    GraphEdge(
                        sourceId = rel.sourceObjectId,
                        targetId = rel.targetObjectId,
                        type = EdgeType.OBJECT_RELATIONSHIP,
                        label = relType.displayName.lowercase(),
                    ),
                )
            }

            // 3. Build nodes with O(1) orphan calculation
            for (note in notes) {
                coroutineContext.ensureActive()
                val outgoingCount = outgoingLinksCountMap[note.id] ?: 0
                val incomingCount = incomingLinksCountMap[note.id] ?: 0
                val hasTags = note.tags.isNotBlank()
                val hasRels = hasRelationshipSet.contains(note.id)

                val isOrphan = (outgoingCount == 0 && incomingCount == 0 && !hasTags && !hasRels)

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

                // Tag nodes
                if (hasTags) {
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

            // Combine edges
            edges.addAll(explicitEdges)

            for (relEdge in relationshipEdges) {
                if (nodesMap.containsKey(relEdge.sourceId) && nodesMap.containsKey(relEdge.targetId)) {
                    edges.add(relEdge)
                }
            }

            KnowledgeGraph(nodes = nodesMap.values.toList(), edges = edges)
        }
    }

    suspend fun buildLocalGraph(centerNoteId: String, depth: Int = 1): KnowledgeGraph = withContext(Dispatchers.IO) {
        val globalGraph = buildGlobalGraph()
        withContext(Dispatchers.Default) {
            val selectedIds = mutableSetOf(centerNoteId)

            var currentBoundary = setOf(centerNoteId)
            repeat(depth) {
                coroutineContext.ensureActive()
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

            KnowledgeGraph(nodes = filteredNodes, edges = filteredEdges)
        }
    }
}
