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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.graph.GraphPathSearch.ALL_PATHS;

/**
 * Test of the Suurballe backup path algorithm.
 */
public class SrlgGraphSearchTest extends BreadthFirstSearchTest {

    @Override
    protected AbstractGraphPathSearch<TestVertex, TestEdge> graphSearch() {
        return new SrlgGraphSearch<>(null);
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
    public void onePathPair() {
        setDefaultWeights();
        TestEdge aB = new TestEdge(A, B, 1);
        TestEdge bC = new TestEdge(B, C, 1);
        TestEdge aD = new TestEdge(A, D, 1);
        TestEdge dC = new TestEdge(D, C, 1);
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                                                      of(aB, bC, aD, dC));
        Map<TestEdge, Integer> riskProfile = new HashMap<>();
        riskProfile.put(aB, 0);
        riskProfile.put(bC, 0);
        riskProfile.put(aD, 1);
        riskProfile.put(dC, 1);
        SrlgGraphSearch<TestVertex, TestEdge> search = new SrlgGraphSearch<>(2, riskProfile);
        Set<Path<TestVertex, TestEdge>> paths = search.search(graph, A, C, weight, ALL_PATHS).paths();
        System.out.println("\n\n\n" + paths + "\n\n\n");
        assertEquals("one disjoint path pair found", 1, paths.size());
        checkIsDisjoint(paths.iterator().next(), riskProfile);
    }

    public void checkIsDisjoint(Path<TestVertex, TestEdge> p, Map<TestEdge, Integer> risks) {
        assertTrue("The path is not a DisjointPathPair", (p instanceof DisjointPathPair));
        DisjointPathPair<TestVertex, TestEdge> q = (DisjointPathPair) p;
        Set<Integer> p1Risks = new HashSet<>();
        for (TestEdge e : q.edges()) {
            p1Risks.add(risks.get(e));
        }
        if (!q.hasBackup()) {
            return;
        }
        Path<TestVertex, TestEdge> pq = q.secondary();
        for (TestEdge e: pq.edges()) {
            assertTrue("The paths are not disjoint", !p1Risks.contains(risks.get(e)));
        }
    }

    @Test
    public void complexGraphTest() {
        setDefaultWeights();
        TestEdge aB = new TestEdge(A, B, 1);
        TestEdge bC = new TestEdge(B, C, 1);
        TestEdge aD = new TestEdge(A, D, 1);
        TestEdge dC = new TestEdge(D, C, 1);
        TestEdge cE = new TestEdge(C, E, 1);
        TestEdge bE = new TestEdge(B, E, 1);
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D, E),
                                                                      of(aB, bC, aD, dC, cE, bE));
        Map<TestEdge, Integer> riskProfile = new HashMap<>();
        riskProfile.put(aB, 0);
        riskProfile.put(bC, 0);
        riskProfile.put(aD, 1);
        riskProfile.put(dC, 1);
        riskProfile.put(cE, 2);
        riskProfile.put(bE, 3);
        SrlgGraphSearch<TestVertex, TestEdge> search = new SrlgGraphSearch<>(4, riskProfile);
        search.search(graph, A, E, weight, ALL_PATHS).paths();
    }

    @Test
    public void multiplePathGraphTest() {
        setDefaultWeights();
        TestEdge aB = new TestEdge(A, B, 1);
        TestEdge bE = new TestEdge(B, E, 1);
        TestEdge aD = new TestEdge(A, D, 1);
        TestEdge dE = new TestEdge(D, E, 1);
        TestEdge aC = new TestEdge(A, C, 1);
        TestEdge cE = new TestEdge(C, E, 1);
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D, E),
                                                                      of(aB, bE, aD, dE, aC, cE));
        Map<TestEdge, Integer> riskProfile = new HashMap<>();
        riskProfile.put(aB, 0);
        riskProfile.put(bE, 1);
        riskProfile.put(aD, 2);
        riskProfile.put(dE, 3);
        riskProfile.put(aC, 4);
        riskProfile.put(cE, 5);
        SrlgGraphSearch<TestVertex, TestEdge> search = new SrlgGraphSearch<>(6, riskProfile);
        Set<Path<TestVertex, TestEdge>> paths = search.search(graph, A, E, weight, ALL_PATHS).paths();
        assertTrue("> one disjoint path pair found", paths.size() >= 1);
        checkIsDisjoint(paths.iterator().next(), riskProfile);
    }

    @Test
    public void onePath() {
        setDefaultWeights();
        TestEdge aB = new TestEdge(A, B, 1);
        TestEdge bC = new TestEdge(B, C, 1);
        TestEdge aD = new TestEdge(A, D, 1);
        TestEdge dC = new TestEdge(D, C, 1);
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                                                      of(aB, bC, aD, dC));
        Map<TestEdge, Integer> riskProfile = new HashMap<>();
        riskProfile.put(aB, 0);
        riskProfile.put(bC, 0);
        riskProfile.put(aD, 1);
        riskProfile.put(dC, 0);
        SrlgGraphSearch<TestVertex, TestEdge> search = new SrlgGraphSearch<>(2, riskProfile);
        Set<Path<TestVertex, TestEdge>> paths = search.search(graph, A, C, weight, ALL_PATHS).paths();
        System.out.println(paths);
        assertTrue("no disjoint path pairs found", paths.size() == 0);
    }

    @Test
    public void noPath() {
        setDefaultWeights();
        TestEdge aB = new TestEdge(A, B, 1);
        TestEdge bC = new TestEdge(B, C, 1);
        TestEdge aD = new TestEdge(A, D, 1);
        TestEdge dC = new TestEdge(D, C, 1);
        Graph<TestVertex, TestEdge> graph = new AdjacencyListsGraph<>(of(A, B, C, D, E),
                                                                      of(aB, bC, aD, dC));
        Map<TestEdge, Integer> riskProfile = new HashMap<>();
        riskProfile.put(aB, 0);
        riskProfile.put(bC, 0);
        riskProfile.put(aD, 1);
        riskProfile.put(dC, 0);
        SrlgGraphSearch<TestVertex, TestEdge> search = new SrlgGraphSearch<>(2, riskProfile);
        Set<Path<TestVertex, TestEdge>> paths = search.search(graph, A, E, weight, ALL_PATHS).paths();
        assertTrue("no disjoint path pairs found", paths.size() == 0);
    }
}
