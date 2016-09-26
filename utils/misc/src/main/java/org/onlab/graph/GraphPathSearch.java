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

import java.util.Map;
import java.util.Set;

/**
 * Representation of a graph path search algorithm.
 *
 * @param <V> vertex type
 * @param <E> edge type
 */
public interface GraphPathSearch<V extends Vertex, E extends Edge<V>> {

    static int ALL_PATHS = -1;

    /**
     * Abstraction of a path search result.
     */
    interface Result<V extends Vertex, E extends Edge<V>> {

        /**
         * Returns the search source.
         *
         * @return search source
         */
        V src();

        /**
         * Returns the search destination, if was was given.
         *
         * @return optional search destination
         */
        V dst();

        /**
         * Returns the set of paths produced as a result of the graph search.
         *
         * @return set of paths
         */
        Set<Path<V, E>> paths();

        /**
         * Returns bindings of each vertex to its parent edges in the path.
         *
         * @return map of vertex to its parent edge bindings
         */
        Map<V, Set<E>> parents();

        /**
         * Return a bindings of each vertex to its cost in the path.
         *
         * @return map of vertex to path cost bindings
         */
        Map<V, Weight> costs();
    }

    /**
     * Searches the specified graph for paths between vertices.
     *
     * @param graph    graph to be searched
     * @param src      optional source vertex
     * @param dst      optional destination vertex; if null paths to all vertex
     *                 destinations will be searched
     * @param weigher  optional edge-weigher; if null, {@link DefaultEdgeWeigher}
     *                 will be used (assigns equal weights to all links)
     * @param maxPaths limit on number of paths; {@link GraphPathSearch#ALL_PATHS} if no limit
     * @return search results
     */
    Result<V, E> search(Graph<V, E> graph, V src, V dst,
                        EdgeWeigher<V, E> weigher, int maxPaths);

}
