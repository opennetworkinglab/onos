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

import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;

/**
 * Base class for various graph-related tests.
 */
public class GraphTest {

    static final TestVertex A = new TestVertex("A");
    static final TestVertex B = new TestVertex("B");
    static final TestVertex C = new TestVertex("C");
    static final TestVertex D = new TestVertex("D");
    static final TestVertex E = new TestVertex("E");
    static final TestVertex F = new TestVertex("F");
    static final TestVertex G = new TestVertex("G");
    static final TestVertex H = new TestVertex("H");
    static final TestVertex Z = new TestVertex("Z");

    protected Graph<TestVertex, TestEdge> graph;

    protected EdgeWeight<TestVertex, TestEdge> weight =
            new EdgeWeight<TestVertex, TestEdge>() {
                @Override
                public double weight(TestEdge edge) {
                    return edge.weight();
                }
            };

    protected void printPaths(Set<Path<TestVertex, TestEdge>> paths) {
        for (Path p : paths) {
            System.out.println(p);
        }
    }

    protected Set<TestVertex> vertexes() {
        return of(A, B, C, D, E, F, G, H);
    }

    protected Set<TestEdge> edges() {
        return of(new TestEdge(A, B, 1), new TestEdge(A, C, 3),
                  new TestEdge(B, D, 2), new TestEdge(B, C, 1),
                  new TestEdge(B, E, 4), new TestEdge(C, E, 1),
                  new TestEdge(D, H, 5), new TestEdge(D, E, 1),
                  new TestEdge(E, F, 1), new TestEdge(F, D, 1),
                  new TestEdge(F, G, 1), new TestEdge(F, H, 1));
    }

}
