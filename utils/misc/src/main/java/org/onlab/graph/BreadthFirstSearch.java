package org.onlab.graph;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the BFS algorithm.
 */
public class BreadthFirstSearch<V extends Vertex, E extends Edge<V>>
        extends AbstractGraphPathSearch<V, E> {

    @Override
    public Result<V, E> search(Graph<V, E> graph, V src, V dst, EdgeWeight<V, E> ew) {
        checkArguments(graph, src, dst);

        // Prepare the graph result.
        DefaultResult result = new DefaultResult(src, dst);

        // Setup the starting frontier with the source as the sole vertex.
        Set<V> frontier = new HashSet<>();
        result.costs.put(src, 0.0);
        frontier.add(src);

        search:
        while (!frontier.isEmpty()) {
            // Prepare the next frontier.
            Set<V> next = new HashSet<>();

            // Visit all vertexes in the current frontier.
            for (V vertex : frontier) {
                double cost = result.cost(vertex);

                // Visit all egress edges of the current frontier vertex.
                for (E edge : graph.getEdgesFrom(vertex)) {
                    V nextVertex = edge.dst();
                    if (!result.hasCost(nextVertex)) {
                        // If this vertex has not been visited yet, update it.
                        result.updateVertex(nextVertex, edge,
                                            cost + (ew == null ? 1.0 : ew.weight(edge)),
                                            true);
                        // If we have reached our intended destination, bail.
                        if (nextVertex.equals(dst)) {
                            break search;
                        }
                        next.add(nextVertex);
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
