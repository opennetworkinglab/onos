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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable graph implemented using adjacency lists.
 *
 * @param <V> vertex type
 * @param <E> edge type
 */
public class AdjacencyListsGraph<V extends Vertex, E extends Edge<V>>
        implements Graph<V, E> {

    private final Set<V> vertexes;
    private final Set<E> edges;

    private final ImmutableSetMultimap<V, E> sources;
    private final ImmutableSetMultimap<V, E> destinations;

    /**
     * Creates a graph comprising of the specified vertexes and edges.
     *
     * @param vertexes set of graph vertexes
     * @param edges    set of graph edges
     */
    public AdjacencyListsGraph(Set<V> vertexes, Set<E> edges) {
        checkNotNull(vertexes, "Vertex set cannot be null");
        checkNotNull(edges, "Edge set cannot be null");

        // Record ingress/egress edges for each vertex.
        ImmutableSetMultimap.Builder<V, E> srcMap = ImmutableSetMultimap.builder();
        ImmutableSetMultimap.Builder<V, E> dstMap = ImmutableSetMultimap.builder();

        // Also make sure that all edge end-points are added as vertexes
        ImmutableSet.Builder<V> actualVertexes = ImmutableSet.builder();
        actualVertexes.addAll(vertexes);

        for (E edge : edges) {
            srcMap.put(edge.src(), edge);
            actualVertexes.add(edge.src());
            dstMap.put(edge.dst(), edge);
            actualVertexes.add(edge.dst());
        }

        // Make an immutable copy of the edge and vertex sets
        this.edges = ImmutableSet.copyOf(edges);
        this.vertexes = actualVertexes.build();

        // Build immutable copies of sources and destinations edge maps
        sources = srcMap.build();
        destinations = dstMap.build();
    }

    @Override
    public Set<V> getVertexes() {
        return vertexes;
    }

    @Override
    public Set<E> getEdges() {
        return edges;
    }

    @Override
    public Set<E> getEdgesFrom(V src) {
        return sources.get(src);
    }

    @Override
    public Set<E> getEdgesTo(V dst) {
        return destinations.get(dst);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AdjacencyListsGraph) {
            AdjacencyListsGraph that = (AdjacencyListsGraph) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.vertexes, that.vertexes) &&
                    Objects.equals(this.edges, that.edges);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertexes, edges);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("vertexes", vertexes)
                .add("edges", edges)
                .toString();
    }
}
