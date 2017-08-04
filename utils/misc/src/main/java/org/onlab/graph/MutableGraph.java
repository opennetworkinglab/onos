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
