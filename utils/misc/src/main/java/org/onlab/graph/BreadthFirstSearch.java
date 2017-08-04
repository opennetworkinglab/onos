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

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the BFS algorithm.
 */
public class BreadthFirstSearch<V extends Vertex, E extends Edge<V>>
        extends AbstractGraphPathSearch<V, E> {

    @Override
    protected Result<V, E> internalSearch(Graph<V, E> graph, V src, V dst,
                               EdgeWeigher<V, E> weigher, int maxPaths) {

        // Prepare the graph result.
        DefaultResult result = new DefaultResult(src, dst, maxPaths);

        // Setup the starting frontier with the source as the sole vertex.
        Set<V> frontier = new HashSet<>();
        result.updateVertex(src, null, weigher.getInitialWeight(), true);
        frontier.add(src);

        boolean reachedEnd = false;
        while (!reachedEnd && !frontier.isEmpty()) {
            // Prepare the next frontier.
            Set<V> next = new HashSet<>();

            // Visit all vertexes in the current frontier.
            for (V vertex : frontier) {
                Weight cost = result.cost(vertex);

                // Visit all egress edges of the current frontier vertex.
                for (E edge : graph.getEdgesFrom(vertex)) {
                    V nextVertex = edge.dst();
                    if (!result.hasCost(nextVertex)) {
                        // If this vertex has not been visited yet, update it.
                        Weight newCost = cost.merge(weigher.weight(edge));
                        result.updateVertex(nextVertex, edge, newCost, true);
                        // If we have reached our intended destination, bail.
                        if (nextVertex.equals(dst)) {
                            reachedEnd = true;
                            break;
                        }
                        next.add(nextVertex);
                    }

                    if (reachedEnd) {
                        break;
                    }
                }
            }

            // Promote the next frontier.
            frontier = next;
        }

        // Finally, but the paths on the search result and return.
        result.buildPaths();
        return result;
    }

}
