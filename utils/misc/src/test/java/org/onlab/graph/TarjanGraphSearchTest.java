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

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertEquals;
import static org.onlab.graph.TarjanGraphSearch.SccResult;

/**
 * Tarjan graph search tests.
 */
public class TarjanGraphSearchTest extends GraphTest {

    private void validate(SccResult<TestVertex, TestEdge> result, int cc) {
        System.out.println("Cluster count: " + result.clusterVertexes().size());
        System.out.println("Clusters: " + result.clusterVertexes());
        assertEquals("incorrect cluster count", cc, result.clusterCount());
    }

    private void validate(SccResult<TestVertex, TestEdge> result,
                          int i, int vc, int ec) {
        assertEquals("incorrect cluster count", vc, result.clusterVertexes().get(i).size());
        assertEquals("incorrect edge count", ec, result.clusterEdges().get(i).size());
    }

    @Test
    public void basic() {
        graph = new AdjacencyListsGraph<>(vertexes(), edges());
        TarjanGraphSearch<TestVertex, TestEdge> gs = new TarjanGraphSearch<>();
        SccResult<TestVertex, TestEdge> result = gs.search(graph, null);
        validate(result, 6);
    }

    @Test
    public void singleCluster() {
        graph = new AdjacencyListsGraph<>(vertexes(),
                                          of(new TestEdge(A, B),
                                             new TestEdge(B, C),
                                             new TestEdge(C, D),
                                             new TestEdge(D, E),
                                             new TestEdge(E, F),
                                             new TestEdge(F, G),
                                             new TestEdge(G, H),
                                             new TestEdge(H, A)));

        TarjanGraphSearch<TestVertex, TestEdge> gs = new TarjanGraphSearch<>();
        SccResult<TestVertex, TestEdge> result = gs.search(graph, null);
        validate(result, 1);
        validate(result, 0, 8, 8);
    }

    @Test
    public void twoUnconnectedCluster() {
        graph = new AdjacencyListsGraph<>(vertexes(),
                                          of(new TestEdge(A, B),
                                             new TestEdge(B, C),
                                             new TestEdge(C, D),
                                             new TestEdge(D, A),
                                             new TestEdge(E, F),
                                             new TestEdge(F, G),
                                             new TestEdge(G, H),
                                             new TestEdge(H, E)));
        TarjanGraphSearch<TestVertex, TestEdge> gs = new TarjanGraphSearch<>();
        SccResult<TestVertex, TestEdge> result = gs.search(graph, null);
        validate(result, 2);
        validate(result, 0, 4, 4);
        validate(result, 1, 4, 4);
    }

    @Test
    public void twoWeaklyConnectedClusters() {
        graph = new AdjacencyListsGraph<>(vertexes(),
                                          of(new TestEdge(A, B),
                                             new TestEdge(B, C),
                                             new TestEdge(C, D),
                                             new TestEdge(D, A),
                                             new TestEdge(E, F),
                                             new TestEdge(F, G),
                                             new TestEdge(G, H),
                                             new TestEdge(H, E),
                                             new TestEdge(B, E)));
        TarjanGraphSearch<TestVertex, TestEdge> gs = new TarjanGraphSearch<>();
        SccResult<TestVertex, TestEdge> result = gs.search(graph, null);
        validate(result, 2);
        validate(result, 0, 4, 4);
        validate(result, 1, 4, 4);
    }

    @Test
    public void twoClustersConnectedWithIgnoredEdges() {
        graph = new AdjacencyListsGraph<>(vertexes(),
                                          of(new TestEdge(A, B),
                                             new TestEdge(B, C),
                                             new TestEdge(C, D),
                                             new TestEdge(D, A),
                                             new TestEdge(E, F),
                                             new TestEdge(F, G),
                                             new TestEdge(G, H),
                                             new TestEdge(H, E),
                                             new TestEdge(B, E,
                                                     weigher.getNonViableWeight()),
                                             new TestEdge(E, B,
                                                     weigher.getNonViableWeight())));

        TarjanGraphSearch<TestVertex, TestEdge> gs = new TarjanGraphSearch<>();
        SccResult<TestVertex, TestEdge> result = gs.search(graph, weigher);
        validate(result, 2);
        validate(result, 0, 4, 4);
        validate(result, 1, 4, 4);
    }

}
