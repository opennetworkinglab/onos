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
     * Returns the total cost of the path as a weight object.
     *
     * @return path cost as a weight object
     */
    Weight cost();

}
