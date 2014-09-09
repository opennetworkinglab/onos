package org.onlab.onos.net.topology;

import org.onlab.graph.Graph;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Path;

import java.util.Set;

/**
 * Service for providing network topology information.
 */
public interface TopologyService {

    /**
     * Returns the current topology descriptor.
     *
     * @return current topology
     */
    Topology currentTopology();

    /**
     * Returns the set of clusters in the specified topology.
     *
     * @param topology topology descriptor
     * @return set of topology clusters
     */
    Set<TopologyCluster> getClusters(Topology topology);

    /**
     * Returns the graph view of the specified topology.
     *
     * @param topology topology descriptor
     * @return topology graph view
     */
    Graph<TopoVertex, TopoEdge> getGraph(Topology topology);

    /**
     * Returns the set of all shortest paths, precomputed in terms of hop-count,
     * between the specified source and destination devices.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @return set of all shortest paths between the two devices
     */
    Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst);

    /**
     * Returns the set of all shortest paths, computed using the supplied
     * edge-weight entity, between the specified source and destination devices.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @return set of all shortest paths between the two devices
     */
    Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst,
                       LinkWeight weight);

    /**
     * Indicates whether the specified connection point is part of the network
     * infrastructure or part of network edge.
     *
     * @param topology     topology descriptor
     * @param connectPoint connection point
     * @return true of connection point is in infrastructure; false if edge
     */
    boolean isInfrastructure(Topology topology, ConnectPoint connectPoint);


    /**
     * Indicates whether the specified connection point belong to the
     * broadcast tree.
     *
     * @param topology     topology descriptor
     * @param connectPoint connection point
     * @return true if broadcast is permissible
     */
    boolean isInBroadcastTree(Topology topology, ConnectPoint connectPoint);

    /**
     * Adds the specified topology listener.
     *
     * @param listener topology listener
     */
    void addListener(TopologyListener listener);

    /**
     * Removes the specified topology listener.
     *
     * @param listener topology listener
     */
    void removeListener(TopologyListener listener);

}
