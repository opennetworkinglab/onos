/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.net.topology;

import org.onlab.graph.GraphPathSearch;

/**
 * Provides administrative abilities to tailor the path service behaviours.
 */
public interface PathAdminService {

    /**
     * Sets the specified link-weight function to be used as a default.
     * If null is specified, the builtin default hop-count link-weight will be
     * used.
     *
     * @param linkWeigher link-weight function to be used as default
     */
    void setDefaultLinkWeigher(LinkWeigher linkWeigher);

    /**
     * Sets the specified graph path search algorightm to be used as a default.
     * If null is specified, the builtin default all-shortest-paths Dijkstra
     * algorithm will be used.
     *
     * @param graphPathSearch default graph path search algorithm
     */
    void setDefaultGraphPathSearch(GraphPathSearch<TopologyVertex, TopologyEdge> graphPathSearch);

    /**
     * Sets the default maximum path count to be used when computing paths. If
     * -1 is specified, the builtin default <code>ALL_PATHS</code>, signifying
     * that all available paths should be returned is used.
     *
     * @param maxPaths new default max path count
     */
    default void setDefaultMaxPaths(int maxPaths) {};

}
