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

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * DFS graph search algorithm implemented via iteration rather than recursion.
 */
public class DepthFirstSearch<V extends Vertex, E extends Edge<V>>
        extends AbstractGraphPathSearch<V, E> {

    /**
     * Graph edge types as classified by the DFS algorithm.
     */
    public static enum EdgeType {
        TREE_EDGE, FORWARD_EDGE, BACK_EDGE, CROSS_EDGE
    }

    @Override
    protected SpanningTreeResult internalSearch(Graph<V, E> graph, V src, V dst,
                                             EdgeWeigher<V, E> weigher, int maxPaths) {

        // Prepare the search result.
        SpanningTreeResult result = new SpanningTreeResult(src, dst, maxPaths);

        // The source vertex has cost 0, of course.
        result.updateVertex(src, null, weigher.getInitialWeight(), true);

        // Track finished vertexes and keep a stack of vertexes that have been
        // started; start this stack with the source on it.
        Set<V> finished = new HashSet<>();
        Deque<V> stack = new LinkedList<>();
        stack.push(src);

        while (!stack.isEmpty()) {
            V vertex = stack.peek();
            if (vertex.equals(dst)) {
                // If we have reached our destination, bail.
                break;
            }

            Weight cost = result.cost(vertex);
            boolean tangent = false;

            // Visit all egress edges of the current vertex.
            for (E edge : graph.getEdgesFrom(vertex)) {
                // If we have seen the edge already, skip it.
                if (result.isEdgeMarked(edge)) {
                    continue;
                }

                // Examine the destination of the current edge.
                V nextVertex = edge.dst();
                if (!result.hasCost(nextVertex)) {
                    // If this vertex have not finished this vertex yet,
                    // not started it, then start it as a tree-edge.
                    result.markEdge(edge, EdgeType.TREE_EDGE);
                    Weight newCost = cost.merge(weigher.weight(edge));
                    result.updateVertex(nextVertex, edge, newCost, true);
                    stack.push(nextVertex);
                    tangent = true;
                    break;

                } else if (!finished.contains(nextVertex)) {
                    // We started the vertex, but did not yet finish it, so
                    // it must be a back-edge.
                    result.markEdge(edge, EdgeType.BACK_EDGE);
                } else {
                    // The target has been finished already, so what we have
                    // here is either a forward-edge or a cross-edge.
                    result.markEdge(edge, isForwardEdge(result, edge) ?
                            EdgeType.FORWARD_EDGE : EdgeType.CROSS_EDGE);
                }
            }

            // If we have not been sent on a tangent search and reached the
            // end of the current scan normally, mark the node as finished
            // and pop it off the vertex stack.
            if (!tangent) {
                finished.add(vertex);
                stack.pop();
            }
        }

        // Finally, but the paths on the search result and return.
        result.buildPaths();
        return result;
    }

    /**
     * Determines whether the specified edge is a forward edge using the
     * accumulated set of parent edges for each vertex.
     *
     * @param result search result
     * @param edge   edge to be classified
     * @return true if the edge is a forward edge
     */
    protected boolean isForwardEdge(DefaultResult result, E edge) {
        // Follow the parent edges until we hit the edge source vertex
        V target = edge.src();
        V vertex = edge.dst();
        Set<E> parentEdges;
        while ((parentEdges = result.parents.get(vertex)) != null) {
            for (E parentEdge : parentEdges) {
                vertex = parentEdge.src();
                if (vertex.equals(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Graph search result which includes edge classification for building
     * a spanning tree.
     */
    public class SpanningTreeResult extends DefaultResult {

        protected final Map<E, EdgeType> edges = new HashMap<>();

        /**
         * Creates a new spanning tree result.
         *
         * @param src      search source
         * @param dst      optional search destination
         * @param maxPaths limit on the number of paths
         */
        public SpanningTreeResult(V src, V dst, int maxPaths) {
            super(src, dst, maxPaths);
        }

        /**
         * Returns the map of edge type.
         *
         * @return edge to edge type bindings
         */
        public Map<E, EdgeType> edges() {
            return edges;
        }

        /**
         * Indicates whether or not the edge has been marked with type.
         *
         * @param edge edge to test
         * @return true if the edge has been marked already
         */
        boolean isEdgeMarked(E edge) {
            return edges.containsKey(edge);
        }

        /**
         * Marks the edge with the specified type.
         *
         * @param edge edge to mark
         * @param type edge type
         */
        void markEdge(E edge, EdgeType type) {
            edges.put(edge, type);
        }

    }

}
