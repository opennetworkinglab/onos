package org.onlab.onos.net.topology;

import com.google.common.collect.ImmutableSet;
import org.onlab.onos.net.Description;

/**
 * Describes attribute(s) of a network graph.
 */
public interface GraphDescription extends Description {

    /**
     * Returns the creation timestamp of the graph description. This is
     * expressed in system nanos to allow proper sequencing.
     *
     * @return graph description creation timestamp
     */
    long timestamp();

    /**
     * Returns the set of topology graph vertexes.
     *
     * @return set of graph vertexes
     */
    ImmutableSet<TopologyVertex> vertexes();

    /**
     * Returns the set of topology graph edges.
     *
     * @return set of graph edges
     */
    ImmutableSet<TopologyEdge> edges();

}

