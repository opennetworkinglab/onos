package org.onlab.graph;

/**
 * Abstraction of a mutable path that allows gradual construction.
 */
public interface MutablePath<V extends Vertex, E extends Edge<V>> extends Path<V, E> {

    /**
     * Inserts a new edge at the beginning of this path. The edge must be
     * adjacent to the prior start of the path.
     *
     * @param edge edge to be inserted
     */
    void insertEdge(E edge);

    /**
     * Appends a new edge at the end of the this path. The edge must be
     * adjacent to the prior end of the path.
     *
     * @param edge edge to be inserted
     */
    void appendEdge(E edge);

    /**
     * Removes the specified edge. This edge must be either at the start or
     * at the end of the path, or it must be a cyclic edge in order not to
     * violate the contiguous path property.
     *
     * @param edge edge to be removed
     */
    void removeEdge(E edge);

    /**
     * Sets the total path cost as a unit-less double.
     *
     * @param cost new path cost
     */
    void setCost(double cost);

    /**
     * Returns an immutable copy of this path.
     *
     * @return immutable copy
     */
    Path<V, E> toImmutable();

}
