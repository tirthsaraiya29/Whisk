package com.whisk

import com.whisk.domain.model.EdgeType
import com.whisk.domain.model.GraphEdge
import com.whisk.domain.model.GraphNode
import com.whisk.domain.model.KnowledgeGraph
import com.whisk.domain.model.NodeType
import org.junit.Assert.assertEquals
import org.junit.Test

class GraphRepositoryTest {

    @Test
    fun testGraphConstruction() {
        val node1 = GraphNode(id = "n1", label = "Note 1", type = NodeType.NOTE)
        val node2 = GraphNode(id = "n2", label = "Note 2", type = NodeType.NOTE)
        val edge = GraphEdge(sourceId = "n1", targetId = "n2", type = EdgeType.EXPLICIT_LINK)

        val graph = KnowledgeGraph(nodes = listOf(node1, node2), edges = listOf(edge))

        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.edges.size)
        assertEquals("n1", graph.edges[0].sourceId)
        assertEquals("n2", graph.edges[0].targetId)
    }
}
