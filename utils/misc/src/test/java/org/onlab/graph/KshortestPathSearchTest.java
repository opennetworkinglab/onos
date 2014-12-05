/*
 * Copyright 2014 Open Networking Laboratory
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
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
//import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class KshortestPathSearchTest extends BreadthFirstSearchTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @Test
    public void noPath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                          of(new TestEdge(A, B, 1),
                                             new TestEdge(B, A, 1),
                                             new TestEdge(C, D, 1),
                                             new TestEdge(D, C, 1)));
        KshortestPathSearch<TestVertex, TestEdge> gs = new KshortestPathSearch<TestVertex, TestEdge>(graph);
        List<List<TestEdge>> result = gs.search(A, D, weight, 1);
        List<Path> paths = new ArrayList<>();
        Iterator<List<TestEdge>> itr = result.iterator();
        while (itr.hasNext()) {
            System.out.println(itr.next().toString());
        }
        assertEquals("incorrect paths count", 0, result.size());
    }

    @Test
    public void test2Path() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                          of(new TestEdge(A, B, 1),
                                             new TestEdge(B, A, 1),
                                             new TestEdge(B, D, 1),
                                             new TestEdge(D, B, 1),
                                             new TestEdge(A, C, 1),
                                             new TestEdge(C, A, 1),
                                             new TestEdge(C, D, 1),
                                             new TestEdge(D, C, 1)));
        KshortestPathSearch<TestVertex, TestEdge> gs = new KshortestPathSearch<TestVertex, TestEdge>(graph);
        List<List<TestEdge>> result = gs.search(A, D, weight, 2);
        List<Path> paths = new ArrayList<>();
        Iterator<List<TestEdge>> itr = result.iterator();
        while (itr.hasNext()) {
            System.out.println(itr.next().toString());
        }
        assertEquals("incorrect paths count", 2, result.size());
        // assertEquals("printing the paths", outContent.toString());
    }

    @Test
    public void test3Path() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D),
                                          of(new TestEdge(A, B, 1),
                                             new TestEdge(B, A, 1),
                                             new TestEdge(A, D, 1),
                                             new TestEdge(D, A, 1),
                                             new TestEdge(B, D, 1),
                                             new TestEdge(D, B, 1),
                                             new TestEdge(A, C, 1),
                                             new TestEdge(C, A, 1),
                                             new TestEdge(C, D, 1),
                                             new TestEdge(D, C, 1)));
        KshortestPathSearch<TestVertex, TestEdge> gs = new KshortestPathSearch<TestVertex, TestEdge>(graph);
        List<List<TestEdge>> result = gs.search(A, D, weight, 3);
        List<Path> paths = new ArrayList<>();
        Iterator<List<TestEdge>> itr = result.iterator();
        while (itr.hasNext()) {
            System.out.println(itr.next().toString());
        }
        assertEquals("incorrect paths count", 3, result.size());
        // assertEquals("printing the paths", outContent.toString());
    }

    @Test
    public void test4Path() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D, E, F),
                                          of(new TestEdge(A, B, 1),
                                             new TestEdge(B, A, 1),
                                             new TestEdge(A, C, 1),
                                             new TestEdge(C, A, 1),
                                             new TestEdge(B, D, 1),
                                             new TestEdge(D, B, 1),
                                             new TestEdge(C, E, 1),
                                             new TestEdge(E, C, 1),
                                             new TestEdge(D, F, 1),
                                             new TestEdge(F, D, 1),
                                             new TestEdge(F, E, 1),
                                             new TestEdge(E, F, 1),
                                             new TestEdge(C, D, 1),
                                             new TestEdge(D, C, 1)));
        KshortestPathSearch<TestVertex, TestEdge> gs = new KshortestPathSearch<TestVertex, TestEdge>(graph);
        List<List<TestEdge>> result = gs.search(A, F, weight, 4);
        List<Path> paths = new ArrayList<>();
        Iterator<List<TestEdge>> itr = result.iterator();
        while (itr.hasNext()) {
            System.out.println(itr.next().toString());
        }
        assertEquals("incorrect paths count", 4, result.size());
        // assertEquals("printing the paths", outContent.toString());
    }

    @Test
    public void test6Path() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D, E, F),
                                          of(new TestEdge(A, B, 1),
                                             new TestEdge(B, A, 1),
                                             new TestEdge(A, C, 1),
                                             new TestEdge(C, A, 1),
                                             new TestEdge(B, D, 1),
                                             new TestEdge(D, B, 1),
                                             new TestEdge(B, C, 1),
                                             new TestEdge(C, B, 1),
                                             new TestEdge(D, E, 1),
                                             new TestEdge(E, D, 1),
                                             new TestEdge(C, E, 1),
                                             new TestEdge(E, C, 1),
                                             new TestEdge(D, F, 1),
                                             new TestEdge(F, D, 1),
                                             new TestEdge(E, F, 1),
                                             new TestEdge(F, E, 1)));
        KshortestPathSearch<TestVertex, TestEdge> gs = new KshortestPathSearch<TestVertex, TestEdge>(graph);
        List<List<TestEdge>> result = gs.search(A, F, weight, 6);
        List<Path> paths = new ArrayList<>();
        Iterator<List<TestEdge>> itr = result.iterator();
        while (itr.hasNext()) {
            System.out.println(itr.next().toString());
        }
        assertEquals("incorrect paths count", 6, result.size());
        // assertEquals("printing the paths", outContent.toString());
    }

    @Test
    public void dualEdgePath() {
        graph = new AdjacencyListsGraph<>(of(A, B, C, D, E, F, G, H),
                                          of(new TestEdge(A, B, 1), new TestEdge(A, C, 3),
                                             new TestEdge(B, D, 2), new TestEdge(B, C, 1),
                                             new TestEdge(B, E, 4), new TestEdge(C, E, 1),
                                             new TestEdge(D, H, 5), new TestEdge(D, E, 1),
                                             new TestEdge(E, F, 1), new TestEdge(F, D, 1),
                                             new TestEdge(F, G, 1), new TestEdge(F, H, 1),
                                             new TestEdge(A, E, 3), new TestEdge(B, D, 1)));
        KshortestPathSearch<TestVertex, TestEdge> gs = new KshortestPathSearch<TestVertex, TestEdge>(graph);
        List<List<TestEdge>> result = gs.search(A, G, weight, 6);
        List<Path> paths = new ArrayList<>();
        Iterator<List<TestEdge>> itr = result.iterator();
        while (itr.hasNext()) {
            System.out.println(itr.next().toString());
        }
        assertEquals("incorrect paths count", 6, result.size());
        // assertEquals("printing the paths", outContent.toString());
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        // System.setOut(new PrintStream(outContent));
    }

    @After
    public void tearDown() throws Exception {
         // System.setOut(null);
    }

}
