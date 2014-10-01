package org.onlab.onos.store.trivial.impl;

import org.onlab.graph.AdjacencyListsGraph;
import org.onlab.onos.net.topology.TopologyEdge;
import org.onlab.onos.net.topology.TopologyGraph;
import org.onlab.onos.net.topology.TopologyVertex;

import java.util.Set;

/**
 * Default implementation of an immutable topology graph based on a generic
 * implementation of adjacency lists graph.
 */
public class DefaultTopologyGraph
        extends AdjacencyListsGraph<TopologyVertex, TopologyEdge>
        implements TopologyGraph {

    /**
     * Creates a topology graph comprising of the specified vertexes and edges.
     *
     * @param vertexes set of graph vertexes
     * @param edges    set of graph edges
     */
    public DefaultTopologyGraph(Set<TopologyVertex> vertexes, Set<TopologyEdge> edges) {
        super(vertexes, edges);
    }

}
