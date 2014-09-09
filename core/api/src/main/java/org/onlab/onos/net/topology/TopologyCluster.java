package org.onlab.onos.net.topology;

import org.onlab.onos.net.DeviceId;

/**
 * Representation of an SCC (strongly-connected component) in a network topology.
 */
public interface TopologyCluster {

    /**
     * Returns the cluster id.
     *
     * @return cluster identifier
     */
    ClusterId id();

    /**
     * Returns the number of devices in the cluster.
     *
     * @return number of cluster devices
     */
    int deviceCount();

    /**
     * Returns the number of infrastructure links in the cluster.
     *
     * @return number of cluster links
     */
    int linkCount();

    /**
     * Returns the device identifier of the cluster root device.
     *
     * @return cluster root device identifier
     */
    DeviceId root();

}
