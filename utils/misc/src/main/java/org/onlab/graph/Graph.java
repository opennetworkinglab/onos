package org.onlab.graph;


import java.util.Set;

/**
 * Abstraction of a directed graph structure.
 *
 * @param <V> vertex type
 * @param <E> edge type
 */
public interface Graph<V extends Vertex, E extends Edge> {

    /**
     * Returns the set of vertexes comprising the graph.
     *
     * @return set of vertexes
     */
    Set<V> getVertexes();

    /**
     * Returns the set of edges comprising the graph.
     *
     * @return set of edges
     */
    Set<E> getEdges();

    /**
     * Returns all edges leading out from the specified source vertex.
     *
     * @param src source vertex
     * @return set of egress edges; empty if no such edges
     */
    Set<E> getEdgesFrom(V src);

    /**
     * Returns all edges leading towards the specified destination vertex.
     *
     * @param dst destination vertex
     * @return set of ingress vertexes; empty if no such edges
     */
    Set<E> getEdgesTo(V dst);

}
