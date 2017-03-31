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

    static final TestDoubleWeight ZW = new TestDoubleWeight(0);
    static final TestDoubleWeight NW5 = new TestDoubleWeight(-5);
    static final TestDoubleWeight NW2 = new TestDoubleWeight(-2);
    static final TestDoubleWeight NW1 = new TestDoubleWeight(-1);
    static final TestDoubleWeight W1 = new TestDoubleWeight(1);
    static final TestDoubleWeight W2 = new TestDoubleWeight(2);
    static final TestDoubleWeight W3 = new TestDoubleWeight(3);
    static final TestDoubleWeight W4 = new TestDoubleWeight(4);
    static final TestDoubleWeight W5 = new TestDoubleWeight(5);

    protected Graph<TestVertex, TestEdge> graph;

    protected EdgeWeigher<TestVertex, TestEdge> weigher =
            new EdgeWeigher<TestVertex, TestEdge>() {
                @Override
                public Weight weight(TestEdge edge) {
                    return edge.weight();
                }

                @Override
                public Weight getInitialWeight() {
                    return ZW;
                }

                @Override
                public Weight getNonViableWeight() {
                    return TestDoubleWeight.NON_VIABLE_WEIGHT;
                }
            };

    /**
     * EdgeWeigher which only looks at hop count.
     */
    protected final EdgeWeigher<TestVertex, TestEdge> hopWeigher =
            new EdgeWeigher<TestVertex, TestEdge>() {
                @Override
                public Weight weight(TestEdge edge) {
                    return W1;
                }

                @Override
                public Weight getInitialWeight() {
                    return ZW;
                }

                @Override
                public Weight getNonViableWeight() {
                    return TestDoubleWeight.NON_VIABLE_WEIGHT;
                }
            };

    protected void printPaths(Set<Path<TestVertex, TestEdge>> paths) {
        for (Path p : paths) {
            System.out.println(p);
        }
    }

    /**
     * @return 8 vertices A to H.
     */
    protected Set<TestVertex> vertexes() {
        return of(A, B, C, D, E, F, G, H);
    }

    /**
     * <pre>
     * A → B → D → H
     * ↓ ↙ ↓ ↙ ↑ ↗
     * C → E → F → G
     * </pre>
     * Note: not all edges have same weight, see method body for details.
     * @return 12 edges illustrated as above.
     */
    protected Set<TestEdge> edges() {
        return of(new TestEdge(A, B, W1),
                  new TestEdge(A, C, W3),
                  new TestEdge(B, D, W2),
                  new TestEdge(B, C, W1),
                  new TestEdge(B, E, W4),
                  new TestEdge(C, E, W1),
                  new TestEdge(D, H, W5),
                  new TestEdge(D, E, W1),
                  new TestEdge(E, F, W1),
                  new TestEdge(F, D, W1),
                  new TestEdge(F, G, W1),
                  new TestEdge(F, H, W1));
    }

}
