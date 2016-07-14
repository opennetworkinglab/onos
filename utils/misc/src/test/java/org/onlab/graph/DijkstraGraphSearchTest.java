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
        graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                          of(new TestEdge(A, B, 1),
                                             new TestEdge(B, A, 1),
                                             new TestEdge(C, D, 1),
                                             new TestEdge(D, C, 1)));
        GraphPathSearch<TestVertex, TestEdge> gs = graphSearch();
        Set<Path<TestVertex, TestEdge>> paths = gs.search(graph, A, B, weight, 1).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 1, paths.size());
        assertEquals("incorrect path cost", 1.0, paths.iterator().next().cost(), 0.1);

        paths = gs.search(graph, A, D, weight, 1).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 0, paths.size());

        paths = gs.search(graph, A, null, weight, 1).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 1, paths.size());
        assertEquals("incorrect path cost", 1.0, paths.iterator().next().cost(), 0.1);
    }

    @Test
    public void simpleMultiplePath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                          of(new TestEdge(A, B, 1),
                                             new TestEdge(A, C, 1),
                                             new TestEdge(B, D, 1),
                                             new TestEdge(C, D, 1)));
        executeSearch(graphSearch(), graph, A, D, weight, 2, 2.0);
        executeSinglePathSearch(graphSearch(), graph, A, D, weight, 1, 2.0);
    }

    @Test
    public void denseMultiplePath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D, E, F, G),
                                          of(new TestEdge(A, B, 1),
                                             new TestEdge(A, C, 1),
                                             new TestEdge(B, D, 1),
                                             new TestEdge(C, D, 1),
                                             new TestEdge(D, E, 1),
                                             new TestEdge(D, F, 1),
                                             new TestEdge(E, G, 1),
                                             new TestEdge(F, G, 1),
                                             new TestEdge(A, G, 4)));
        executeSearch(graphSearch(), graph, A, G, weight, 5, 4.0);
        executeSinglePathSearch(graphSearch(), graph, A, G, weight, 1, 4.0);
    }

    @Test
    public void dualEdgeMultiplePath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D, E, F, G, H),
                                          of(new TestEdge(A, B, 1), new TestEdge(A, C, 3),
                                             new TestEdge(B, D, 2), new TestEdge(B, C, 1),
                                             new TestEdge(B, E, 4), new TestEdge(C, E, 1),
                                             new TestEdge(D, H, 5), new TestEdge(D, E, 1),
                                             new TestEdge(E, F, 1), new TestEdge(F, D, 1),
                                             new TestEdge(F, G, 1), new TestEdge(F, H, 1),
                                             new TestEdge(A, E, 3), new TestEdge(B, D, 1)));
        executeSearch(graphSearch(), graph, A, E, weight, 3, 3.0);
        executeSinglePathSearch(graphSearch(), graph, A, E, weight, 1, 3.0);
    }

    @Test
    public void negativeWeights() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D, E, F, G),
                                          of(new TestEdge(A, B, 1),
                                             new TestEdge(A, C, -1),
                                             new TestEdge(B, D, 1),
                                             new TestEdge(D, A, -2),
                                             new TestEdge(C, D, 1),
                                             new TestEdge(D, E, 1),
                                             new TestEdge(D, F, 1),
                                             new TestEdge(E, G, 1),
                                             new TestEdge(F, G, 1),
                                             new TestEdge(G, A, -5),
                                             new TestEdge(A, G, 4)));
        executeSearch(graphSearch(), graph, A, G, weight, 3, 4.0);
        executeSinglePathSearch(graphSearch(), graph, A, G, weight, 1, 4.0);
    }

    @Test
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


    @Test
    public void disconnected() {
        Set<TestVertex> vertexes = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            vertexes.add(new TestVertex("v" + i));
        }

        graph = new AdjacencyListsGraph<>(vertexes, of());

        long start = System.nanoTime();
        for (TestVertex src : vertexes) {
            executeSearch(graphSearch(), graph, src, null, null, 0, 0);
        }
        long end = System.nanoTime();
        DecimalFormat fmt = new DecimalFormat("#,###");
        System.out.println("Compute cost is " + fmt.format(end - start) + " nanos");
    }

}
