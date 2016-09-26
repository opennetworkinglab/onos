/*
 * Copyright 2015-present Open Networking Laboratory
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
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;



/**
 * Test of the Suurballe backup path algorithm.
 */
public class SuurballeGraphSearchTest extends BreadthFirstSearchTest {

    @Override
    protected AbstractGraphPathSearch<TestVertex, TestEdge> graphSearch() {
        return new SuurballeGraphSearch<>();
    }

    @Override
    public void defaultGraphTest() {

    }

    @Override
    public void defaultHopCountWeight() {

    }

    @Test
    public void basicGraphTest() {
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                                                      of(new TestEdge(A, B),
                                                                         new TestEdge(B, C),
                                                                         new TestEdge(A, D),
                                                                         new TestEdge(D, C)));
        executeSearch(graphSearch(), graph, A, C, null, 1, new ScalarWeight(4.0));
    }

    @Test
    public void multiplePathOnePairGraphTest() {
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D, E),
                                                                      of(new TestEdge(A, B, W1),
                                                                         new TestEdge(B, C, W1),
                                                                         new TestEdge(A, D, W1),
                                                                         new TestEdge(D, C, W1),
                                                                         new TestEdge(B, E, W2),
                                                                         new TestEdge(C, E, W1)));
        executeSearch(graphSearch(), graph, A, E, weigher, 1, new TestDoubleWeight(6.0));
    }

    @Test
    public void multiplePathsMultiplePairs() {
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D, E),
                                                                      of(new TestEdge(A, B, W1),
                                                                         new TestEdge(B, E, W1),
                                                                         new TestEdge(A, C, W1),
                                                                         new TestEdge(C, E, W1),
                                                                         new TestEdge(A, D, W1),
                                                                         new TestEdge(D, E, W1),
                                                                         new TestEdge(A, E, W2)));
        GraphPathSearch.Result<TestVertex, TestEdge> result =
                graphSearch().search(graph, A, E, weigher, GraphPathSearch.ALL_PATHS);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        System.out.println("\n\n" + paths + "\n\n\ndone\n");
        assertEquals("incorrect paths count", 3, paths.size());
        DisjointPathPair<TestVertex, TestEdge> dpp = (DisjointPathPair<TestVertex, TestEdge>) paths.iterator().next();
        assertEquals("incorrect disjoint paths per path", 2, dpp.size());
    }

    @Test
    public void differingPrimaryAndBackupPathLengths() {
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D, E),
                                                                      of(new TestEdge(A, B),
                                                                         new TestEdge(B, C),
                                                                         new TestEdge(A, D),
                                                                         new TestEdge(D, C),
                                                                         new TestEdge(B, E),
                                                                         new TestEdge(C, E)));
        executeSearch(graphSearch(), graph, A, E, weigher, 1, new TestDoubleWeight(5.0));
    }

    @Test
    public void onePath() {
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                                                      of(new TestEdge(A, B, W1),
                                                                         new TestEdge(B, C, W1),
                                                                         new TestEdge(A, C, W4),
                                                                         new TestEdge(C, D, W1)));
        GraphPathSearch.Result<TestVertex, TestEdge> result =
                graphSearch().search(graph, A, D, weigher, GraphPathSearch.ALL_PATHS);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        assertEquals("incorrect paths count", 1, paths.size());
        DisjointPathPair<TestVertex, TestEdge> dpp = (DisjointPathPair<TestVertex, TestEdge>) paths.iterator().next();
        assertEquals("incorrect disjoint paths count", 1, dpp.size());
    }

    @Test
    public void noPath() {
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                                                      of(new TestEdge(A, B, W1),
                                                                         new TestEdge(B, C, W1),
                                                                         new TestEdge(A, C, W4)));
        GraphPathSearch.Result<TestVertex, TestEdge> result =
                graphSearch().search(graph, A, D, weigher, GraphPathSearch.ALL_PATHS);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        assertEquals("incorrect paths count", paths.size(), 0);
    }

    @Test
    public void disconnected() {
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                                                      of());
        GraphPathSearch.Result<TestVertex, TestEdge> result =
                graphSearch().search(graph, A, D, weigher, GraphPathSearch.ALL_PATHS);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        assertEquals("incorrect paths count", 0, paths.size());
    }
}
