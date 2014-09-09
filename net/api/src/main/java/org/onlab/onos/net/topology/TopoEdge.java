package org.onlab.onos.net.topology;

import org.onlab.graph.Edge;
import org.onlab.onos.net.Link;

/**
 * Represents an edge in the topology graph.
 */
public interface TopoEdge extends Edge<TopoVertex> {

    /**
     * Returns the associated infrastructure link.
     *
     * @return backing infrastructure link
     */
    Link link();

}
