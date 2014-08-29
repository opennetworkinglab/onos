package org.onlab.graph;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Tests of the graph implementation.
 */
public class AdjacencyListsGraphTest {

    private static final TestVertex A = new TestVertex("A");
    private static final TestVertex B = new TestVertex("B");
    private static final TestVertex C = new TestVertex("C");
    private static final TestVertex D = new TestVertex("D");
    private static final TestVertex E = new TestVertex("E");
    private static final TestVertex F = new TestVertex("F");
    private static final TestVertex G = new TestVertex("G");

    private final Set<TestEdge> edges =
            ImmutableSet.of(new TestEdge(A, B, 1), new TestEdge(A, C, 1),
                            new TestEdge(B, C, 1), new TestEdge(C, D, 1),
                            new TestEdge(D, A, 1));

    @Test
    public void basics() {
        Set<TestVertex> vertexes = ImmutableSet.of(A, B, C, D, E, F);
        AdjacencyListsGraph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(vertexes, edges);
        assertEquals("incorrect vertex count", 6, graph.getVertexes().size());
        assertEquals("incorrect edge count", 5, graph.getEdges().size());

        assertEquals("incorrect egress edge count", 2, graph.getEdgesFrom(A).size());
        assertEquals("incorrect ingress edge count", 1, graph.getEdgesTo(A).size());
        assertEquals("incorrect ingress edge count", 2, graph.getEdgesTo(C).size());
        assertEquals("incorrect egress edge count", 1, graph.getEdgesFrom(C).size());
    }

    @Test
    public void equality() {
        Set<TestVertex> vertexes = ImmutableSet.of(A, B, C, D, E, F);
        Set<TestVertex> vertexes2 = ImmutableSet.of(A, B, C, D, E, F, G);

        AdjacencyListsGraph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(vertexes, edges);
        AdjacencyListsGraph<TestVertex, TestEdge> same = new AdjacencyListsGraph<>(vertexes, edges);
        AdjacencyListsGraph<TestVertex, TestEdge> different = new AdjacencyListsGraph<>(vertexes2, edges);

        new EqualsTester()
                .addEqualityGroup(graph, same)
                .addEqualityGroup(different)
                .testEquals();
    }
}
