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
 * Abstraction of a graph edge weight function.
 */
public interface EdgeWeigher<V extends Vertex, E extends Edge<V>> {

    /**
     * Returns the weight of the given edge.
     *
     * @param edge edge to be weighed
     * @return edge weight
     */
    Weight weight(E edge);

    /**
     * Returns initial weight value (i.e. weight of a "path" starting and
     * terminating in the same vertex; typically 0 value is used).
     *
     * @return null path weight
     */
    Weight getInitialWeight();

    /**
     * Returns weight of a link/path that should be skipped
     * (can be considered as an infinite weight).
     *
     * @return non viable weight
     */
    Weight getNonViableWeight();
}
