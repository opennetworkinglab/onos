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
import java.util.Comparator;
import java.util.Set;

/**
 * Dijkstra shortest-path graph search algorithm capable of finding not just
 * one, but all shortest paths between the source and destinations.
 */
public class DijkstraGraphSearch<V extends Vertex, E extends Edge<V>>
        extends AbstractGraphPathSearch<V, E> {

    @Override
    protected Result<V, E> internalSearch(Graph<V, E> graph, V src, V dst,
                               EdgeWeigher<V, E> weigher, int maxPaths) {

        // Use the default result to remember cumulative costs and parent
        // edges to each each respective vertex.
        DefaultResult result = new DefaultResult(src, dst, maxPaths);

        // Cost to reach the source vertex is 0 of course.
        result.updateVertex(src, null, weigher.getInitialWeight(), false);

        if (graph.getEdges().isEmpty()) {
            result.buildPaths();
            return result;
        }

        // Use the min priority queue to progressively find each nearest
        // vertex until we reach the desired destination, if one was given,
        // or until we reach all possible destinations.
        Heap<V> minQueue = createMinQueue(graph.getVertexes(),
                                          new PathCostComparator(result));
        while (!minQueue.isEmpty()) {
            // Get the nearest vertex
            V nearest = minQueue.extractExtreme();
            if (nearest.equals(dst)) {
                break;
            }

            // Find its cost and use it to determine if the vertex is reachable.
            if (result.hasCost(nearest)) {
                Weight cost = result.cost(nearest);

                // If the vertex is reachable, relax all its egress edges.
                for (E e : graph.getEdgesFrom(nearest)) {
                    result.relaxEdge(e, cost, weigher, true);
                }
            }

            // Re-prioritize the min queue.
            minQueue.heapify();
        }

        // Now construct a set of paths from the results.
        result.buildPaths();
        return result;
    }

    // Compares path weights using their accrued costs; used for sorting the
    // min priority queue.
    private final class PathCostComparator implements Comparator<V> {
        private final DefaultResult result;

        private PathCostComparator(DefaultResult result) {
            this.result = result;
        }

        @Override
        public int compare(V v1, V v2) {
            //not accessed vertices should be pushed to the back of the queue
            if (!result.hasCost(v1) && !result.hasCost(v2)) {
                return 0;
            } else if (!result.hasCost(v1)) {
                return -1;
            } else if (!result.hasCost(v2)) {
                return 1;
            }

            return result.cost(v2).compareTo(result.cost(v1));
        }
    }

    // Creates a min priority queue from the specified vertexes and comparator.
    private Heap<V> createMinQueue(Set<V> vertexes, Comparator<V> comparator) {
        return new Heap<>(new ArrayList<>(vertexes), comparator);
    }

}
