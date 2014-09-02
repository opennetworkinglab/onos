package org.onlab.graph;

import java.util.List;

/**
 * Representation of a path in a graph as a sequence of edges. Paths are
 * assumed to be continuous, where adjacent edges must share a vertex.
 *
 * @param <V> vertex type
 * @param <E> edge type
 */
public interface Path<V extends Vertex, E extends Edge<V>> extends Edge<V> {

    /**
     * Returns the list of edges comprising the path. Adjacent edges will
     * share the same vertex, meaning that a source of one edge, will be the
     * same as the destination of the prior edge.
     *
     * @return list of path edges
     */
    List<E> edges();

    /**
     * Returns the total cost of the path as a unit-less number.
     *
     * @return path cost as a unit-less number
     */
    double cost();

}
