/*
 * Copyright 2014-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.graph;

import org.junit.Test;

import java.text.DecimalFormat;
import java.util.HashSet;
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
        executeDefaultTest(7, 5, new TestDoubleWeight(5.0));
    }

    @Test
    @Override
    public void defaultHopCountWeight() {
        weigher = null;
        executeDefaultTest(10, 3, new ScalarWeight(3.0));
    }

    @Test
    public void noPath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                          of(new TestEdge(A, B, W1),
                                             new TestEdge(B, A, W1),
                                             new TestEdge(C, D, W1),
                                             new TestEdge(D, C, W1)));
        GraphPathSearch<TestVertex, TestEdge> gs = graphSearch();
        Set<Path<TestVertex, TestEdge>> paths = gs.search(graph, A, B, weigher, 1).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 1, paths.size());
        assertEquals("incorrect path cost", new TestDoubleWeight(1.0), paths.iterator().next().cost());

        paths = gs.search(graph, A, D, weigher, 1).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 0, paths.size());

        paths = gs.search(graph, A, null, weigher, 1).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 1, paths.size());
        assertEquals("incorrect path cost", new TestDoubleWeight(1.0), paths.iterator().next().cost());
    }

    @Test
    public void simpleMultiplePath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                          of(new TestEdge(A, B, W1),
                                             new TestEdge(A, C, W1),
                                             new TestEdge(B, D, W1),
                                             new TestEdge(C, D, W1)));
        executeSearch(graphSearch(), graph, A, D, weigher, 2, W2);
        executeSinglePathSearch(graphSearch(), graph, A, D, weigher, 1, W2);
    }

    @Test
    public void denseMultiplePath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D, E, F, G),
                                          of(new TestEdge(A, B, W1),
                                             new TestEdge(A, C, W1),
                                             new TestEdge(B, D, W1),
                                             new TestEdge(C, D, W1),
                                             new TestEdge(D, E, W1),
                                             new TestEdge(D, F, W1),
                                             new TestEdge(E, G, W1),
                                             new TestEdge(F, G, W1),
                                             new TestEdge(A, G, W4)));
        executeSearch(graphSearch(), graph, A, G, weigher, 5, W4);
        executeSinglePathSearch(graphSearch(), graph, A, G, weigher, 1, W4);
    }

    @Test
    public void dualEdgeMultiplePath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D, E, F, G, H),
                                          of(new TestEdge(A, B, W1),
                                             new TestEdge(A, C, W3),
                                             new TestEdge(B, D, W2),
                                             new TestEdge(B, C, W1),
                                             new TestEdge(B, E, W4),
                                             new TestEdge(C, E, W1),
                                             new TestEdge(D, H, W5),
                                             new TestEdge(D, E, W1),
                                             new TestEdge(E, F, W1),
                                             new TestEdge(F, D, W1),
                                             new TestEdge(F, G, W1),
                                             new TestEdge(F, H, W1),
                                             new TestEdge(A, E, W3),
                                             new TestEdge(B, D, W1)));
        executeSearch(graphSearch(), graph, A, E, weigher, 3, W3);
        executeSinglePathSearch(graphSearch(), graph, A, E, weigher, 1, W3);
    }

    @Test
    public void negativeWeights() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D, E, F, G),
                                          of(new TestEdge(A, B, W1),
                                             new TestEdge(A, C, NW1),
                                             new TestEdge(B, D, W1),
                                             new TestEdge(D, A, NW2),
                                             new TestEdge(C, D, W1),
                                             new TestEdge(D, E, W1),
                                             new TestEdge(D, F, W1),
                                             new TestEdge(E, G, W1),
                                             new TestEdge(F, G, W1),
                                             new TestEdge(G, A, NW5),
                                             new TestEdge(A, G, W4)));
        executeSearch(graphSearch(), graph, A, G, weigher, 3, new TestDoubleWeight(4.0));
        executeSinglePathSearch(graphSearch(), graph, A, G, weigher, 1, new TestDoubleWeight(4.0));
    }

    public void disconnectedPerf() {
        disconnected();
        disconnected();
        disconnected();
        disconnected();
        disconnected();
        disconnected();
        disconnected();
        disconnected();
        disconnected();
        disconnected();
    }

    public void disconnected() {
        Set<TestVertex> vertexes = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            vertexes.add(new TestVertex("v" + i));
        }

        graph = new AdjacencyListsGraph<>(vertexes, of());

        long start = System.nanoTime();
        for (TestVertex src : vertexes) {
            executeSearch(graphSearch(), graph, src, null, null, 0, new TestDoubleWeight(0));
        }
        long end = System.nanoTime();
        DecimalFormat fmt = new DecimalFormat("#,###");
        System.out.println("Compute cost is " + fmt.format(end - start) + " nanos");
    }

}
