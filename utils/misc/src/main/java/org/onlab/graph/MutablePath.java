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
     * Sets the total path cost as a weight object.
     *
     * @param cost new path cost
     */
    void setCost(Weight cost);

    /**
     * Returns an immutable copy of this path.
     *
     * @return immutable copy
     */
    Path<V, E> toImmutable();

}
