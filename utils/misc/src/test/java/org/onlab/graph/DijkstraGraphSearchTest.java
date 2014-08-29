package org.onlab.graph;

import org.junit.Test;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertEquals;

/**
 * Test of the Dijkstra algorithm.
 */
public class DijkstraGraphSearchTest extends BreadthFirstSearchTest {

    @Override
    protected AbstractGraphPathSearch<TestVertex, TestEdge> graphSearch() {
        return new DijkstraGraphSearch<>();
    }

    @Test
    @Override
    public void defaultGraphTest() {
        executeDefaultTest(7, 5, 5.0);
    }

    @Test
    @Override
    public void defaultHopCountWeight() {
        weight = null;
        executeDefaultTest(10, 3, 3.0);
    }

    @Test
    public void noPath() {
        g = new AdjacencyListsGraph<>(of(A, B, C, D),
                                      of(new TestEdge(A, B, 1),
                                         new TestEdge(B, A, 1),
                                         new TestEdge(C, D, 1),
                                         new TestEdge(D, C, 1)));
        GraphPathSearch<TestVertex, TestEdge> gs = graphSearch();
        Set<Path<TestVertex, TestEdge>> paths = gs.search(g, A, B, weight).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 1, paths.size());
        assertEquals("incorrect path cost", 1.0, paths.iterator().next().cost(), 0.1);

        paths = gs.search(g, A, D, weight).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 0, paths.size());

        paths = gs.search(g, A, null, weight).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 1, paths.size());
        assertEquals("incorrect path cost", 1.0, paths.iterator().next().cost(), 0.1);
    }

    @Test
    public void simpleMultiplePath() {
        g = new AdjacencyListsGraph<>(of(A, B, C, D),
                                      of(new TestEdge(A, B, 1),
                                         new TestEdge(A, C, 1),
                                         new TestEdge(B, D, 1),
                                         new TestEdge(C, D, 1)));
        executeSearch(graphSearch(), g, A, D, weight, 2, 2.0);
    }

    @Test
    public void denseMultiplePath() {
        g = new AdjacencyListsGraph<>(of(A, B, C, D, E, F, G),
                                      of(new TestEdge(A, B, 1),
                                         new TestEdge(A, C, 1),
                                         new TestEdge(B, D, 1),
                                         new TestEdge(C, D, 1),
                                         new TestEdge(D, E, 1),
                                         new TestEdge(D, F, 1),
                                         new TestEdge(E, G, 1),
                                         new TestEdge(F, G, 1),
                                         new TestEdge(A, G, 4)));
        executeSearch(graphSearch(), g, A, G, weight, 5, 4.0);
    }

    @Test
    public void dualEdgeMultiplePath() {
        g = new AdjacencyListsGraph<>(of(A, B, C, D, E, F, G, H),
                                      of(new TestEdge(A, B, 1), new TestEdge(A, C, 3),
                                         new TestEdge(B, D, 2), new TestEdge(B, C, 1),
                                         new TestEdge(B, E, 4), new TestEdge(C, E, 1),
                                         new TestEdge(D, H, 5), new TestEdge(D, E, 1),
                                         new TestEdge(E, F, 1), new TestEdge(F, D, 1),
                                         new TestEdge(F, G, 1), new TestEdge(F, H, 1),
                                         new TestEdge(A, E, 3), new TestEdge(B, D, 1)));
        executeSearch(graphSearch(), g, A, E, weight, 3, 3.0);
    }

}
