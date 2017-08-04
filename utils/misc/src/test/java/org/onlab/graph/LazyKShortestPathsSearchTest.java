/*
 * Copyright 2017-present Open Networking Foundation
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

import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class LazyKShortestPathsSearchTest extends GraphTest {

    LazyKShortestPathsSearch<TestVertex, TestEdge> sut;

    @Before
    public void setUp() {
        sut = new LazyKShortestPathsSearch<>();
    }

    @Test
    public void noPath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                          of(new TestEdge(A, B),
                                             new TestEdge(B, A),
                                             new TestEdge(C, D),
                                             new TestEdge(D, C)));
        Stream<Path<TestVertex, TestEdge>> result = sut.lazyPathSearch(graph, A, D, weigher);
        assertEquals("There should not be any paths.", 0, result.count());
    }

    @Test
    public void fourPath() {
        graph = new AdjacencyListsGraph<>(vertexes(), edges());
        Stream<Path<TestVertex, TestEdge>> result = sut.lazyPathSearch(graph, A, E, weigher);
        List<Path<TestVertex, TestEdge>> rList = result.limit(42).collect(Collectors.toList());

        assertEquals("There are an unexpected number of paths.", 4, rList.size());

        // The shortest path
        List<TestEdge> expectedEdges = new ArrayList<>();
        expectedEdges.add(new TestEdge(A, B, W1));
        expectedEdges.add(new TestEdge(B, C, W1));
        expectedEdges.add(new TestEdge(C, E, W1));

        assertEquals("The first path from A to E was incorrect.",
                     expectedEdges, rList.get(0).edges());
        assertEquals(W3, rList.get(0).cost());


        // There are two paths of equal cost as next shortest path
        expectedEdges.clear();
        expectedEdges.add(new TestEdge(A, C, W3));
        expectedEdges.add(new TestEdge(C, E, W1));

        List<TestEdge> alternateEdges = new ArrayList<>();
        alternateEdges.add(new TestEdge(A, B, W1));
        alternateEdges.add(new TestEdge(B, D, W2));
        alternateEdges.add(new TestEdge(D, E, W1));

        assertThat(ImmutableList.of(rList.get(1).edges(), rList.get(2).edges()),
                   containsInAnyOrder(expectedEdges, alternateEdges));
        assertEquals(W4, rList.get(1).cost());
        assertEquals(W4, rList.get(2).cost());


        // last shortest path
        expectedEdges.clear();
        expectedEdges.add(new TestEdge(A, B, W1));
        expectedEdges.add(new TestEdge(B, E, W4));

        assertEquals("The fourth path rom A to E was incorrect",
                   expectedEdges, rList.get(3).edges());
        assertEquals(W5, rList.get(3).cost());
    }

}
