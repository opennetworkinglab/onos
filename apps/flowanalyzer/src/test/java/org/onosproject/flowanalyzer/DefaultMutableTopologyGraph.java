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
