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

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Test edge.
 */
public class TestEdge extends AbstractEdge<TestVertex> {

    private final double weight;

    /**
     * Creates a new edge between the specified source and destination vertexes.
     *
     * @param src    source vertex
     * @param dst    destination vertex
     * @param weight edge weight
     */
    public TestEdge(TestVertex src, TestVertex dst, double weight) {
        super(src, dst);
        this.weight = weight;
    }

    /**
     * Returns the edge weight.
     *
     * @return edge weight
     */
    public double weight() {
        return weight;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(weight);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TestEdge) {
            final TestEdge other = (TestEdge) obj;
            return super.equals(obj) && Objects.equals(this.weight, other.weight);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("src", src()).add("dst", dst()).
                add("weight", weight).toString();
    }

}
