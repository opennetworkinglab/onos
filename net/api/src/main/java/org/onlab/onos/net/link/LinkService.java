package org.onlab.onos.net.link;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;

import java.util.Set;

/**
 * Service for interacting with the inventory of infrastructure links.
 */
public interface LinkService {

    /**
     * Returns a collection of all known infrastructure links.
     *
     * @return all infrastructure links
     */
    Iterable<Link> getLinks();

    /**
     * Returns set of all infrastructure links leading to and from the
     * specified device.
     *
     * @param deviceId device identifier
     * @return set of device links
     */
    Set<Link> getDeviceLinks(DeviceId deviceId);

    /**
     * Returns set of all infrastructure links leading from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device egress links
     */
    Set<Link> getDeviceEgressLinks(DeviceId deviceId);

    /**
     * Returns set of all infrastructure links leading to the specified device.
     *
     * @param deviceId device identifier
     * @return set of device ingress links
     */
    Set<Link> getDeviceInressLinks(DeviceId deviceId);

    /**
     * Adds the specified link listener.
     *
     * @param listener link listener
     */
    void addListener(LinkListener listener);

    /**
     * Removes the specified link listener.
     *
     * @param listener link listener
     */
    void removeListener(LinkListener listener);

}
