package org.onlab.graph;

/**
 * Representation of a graph edge.
 *
 * @param <V> vertex type
 */
public interface Edge<V extends Vertex> {

    /**
     * Returns the edge source vertex.
     *
     * @return source vertex
     */
    V src();

    /**
     * Returns the edge destination vertex.
     *
     * @return destination vertex
     */
    V dst();

}
