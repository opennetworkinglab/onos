package org.onlab.graph;

/**
 * Abstraction of a graph edge weight function.
 */
public interface EdgeWeight<V extends Vertex, E extends Edge<V>> {

    /**
     * Returns the weight of the given edge as a unit-less number.
     *
     * @param edge edge to be weighed
     * @return edge weight as a unit-less number
     */
    double weight(E edge);

}
