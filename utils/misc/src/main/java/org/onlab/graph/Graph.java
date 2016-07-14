/*
 * Copyright 2014-present Open Networking Laboratory
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
