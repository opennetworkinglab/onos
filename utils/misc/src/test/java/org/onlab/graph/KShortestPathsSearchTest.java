/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Class for test KshortestPathsSearch.
 */
public class KShortestPathsSearchTest extends GraphTest {
    private KShortestPathsSearch<TestVertex, TestEdge> kShortestPathsSearch = new KShortestPathsSearch<>();
    private GraphPathSearch.Result<TestVertex, TestEdge> result;

    @Before
    public void setUp() {
        graph = new AdjacencyListsGraph<>(vertexes(), edges());
    }

    @Test
    public void noPath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                          of(new TestEdge(A, B),
                                             new TestEdge(B, A),
                                             new TestEdge(C, D),
                                             new TestEdge(D, C)));
        KShortestPathsSearch<TestVertex, TestEdge> kShortestPathsSearch = new KShortestPathsSearch<>();
        GraphPathSearch.Result<TestVertex, TestEdge> result = kShortestPathsSearch.search(graph, A, D, weigher, 1);
        Set<Path<TestVertex, TestEdge>> resultPathSet = result.paths();
        assertTrue("There should not be any paths.", resultPathSet.isEmpty());
    }

    @Test
    public void testSinglePath() {
        //Tests that there is only a single path possible between A and B
        graph = new AdjacencyListsGraph<>(vertexes(), edges());
        this.result = kShortestPathsSearch.search(graph, A, B, weigher, 2);
        assertEquals("incorrect paths count", 1, result.paths().size());
        List<TestEdge> correctEdgeList = Lists.newArrayList();
        correctEdgeList.add(new TestEdge(A, B, W1));
        assertTrue("That wrong path was returned.",
                   edgeListsAreEqual(correctEdgeList, result.paths().iterator().next().edges()));
    }

    @Test
    public void testResultsAreOneHopPathPlusLongerOnes() {
        graph = new AdjacencyListsGraph<>(vertexes(), edges());
        this.result = kShortestPathsSearch.search(graph, B, D, hopWeigher, 42);
        assertEquals("incorrect paths count", 3, result.paths().size());
        assertThat("the shortest path size is 1 hop",
                   Iterables.get(result.paths(), 0).edges().size(),
                   is(1));
        assertThat("the 2nd shortest path size is 3 hop",
                   Iterables.get(result.paths(), 1).edges().size(),
                   is(3));
        assertThat("the 3rd shortest path size is 4 hop",
                   Iterables.get(result.paths(), 2).edges().size(),
                   is(4));
    }

    @Test
    public void testTwoPath() {
        //Tests that there are only two paths between A and C and that they are returned in the correct order
        result = kShortestPathsSearch.search(graph, A, C, weigher, 3);
        assertTrue("There are an unexpected number of paths.", result.paths().size() == 2);
        Iterator<Path<TestVertex, TestEdge>> edgeListIterator = result.paths().iterator();
        List<TestEdge> correctEdgeList = Lists.newArrayList();
        correctEdgeList.add(new TestEdge(A, B, W1));
        correctEdgeList.add(new TestEdge(B, C, W1));
        assertTrue("The first path from A to C was incorrect.",
                   edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));
        correctEdgeList.clear();
        correctEdgeList.add(new TestEdge(A, C, W3));
        assertTrue("The second path from A to C was incorrect.",
                   edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));
    }

    @Test
    public void testFourPath() {
        //Tests that there are only four paths between A and E and that they are returned in the correct order
        //Also tests the special case where some correct solutions are equal
        result = kShortestPathsSearch.search(graph, A, E, weigher, 5);
        assertTrue("There are an unexpected number of paths.", result.paths().size() == 4);
        Iterator<Path<TestVertex, TestEdge>> edgeListIterator = result.paths().iterator();
        List<TestEdge> correctEdgeList = Lists.newArrayList();
        correctEdgeList.add(new TestEdge(A, B, W1));
        correctEdgeList.add(new TestEdge(B, C, W1));
        correctEdgeList.add(new TestEdge(C, E, W1));
        assertTrue("The first path from A to E was incorrect.",
                   edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));
        correctEdgeList.clear();
        //There are two paths of equal length that should hold positions two and three
        List<TestEdge> alternateCorrectEdgeList = Lists.newArrayList();
        correctEdgeList.add(new TestEdge(A, C, W3));
        correctEdgeList.add(new TestEdge(C, E, W1));
        alternateCorrectEdgeList.add(new TestEdge(A, B, W1));
        alternateCorrectEdgeList.add(new TestEdge(B, D, W2));
        alternateCorrectEdgeList.add(new TestEdge(D, E, W1));
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
        correctEdgeList.add(new TestEdge(A, B, W1));
        correctEdgeList.add(new TestEdge(B, E, W4));
        assertTrue("The fourth path rom A to E was incorrect",
                   edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));

    }

    @Test
    public void testPathsFromSink() {
        //H is a sink in this topology, insure there are no paths from it to any other location
        for (TestVertex vertex : vertexes()) {
            assertTrue("There should be no paths from vertex H to any other node.",
                       kShortestPathsSearch.search(graph, H, vertex, weigher, 1).paths().size() == 0);
        }
    }

    @Test
    public void testLimitPathSetSize() {
        //Checks to make sure that no more than K paths are returned
        result = kShortestPathsSearch.search(graph, A, E, weigher, 3);
        assertEquals("There are an unexpected number of paths.", 3, result.paths().size());
        result = kShortestPathsSearch.search(graph, A, G, weigher, 1);
        assertEquals("There are an unexpected number of paths.", 1, result.paths().size());
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
                new TestEdge(A, B),
                new TestEdge(B, A),
                new TestEdge(B, C),
                new TestEdge(C, B),
                new TestEdge(C, D),
                new TestEdge(D, C),
                new TestEdge(D, E),
                new TestEdge(E, D),
                new TestEdge(E, H),
                new TestEdge(H, E),
                new TestEdge(H, G),
                new TestEdge(G, H),
                new TestEdge(G, F),
                new TestEdge(F, G),
                new TestEdge(F, B),
                new TestEdge(B, F)
        ));

        //Tests that there are only two paths between A and G and that they are returned in the correct order
        result = kShortestPathsSearch.search(graph, A, G, weigher, 5);

        assertEquals("There are an unexpected number of paths.", 2, result.paths().size());

        Iterator<Path<TestVertex, TestEdge>> edgeListIterator = result.paths().iterator();

        List<TestEdge> correctEdgeList = Lists.newArrayList();
        correctEdgeList.add(new TestEdge(A, B));
        correctEdgeList.add(new TestEdge(B, F));
        correctEdgeList.add(new TestEdge(F, G));
        assertTrue("The first path from A to G was incorrect.",
                edgeListsAreEqual(edgeListIterator.next().edges(), correctEdgeList));

        correctEdgeList.clear();
        correctEdgeList.add(new TestEdge(A, B));
        correctEdgeList.add(new TestEdge(B, C));
        correctEdgeList.add(new TestEdge(C, D));
        correctEdgeList.add(new TestEdge(D, E));
        correctEdgeList.add(new TestEdge(E, H));
        correctEdgeList.add(new TestEdge(H, G));
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
