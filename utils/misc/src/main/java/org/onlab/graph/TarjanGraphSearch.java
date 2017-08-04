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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tarjan algorithm for searching a graph and producing results describing
 * the graph SCC (strongly-connected components).
 */
public class TarjanGraphSearch<V extends Vertex, E extends Edge<V>>
        implements GraphSearch<V, E> {

    /**
     * {@inheritDoc}
     * <p>
     * This implementation produces results augmented with information on
     * SCCs within the graph.
     * </p>
     * <p>
     * To prevent traversal of an edge, the {@link EdgeWeigher#weight} should
     * return a negative value as an edge weigher.
     * </p>
     */
    @Override
    public SccResult<V, E> search(Graph<V, E> graph, EdgeWeigher<V, E> weigher) {
        SccResult<V, E> result = new SccResult<>(graph);
        for (V vertex : graph.getVertexes()) {
            VertexData data = result.data(vertex);
            if (data == null) {
                connect(graph, vertex, weigher, result);
            }
        }
        return result.build();
    }

    /**
     * Scans the specified graph, using recursion, and produces SCC results.
     *
     * @param graph   graph to search
     * @param vertex  current vertex to scan and connect
     * @param weigher optional edge weigher
     * @param result  graph search result
     * @return augmentation vertexData for the current vertex
     */
    private VertexData<V> connect(Graph<V, E> graph, V vertex,
                                  EdgeWeigher<V, E> weigher,
                                  SccResult<V, E> result) {
        VertexData<V> data = result.addData(vertex);

        // Scan through all egress edges of the current vertex.
        for (E edge : graph.getEdgesFrom(vertex)) {
            V nextVertex = edge.dst();

            // If edge is not viable, skip it.
            if (weigher != null && !weigher.weight(edge).isViable()) {
                continue;
            }

            // Attempt to get the augmentation vertexData for the next vertex.
            VertexData<V> nextData = result.data(nextVertex);
            if (nextData == null) {
                // Next vertex has not been visited yet, so do this now.
                nextData = connect(graph, nextVertex, weigher, result);
                data.lowLink = Math.min(data.lowLink, nextData.lowLink);

            } else if (result.visited(nextData)) {
                // Next vertex has been visited, which means it is in the
                // same cluster as the current vertex.
                data.lowLink = Math.min(data.lowLink, nextData.index);
            }
        }

        if (data.lowLink == data.index) {
            result.addCluster(data);
        }
        return data;
    }

    /**
     * Graph search result augmented with SCC vertexData.
     */
    public static final class SccResult<V extends Vertex, E extends Edge<V>>
            implements Result {

        private final Graph<V, E> graph;
        private List<Set<V>> clusterVertexes = new ArrayList<>();
        private List<Set<E>> clusterEdges = new ArrayList<>();

        private int index = 0;
        private final Map<V, VertexData<V>> vertexData = new HashMap<>();
        private final List<VertexData<V>> visited = new ArrayList<>();

        private SccResult(Graph<V, E> graph) {
            this.graph = graph;
        }

        /**
         * Returns the number of SCC clusters in the graph.
         *
         * @return number of clusters
         */
        public int clusterCount() {
            return clusterEdges.size();
        }

        /**
         * Returns the list of strongly connected vertex clusters.
         *
         * @return list of strongly connected vertex sets
         */
        public List<Set<V>> clusterVertexes() {
            return clusterVertexes;
        }

        /**
         * Returns the list of edges linking strongly connected vertex clusters.
         *
         * @return list of strongly connected edge sets
         */
        public List<Set<E>> clusterEdges() {
            return clusterEdges;
        }

        // Gets the augmentation vertexData for the specified vertex
        private VertexData<V> data(V vertex) {
            return vertexData.get(vertex);
        }

        // Adds augmentation vertexData for the specified vertex
        private VertexData<V> addData(V vertex) {
            VertexData<V> d = new VertexData<>(vertex, index);
            vertexData.put(vertex, d);
            visited.add(0, d);
            index++;
            return d;
        }

        // Indicates whether the given vertex has been visited
        private boolean visited(VertexData data) {
            return visited.contains(data);
        }

        // Adds a new cluster for the specified vertex
        private void addCluster(VertexData data) {
            Set<V> vertexes = findClusterVertices(data);
            clusterVertexes.add(vertexes);
            clusterEdges.add(findClusterEdges(vertexes));
        }

        private Set<V> findClusterVertices(VertexData data) {
            VertexData<V> nextVertexData;
            Set<V> vertexes = new HashSet<>();
            do {
                nextVertexData = visited.remove(0);
                vertexes.add(nextVertexData.vertex);
            } while (data != nextVertexData);
            return Collections.unmodifiableSet(vertexes);
        }

        private Set<E> findClusterEdges(Set<V> vertexes) {
            Set<E> edges = new HashSet<>();
            for (V vertex : vertexes) {
                for (E edge : graph.getEdgesFrom(vertex)) {
                    if (vertexes.contains((edge.dst()))) {
                        edges.add(edge);
                    }
                }
            }
            return Collections.unmodifiableSet(edges);
        }

        public SccResult<V, E> build() {
            clusterVertexes = Collections.unmodifiableList(clusterVertexes);
            clusterEdges = Collections.unmodifiableList(clusterEdges);
            return this;
        }
    }

    // Augments the vertex to assist in determining SCC clusters.
    private static final class VertexData<V extends Vertex> {
        final V vertex;
        int index;
        int lowLink;

        private VertexData(V vertex, int index) {
            this.vertex = vertex;
            this.index = index;
            this.lowLink = index;
        }
    }

}
