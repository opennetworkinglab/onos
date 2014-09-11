package org.onlab.onos.net.topology;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import org.onlab.graph.Graph;
import org.onlab.onos.net.Description;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;

import static org.onlab.graph.GraphPathSearch.Result;

/**
 * Describes attribute(s) of a network topology.
 */
public interface TopologyDescription extends Description {

    /**
     * Returns the creation timestamp of the topology description. This is
     * expressed in system nanos to allow proper sequencing.
     *
     * @return topology description creation timestamp
     */
    long timestamp();

    /**
     * Returns the topology graph in immutable form.
     *
     * @return network graph
     */
    Graph<TopoVertex, TopoEdge> graph();

    /**
     * Returns an immutable map of path search results for each source device.
     *
     * @return map of path search result for each source node
     */
    ImmutableMap<DeviceId, Result<TopoVertex, TopoEdge>> pathsBySource();

    /**
     * Returns the set of topology SCC clusters.
     *
     * @return set of SCC clusters
     */
    ImmutableSet<TopologyCluster> clusters();

    /**
     * Returns an immutable set multi-map of devices for each cluster.
     *
     * @return set multi-map of devices that belong to each cluster
     */
    ImmutableSetMultimap<TopologyCluster, DeviceId> devicesByCluster();

    /**
     * Returns an immutable set multi-map of links for each cluster.
     *
     * @return set multi-map of links that belong to each cluster
     */
    ImmutableSetMultimap<TopologyCluster, Link> linksByCluster();

}

