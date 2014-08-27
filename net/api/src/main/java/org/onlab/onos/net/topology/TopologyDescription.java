package org.onlab.onos.net.topology;

import org.onlab.onos.net.Description;

import java.util.Collection;

/**
 * Describes attribute(s) of a network topology.
 */
public interface TopologyDescription extends Description {

    /**
     * A collection of Device, Link, and Host descriptors that describe
     * the changes tha have occurred in the network topology.
     *
     * @return network element descriptions describing topology change
     */
    Collection<Description> details();

    // Default topology provider/computor should do the following:
    // create graph
    // search graph for SCC clusters (Tarjan)
    // search graph for all pairs shortest paths based on hop-count
    //      this means all shortest paths, between all pairs; not just one shortest path
    // optionally use path results to produce destination-rooted broadcast trees

    // provide description with the graph, clusters, paths and trees upwards

}

