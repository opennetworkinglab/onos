/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.flowanalyzer;

import org.onlab.graph.MutableAdjacencyListsGraph;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyVertex;

import java.util.Set;

/**
 * Default implementation of an immutable topology graph based on a generic
 * implementation of adjacency lists graph.
 */
public class DefaultMutableTopologyGraph
        extends MutableAdjacencyListsGraph<TopologyVertex, TopologyEdge>
        implements TopologyGraph {

    /**
     * Creates a topology graph comprising of the specified vertexes and edges.
     *
     * @param vertexes set of graph vertexes
     * @param edges    set of graph edges
     */
    public DefaultMutableTopologyGraph(Set<TopologyVertex> vertexes, Set<TopologyEdge> edges) {
        super(vertexes, edges);
    }

}
