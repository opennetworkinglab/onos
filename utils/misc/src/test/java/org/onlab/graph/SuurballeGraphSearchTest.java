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

    public void setWeights() {
        weight = new EdgeWeight<TestVertex, TestEdge>() {
            @Override
            public double weight(TestEdge edge) {
                return edge.weight();
            }
        };
    }
    public void setDefaultWeights() {
        weight = null;
    }
    @Override
    public void defaultGraphTest() {

    }

    @Override
    public void defaultHopCountWeight() {

    }

    @Test
    public void basicGraphTest() {
        setDefaultWeights();
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                                                      of(new TestEdge(A, B, 1),
                                                                         new TestEdge(B, C, 1),
                                                                         new TestEdge(A, D, 1),
                                                                         new TestEdge(D, C, 1)));
        executeSearch(graphSearch(), graph, A, C, weight, 1, 4.0);
    }

    @Test
    public void multiplePathOnePairGraphTest() {
        setWeights();
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D, E),
                                                                      of(new TestEdge(A, B, 1),
                                                                         new TestEdge(B, C, 1),
                                                                         new TestEdge(A, D, 1),
                                                                         new TestEdge(D, C, 1),
                                                                         new TestEdge(B, E, 2),
                                                                         new TestEdge(C, E, 1)));
        executeSearch(graphSearch(), graph, A, E, weight, 1, 6.0);
    }

    @Test
    public void multiplePathsMultiplePairs() {
        setWeights();
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D, E),
                                                                      of(new TestEdge(A, B, 1),
                                                                         new TestEdge(B, E, 1),
                                                                         new TestEdge(A, C, 1),
                                                                         new TestEdge(C, E, 1),
                                                                         new TestEdge(A, D, 1),
                                                                         new TestEdge(D, E, 1),
                                                                         new TestEdge(A, E, 2)));
        GraphPathSearch.Result<TestVertex, TestEdge> result =
                graphSearch().search(graph, A, E, weight, GraphPathSearch.ALL_PATHS);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        System.out.println("\n\n" + paths + "\n\n\ndone\n");
        assertEquals("incorrect paths count", 3, paths.size());
        DisjointPathPair<TestVertex, TestEdge> dpp = (DisjointPathPair<TestVertex, TestEdge>) paths.iterator().next();
        assertEquals("incorrect disjoint paths per path", 2, dpp.size());
    }

    @Test
    public void differingPrimaryAndBackupPathLengths() {
        setWeights();
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D, E),
                                                                      of(new TestEdge(A, B, 1),
                                                                         new TestEdge(B, C, 1),
                                                                         new TestEdge(A, D, 1),
                                                                         new TestEdge(D, C, 1),
                                                                         new TestEdge(B, E, 1),
                                                                         new TestEdge(C, E, 1)));
        executeSearch(graphSearch(), graph, A, E, weight, 1, 5.0);
    }

    @Test
    public void onePath() {
        setWeights();
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                                                      of(new TestEdge(A, B, 1),
                                                                         new TestEdge(B, C, 1),
                                                                         new TestEdge(A, C, 4),
                                                                         new TestEdge(C, D, 1)));
        GraphPathSearch.Result<TestVertex, TestEdge> result =
                graphSearch().search(graph, A, D, weight, GraphPathSearch.ALL_PATHS);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        assertEquals("incorrect paths count", 1, paths.size());
        DisjointPathPair<TestVertex, TestEdge> dpp = (DisjointPathPair<TestVertex, TestEdge>) paths.iterator().next();
        assertEquals("incorrect disjoint paths count", 1, dpp.size());
    }

    @Test
    public void noPath() {
        setWeights();
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                                                      of(new TestEdge(A, B, 1),
                                                                         new TestEdge(B, C, 1),
                                                                         new TestEdge(A, C, 4)));
        GraphPathSearch.Result<TestVertex, TestEdge> result =
                graphSearch().search(graph, A, D, weight, GraphPathSearch.ALL_PATHS);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        assertEquals("incorrect paths count", paths.size(), 0);
    }

    @Test
    public void disconnected() {
        setWeights();
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                                                      of());
        GraphPathSearch.Result<TestVertex, TestEdge> result =
                graphSearch().search(graph, A, D, weight, GraphPathSearch.ALL_PATHS);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        assertEquals("incorrect paths count", 0, paths.size());
    }
}
