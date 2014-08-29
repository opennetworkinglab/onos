package org.onlab.graph;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Test of the BFS algorithm.
 */
public abstract class BreadthFirstSearchTest extends AbstractGraphSearchTest {

    @Override
    protected GraphPathSearch<TestVertex, TestEdge> graphSearch() {
        return null; // new BreadthFirstSearch();
    }

    @Test
    public void basics() {
        runBasics(3, 8.0, 7);
    }

    @Test
    public void defaultWeight() {
        weight = null;
        runBasics(3, 3.0, 7);
    }

    protected void runBasics(int expectedLength, double expectedCost, int expectedPaths) {
        g = new AdjacencyListsGraph<>(vertices(), edges());

        GraphPathSearch<TestVertex, TestEdge> search = graphSearch();
        Set<Path<TestVertex, TestEdge>> paths = search.search(g, A, H, weight).paths();
        assertEquals("incorrect paths count", 1, paths.size());

        Path p = paths.iterator().next();
        assertEquals("incorrect src", A, p.src());
        assertEquals("incorrect dst", H, p.dst());
        assertEquals("incorrect path length", expectedLength, p.edges().size());
        assertEquals("incorrect path cost", expectedCost, p.cost(), 0.1);

        paths = search.search(g, A, null, weight).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", expectedPaths, paths.size());
    }

}
