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

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test of the default mutable path.
 */
public class DefaultMutablePathTest extends DefaultPathTest {

    @Test
    public void equality() {
        DefaultPath<TestVertex, TestEdge> p1 =
                new DefaultPath<>(of(new TestEdge(A, B),
                                     new TestEdge(B, C)),
                        new TestDoubleWeight(2.0));
        DefaultPath<TestVertex, TestEdge> p2 =
                new DefaultPath<>(of(new TestEdge(A, B),
                                     new TestEdge(B, D)),
                        new TestDoubleWeight(2.0));
        new EqualsTester().addEqualityGroup(new DefaultMutablePath<>(p1),
                                            new DefaultMutablePath<>(p1))
                .addEqualityGroup(new DefaultMutablePath<>(p2))
                .testEquals();
    }

    @Test
    public void empty() {
        MutablePath<TestVertex, TestEdge> p = new DefaultMutablePath<>();
        assertNull("src should be null", p.src());
        assertNull("dst should be null", p.dst());
        assertEquals("incorrect edge count", 0, p.edges().size());
        assertEquals("incorrect path cost", null, p.cost());
    }

    @Test
    public void pathCost() {
        MutablePath<TestVertex, TestEdge> p = new DefaultMutablePath<>();
        Weight weight = new TestDoubleWeight(4);
        p.setCost(weight);
        assertEquals("incorrect path cost", weight, p.cost());
    }

    private void validatePath(Path<TestVertex, TestEdge> p,
                              TestVertex src, TestVertex dst, int length) {
        validatePath(p, src, dst, length, null);
    }

    @Test
    public void insertEdge() {
        MutablePath<TestVertex, TestEdge> p = new DefaultMutablePath<>();
        p.insertEdge(new TestEdge(B, C));
        p.insertEdge(new TestEdge(A, B));
        validatePath(p, A, C, 2);
    }

    @Test
    public void appendEdge() {
        MutablePath<TestVertex, TestEdge> p = new DefaultMutablePath<>();
        p.appendEdge(new TestEdge(A, B));
        p.appendEdge(new TestEdge(B, C));
        validatePath(p, A, C, 2);
    }

    @Test
    public void removeEdge() {
        MutablePath<TestVertex, TestEdge> p = new DefaultMutablePath<>();
        p.appendEdge(new TestEdge(A, B));
        p.appendEdge(new TestEdge(B, C));
        p.appendEdge(new TestEdge(C, C));
        p.appendEdge(new TestEdge(C, D));
        validatePath(p, A, D, 4);

        p.removeEdge(new TestEdge(A, B));
        validatePath(p, B, D, 3);

        p.removeEdge(new TestEdge(C, C));
        validatePath(p, B, D, 2);

        p.removeEdge(new TestEdge(C, D));
        validatePath(p, B, C, 1);
    }

    @Test
    public void toImmutable() {
        MutablePath<TestVertex, TestEdge> p = new DefaultMutablePath<>();
        p.appendEdge(new TestEdge(A, B));
        p.appendEdge(new TestEdge(B, C));
        validatePath(p, A, C, 2);

        assertEquals("immutables should equal", p.toImmutable(), p.toImmutable());
        validatePath(p.toImmutable(), A, C, 2);
    }
}
