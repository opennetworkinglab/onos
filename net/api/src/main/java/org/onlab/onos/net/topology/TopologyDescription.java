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

}
