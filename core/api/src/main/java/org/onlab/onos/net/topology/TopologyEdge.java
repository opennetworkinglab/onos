package org.onlab.onos.net.topology;

import org.onlab.graph.Edge;
import org.onlab.onos.net.Link;

/**
 * Represents an edge in the topology graph.
 */
public interface TopologyEdge extends Edge<TopologyVertex> {

    /**
     * Returns the associated infrastructure link.
     *
     * @return backing infrastructure link
     */
    Link link();

}
