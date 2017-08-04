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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Basis for various graph path search algorithm implementations.
 *
 * @param <V> vertex type
 * @param <E> edge type
 */
public abstract class AbstractGraphPathSearch<V extends Vertex, E extends Edge<V>>
        implements GraphPathSearch<V, E> {

    /**
     * Default path search result that uses the DefaultPath to convey paths
     * in a graph.
     */
    protected class DefaultResult implements Result<V, E> {

        private final V src;
        private final V dst;
        protected final Set<Path<V, E>> paths = new HashSet<>();
        protected final Map<V, Weight> costs = new HashMap<>();
        protected final Map<V, Set<E>> parents = new HashMap<>();
        protected final int maxPaths;

        /**
         * Creates the result of a single-path search.
         *
         * @param src path source
         * @param dst optional path destination
         */
        public DefaultResult(V src, V dst) {
            this(src, dst, 1);
        }

        /**
         * Creates the result of path search.
         *
         * @param src      path source
         * @param dst      optional path destination
         * @param maxPaths optional limit of number of paths;
         *                 {@link GraphPathSearch#ALL_PATHS} if no limit
         */
        public DefaultResult(V src, V dst, int maxPaths) {
            checkNotNull(src, "Source cannot be null");
            this.src = src;
            this.dst = dst;
            this.maxPaths = maxPaths;
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
        public Set<Path<V, E>> paths() {
            return paths;
        }

        @Override
        public Map<V, Weight> costs() {
            return costs;
        }

        @Override
        public Map<V, Set<E>> parents() {
            return parents;
        }

        /**
         * Indicates whether or not the given vertex has a cost yet.
         *
         * @param v vertex to test
         * @return true if the vertex has cost already
         */
        boolean hasCost(V v) {
            return costs.containsKey(v);
        }

        /**
         * Returns the current cost to reach the specified vertex.
         * If the vertex has not been accessed yet, it has no cost
         * associated and null will be returned.
         *
         * @param v vertex to reach
         * @return weight cost to reach the vertex if already accessed;
         *         null otherwise
         */
        Weight cost(V v) {
            return costs.get(v);
        }

        /**
         * Updates the cost of the vertex using its existing cost plus the
         * cost to traverse the specified edge. If the search is in single
         * path mode, only one path will be accrued.
         *
         * @param vertex  vertex to update
         * @param edge    edge through which vertex is reached
         * @param cost    current cost to reach the vertex from the source
         * @param replace true to indicate that any accrued edges are to be
         *                cleared; false to indicate that the edge should be
         *                added to the previously accrued edges as they yield
         *                the same cost
         */
        void updateVertex(V vertex, E edge, Weight cost, boolean replace) {
            costs.put(vertex, cost);
            if (edge != null) {
                Set<E> edges = parents.get(vertex);
                if (edges == null) {
                    edges = new HashSet<>();
                    parents.put(vertex, edges);
                }
                if (replace) {
                    edges.clear();
                }
                if (maxPaths == ALL_PATHS || edges.size() < maxPaths) {
                    edges.add(edge);
                }
            }
        }

        /**
         * Removes the set of parent edges for the specified vertex.
         *
         * @param v vertex
         */
        void removeVertex(V v) {
            parents.remove(v);
        }

        /**
         * If possible, relax the specified edge using the supplied base cost
         * and edge-weight function.
         *
         * @param edge            edge to be relaxed
         * @param cost            base cost to reach the edge destination vertex
         * @param ew              optional edge weight function
         * @param forbidNegatives if true negative values will forbid the link
         * @return true if the edge was relaxed; false otherwise
         */
        boolean relaxEdge(E edge, Weight cost, EdgeWeigher<V, E> ew,
                          boolean... forbidNegatives) {
            V v = edge.dst();

            Weight hopCost = ew.weight(edge);
            if ((!hopCost.isViable()) ||
                    (hopCost.isNegative() && forbidNegatives.length == 1 && forbidNegatives[0])) {
                return false;
            }
            Weight newCost = cost.merge(hopCost);

            int compareResult = -1;
            if (hasCost(v)) {
                Weight oldCost = cost(v);
                compareResult = newCost.compareTo(oldCost);
            }

            if (compareResult <= 0) {
                updateVertex(v, edge, newCost, compareResult < 0);
            }
            return compareResult < 0;
        }

        /**
         * Builds a set of paths for the specified src/dst vertex pair.
         */
        protected void buildPaths() {
            Set<V> destinations = new HashSet<>();
            if (dst == null) {
                destinations.addAll(costs.keySet());
            } else {
                destinations.add(dst);
            }

            // Build all paths between the source and all requested destinations.
            for (V v : destinations) {
                // Ignore the source, if it is among the destinations.
                if (!v.equals(src)) {
                    buildAllPaths(this, src, v, maxPaths);
                }
            }
        }

    }

    /**
     * Builds a set of all paths between the source and destination using the
     * graph search result by applying breadth-first search through the parent
     * edges and vertex costs.
     *
     * @param result   graph search result
     * @param src      source vertex
     * @param dst      destination vertex
     * @param maxPaths limit on the number of paths built;
     *                 {@link GraphPathSearch#ALL_PATHS} if no limit
     */
    private void buildAllPaths(DefaultResult result, V src, V dst, int maxPaths) {
        DefaultMutablePath<V, E> basePath = new DefaultMutablePath<>();
        basePath.setCost(result.cost(dst));

        Set<DefaultMutablePath<V, E>> pendingPaths = new HashSet<>();
        pendingPaths.add(basePath);

        while (!pendingPaths.isEmpty() &&
                (maxPaths == ALL_PATHS || result.paths.size() < maxPaths)) {
            Set<DefaultMutablePath<V, E>> frontier = new HashSet<>();

            for (DefaultMutablePath<V, E> path : pendingPaths) {
                // For each pending path, locate its first vertex since we
                // will be moving backwards from it.
                V firstVertex = firstVertex(path, dst);

                // If the first vertex is our expected source, we have reached
                // the beginning, so add the this path to the result paths.
                if (firstVertex.equals(src)) {
                    path.setCost(result.cost(dst));
                    result.paths.add(new DefaultPath<>(path.edges(), path.cost()));

                } else {
                    // If we have not reached the beginning, i.e. the source,
                    // fetch the set of edges leading to the first vertex of
                    // this pending path; if there are none, abandon processing
                    // this path for good.
                    Set<E> firstVertexParents = result.parents.get(firstVertex);
                    if (firstVertexParents == null || firstVertexParents.isEmpty()) {
                        break;
                    }

                    // Now iterate over all the edges and for each of them
                    // cloning the current path and then insert that edge to
                    // the path and then add that path to the pending ones.
                    // When processing the last edge, modify the current
                    // pending path rather than cloning a new one.
                    Iterator<E> edges = firstVertexParents.iterator();
                    while (edges.hasNext()) {
                        E edge = edges.next();
                        boolean isLast = !edges.hasNext();
                        // Exclude any looping paths
                        if (!isInPath(edge, path)) {
                            DefaultMutablePath<V, E> pendingPath = isLast ? path : new DefaultMutablePath<>(path);
                            pendingPath.insertEdge(edge);
                            frontier.add(pendingPath);
                        }
                    }
                }
            }

            // All pending paths have been scanned so promote the next frontier
            pendingPaths = frontier;
        }
    }

    /**
     * Indicates whether or not the specified edge source is already visited
     * in the specified path.
     *
     * @param edge edge to test
     * @param path path to test
     * @return true if the edge.src() is a vertex in the path already
     */
    private boolean isInPath(E edge, DefaultMutablePath<V, E> path) {
        return path.edges().stream().anyMatch(e -> edge.src().equals(e.dst()));
    }

    // Returns the first vertex of the specified path. This is either the source
    // of the first edge or, if there are no edges yet, the given destination.
    private V firstVertex(Path<V, E> path, V dst) {
        return path.edges().isEmpty() ? dst : path.edges().get(0).src();
    }

    /**
     * Checks the specified path search arguments for validity.
     *
     * @param graph graph; must not be null
     * @param src   source vertex; must not be null and belong to graph
     * @param dst   optional target vertex; must belong to graph
     */
    protected void checkArguments(Graph<V, E> graph, V src, V dst) {
        checkNotNull(graph, "Graph cannot be null");
        checkNotNull(src, "Source cannot be null");
        Set<V> vertices = graph.getVertexes();
        checkArgument(vertices.contains(src), "Source not in the graph");
        checkArgument(dst == null || vertices.contains(dst),
                      "Destination not in graph");
    }

    @Override
    public Result<V, E> search(Graph<V, E> graph, V src, V dst,
                               EdgeWeigher<V, E> weigher, int maxPaths) {
        checkArguments(graph, src, dst);

        return internalSearch(graph, src, dst,
                weigher != null ? weigher : new DefaultEdgeWeigher<>(),
                maxPaths);
    }

    protected abstract Result<V, E> internalSearch(Graph<V, E> graph, V src, V dst,
                                          EdgeWeigher<V, E> weigher, int maxPaths);
}
