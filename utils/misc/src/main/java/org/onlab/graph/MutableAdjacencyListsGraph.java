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

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class MutableAdjacencyListsGraph<V extends Vertex, E extends Edge<V>>
        implements MutableGraph<V, E> {
    private Set<V> vertexes = new HashSet<>();
    private Set<E> edges = new HashSet<>();

    private SetMultimap<V, E> sources = HashMultimap.create();
    private SetMultimap<V, E> destinations = HashMultimap.create();

    /**
     * Creates a graph comprising of the specified vertexes and edges.
     *
     * @param vertex set of graph vertexes
     * @param edge   set of graph edges
     */
    public MutableAdjacencyListsGraph(Set<V> vertex, Set<E> edge) {
        vertexes.addAll(vertex);
        edges.addAll(edge);
        for (E e : edge) {
            sources.put(e.src(), e);
            vertexes.add(e.src());
            destinations.put(e.dst(), e);
            vertexes.add(e.dst());
        }
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
        if (obj instanceof MutableAdjacencyListsGraph) {
            MutableAdjacencyListsGraph that = (MutableAdjacencyListsGraph) obj;
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


    @Override
    public void addVertex(V vertex) {
        vertexes.add(vertex);
    }

    @Override
    public void removeVertex(V vertex) {
        if (vertexes.remove(vertex)) {
            Set<E> srcEdgesList = sources.get(vertex);
            Set<E> dstEdgesList = destinations.get(vertex);
            edges.removeAll(srcEdgesList);
            edges.removeAll(dstEdgesList);
            sources.remove(vertex, srcEdgesList);
            sources.remove(vertex, dstEdgesList);
        }
    }

    @Override
    public void addEdge(E edge) {
        if (edges.add(edge)) {
            sources.put(edge.src(), edge);
            destinations.put(edge.dst(), edge);
        }
    }

    @Override
    public void removeEdge(E edge) {
        if (edges.remove(edge)) {
            sources.remove(edge.src(), edge);
            destinations.remove(edge.dst(), edge);
        }
    }

    @Override
    public Graph<V, E> toImmutable() {
        return new AdjacencyListsGraph<>(vertexes, edges);
    }

    /**
     * Clear the graph.
     */
    public void clear() {
        edges.clear();
        vertexes.clear();
        sources.clear();
        destinations.clear();
    }
}
