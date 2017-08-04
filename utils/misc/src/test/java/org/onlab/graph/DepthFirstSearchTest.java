/*
 * Copyright 2014-present Open Networking Foundation
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
import static org.junit.Assert.assertTrue;
import static org.onlab.graph.DepthFirstSearch.EdgeType;
import static org.onlab.graph.GraphPathSearch.ALL_PATHS;

/**
 * Test of the DFS algorithm.
 */
public class DepthFirstSearchTest extends AbstractGraphPathSearchTest {

    @Override
    protected DepthFirstSearch<TestVertex, TestEdge> graphSearch() {
        return new DepthFirstSearch<>();
    }

    @Test
    public void defaultGraphTest() {
        executeDefaultTest(3, 6, new TestDoubleWeight(5.0), new TestDoubleWeight(12.0));
        executeBroadSearch();
    }

    @Test
    public void defaultHopCountWeight() {
        weigher = null;
        executeDefaultTest(3, 6, new ScalarWeight(3.0), new ScalarWeight(6.0));
        executeBroadSearch();
    }

    protected void executeDefaultTest(int minLength, int maxLength,
                                      Weight minCost, Weight maxCost) {
        graph = new AdjacencyListsGraph<>(vertexes(), edges());
        DepthFirstSearch<TestVertex, TestEdge> search = graphSearch();

        DepthFirstSearch<TestVertex, TestEdge>.SpanningTreeResult result =
                (DepthFirstSearch<TestVertex, TestEdge>.SpanningTreeResult)
                        search.search(graph, A, H, weigher, 1);
        Set<Path<TestVertex, TestEdge>> paths = result.paths();
        assertEquals("incorrect path count", 1, paths.size());

        Path path = paths.iterator().next();
        System.out.println(path);
        assertEquals("incorrect src", A, path.src());
        assertEquals("incorrect dst", H, path.dst());

        int l = path.edges().size();
        assertTrue("incorrect path length " + l,
                   minLength <= l && l <= maxLength);
        assertTrue("incorrect path cost " + path.cost(),
                   path.cost().compareTo(minCost) >= 0 &&
                   path.cost().compareTo(maxCost) <= 0);

        System.out.println(result.edges());
        printPaths(paths);
    }

    public void executeBroadSearch() {
        graph = new AdjacencyListsGraph<>(vertexes(), edges());
        DepthFirstSearch<TestVertex, TestEdge> search = graphSearch();

        // Perform narrow path search to a specific destination.
        DepthFirstSearch<TestVertex, TestEdge>.SpanningTreeResult result =
                (DepthFirstSearch<TestVertex, TestEdge>.SpanningTreeResult)
                        search.search(graph, A, null, weigher, ALL_PATHS);
        assertEquals("incorrect paths count", 7, result.paths().size());

        int[] types = new int[]{0, 0, 0, 0};
        for (EdgeType t : result.edges().values()) {
            types[t.ordinal()] += 1;
        }
        assertEquals("incorrect tree-edge count", 7,
                     types[EdgeType.TREE_EDGE.ordinal()]);
        assertEquals("incorrect back-edge count", 1,
                     types[EdgeType.BACK_EDGE.ordinal()]);
        assertEquals("incorrect cross-edge & forward-edge count", 4,
                     types[EdgeType.FORWARD_EDGE.ordinal()] +
                             types[EdgeType.CROSS_EDGE.ordinal()]);
    }

}
