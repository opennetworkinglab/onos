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
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract graph edge implementation.
 */
public abstract class AbstractEdge<V extends Vertex> implements Edge<V> {

    private final V src;
    private final V dst;

    /**
     * Creates a new edge between the specified source and destination vertexes.
     *
     * @param src source vertex
     * @param dst destination vertex
     */
    public AbstractEdge(V src, V dst) {
        this.src = checkNotNull(src, "Source vertex cannot be null");
        this.dst = checkNotNull(dst, "Destination vertex cannot be null");
    }

    @Override
    public V src() {
        return src;
    }

    @Override
    public V dst() {
        return dst;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AbstractEdge) {
            final AbstractEdge other = (AbstractEdge) obj;
            return Objects.equals(this.src, other.src) && Objects.equals(this.dst, other.dst);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("src", src)
                .add("dst", dst)
                .toString();
    }
}
