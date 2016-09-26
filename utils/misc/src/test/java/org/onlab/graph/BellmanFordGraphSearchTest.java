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

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.onlab.graph.GraphPathSearch.ALL_PATHS;

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
        executeDefaultTest(7, 5, new TestDoubleWeight(5.0));
    }

    @Test
    public void defaultHopCountWeight() {
        weigher = null;
        executeDefaultTest(10, 3, new ScalarWeight(3.0));
    }

    @Test
    public void searchGraphWithNegativeCycles() {
        Set<TestVertex> vertexes = new HashSet<>(vertexes());
        vertexes.add(Z);

        Set<TestEdge> edges = new HashSet<>(edges());
        edges.add(new TestEdge(G, Z, new TestDoubleWeight(1.0)));
        edges.add(new TestEdge(Z, G, new TestDoubleWeight(-2.0)));

        graph = new AdjacencyListsGraph<>(vertexes, edges);

        GraphPathSearch<TestVertex, TestEdge> search = graphSearch();
        Set<Path<TestVertex, TestEdge>> paths = search.search(graph, A, H, weigher, ALL_PATHS).paths();
        assertEquals("incorrect paths count", 1, paths.size());

        Path p = paths.iterator().next();
        assertEquals("incorrect src", A, p.src());
        assertEquals("incorrect dst", H, p.dst());
        assertEquals("incorrect path length", 5, p.edges().size());
        assertEquals("incorrect path cost", new TestDoubleWeight(5), p.cost());

        paths = search.search(graph, A, G, weigher, ALL_PATHS).paths();
        assertEquals("incorrect paths count", 0, paths.size());

        paths = search.search(graph, A, null, weigher, ALL_PATHS).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", 6, paths.size());
    }

}
