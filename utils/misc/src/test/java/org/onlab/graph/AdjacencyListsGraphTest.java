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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Tests of the graph implementation.
 */
public class AdjacencyListsGraphTest {

    private static final TestVertex A = new TestVertex("A");
    private static final TestVertex B = new TestVertex("B");
    private static final TestVertex C = new TestVertex("C");
    private static final TestVertex D = new TestVertex("D");
    private static final TestVertex E = new TestVertex("E");
    private static final TestVertex F = new TestVertex("F");
    private static final TestVertex G = new TestVertex("G");

    private final Set<TestEdge> edges =
            ImmutableSet.of(new TestEdge(A, B),
                            new TestEdge(B, C),
                            new TestEdge(C, D),
                            new TestEdge(D, A),
                            new TestEdge(B, D));

    @Test
    public void equality() {
        Set<TestVertex> vertexes = ImmutableSet.of(A, B, C, D, E, F);
        Set<TestVertex> vertexes2 = ImmutableSet.of(A, B, C, D, E, F, G);

        AdjacencyListsGraph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(vertexes, edges);
        AdjacencyListsGraph<TestVertex, TestEdge> same = new AdjacencyListsGraph<>(vertexes, edges);
        AdjacencyListsGraph<TestVertex, TestEdge> different = new AdjacencyListsGraph<>(vertexes2, edges);

        new EqualsTester()
                .addEqualityGroup(graph, same)
                .addEqualityGroup(different)
                .testEquals();
    }

    @Test
    public void basics() {
        Set<TestVertex> vertexes = ImmutableSet.of(A, B, C, D, E, F);
        AdjacencyListsGraph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(vertexes, edges);
        assertEquals("incorrect vertex count", 6, graph.getVertexes().size());
        assertEquals("incorrect edge count", 5, graph.getEdges().size());

        assertEquals("incorrect egress edge count", 1, graph.getEdgesFrom(A).size());
        assertEquals("incorrect ingress edge count", 1, graph.getEdgesTo(A).size());
        assertEquals("incorrect ingress edge count", 1, graph.getEdgesTo(C).size());
        assertEquals("incorrect egress edge count", 2, graph.getEdgesFrom(B).size());
        assertEquals("incorrect ingress edge count", 2, graph.getEdgesTo(D).size());
    }
}
