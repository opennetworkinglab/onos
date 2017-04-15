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

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple concrete implementation of a directed graph path.
 */
public class DefaultPath<V extends Vertex, E extends Edge<V>> implements Path<V, E> {

    private final V src;
    private final V dst;
    private final List<E> edges;
    private Weight cost;

    /**
     * Creates a new path from the specified list of edges and cost.
     *
     * @param edges list of path edges
     * @param cost  path cost as a weight object
     */
    public DefaultPath(List<E> edges, Weight cost) {
        checkNotNull(edges, "Edges list must not be null");
        checkArgument(!edges.isEmpty(), "There must be at least one edge");
        this.edges = ImmutableList.copyOf(edges);
        this.src = edges.get(0).src();
        this.dst = edges.get(edges.size() - 1).dst();
        this.cost = cost;
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
    public Weight cost() {
        return cost;
    }

    @Override
    public List<E> edges() {
        return edges;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("src", src)
                .add("dst", dst)
                .add("cost", cost)
                .add("edges", edges)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, edges, cost);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPath) {
            final DefaultPath other = (DefaultPath) obj;
            return Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst) &&
                    Objects.equals(this.cost, other.cost) &&
                    Objects.equals(this.edges, other.edges);
        }
        return false;
    }

}
