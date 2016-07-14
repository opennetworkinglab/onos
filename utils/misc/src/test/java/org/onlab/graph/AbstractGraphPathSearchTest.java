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

import org.junit.Test;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertEquals;

/**
 * Base for all graph search tests.
 */
public abstract class AbstractGraphPathSearchTest extends GraphTest {

    /**
     * Creates a test-specific graph search to exercise.
     *
     * @return graph search
     */
    protected abstract AbstractGraphPathSearch<TestVertex, TestEdge> graphSearch();

    @Test(expected = IllegalArgumentException.class)
    public void noSuchSourceArgument() {
        graphSearch().search(new AdjacencyListsGraph<>(of(B, C),
                                                       of(new TestEdge(B, C, 1))),
                             A, H, weight, 1);
    }

    @Test(expected = NullPointerException.class)
    public void nullGraphArgument() {
        graphSearch().search(null, A, H, weight, 1);
    }

    @Test(expected = NullPointerException.class)
    public void nullSourceArgument() {
        graphSearch().search(new AdjacencyListsGraph<>(of(B, C),
                                                       of(new TestEdge(B, C, 1))),
                             null, H, weight, 1);
    }

    @Test
    public void samenessThreshold() {
        AbstractGraphPathSearch<TestVertex, TestEdge> search = graphSearch();
        search.setSamenessThreshold(0.3);
        assertEquals("incorrect threshold", 0.3, search.samenessThreshold(), 0.01);
    }

}
