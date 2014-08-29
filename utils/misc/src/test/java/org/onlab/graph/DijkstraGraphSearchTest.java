package org.onlab.graph;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Test of the Dijkstra algorithm.
 */
public class DijkstraGraphSearchTest extends BreadthFirstSearchTest {

    @Override
    protected GraphPathSearch<TestVertex, TestEdge> graphSearch() {
        return new DijkstraGraphSearch<>();
    }

    @Test
    @Override
    public void basics() {
        runBasics(5, 5.0, 7);
    }

    @Test
    public void defaultWeight() {
        weight = null;
        runBasics(3, 3.0, 10);
    }

    @Test
    public void noPath() {
        g = new AdjacencyListsGraph<>(ImmutableSet.of(A, B, C, D),
                                      ImmutableSet.of(new TestEdge(A, B, 1),
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
    public void multiPath1() {
        g = new AdjacencyListsGraph<>(ImmutableSet.of(A, B, C, D),
                                      ImmutableSet.of(new TestEdge(A, B, 1),
                                                      new TestEdge(A, C, 1),
                                                      new TestEdge(B, D, 1),
                                                      new TestEdge(C, D, 1)));

        GraphPathSearch<TestVertex, TestEdge> gs = graphSearch();
        Set<Path<TestVertex, TestEdge>> paths = gs.search(g, A, D, weight).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 2, paths.size());
        assertEquals("incorrect path cost", 2.0, paths.iterator().next().cost(), 0.1);
    }

    @Test
    public void multiPath2() {
        g = new AdjacencyListsGraph<>(ImmutableSet.of(A, B, C, D, E, F, G),
                                      ImmutableSet.of(new TestEdge(A, B, 1),
                                                      new TestEdge(A, C, 1),
                                                      new TestEdge(B, D, 1),
                                                      new TestEdge(C, D, 1),
                                                      new TestEdge(D, E, 1),
                                                      new TestEdge(D, F, 1),
                                                      new TestEdge(E, G, 1),
                                                      new TestEdge(F, G, 1),
                                                      new TestEdge(A, G, 4)));

        GraphPathSearch<TestVertex, TestEdge> gs = graphSearch();
        GraphPathSearch.Result<TestVertex, TestEdge> gsr = gs.search(g, A, G, weight);
        Set<Path<TestVertex, TestEdge>> paths = gsr.paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 5, paths.size());
        assertEquals("incorrect path cost", 4.0, paths.iterator().next().cost(), 0.1);
    }

    @Test
    public void multiPath3() {
        g = new AdjacencyListsGraph<>(ImmutableSet.of(A, B, C, D, E, F, G, H),
                                      ImmutableSet.of(new TestEdge(A, B, 1), new TestEdge(A, C, 3),
                                                      new TestEdge(B, D, 2), new TestEdge(B, C, 1),
                                                      new TestEdge(B, E, 4), new TestEdge(C, E, 1),
                                                      new TestEdge(D, H, 5), new TestEdge(D, E, 1),
                                                      new TestEdge(E, F, 1), new TestEdge(F, D, 1),
                                                      new TestEdge(F, G, 1), new TestEdge(F, H, 1),
                                                      new TestEdge(A, E, 3), new TestEdge(B, D, 1)));

        GraphPathSearch<TestVertex, TestEdge> gs = graphSearch();
        Set<Path<TestVertex, TestEdge>> paths = gs.search(g, A, E, weight).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 3, paths.size());
        assertEquals("incorrect path cost", 3.0, paths.iterator().next().cost(), 0.1);
    }

}
