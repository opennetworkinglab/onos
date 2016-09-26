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

import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.assertEquals;

/**
 * Test of the default path.
 */
public class DefaultPathTest extends GraphTest {

    @Test
    public void equality() {
        List<TestEdge> edges = of(new TestEdge(A, B), new TestEdge(B, C));
        new EqualsTester().addEqualityGroup(new DefaultPath<>(edges, new TestDoubleWeight(2.0)),
                                            new DefaultPath<>(edges, new TestDoubleWeight(2.0)))
                .addEqualityGroup(new DefaultPath<>(edges, new TestDoubleWeight(3.0)))
                .testEquals();
    }

    @Test
    public void basics() {
        Path<TestVertex, TestEdge> p = new DefaultPath<>(of(new TestEdge(A, B),
                                                            new TestEdge(B, C)),
                new TestDoubleWeight(2.0));
        validatePath(p, A, C, 2, new TestDoubleWeight(2.0));
    }

    // Validates the path against expected attributes
    protected void validatePath(Path<TestVertex, TestEdge> p,
                                TestVertex src, TestVertex dst,
                                int length, Weight cost) {
        assertEquals("incorrect path length", length, p.edges().size());
        assertEquals("incorrect source", src, p.src());
        assertEquals("incorrect destination", dst, p.dst());
        assertEquals("incorrect path cost", cost, p.cost());
    }

}
