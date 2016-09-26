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

/**
 * Representation of a graph search algorithm and its outcome.
 *
 * @param <V>    vertex type
 * @param <E>    edge type
 */
public interface GraphSearch<V extends Vertex, E extends Edge<V>> {

    /**
     * Notion of a graph search result.
     */
    interface Result<V extends Vertex, E extends Edge<V>> {
    }

    /**
     * Searches the specified graph.
     *
     * @param graph   graph to be searched
     * @param weigher optional edge-weigher; if null, {@link DefaultEdgeWeigher}
     *                will be used (assigns equal weights to all links)
     *
     * @return search results
     */
    Result search(Graph<V, E> graph, EdgeWeigher<V, E> weigher);

}
