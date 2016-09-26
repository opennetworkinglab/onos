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

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.onlab.graph.GraphPathSearch.ALL_PATHS;

/**
 * Test of the BFS and similar path search algorithms.
 */
public class BreadthFirstSearchTest extends AbstractGraphPathSearchTest {

    @Override
    protected AbstractGraphPathSearch<TestVertex, TestEdge> graphSearch() {
        return new BreadthFirstSearch<>();
    }

    @Test
    public void defaultGraphTest() {
        executeDefaultTest(7, 3, new TestDoubleWeight(8.0));
    }

    @Test
    public void defaultHopCountWeight() {
        weigher = null;
        executeDefaultTest(7, 3, new ScalarWeight(3.0));
    }

    // Executes the default test
    protected void executeDefaultTest(int pathCount, int pathLength, Weight pathCost) {
        graph = new AdjacencyListsGraph<>(vertexes(), edges());

        GraphPathSearch<TestVertex, TestEdge> search = graphSearch();
        Set<Path<TestVertex, TestEdge>> paths =
                search.search(graph, A, H, weigher, ALL_PATHS).paths();
        assertEquals("incorrect paths count", 1, paths.size());

        Path p = paths.iterator().next();
        assertEquals("incorrect src", A, p.src());
        assertEquals("incorrect dst", H, p.dst());
        assertEquals("incorrect path length", pathLength, p.edges().size());
        assertEquals("incorrect path cost", pathCost, p.cost());

        paths = search.search(graph, A, null, weigher, ALL_PATHS).paths();
        printPaths(paths);
        assertEquals("incorrect paths count", pathCount, paths.size());
    }

    // Executes the search and validates its results.
    protected void executeSearch(GraphPathSearch<TestVertex, TestEdge> search,
                                 Graph<TestVertex, TestEdge> graph,
                                 TestVertex src, TestVertex dst,
                                 EdgeWeigher<TestVertex, TestEdge> weigher,
                                 int pathCount, Weight pathCost) {
        GraphPathSearch.Result<TestVertex, TestEdge> result =
                search.search(graph, src, dst, weigher, ALL_PATHS);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        printPaths(paths);
        assertEquals("incorrect paths count", pathCount, paths.size());
        if (pathCount > 0) {
            Path<TestVertex, TestEdge> path = paths.iterator().next();
            assertEquals("incorrect path cost", pathCost, path.cost());
        }
    }

    // Executes the single-path search and validates its results.
    protected void executeSinglePathSearch(GraphPathSearch<TestVertex, TestEdge> search,
                                           Graph<TestVertex, TestEdge> graph,
                                           TestVertex src, TestVertex dst,
                                           EdgeWeigher<TestVertex, TestEdge> weigher,
                                           int pathCount, Weight pathCost) {
        GraphPathSearch.Result<TestVertex, TestEdge> result =
                search.search(graph, src, dst, weigher, 1);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        printPaths(paths);
        assertEquals("incorrect paths count", Math.min(pathCount, 1), paths.size());
        if (pathCount > 0) {
            Path<TestVertex, TestEdge> path = paths.iterator().next();
            assertEquals("incorrect path cost", pathCost, path.cost());
        }
    }

}
