package org.onlab.onos.net.trivial.topology.impl;

import org.onlab.graph.Graph;
import org.onlab.onos.event.Event;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.TopoEdge;
import org.onlab.onos.net.topology.TopoVertex;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyCluster;
import org.onlab.onos.net.topology.TopologyDescription;
import org.onlab.onos.net.topology.TopologyEvent;

import java.util.List;
import java.util.Set;

/**
 * Manages inventory of topology snapshots using trivial in-memory
 * structures implementation.
 */
class SimpleTopologyStore {

    private volatile DefaultTopology current;

    /**
     * Returns the current topology snapshot.
     *
     * @return current topology descriptor
     */
    Topology currentTopology() {
        return current;
    }

    /**
     * Indicates whether the topology is the latest.
     *
     * @param topology topology descriptor
     * @return true if topology is the most recent one
     */
    boolean isLatest(Topology topology) {
        // Topology is current only if it is the same as our current topology
        return topology == current;
    }

    /**
     * Returns the set of topology SCC clusters.
     *
     * @param topology topology descriptor
     * @return set of clusters
     */
    Set<TopologyCluster> getClusters(Topology topology) {
        return null;
    }

    /**
     * Returns the immutable graph view of the current topology.
     *
     * @param topology topology descriptor
     * @return graph view
     */
    Graph<TopoVertex, TopoEdge> getGraph(Topology topology) {
        return null;
    }

    /**
     * Returns the set of pre-computed shortest paths between src and dest.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @return set of shortest paths
     */
    Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst) {
        return null;
    }

    /**
     * Computes and returns the set of shortest paths between src and dest.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @param weight   link weight function
     * @return set of shortest paths
     */
    Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst,
                       LinkWeight weight) {
        return null;
    }

    /**
     * Indicates whether the given connect point is part of the network fabric.
     *
     * @param topology     topology descriptor
     * @param connectPoint connection point
     * @return true if infrastructure; false otherwise
     */
    boolean isInfrastructure(Topology topology, ConnectPoint connectPoint) {
        return false;
    }

    /**
     * Indicates whether the given connect point is part of the broadcast tree.
     *
     * @param topology     topology descriptor
     * @param connectPoint connection point
     * @return true if in broadcast tree; false otherwise
     */
    boolean isInBroadcastTree(Topology topology, ConnectPoint connectPoint) {
        return false;
    }

    /**
     * Generates a new topology snapshot from the specified description.
     *
     * @param topoDescription topology description
     * @param reasons         list of events that triggered the update
     * @return topology update event or null if the description is old
     */
    TopologyEvent updateTopology(TopologyDescription topoDescription,
                                 List<Event> reasons) {
        return null;
    }

}
