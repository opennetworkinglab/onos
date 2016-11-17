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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Class for test KshortestPathsSearch.
 */
public class KShortestPathsSearchTest<V extends Vertex, E extends Edge<V>> extends GraphTest {
    private KShortestPathsSearch<TestVertex, TestEdge> kShortestPathsSearch = new KShortestPathsSearch<>();
    private GraphPathSearch.Result<TestVertex, TestEdge> result;

    @Before
    public void setUp() {
        graph = new AdjacencyListsGraph<>(vertexes(), edges());
    }
    @Test
    public void noPath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                          of(new TestEdge(A, B, 1),
                                             new TestEdge(B, A, 1),
                                             new TestEdge(C, D, 1),
                                             new TestEdge(D, C, 1)));
        KShortestPathsSearch<TestVertex, TestEdge> kShortestPathsSearch = new KShortestPathsSearch<>();
        GraphPathSearch.Result<TestVertex, TestEdge> result = kShortestPathsSearch.search(graph, A, D, weight, 1);
        Set<Path<TestVertex, TestEdge>> resultPathSet = result.paths();
        assertTrue("There should not be any paths.", resultPathSet.isEmpty());
    }

    @Test
    public void testSinglePath() {
        //Tests that there is only a single path possible between A and B
        graph = new AdjacencyListsGraph<>(vertexes(), edges());
        this.result = kShortestPathsSearch.search(graph, A, B, weight, 2);
        Iterator<Path<TestVertex, TestEdge>> itr = result.paths().iterator();
        assertEquals("incorrect paths count", 1, result.paths().size());
        List<TestEdge> correctEdgeList = Lists.newArrayList();
        correctEdgeList.add(new TestEdge(A, B, 1));
        assertTrue("That wrong path was returned.",
                   edgeListsAreEqual(correctEdgeList, result.paths().iterator().next().edges()));
    }

    @Test
    public void testTwoPath() {
        //Tests that there are only two paths between A and C and that they are returned in the correct order
        result = kShortestPathsSearch.search(graph, A, C, weight, 3);
        assertTrue("There are an unexpected number of paths.", result.paths().size() == 2);
        Iterator<Path<TestVertex, TestEdge>> edgeListIterator = result.paths().iterator();
        List<TestEdge> correctEdgeList = Lists.newArrayList();
        correctEdgeList.add(new TestEdge(A, B, 1));
        correctEdgeList.add(new TestEdge(B, C, 1));
        assertTrue("The first path from A to C was incorrect.",
                   edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));
        correctEdgeList.clear();
        correctEdgeList.add(new TestEdge(A, C, 3));
        assertTrue("The second path from A to C was incorrect.",
                   edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));
    }

    @Test
    public void testFourPath() {
        //Tests that there are only four paths between A and E and that they are returned in the correct order
        //Also tests the special case where some correct solutions are equal
        result = kShortestPathsSearch.search(graph, A, E, weight, 5);
        assertTrue("There are an unexpected number of paths.", result.paths().size() == 4);
        Iterator<Path<TestVertex, TestEdge>> edgeListIterator = result.paths().iterator();
        List<TestEdge> correctEdgeList = Lists.newArrayList();
        correctEdgeList.add(new TestEdge(A, B, 1));
        correctEdgeList.add(new TestEdge(B, C, 1));
        correctEdgeList.add(new TestEdge(C, E, 1));
        assertTrue("The first path from A to E was incorrect.",
                   edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));
        correctEdgeList.clear();
        //There are two paths of equal length that should hold positions two and three
        List<TestEdge> alternateCorrectEdgeList = Lists.newArrayList();
        correctEdgeList.add(new TestEdge(A, C, 3));
        correctEdgeList.add(new TestEdge(C, E, 1));
        alternateCorrectEdgeList.add(new TestEdge(A, B, 1));
        alternateCorrectEdgeList.add(new TestEdge(B, D, 2));
        alternateCorrectEdgeList.add(new TestEdge(D, E, 1));
        List<TestEdge> candidateOne = edgeListIterator.next().edges();
        List<TestEdge> candidateTwo = edgeListIterator.next().edges();
        if (candidateOne.size() == 2) {
            assertTrue("The second path from A to E was incorrect.",
                       edgeListsAreEqual(candidateOne, correctEdgeList));
            assertTrue("The third path from A to E was incorrect.",
                       edgeListsAreEqual(candidateTwo, alternateCorrectEdgeList));
        } else {
            assertTrue("The second path from A to E was incorrect.",
                       edgeListsAreEqual(candidateOne, alternateCorrectEdgeList));
            assertTrue("The third path from A to E was incorrect.",
                       edgeListsAreEqual(candidateTwo, correctEdgeList));
        }
        correctEdgeList.clear();
        correctEdgeList.add(new TestEdge(A, B, 1));
        correctEdgeList.add(new TestEdge(B, E, 4));
        assertTrue("The fourth path rom A to E was incorrect",
                   edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));

    }

    @Test
    public void testPathsFromSink() {
        //H is a sink in this topology, insure there are no paths from it to any other location
        for (TestVertex vertex : vertexes()) {
            assertTrue("There should be no paths from vertex H to any other node.",
                       kShortestPathsSearch.search(graph, H, vertex, weight, 1).paths().size() == 0);
        }
    }

    @Test
    public void testLimitPathSetSize() {
        //Checks to make sure that no more than K paths are returned
        result = kShortestPathsSearch.search(graph, A, E, weight, 3);
        assertTrue("There are an unexpected number of paths.", result.paths().size() == 3);
        result = kShortestPathsSearch.search(graph, A, G, weight, 1);
        assertTrue("There are an unexpected number of paths.", result.paths().size() == 1);
    }


    @Test
    public void testVariableLenPathsWithConstantLinkWeight() {

        /*
         * Test graph:
         *
         *      +-+-+  +---+ +---+  +-+-+
         *   +--+ B +--+ C +-+ D +--+ E |
         *   |  +-+-+  +---+ +---+  +-+-+
         *   |    |                   |
         * +-+-+  |    +---+ +---+  +-+-+
         * | A |  +----+ F +-+ G +--+ H |
         * +---+       +---+ +---+  +---+
         */
        graph = new AdjacencyListsGraph<>(of(A, B, C, D, E, F, G, H), of(
                new TestEdge(A, B, 1),
                new TestEdge(B, A, 1),
                new TestEdge(B, C, 1),
                new TestEdge(C, B, 1),
                new TestEdge(C, D, 1),
                new TestEdge(D, C, 1),
                new TestEdge(D, E, 1),
                new TestEdge(E, D, 1),
                new TestEdge(E, H, 1),
                new TestEdge(H, E, 1),
                new TestEdge(H, G, 1),
                new TestEdge(G, H, 1),
                new TestEdge(G, F, 1),
                new TestEdge(F, G, 1),
                new TestEdge(F, B, 1),
                new TestEdge(B, F, 1)
        ));

        weight = edge -> 1.0;

        //Tests that there are only two paths between A and G and that they are returned in the correct order
        result = kShortestPathsSearch.search(graph, A, G, weight, 5);

        assertEquals("There are an unexpected number of paths.", 2, result.paths().size());

        Iterator<Path<TestVertex, TestEdge>> edgeListIterator = result.paths().iterator();

        List<TestEdge> correctEdgeList = Lists.newArrayList();
        correctEdgeList.add(new TestEdge(A, B, 1));
        correctEdgeList.add(new TestEdge(B, F, 1));
        correctEdgeList.add(new TestEdge(F, G, 1));
        assertTrue("The first path from A to G was incorrect.",
                edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));

        correctEdgeList.clear();
        correctEdgeList.add(new TestEdge(A, B, 1));
        correctEdgeList.add(new TestEdge(B, C, 1));
        correctEdgeList.add(new TestEdge(C, D, 1));
        correctEdgeList.add(new TestEdge(D, E, 1));
        correctEdgeList.add(new TestEdge(E, H, 1));
        correctEdgeList.add(new TestEdge(H, G, 1));
        assertTrue("The second path from A to G was incorrect.",
                edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));
    }


    private boolean edgeListsAreEqual(List<TestEdge> edgeListOne, List<TestEdge> edgeListTwo) {
        if (edgeListOne.size() != edgeListTwo.size()) {
            return false;
        }
        TestEdge edgeOne;
        TestEdge edgeTwo;
        for (int i = 0; i < edgeListOne.size(); i++) {
            edgeOne = edgeListOne.get(i);
            edgeTwo = edgeListTwo.get(i);
            if (!edgeOne.equals(edgeTwo)) {
                return false;
            }
        }
        return true;
    }
}
