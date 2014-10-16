package org.onlab.onos.sdnip;

import java.util.Set;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.sdnip.config.Interface;
import org.onlab.packet.IpAddress;

/**
 * Provides information about the interfaces in the network.
 */
public interface InterfaceService {
    /**
     * Retrieves the entire set of interfaces in the network.
     *
     * @return the set of interfaces
     */
    Set<Interface> getInterfaces();

    /**
     * Retrieves the interface associated with the given connect point.
     *
     * @param connectPoint the connect point to retrieve interface information
     * for
     * @return the interface
     */
    Interface getInterface(ConnectPoint connectPoint);

    /**
     * Retrieves the interface that matches the given IP address. Matching
     * means that the IP address is in one of the interface's assigned subnets.
     *
     * @param ipAddress IP address to match
     * @return the matching interface
     */
    Interface getMatchingInterface(IpAddress ipAddress);
}
