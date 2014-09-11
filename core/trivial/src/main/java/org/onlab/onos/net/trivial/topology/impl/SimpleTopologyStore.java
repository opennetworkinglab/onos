package org.onlab.onos.net.trivial.topology.impl;

import org.onlab.onos.event.Event;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.GraphDescription;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyCluster;
import org.onlab.onos.net.topology.TopologyEvent;
import org.onlab.onos.net.topology.TopologyGraph;

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
    Set<TopologyCluster> getClusters(DefaultTopology topology) {
        return topology.getClusters();
    }

    /**
     * Returns the immutable graph view of the current topology.
     *
     * @param topology topology descriptor
     * @return graph view
     */
    TopologyGraph getGraph(DefaultTopology topology) {
        return topology.getGraph();
    }

    /**
     * Returns the set of pre-computed shortest paths between src and dest.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @return set of shortest paths
     */
    Set<Path> getPaths(DefaultTopology topology, DeviceId src, DeviceId dst) {
        return topology.getPaths(src, dst);
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
    Set<Path> getPaths(DefaultTopology topology, DeviceId src, DeviceId dst,
                       LinkWeight weight) {
        return topology.getPaths(src, dst, weight);
    }

    /**
     * Indicates whether the given connect point is part of the network fabric.
     *
     * @param topology     topology descriptor
     * @param connectPoint connection point
     * @return true if infrastructure; false otherwise
     */
    boolean isInfrastructure(DefaultTopology topology, ConnectPoint connectPoint) {
        return topology.isInfrastructure(connectPoint);
    }

    /**
     * Indicates whether the given connect point is part of the broadcast tree.
     *
     * @param topology     topology descriptor
     * @param connectPoint connection point
     * @return true if in broadcast tree; false otherwise
     */
    boolean isInBroadcastTree(DefaultTopology topology, ConnectPoint connectPoint) {
        return topology.isInBroadcastTree(connectPoint);
    }

    /**
     * Generates a new topology snapshot from the specified description.
     *
     * @param providerId       provider identification
     * @param graphDescription topology graph description
     * @param reasons          list of events that triggered the update
     * @return topology update event or null if the description is old
     */
    TopologyEvent updateTopology(ProviderId providerId,
                                 GraphDescription graphDescription,
                                 List<Event> reasons) {
        // First off, make sure that what we're given is indeed newer than
        // what we already have.
        if (current != null && graphDescription.timestamp() < current.time()) {
            return null;
        }

        // Have the default topology construct self from the description data.
        DefaultTopology newTopology =
                new DefaultTopology(providerId, graphDescription);

        // Promote the new topology to current and return a ready-to-send event.
        synchronized (this) {
            current = newTopology;
            return new TopologyEvent(TopologyEvent.Type.TOPOLOGY_CHANGED, current);
        }
    }

}
