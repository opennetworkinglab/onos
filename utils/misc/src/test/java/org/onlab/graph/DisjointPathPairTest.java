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

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;

/**
 * Test of DisjointPathPair.
 */
public class DisjointPathPairTest {

    private static final TestVertex A = new TestVertex("A");
    private static final TestVertex B = new TestVertex("B");
    private static final TestVertex C = new TestVertex("C");
    private static final TestVertex D = new TestVertex("D");

    private static final TestEdge AB = new TestEdge(A, B, 1.0);
    private static final TestEdge BC = new TestEdge(B, C, 1.0);
    private static final TestEdge AD = new TestEdge(A, D, 1.0);
    private static final TestEdge DC = new TestEdge(D, C, 1.0);

    private static final Path<TestVertex, TestEdge> ABC
                            = new DefaultPath<>(ImmutableList.of(AB, BC), 1.0);
    private static final Path<TestVertex, TestEdge> ADC
                            = new DefaultPath<>(ImmutableList.of(AD, DC), 1.0);

    @Test
    public void testSwappingPrimarySecondaryDoesntImpactHashCode() {
        assertEquals(new DisjointPathPair<>(ABC, ADC).hashCode(),
                     new DisjointPathPair<>(ADC, ABC).hashCode());
    }

    @Test
    public void testSwappingPrimarySecondaryDoesntImpactEquality() {
        new EqualsTester()
            .addEqualityGroup(new DisjointPathPair<>(ABC, ADC),
                              new DisjointPathPair<>(ADC, ABC));
    }

}
