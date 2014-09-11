package org.onlab.graph;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Test of the Bellman-Ford algorithm.
 */
public class BellmanFordGraphSearchTest extends BreadthFirstSearchTest {

    @Override
    protected AbstractGraphPathSearch<TestVertex, TestEdge> graphSearch() {
        return new BellmanFordGraphSearch<>();
    }

    @Test
    @Override
    public void defaultGraphTest() {
        executeDefaultTest(7, 5, 5.0);
    }

    @Test
    public void defaultHopCountWeight() {
        weight = null;
        executeDefaultTest(10, 3, 3.0);
    }

    @Test
    public void searchGraphWithNegativeCycles() {
        Set<TestVertex> vertexes = new HashSet<>(vertexes());
        vertexes.add(Z);

        Set<TestEdge> edges = new HashSet<>(edges());
        edges.add(new TestEdge(G, Z, 1.0));
        edges.add(new TestEdge(Z, G, -2.0));

        graph = new AdjacencyListsGraph<>(vertexes, edges);

        GraphPathSearch<TestVertex, TestEdge> search = graphSearch();
        Set<Path<TestVertex, TestEdge>> paths = search.search(graph, A, H, weight).paths();
        assertEquals("incorrect paths count", 1, paths.size());

        Path p = paths.iterator().next();
        assertEquals("incorrect src", A, p.src());
        assertEquals("incorrect dst", H, p.dst());
        assertEquals("incorrect path length", 5, p.edges().size());
        assertEquals("incorrect path cost", 5.0, p.cost(), 0.1);

        paths = search.search(graph, A, G, weight).paths();
        assertEquals("incorrect paths count", 0, paths.size());

        paths = search.search(graph, A, null, weight).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 6, paths.size());
    }

}
