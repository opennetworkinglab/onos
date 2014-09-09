package org.onlab.onos.net.topology;

import org.onlab.graph.Graph;
import org.onlab.graph.GraphPathSearch;
import org.onlab.onos.net.Description;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;

import java.util.Set;

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
     * Returns the topology graph.
     *
     * @return network graph
     */
    Graph<TopoVertex, TopoEdge> graph();

    /**
     * Returns the results of the path search through the network graph. This
     * is assumed to contain results of seach fro the given device to all
     * other devices.
     *
     * @param srcDeviceId source device identifier
     * @return path search result for the given source node
     */
    GraphPathSearch.Result pathResults(DeviceId srcDeviceId);

    /**
     * Returns the set of topology SCC clusters.
     *
     * @return set of SCC clusters
     */
    Set<TopologyCluster> clusters();

    /**
     * Returns the set of devices contained by the specified topology cluster.
     *
     * @return set of devices that belong to the specified cluster
     */
    Set<DeviceId> clusterDevices(TopologyCluster cluster);

    /**
     * Returns the set of infrastructure links contained by the specified cluster.
     *
     * @return set of links that form the given cluster
     */
    Set<Link> clusterLinks(TopologyCluster cluster);

    /**
     * Returns the topology SCC cluster which contains the given device.
     *
     * @param deviceId device identifier
     * @return topology cluster that contains the specified device
     */
    TopologyCluster clusterFor(DeviceId deviceId);

}

