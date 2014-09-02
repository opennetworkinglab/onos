package org.onlab.graph;

/**
 * Abstraction of a mutable graph that can be constructed gradually.
 */
public interface MutableGraph<V extends Vertex, E extends Edge> extends Graph<V, E> {

    /**
     * Adds the specified vertex to this graph.
     *
     * @param vertex new vertex
     */
    void addVertex(V vertex);

    /**
     * Removes the specified vertex from the graph.
     *
     * @param vertex vertex to be removed
     */
    void removeVertex(V vertex);

    /**
     * Adds the specified edge to this graph. If the edge vertexes are not
     * already in the graph, they will be added as well.
     *
     * @param edge new edge
     */
    void addEdge(E edge);

    /**
     * Removes the specified edge from the graph.
     *
     * @param edge edge to be removed
     */
    void removeEdge(E edge);

    /**
     * Returns an immutable copy of this graph.
     *
     * @return immutable copy
     */
    Graph<V, E> toImmutable();

}
