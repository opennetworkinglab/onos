/*
 * Copyright 2014-present Open Networking Foundation
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple concrete implementation of a directed graph path.
 */
public class DefaultMutablePath<V extends Vertex, E extends Edge<V>> implements MutablePath<V, E> {

    private final List<E> edges = new ArrayList<>();
    private Weight cost;

    /**
     * Creates a new empty path.
     */
    public DefaultMutablePath() {
    }

    /**
     * Creates a new path as a copy of another path.
     *
     * @param path path to be copied
     */
    public DefaultMutablePath(Path<V, E> path) {
        checkNotNull(path, "Path cannot be null");
        this.cost = path.cost();
        edges.addAll(path.edges());
    }

    @Override
    public V src() {
        return edges.isEmpty() ? null : edges.get(0).src();
    }

    @Override
    public V dst() {
        return edges.isEmpty() ? null : edges.get(edges.size() - 1).dst();
    }

    @Override
    public Weight cost() {
        return cost;
    }

    @Override
    public List<E> edges() {
        return ImmutableList.copyOf(edges);
    }

    @Override
    public void setCost(Weight cost) {
        this.cost = cost;
    }

    @Override
    public Path<V, E> toImmutable() {
        return new DefaultPath<>(edges, cost);
    }

    @Override
    public void insertEdge(E edge) {
        checkNotNull(edge, "Edge cannot be null");
        checkArgument(edges.isEmpty() || src().equals(edge.dst()),
                      "Edge destination must be the same as the current path source");
        edges.add(0, edge);
    }

    @Override
    public void appendEdge(E edge) {
        checkNotNull(edge, "Edge cannot be null");
        checkArgument(edges.isEmpty() || dst().equals(edge.src()),
                      "Edge source must be the same as the current path destination");
        edges.add(edge);
    }

    @Override
    public void removeEdge(E edge) {
        checkArgument(edge.src().equals(edge.dst()) ||
                              edges.indexOf(edge) == 0 ||
                              edges.lastIndexOf(edge) == edges.size() - 1,
                      "Edge must be at start or end of path, or it must be a cyclic edge");
        edges.remove(edge);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("src", src())
                .add("dst", dst())
                .add("cost", cost)
                .add("edges", edges)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(edges, cost);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultMutablePath) {
            final DefaultMutablePath other = (DefaultMutablePath) obj;
            return Objects.equals(this.cost, other.cost) &&
                    Objects.equals(this.edges, other.edges);
        }
        return false;
    }

}
